/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isConst
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.name.StandardClassIds

fun ConeKotlinType.canBeUsedForConstVal(): Boolean = with(lowerBoundIfFlexible()) { isPrimitive || isString || isUnsignedType }

internal fun checkConstantArguments(
    expression: FirExpression,
    session: FirSession,
): ConstantArgumentKind? {
    val expressionSymbol = expression.toReference()?.toResolvedCallableSymbol(discardErrorReference = true)
    val classKindOfParent = (expressionSymbol?.getReferencedClassSymbol(session) as? FirRegularClassSymbol)?.classKind

    when {
        expression is FirNamedArgumentExpression -> {
            checkConstantArguments(expression.expression, session)
        }
        expression is FirTypeOperatorCall -> if (expression.operation == FirOperation.AS) return ConstantArgumentKind.NOT_CONST
        expression is FirWhenExpression -> {
            if (!expression.isProperlyExhaustive) {
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
                if (exp is FirResolvedQualifier) {
                    return ConstantArgumentKind.NOT_CONST
                }
                checkConstantArguments(exp, session).let { return it }
            }
        }
        expression is FirGetClassCall -> {
            var coneType = (expression as? FirCall)?.argument?.typeRef?.coneType

            if (coneType is ConeErrorType)
                return ConstantArgumentKind.NOT_CONST

            while (coneType?.classId == StandardClassIds.Array)
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
            //TODO: fix checking of Java fields initializer
            if (!expressionSymbol.isStatic || expressionSymbol.modality != Modality.FINAL) {
                return ConstantArgumentKind.NOT_CONST
            }
        }
        expressionSymbol is FirConstructorSymbol -> {
            if (expression is FirCallableReferenceAccess) return null
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

            if (calleeReference !is FirResolvedNamedReference) return ConstantArgumentKind.NOT_CONST
            val symbol = calleeReference.resolvedSymbol as? FirNamedFunctionSymbol ?: return ConstantArgumentKind.NOT_CONST

            @OptIn(SymbolInternals::class)
            if (symbol.fir.getAnnotationByClassId(INTRINSIC_CONST_EVALUATION_ANNOTATION, session) == null) {
                return ConstantArgumentKind.NOT_CONST
            }

            for (exp in expression.arguments.plus(expression.dispatchReceiver).plus(expression.extensionReceiver)) {
                if (exp is FirNoReceiverExpression) continue
                val expClassId = exp.typeRef.coneType.lowerBoundIfFlexible().classId
                // TODO: add annotation for allowed constant types
                if (expClassId !in StandardClassIds.constantAllowedTypes) {
                    return ConstantArgumentKind.NOT_CONST
                }

                checkConstantArguments(exp, session)?.let { return it }
            }

            return null
        }
        expression is FirQualifiedAccessExpression -> {
            val expressionType = expression.typeRef.coneType
            if (expressionType.isReflectFunctionType(session) || expressionType.isKProperty(session) || expressionType.isKMutableProperty(session)) {
                return checkConstantArguments(expression.dispatchReceiver, session)
            }

            val propertySymbol = expressionSymbol as? FirPropertySymbol ?: return ConstantArgumentKind.NOT_CONST

            @OptIn(SymbolInternals::class)
            val property = propertySymbol.fir
            when {
                property.unwrapFakeOverrides().getAnnotationByClassId(INTRINSIC_CONST_EVALUATION_ANNOTATION, session) != null -> {
                    return checkConstantArguments(expression.dispatchReceiver, session)
                }
                propertySymbol.isLocal || propertySymbol.callableId.className?.isRoot == false -> return ConstantArgumentKind.NOT_CONST
                expressionType.classId == StandardClassIds.KClass -> return ConstantArgumentKind.NOT_KCLASS_LITERAL

                //TODO: UNRESOLVED REFERENCE
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
