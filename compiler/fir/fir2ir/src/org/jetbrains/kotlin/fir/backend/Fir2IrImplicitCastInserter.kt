/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

class Fir2IrImplicitCastInserter(private val c: Fir2IrComponents) : Fir2IrComponents by c {

    fun handleWhenExpression(irExpression: IrExpression) {
        if (irExpression is IrBlock) {
            irExpression.coerceStatementsToUnit()
            return
        }
        val irWhen = irExpression as IrWhen
        irWhen.branches.forEach {
            (it.result as? IrContainerExpression)?.coerceStatementsToUnit()
        }
    }

    /**
     * Currently, it's a bit vaguely defined how implicit casts differ from conversion (e.g., SAM or suspend ones).
     *
     * But the current assumption is that whenever ones need the former they need the latter, too.
     *
     * And for that case, there's an utility [org.jetbrains.kotlin.fir.backend.utils.prepareExpressionForGivenExpectedType].
     */
    @RequiresOptIn
    annotation class NoConversionsExpected

    /**
     * This functions processes the following casts:
     * - coercion to Unit
     * - nullability casts based on nullability annotations
     * - casts for dynamic types
     *
     * This function doesn't apply conversion operations, for which one might use
     * [org.jetbrains.kotlin.fir.backend.utils.prepareExpressionForGivenExpectedType]
     */
    @NoConversionsExpected
    internal fun IrExpression.insertSpecialCast(
        expression: FirExpression,
        valueType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): IrExpression {
        if (this is IrTypeOperatorCall) {
            return this
        }

        if (this is IrContainerExpression) {
            coerceStatementsToUnit(coerceLastExpressionToUnit = type.isUnit())
        }

        val expandedValueType = valueType.fullyExpandedType(session)
        val expandedExpectedType = expectedType.fullyExpandedType(session)

        return when {
            expandedExpectedType.isUnit -> {
                coerceToUnitIfNeeded(this, builtins)
            }
            expandedValueType is ConeDynamicType -> {
                if (expandedExpectedType !is ConeDynamicType && !expandedExpectedType.isNullableAny) {
                    generateImplicitCast(this, expandedExpectedType.toIrType(ConversionTypeOrigin.DEFAULT))
                } else {
                    this
                }
            }
            // If the value has a flexible or enhanced type, it could contain null (Java nullability isn't checked).
            expandedValueType.isEnhancedOrFlexibleMarkedNullable() && !expandedExpectedType.acceptsNullValues() -> {
                insertImplicitNotNullCastIfNeeded(expression)
            }
            else -> this
        }
    }

    private fun ConeKotlinType.isEnhancedOrFlexibleMarkedNullable(): Boolean {
        return hasEnhancedNullability || hasFlexibleMarkedNullability
    }

    private fun ConeKotlinType.acceptsNullValues(): Boolean {
        // For Captured(in Type) it only accepts nulls if `Type` does
        if (this is ConeCapturedType && this.constructor.projection.kind == ProjectionKind.IN) {
            // But `Captured(in Type)?` does accepts nulls independently of `Type`
            if (isMarkedNullable) return true
            return constructor.projection.type!!.canBeNull(session)
        }
        return canBeNull(session) || hasEnhancedNullability
    }

    private fun IrExpression.insertImplicitNotNullCastIfNeeded(expression: FirExpression): IrExpression {
        if (this is IrGetEnumValue) return this
        // [TypeOperatorLowering] will retrieve the source (from start offset to end offset) as an assertion message.
        // Avoid type casting if we can't determine the source for some reasons, e.g., implicit `this` receiver.
        if (expression.source == null) return this
        return implicitNotNullCast(this)
    }

    fun IrStatementContainer.coerceStatementsToUnit(coerceLastExpressionToUnit: Boolean = false): IrStatementContainer {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex || coerceLastExpressionToUnit) {
                    statements[i] = coerceToUnitIfNeeded(irStatement, builtins)
                }
            }
        }

        return this
    }

    fun handleSmartCastExpression(smartCastExpression: FirSmartCastExpression, expression: IrExpression): IrExpression {
        // We don't want an implicit cast to Nothing?. This expression just encompasses nullability after null check.
        return if (smartCastExpression.isStable && smartCastExpression.smartcastTypeWithoutNullableNothing == null) {
            val smartcastedType = smartCastExpression.resolvedType
            val approximatedType = smartcastedType.approximateForIrOrNull(c)
            if (approximatedType != null) {
                val originalType = smartCastExpression.originalExpression.resolvedType
                val originalNotNullType = originalType.withNullability(nullable = false, session.typeContext)
                if (originalNotNullType.isSubtypeOf(approximatedType, session)) {
                    return expression
                }
            }
            implicitCastOrExpression(expression, approximatedType ?: smartcastedType)
        } else {
            expression
        }
    }

    internal fun IrExpression.insertCastForSmartcastWithIntersection(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType
    ): IrExpression {
        if (argumentType !is ConeIntersectionType) return this
        val approximatedArgumentType = argumentType.approximateForIrOrNull(c) ?: argumentType
        if (approximatedArgumentType.isSubtypeOf(expectedType, session)) return this

        return findComponentOfIntersectionForExpectedType(argumentType, expectedType)?.let {
            generateImplicitCast(this, it.toIrType())
        } ?: this
    }

    internal fun implicitCastFromReceivers(
        originalIrReceiver: IrExpression,
        receiver: FirExpression,
        selector: FirQualifiedAccessExpression,
        typeOrigin: ConversionTypeOrigin,
    ): IrExpression {
        return implicitCastFromReceiverForIntersectionTypeOrNull(
            originalIrReceiver,
            receiver,
            selector,
            typeOrigin
        ) ?: implicitCastOrExpression(originalIrReceiver, receiver.resolvedType, typeOrigin)
    }

    private fun implicitCastFromReceiverForIntersectionTypeOrNull(
        originalIrReceiver: IrExpression,
        receiver: FirExpression,
        selector: FirQualifiedAccessExpression,
        typeOrigin: ConversionTypeOrigin,
    ): IrExpression? {
        val receiverExpressionType = receiver.resolvedType.lowerBoundIfFlexible() as? ConeIntersectionType ?: return null
        val referencedDeclaration = selector.calleeReference.toResolvedCallableSymbol()?.unwrapCallRepresentative(c)?.fir

        val receiverType = with(selector) {
            when {
                receiver === dispatchReceiver -> {
                    val dispatchReceiverType = referencedDeclaration?.dispatchReceiverType as? ConeClassLikeType ?: return null
                    dispatchReceiverType.replaceArgumentsWithStarProjections()
                }
                receiver === extensionReceiver -> {
                    val extensionReceiverType = referencedDeclaration?.receiverParameter?.typeRef?.coneType ?: return null
                    val substitutor = selector.buildSubstitutorByCalledCallable(c)
                    val substitutedType = substitutor.substituteOrSelf(extensionReceiverType)
                    // Frontend may write captured types as type arguments (by design), so we need to approximate receiver type after substitution
                    val approximatedType = session.typeApproximator.approximateToSuperType(
                        substitutedType,
                        TypeApproximatorConfiguration.InternalTypesApproximation
                    )
                    approximatedType ?: substitutedType
                }
                else -> return null
            }
        }

        return findComponentOfIntersectionForExpectedType(receiverExpressionType, receiverType)?.let {
            implicitCastOrExpression(originalIrReceiver, it, typeOrigin)
        }
    }

    private fun findComponentOfIntersectionForExpectedType(type: ConeIntersectionType, expectedType: ConeKotlinType): ConeKotlinType? {
        for (componentType in type.intersectedTypes) {
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, componentType, expectedType)) {
                return componentType
            }
        }
        return null
    }

    private fun implicitCastOrExpression(
        original: IrExpression, castType: ConeKotlinType, typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType(typeOrigin))
    }

    companion object {
        internal fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
            if (original.type == castType) return original
            return generateImplicitCast(original, castType)
        }

        private fun generateImplicitCast(original: IrExpression, castType: IrType): IrExpression {
            val typeOperator = if (original.type is IrDynamicType) {
                IrTypeOperator.IMPLICIT_DYNAMIC_CAST
            } else {
                IrTypeOperator.IMPLICIT_CAST
            }

            return implicitCast(original, castType, typeOperator)
        }

        internal fun coerceToUnitIfNeeded(original: IrExpression, builtins: Fir2IrBuiltinSymbolsContainer): IrExpression {
            val valueType = original.type
            return if (valueType.isUnit() || valueType.isNothing())
                original
            else
                IrTypeOperatorCallImpl(
                    original.startOffset, original.endOffset,
                    builtins.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    builtins.unitType,
                    original
                )
        }

        fun implicitNotNullCast(original: IrExpression): IrTypeOperatorCall {
            // Cast type massage 1. Remove @EnhancedNullability
            // Cast type massage 2. Convert it to a non-null variant (in case of @FlexibleNullability)
            val castType = original.type.removeAnnotations { annotationCall ->
                val constructorSymbol = annotationCall.symbol.takeIf { it.isBound } ?: return@removeAnnotations false
                /*
                 * @EnhancedNullability and @FlexibleNullability are symbols from builtins and should be already
                 *   bound at the time of body conversion, so it's safe to take the owner for them
                 * If symbol is unbound then this annotation can not be neither @EnhancedNullability or @FlexibleNullability
                 */
                @OptIn(UnsafeDuringIrConstructionAPI::class)
                val classId = constructorSymbol.owner.parentAsClass.classId
                classId == StandardClassIds.Annotations.EnhancedNullability ||
                        classId == StandardClassIds.Annotations.FlexibleNullability
            }.makeNotNull()
            return IrTypeOperatorCallImpl(
                original.startOffset,
                original.endOffset,
                castType,
                IrTypeOperator.IMPLICIT_NOTNULL,
                castType,
                original
            )
        }
    }
}
