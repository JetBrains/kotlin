/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

fun canBeEvaluatedAtCompileTime(expression: FirExpression?, session: FirSession): Boolean {
    return checkConstantArguments(expression, session) == ConstantArgumentKind.VALID_CONST
}

internal fun checkConstantArguments(
    expression: FirExpression?,
    session: FirSession,
): ConstantArgumentKind {
    if (expression == null) return ConstantArgumentKind.VALID_CONST

    val firConstCheckVisitor = FirConstCheckVisitor()

    val expressionSymbol = expression.toReference()?.toResolvedCallableSymbol(discardErrorReference = true)
    val classKindOfParent = with(firConstCheckVisitor) {
        (expressionSymbol?.getReferencedClassSymbol(session) as? FirRegularClassSymbol)?.classKind
    }
    val intrinsicConstEvaluation = session.languageVersionSettings.supportsFeature(LanguageFeature.IntrinsicConstEvaluation)

    fun FirBasedSymbol<*>.canBeEvaluated(): Boolean {
        return intrinsicConstEvaluation && this.hasAnnotation(StandardClassIds.Annotations.IntrinsicConstEvaluation, session)
    }

    fun FirExpression.getExpandedType() = resolvedType.fullyExpandedType(session)

    when {
        expression is FirNamedArgumentExpression -> {
            return checkConstantArguments(expression.expression, session)
        }
        expression is FirTypeOperatorCall -> if (expression.operation == FirOperation.AS) return ConstantArgumentKind.NOT_CONST
        expression is FirWhenExpression -> {
            if (!expression.isProperlyExhaustive || !intrinsicConstEvaluation) {
                return ConstantArgumentKind.NOT_CONST
            }

            expression.subject?.let { subject -> checkConstantArguments(subject, session).ifNotValidConst { return it } }
            for (branch in expression.branches) {
                checkConstantArguments(branch.condition, session).ifNotValidConst { return it }
                branch.result.statements.forEach { stmt ->
                    if (stmt !is FirExpression) return ConstantArgumentKind.NOT_CONST
                    checkConstantArguments(stmt, session).ifNotValidConst { return it }
                }
            }
            return ConstantArgumentKind.VALID_CONST
        }
        expression is FirConstExpression<*>
                || expressionSymbol is FirEnumEntrySymbol
                || expressionSymbol?.isConst == true
                || expressionSymbol is FirConstructorSymbol && classKindOfParent == ClassKind.ANNOTATION_CLASS -> {
            return ConstantArgumentKind.VALID_CONST
        }
        classKindOfParent == ClassKind.ENUM_CLASS -> {
            return ConstantArgumentKind.ENUM_NOT_CONST
        }
        expression is FirComparisonExpression -> {
            return checkConstantArguments(expression.compareToCall, session)
        }
        expression is FirStringConcatenationCall -> {
            for (exp in expression.arguments) {
                if (exp is FirResolvedQualifier || exp is FirGetClassCall) {
                    return ConstantArgumentKind.NOT_CONST
                }
                checkConstantArguments(exp, session).ifNotValidConst { return it }
            }
        }
        expression is FirEqualityOperatorCall -> {
            if (expression.operation == FirOperation.IDENTITY || expression.operation == FirOperation.NOT_IDENTITY) {
                return ConstantArgumentKind.NOT_CONST
            }

            for (exp in expression.arguments) {
                if (exp is FirConstExpression<*> && exp.value == null) {
                    return ConstantArgumentKind.NOT_CONST
                }

                if (exp is FirResolvedQualifier || exp is FirGetClassCall || exp.getExpandedType().isUnsignedType) {
                    return ConstantArgumentKind.NOT_CONST
                }
                checkConstantArguments(exp, session).ifNotValidConst { return it }
            }
        }
        expression is FirBinaryLogicExpression -> {
            checkConstantArguments(expression.leftOperand, session).ifNotValidConst { return it }
            checkConstantArguments(expression.rightOperand, session).ifNotValidConst { return it }
        }
        expression is FirGetClassCall -> {
            var coneType = expression.argument.getExpandedType()

            if (coneType is ConeErrorType)
                return ConstantArgumentKind.NOT_CONST

            while (coneType.classId == StandardClassIds.Array)
                coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

            return when {
                coneType is ConeTypeParameterType -> ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                expression.argument !is FirResolvedQualifier -> ConstantArgumentKind.NOT_KCLASS_LITERAL
                else -> ConstantArgumentKind.VALID_CONST
            }
        }
        expression is FirArrayLiteral -> {
            for (exp in expression.arguments) {
                checkConstantArguments(exp, session).ifNotValidConst { return it }
            }
        }
        expression is FirThisReceiverExpression -> {
            return ConstantArgumentKind.NOT_CONST
        }
        expressionSymbol == null -> {
            return ConstantArgumentKind.VALID_CONST
        }
        expressionSymbol is FirFieldSymbol -> {
            if (!expressionSymbol.isStatic || expressionSymbol.modality != Modality.FINAL || !expressionSymbol.hasConstantInitializer) {
                return ConstantArgumentKind.NOT_CONST
            }
        }
        expressionSymbol is FirConstructorSymbol -> {
            if (expression is FirCallableReferenceAccess) return ConstantArgumentKind.VALID_CONST
            if (expression.getExpandedType().isUnsignedType) {
                (expression as FirFunctionCall).arguments.forEach { argumentExpression ->
                    checkConstantArguments(argumentExpression, session).ifNotValidConst { return it }
                }
            } else {
                return ConstantArgumentKind.NOT_CONST
            }
        }
        expression is FirFunctionCall -> {
            val calleeReference = expression.calleeReference
            if (calleeReference is FirErrorNamedReference) {
                return ConstantArgumentKind.VALID_CONST
            }
            if (expression.getExpandedType().classId == StandardClassIds.KClass) {
                return ConstantArgumentKind.NOT_KCLASS_LITERAL
            }

            if (calleeReference !is FirResolvedNamedReference) return ConstantArgumentKind.NOT_CONST
            val symbol = calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: return ConstantArgumentKind.NOT_CONST

            if (!symbol.canBeEvaluated() && !with(firConstCheckVisitor) { expression.isCompileTimeBuiltinCall(session) }) {
                return ConstantArgumentKind.NOT_CONST
            }

            for (exp in expression.arguments.plus(expression.dispatchReceiver).plus(expression.extensionReceiver)) {
                if (exp == null) continue
                val expClassId = exp.getExpandedType().lowerBoundIfFlexible().fullyExpandedClassId(session)
                // TODO, KT-59823: add annotation for allowed constant types
                if (expClassId !in StandardClassIds.constantAllowedTypes) {
                    return ConstantArgumentKind.NOT_CONST
                }

                checkConstantArguments(exp, session).ifNotValidConst { return it }
            }

            return ConstantArgumentKind.VALID_CONST
        }
        expression is FirQualifiedAccessExpression -> {
            val expressionType = expression.getExpandedType()
            if (expressionType.isReflectFunctionType(session) || expressionType.isKProperty(session) || expressionType.isKMutableProperty(
                    session
                )
            ) {
                return checkConstantArguments(expression.dispatchReceiver, session)
            }

            val propertySymbol = expressionSymbol as? FirPropertySymbol ?: return ConstantArgumentKind.NOT_CONST

            when {
                propertySymbol.unwrapFakeOverrides().canBeEvaluated() || with(firConstCheckVisitor) { propertySymbol.isCompileTimeBuiltinProperty(session) } -> {
                    val receiver = listOf(expression.dispatchReceiver, expression.extensionReceiver).single { it != null }!!
                    return checkConstantArguments(receiver, session)
                }
                propertySymbol.isLocal -> return ConstantArgumentKind.NOT_CONST
                expressionType.classId == StandardClassIds.KClass -> return ConstantArgumentKind.NOT_KCLASS_LITERAL
            }
            // Ok, because we only look at the structure, not resolution-dependent properties.
            @OptIn(SymbolInternals::class)
            return when (propertySymbol.fir.initializer) {
                is FirConstExpression<*> -> when {
                    propertySymbol.isVal -> ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION
                    else -> ConstantArgumentKind.NOT_CONST
                }
                is FirGetClassCall -> ConstantArgumentKind.NOT_KCLASS_LITERAL
                else -> ConstantArgumentKind.NOT_CONST
            }
        }
        else -> return ConstantArgumentKind.NOT_CONST
    }
    return ConstantArgumentKind.VALID_CONST
}

internal enum class ConstantArgumentKind {
    VALID_CONST,
    NOT_CONST,
    ENUM_NOT_CONST,
    NOT_KCLASS_LITERAL,
    NOT_CONST_VAL_IN_CONST_EXPRESSION,
    KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR;

    inline fun ifNotValidConst(action: (ConstantArgumentKind) -> Unit) {
        if (this != VALID_CONST) {
            action(this)
        }
    }
}

private class FirConstCheckVisitor() : FirVisitor<ConstantArgumentKind, Nothing?>() {
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

    override fun visitElement(element: FirElement, data: Nothing?): ConstantArgumentKind {
        return ConstantArgumentKind.NOT_CONST
    }

    fun FirFunctionCall.isCompileTimeBuiltinCall(session: FirSession): Boolean {
        val calleeReference = this.calleeReference
        if (calleeReference !is FirResolvedNamedReference) return false

        val name = calleeReference.name
        val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
        if (!symbol.fromKotlin()) return false

        val receiverClassId = this.dispatchReceiver?.resolvedType?.fullyExpandedClassId(session)

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

    fun FirPropertySymbol.isCompileTimeBuiltinProperty(session: FirSession): Boolean {
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

    fun FirCallableSymbol<*>?.getReferencedClassSymbol(session: FirSession): FirBasedSymbol<*>? =
        this?.resolvedReturnTypeRef
            ?.coneTypeSafe<ConeLookupTagBasedType>()
            ?.lookupTag
            ?.toSymbol(session)
}