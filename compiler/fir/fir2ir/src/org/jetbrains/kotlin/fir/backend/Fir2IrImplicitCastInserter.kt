/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.backend.utils.ConversionTypeOrigin
import org.jetbrains.kotlin.fir.backend.utils.implicitCast
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker

class Fir2IrImplicitCastInserter(c: Fir2IrComponents) : Fir2IrComponents by c {

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

        val expandedValueType = valueType.fullyExpandedType()
        val expandedExpectedType = expectedType.fullyExpandedType()

        return when {
            expandedExpectedType.isUnit -> {
                coerceToUnitIfNeeded(this)
            }
            expandedValueType is ConeDynamicType -> {
                if (expandedExpectedType !is ConeDynamicType && !expandedExpectedType.isNullableAny) {
                    generateImplicitCast(this, expandedExpectedType.toIrType(ConversionTypeOrigin.DEFAULT))
                } else {
                    this
                }
            }
            // If the value has a flexible or enhanced type, it could contain null (Java nullability isn't checked).
            expandedValueType.isEnhancedOrFlexibleMarkedNullable() && !expandedExpectedType.acceptsNullValues() &&
                    // [TypeOperatorLowering] will retrieve the source (from start offset to end offset) as an assertion message.
                    // Avoid type casting if we can't determine the source for some reasons, e.g., implicit `this` receiver.
                    expression.source != null && this !is IrGetEnumValue -> {
                implicitNotNullCast(this)
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

    fun IrStatementContainer.coerceStatementsToUnit(coerceLastExpressionToUnit: Boolean): IrStatementContainer {
        if (statements.isEmpty()) return this

        val lastIndex = statements.lastIndex
        statements.forEachIndexed { i, irStatement ->
            if (irStatement !is IrErrorCallExpression && irStatement is IrExpression) {
                if (i != lastIndex || coerceLastExpressionToUnit) {
                    statements[i] = coerceToUnitIfNeeded(irStatement)
                }
            }
        }

        return this
    }

    fun handleSmartCastExpression(smartCastExpression: FirSmartCastExpression, expression: IrExpression): IrExpression {
        // We don't want an implicit cast to Nothing?. This expression just encompasses nullability after null check.
        return if (smartCastExpression.isStable && smartCastExpression.smartcastTypeWithoutNullableNothing == null) {
            val smartcastedType = smartCastExpression.resolvedType
            val approximatedType = smartcastedType.approximateForIrOrNull()
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

    internal fun IrExpression.insertCastForReceiver(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): IrExpression {
        return insertCastForIntersectionTypeOrNull(argumentType, expectedType, forReceiver = true)
        // When we generate an implicit this receiver, we assign it the type of the IR declaration.
        // However, dataframe generates FIR and IR anonymous functions with different receiver types
        // and then relies on the fact that FIR2IR generates an implicit cast from the one to the other.
        // That's why we insert a seemingly redundant cast to the argumentType (not the expected type) here.
        // See plugins/kotlin-dataframe/testData/box/groupByAdd.kt and plugins/kotlin-dataframe/testData/box/wrongReceiver.kt.
        // TODO(KT-77691) Remove when fixed on the plugin side.
            ?: implicitCastOrExpression(this, argumentType)
    }

    internal fun IrExpression.insertCastForIntersectionTypeOrSelf(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType,
    ): IrExpression {
        return insertCastForIntersectionTypeOrNull(argumentType, expectedType, forReceiver = false)
            ?: this
    }

    private fun IrExpression.insertCastForIntersectionTypeOrNull(
        argumentType: ConeKotlinType,
        expectedType: ConeKotlinType,
        forReceiver: Boolean,
    ): IrExpression? {
        val argumentTypeLowerBound = argumentType.lowerBoundIfFlexible()
        if (argumentTypeLowerBound !is ConeIntersectionType) return null

        val approximatedExpectedType = expectedType.approximateForIrOrSelf()

        // An intersection type like `Foo<Any?> & Foo<Bar>` is approximated to `Foo<out Any?>`.
        // However, atomic-fu relies on the fact that receivers don't have projections in their type arguments.
        // See plugins/atomicfu/atomicfu-compiler/testData/box/atomics_basic/UncheckedCastTest.kt
        // TODO(KT-77692) Remove if fixed on the plugin side.
        if (!forReceiver) {
            val approximatedArgumentType = argumentTypeLowerBound.approximateForIrOrNull() ?: argumentTypeLowerBound
            if (approximatedArgumentType.isSubtypeOf(approximatedExpectedType, session)) return null
        }

        return argumentTypeLowerBound.intersectedTypes
            .firstOrNull { it.isSubtypeOf(approximatedExpectedType, session) }
            ?.let { generateImplicitCast(this, it.toIrType()) }
    }

    fun implicitCastOrExpression(
        original: IrExpression, castType: ConeKotlinType, typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT
    ): IrExpression {
        return implicitCastOrExpression(original, castType.toIrType(typeOrigin))
    }

    companion object {
        fun implicitCastOrExpression(original: IrExpression, castType: IrType): IrExpression {
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

        context(c: Fir2IrComponents)
        internal fun coerceToUnitIfNeeded(original: IrExpression): IrExpression {
            val valueType = original.type
            return if (valueType.isUnit() || valueType.isNothing())
                original
            else
                IrTypeOperatorCallImpl(
                    original.startOffset, original.endOffset,
                    c.builtins.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    c.builtins.unitType,
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
