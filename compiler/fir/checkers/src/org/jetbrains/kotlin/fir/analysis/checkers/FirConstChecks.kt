/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

internal fun checkConstantArguments(
    expression: FirExpression?,
    session: FirSession,
): ConstantArgumentKind? {
    if (expression == null) return null
    val expressionSymbol = expression.toReference()?.toResolvedCallableSymbol(discardErrorReference = true)
    val classKindOfParent = (expressionSymbol?.getReferencedClassSymbol(session) as? FirRegularClassSymbol)?.classKind
    val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

    fun FirBasedSymbol<*>.canBeEvaluated(): Boolean {
        return intrinsicConstEvaluation && this.hasAnnotation(StandardClassIds.Annotations.IntrinsicConstEvaluation, session)
    }

    when {
        expression is FirNamedArgumentExpression -> {
            checkConstantArguments(expression.expression, session)
        }
        expression is FirTypeOperatorCall -> if (expression.operation == FirOperation.AS) return ConstantArgumentKind.NOT_CONST
        expression is FirWhenExpression -> {
            if (!expression.isProperlyExhaustive || !intrinsicConstEvaluation) {
                return ConstantArgumentKind.NOT_CONST
            }

            expression.subject?.let { subject -> checkConstantArguments(subject, session)?.let { return it } }
            for (branch in expression.branches) {
                checkConstantArguments(branch.condition, session)?.let { return it }
                branch.result.statements.forEach { stmt ->
                    if (stmt !is FirExpression) return ConstantArgumentKind.NOT_CONST
                    checkConstantArguments(stmt, session)?.let { return it }
                }
            }
            return null
        }
        expression is FirConstExpression<*>
                || expressionSymbol is FirEnumEntrySymbol
                || expressionSymbol?.isConst == true
                || expressionSymbol is FirConstructorSymbol && classKindOfParent == ClassKind.ANNOTATION_CLASS -> {
            //DO NOTHING
        }
        classKindOfParent == ClassKind.ENUM_CLASS -> {
            return ConstantArgumentKind.ENUM_NOT_CONST
        }
        expression is FirComparisonExpression -> {
            return checkConstantArguments(expression.compareToCall, session)
        }
        expression is FirStringConcatenationCall || expression is FirEqualityOperatorCall -> {
            for (exp in (expression as FirCall).arguments) {
                if (exp is FirResolvedQualifier || expression.isForbiddenComplexConstant(session)) {
                    return ConstantArgumentKind.NOT_CONST
                }
                checkConstantArguments(exp, session)?.let { return it }
            }
        }
        expression is FirGetClassCall -> {
            var coneType = (expression as? FirCall)?.argument?.resolvedType

            if (coneType is ConeErrorType)
                return ConstantArgumentKind.NOT_CONST

            while (coneType?.fullyExpandedClassId(session) == StandardClassIds.Array)
                coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

            return when {
                coneType is ConeTypeParameterType -> ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                (expression as FirCall).argument !is FirResolvedQualifier -> ConstantArgumentKind.NOT_KCLASS_LITERAL
                else -> null
            }
        }
        expressionSymbol == null -> {
            //DO NOTHING
        }
        expressionSymbol is FirFieldSymbol -> {
            if (!expressionSymbol.isStatic || expressionSymbol.modality != Modality.FINAL) {
                return ConstantArgumentKind.NOT_CONST
            }
        }
        expressionSymbol is FirConstructorSymbol -> {
            if (expression is FirCallableReferenceAccess) return null
            if (expression.resolvedType.isUnsignedType) {
                (expression as FirFunctionCall).arguments.forEach { argumentExpression ->
                    checkConstantArguments(argumentExpression, session)?.let { return it }
                }
            } else {
                return ConstantArgumentKind.NOT_CONST
            }
        }
        expression is FirFunctionCall -> {
            val calleeReference = expression.calleeReference
            if (calleeReference is FirErrorNamedReference) {
                return null
            }
            if (expression.resolvedType.fullyExpandedClassId(session) == StandardClassIds.KClass) {
                return ConstantArgumentKind.NOT_KCLASS_LITERAL
            }

            //TODO, KT-59822: UNRESOLVED REFERENCE
            if (expression.dispatchReceiver is FirThisReceiverExpression) {
                return null
            }

            if (calleeReference !is FirResolvedNamedReference) return ConstantArgumentKind.NOT_CONST
            val symbol = calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: return ConstantArgumentKind.NOT_CONST

            if (!symbol.canBeEvaluated() && !expression.isCompileTimeBuiltinCall(session) || expression.isForbiddenComplexConstant(session)) {
                return ConstantArgumentKind.NOT_CONST
            }

            for (exp in expression.arguments.plus(expression.dispatchReceiver).plus(expression.extensionReceiver)) {
                if (exp == null) continue
                val expClassId = exp.resolvedType.fullyExpandedClassId(session)
                // TODO, KT-59823: add annotation for allowed constant types
                if (expClassId !in StandardClassIds.constantAllowedTypes) {
                    return ConstantArgumentKind.NOT_CONST
                }

                checkConstantArguments(exp, session)?.let { return it }
            }

            return null
        }
        expression is FirQualifiedAccessExpression -> {
            val expressionType = expression.resolvedType
            if (expressionType.isReflectFunctionType(session) || expressionType.isKProperty(session) || expressionType.isKMutableProperty(
                    session
                )
            ) {
                return checkConstantArguments(expression.dispatchReceiver, session)
            }

            val propertySymbol = expressionSymbol as? FirPropertySymbol ?: return ConstantArgumentKind.NOT_CONST

            @OptIn(SymbolInternals::class)
            val property = propertySymbol.fir
            when {
                property.unwrapFakeOverrides().symbol.canBeEvaluated() || property.isCompileTimeBuiltinProperty(session) -> {
                    val receiver = listOf(expression.dispatchReceiver, expression.extensionReceiver).single { it != null }!!
                    return checkConstantArguments(receiver, session)
                }
                propertySymbol.isLocal || propertySymbol.callableId.className?.isRoot == false -> return ConstantArgumentKind.NOT_CONST
                expressionType.fullyExpandedClassId(session) == StandardClassIds.KClass -> return ConstantArgumentKind.NOT_KCLASS_LITERAL

                //TODO, KT-59822: UNRESOLVED REFERENCE
                expression.dispatchReceiver is FirThisReceiverExpression -> return null
            }
            return when (property.initializer) {
                is FirConstExpression<*> -> when {
                    property.isVal -> ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION
                    else -> ConstantArgumentKind.NOT_CONST
                }
                is FirGetClassCall -> ConstantArgumentKind.NOT_KCLASS_LITERAL
                else -> ConstantArgumentKind.NOT_CONST
            }
        }
        else -> return ConstantArgumentKind.NOT_CONST
    }
    return null
}

private fun FirExpression.isForbiddenComplexConstant(session: FirSession): Boolean {
    val forbidComplexBooleanExpressions = session.languageVersionSettings.supportsFeature(
        LanguageFeature.ProhibitSimplificationOfNonTrivialConstBooleanExpressions
    )
    val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(
        LanguageFeature.IntrinsicConstEvaluation
    )
    return !intrinsicConstEvaluation && forbidComplexBooleanExpressions && isComplexBooleanConstant
}

private val FirExpression.isComplexBooleanConstant
    get(): Boolean = when {
        !resolvedType.isBoolean -> false
        this is FirConstExpression<*> -> false
        usesVariableAsConstant -> false
        else -> true
    }

/**
 * See: org.jetbranis.kotlin.resolve.constants.CompileTimeConstant.Parameters.usesVariableAsConstant
 */
@Suppress("RecursivePropertyAccessor")
private val FirExpression.usesVariableAsConstant: Boolean
    get() = this is FirPropertyAccessExpression && toResolvedCallableSymbol()?.isConst == true
            || this is FirQualifiedAccessExpression && explicitReceiver?.usesVariableAsConstant != false
            || this is FirCall && this.arguments.any { it.usesVariableAsConstant }

private val compileTimeFunctions = setOf(
    *OperatorNameConventions.BINARY_OPERATION_NAMES.toTypedArray(), *OperatorNameConventions.UNARY_OPERATION_NAMES.toTypedArray(),
    OperatorNameConventions.SHL, OperatorNameConventions.SHR, OperatorNameConventions.USHR,
    OperatorNameConventions.OR, OperatorNameConventions.AND, OperatorNameConventions.XOR,
    OperatorNameConventions.COMPARE_TO
)

private val compileTimeExtensionFunctions = listOf("floorDiv", "mod", "code").mapTo(hashSetOf()) { Name.identifier(it) }

private val compileTimeConversionFunctions = listOf(
    "toInt", "toLong", "toShort", "toByte", "toFloat", "toDouble", "toChar", "toBoolean"
).mapTo(hashSetOf()) { Name.identifier(it) }

private fun FirFunctionCall.isCompileTimeBuiltinCall(session: FirSession): Boolean {
    val calleeReference = this.calleeReference
    if (calleeReference !is FirResolvedNamedReference) return false

    val name = calleeReference.name
    val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
    if (!symbol.fromKotlin()) return false

    val coneType = this.dispatchReceiver?.resolvedType
    val receiverClassId = coneType?.fullyExpandedClassId(session)

    if (receiverClassId in StandardClassIds.unsignedTypes) return false

    if (
        name in compileTimeFunctions ||
        name in compileTimeExtensionFunctions ||
        name == OperatorNameConventions.TO_STRING ||
        name in compileTimeConversionFunctions
    ) return true

    if (calleeReference.name == OperatorNameConventions.GET && receiverClassId == StandardClassIds.String) return true

    return false
}

private fun FirProperty.isCompileTimeBuiltinProperty(session: FirSession): Boolean {
    val receiverType = dispatchReceiverType ?: receiverParameter?.typeRef?.coneTypeSafe<ConeKotlinType>() ?: return false
    val receiverClassId = receiverType.fullyExpandedClassId(session) ?: return false
    return when (name.asString()) {
        "length" -> receiverClassId == StandardClassIds.String
        "code" -> receiverClassId == StandardClassIds.Char
        else -> false
    }
}

private fun FirCallableSymbol<*>?.fromKotlin(): Boolean {
    return this?.callableId?.packageName?.asString() == "kotlin"
}

private fun FirCallableSymbol<*>?.getReferencedClassSymbol(session: FirSession): FirBasedSymbol<*>? =
    this?.resolvedReturnTypeRef
        ?.coneTypeSafe<ConeLookupTagBasedType>()
        ?.lookupTag
        ?.toSymbol(session)

internal enum class ConstantArgumentKind {
    NOT_CONST,
    ENUM_NOT_CONST,
    NOT_KCLASS_LITERAL,
    NOT_CONST_VAL_IN_CONST_EXPRESSION,
    KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
}
