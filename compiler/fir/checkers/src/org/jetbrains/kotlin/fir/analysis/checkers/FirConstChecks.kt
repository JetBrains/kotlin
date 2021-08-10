/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirIntegerOperatorCall
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

internal fun checkConstantArguments(
    expression: FirExpression,
    session: FirSession,
): ConstantArgumentKind? {
    val expressionSymbol = expression.toResolvedCallableSymbol()
    val classKindOfParent = (expressionSymbol
        ?.getReferencedClassSymbol(session) as? FirRegularClassSymbol)
        ?.classKind

    when {
        expression is FirTypeOperatorCall -> {
            if (expression.operation == FirOperation.AS) return ConstantArgumentKind.NOT_CONST
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
        expression is FirIntegerOperatorCall -> {
            for (exp in (expression as FirCall).arguments.plus(expression.dispatchReceiver))
                checkConstantArguments(exp, session).let { return it }
        }
        expression is FirStringConcatenationCall || expression is FirEqualityOperatorCall -> {
            for (exp in (expression as FirCall).arguments)
                checkConstantArguments(exp, session).let { return it }
        }
        (expression is FirGetClassCall) -> {
            var coneType = (expression as? FirCall)
                ?.argument
                ?.typeRef
                ?.coneType

            if (coneType is ConeClassErrorType)
                return ConstantArgumentKind.NOT_CONST

            while (coneType?.classId == StandardClassIds.Array)
                coneType = (coneType.lowerBoundIfFlexible().typeArguments.first() as? ConeKotlinTypeProjection)?.type ?: break

            return when {
                coneType is ConeTypeParameterType ->
                    ConstantArgumentKind.KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
                (expression as FirCall).argument !is FirResolvedQualifier ->
                    ConstantArgumentKind.NOT_KCLASS_LITERAL
                else ->
                    null
            }
        }
        expressionSymbol == null -> {
            //DO NOTHING
        }
        expressionSymbol is FirFieldSymbol -> {
            //TODO: fix checking of Java fields initializer
            if (
                !expressionSymbol.isStatic
                || expressionSymbol.modality != Modality.FINAL
            )
                return ConstantArgumentKind.NOT_CONST
        }
        expressionSymbol is FirConstructorSymbol -> {
            if (expression.typeRef.coneType.isUnsignedType) {
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
            if (expression.typeRef.coneType.classId == StandardClassIds.KClass) {
                return ConstantArgumentKind.NOT_KCLASS_LITERAL
            }

            //TODO: UNRESOLVED REFERENCE
            if (expression.dispatchReceiver is FirThisReceiverExpression) {
                return null
            }


            when (calleeReference.name) {
                in OperatorNameConventions.BINARY_OPERATION_NAMES, in OperatorNameConventions.UNARY_OPERATION_NAMES,
                OperatorNameConventions.SHL, OperatorNameConventions.SHR, OperatorNameConventions.USHR,
                OperatorNameConventions.OR, OperatorNameConventions.AND -> {
                    val coneType =
                        expression.dispatchReceiver.typeRef.coneTypeSafe<ConeKotlinType>() ?: return ConstantArgumentKind.NOT_CONST
                    val receiverClassId = coneType.lowerBoundIfFlexible().classId


                    if ((calleeReference.name == OperatorNameConventions.DIV || calleeReference.name == OperatorNameConventions.REM)
                        && expression.typeRef.coneType.classId == StandardClassIds.Int
                    ) {
                        val value = expression.arguments.first() as? FirConstExpression<*>
                        if (value?.value == 0L) {
                            return ConstantArgumentKind.NOT_CONST
                        }
                    }

                    for (exp in (expression as FirCall).arguments.plus(expression.dispatchReceiver)) {
                        val expClassId = exp.typeRef.coneType.lowerBoundIfFlexible().classId

                        if (calleeReference.name == OperatorNameConventions.PLUS
                            && expClassId != receiverClassId
                            && (expClassId !in StandardClassIds.constantAllowedTypes || receiverClassId !in StandardClassIds.constantAllowedTypes)
                        ) {
                            return ConstantArgumentKind.NOT_CONST
                        }
                        checkConstantArguments(exp, session)?.let { return it }
                    }
                }
                else -> {
                    if (expression.arguments.isNotEmpty() || calleeReference !is FirResolvedNamedReference) {
                        return ConstantArgumentKind.NOT_CONST
                    }
                    val symbol = calleeReference.resolvedSymbol as? FirCallableSymbol
                    if (calleeReference.name == OperatorNameConventions.TO_STRING ||
                        calleeReference.name in CONVERSION_NAMES && symbol?.callableId?.packageName?.asString() == "kotlin"
                    ) {
                        return checkConstantArguments(expression.dispatchReceiver, session)
                    }
                    return ConstantArgumentKind.NOT_CONST
                }
            }
        }
        expression is FirQualifiedAccessExpression -> {
            val propertySymbol = expressionSymbol as? FirPropertySymbol ?: return ConstantArgumentKind.NOT_CONST

            @OptIn(SymbolInternals::class)
            val property = propertySymbol.fir
            when {
                property.name.asString() == "length" -> {
                    val coneType =
                        expression.dispatchReceiver.typeRef.coneTypeSafe<ConeKotlinType>() ?: return ConstantArgumentKind.NOT_CONST
                    val receiverClassId = coneType.lowerBoundIfFlexible().classId
                    if (receiverClassId == StandardClassIds.String) {
                        return checkConstantArguments(expression.dispatchReceiver, session)
                    }
                }
                propertySymbol.isLocal || propertySymbol.callableId.className?.isRoot == false ->
                    return ConstantArgumentKind.NOT_CONST
                expression.typeRef.coneType.classId == StandardClassIds.KClass ->
                    return ConstantArgumentKind.NOT_KCLASS_LITERAL

                //TODO: UNRESOLVED REFERENCE
                expression.dispatchReceiver is FirThisReceiverExpression ->
                    return null
            }
            return when (property.initializer) {
                is FirConstExpression<*> -> {
                    if (property.isVal)
                        ConstantArgumentKind.NOT_CONST_VAL_IN_CONST_EXPRESSION
                    else
                        ConstantArgumentKind.NOT_CONST
                }
                is FirGetClassCall ->
                    ConstantArgumentKind.NOT_KCLASS_LITERAL
                else ->
                    ConstantArgumentKind.NOT_CONST
            }
        }
        else ->
            return ConstantArgumentKind.NOT_CONST
    }
    return null
}

private fun FirCallableSymbol<*>?.getReferencedClassSymbol(session: FirSession): FirBasedSymbol<*>? =
    this?.resolvedReturnTypeRef
        ?.coneTypeSafe<ConeLookupTagBasedType>()
        ?.lookupTag
        ?.toSymbol(session)

private val CONVERSION_NAMES = listOf(
    "toInt", "toLong", "toShort", "toByte", "toFloat", "toDouble", "toChar", "toBoolean"
).mapTo(hashSetOf()) { Name.identifier(it) }

internal enum class ConstantArgumentKind {
    NOT_CONST,
    ENUM_NOT_CONST,
    NOT_KCLASS_LITERAL,
    NOT_CONST_VAL_IN_CONST_EXPRESSION,
    KCLASS_LITERAL_OF_TYPE_PARAMETER_ERROR
}
