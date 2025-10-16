/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.references.toResolvedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.types.SmartcastStability

/**
 * A type of value that is in scope and can be passed to a call implicitly.
 * These values need special treatment in smart casts.
 * For an explanation, see the KDoc of [computeExpression].
 *
 * The two implementors are [ImplicitContextParameterValue] and [ImplicitReceiverValue].
 *
 * Both implementors can be used as context arguments.
 *
 * [ImplicitReceiverValue] also implements [ReceiverValue] but [ImplicitContextParameterValue] does **not**.
 * That's because context parameters **cannot** be implicit receivers of calls.
 *
 * See the KDoc of [ReceiverValue] for further details.
 */
sealed class ImplicitValue<S : FirBasedSymbol<*>>(
    type: ConeKotlinType,
    val originalType: ConeKotlinType,
    protected val mutable: Boolean,
) {
    abstract val boundSymbol: S

    var type: ConeKotlinType = type
        private set

    /**
     * Every expression type produced by the implementations (in addition to [FirSmartCastExpression])
     * must be handled in [copyImplicitValueExpression].
     */
    protected abstract fun computeOriginalExpression(): FirExpression

    /**
     * This property should have only one instance, but in some cases its calculation is not needed,
     * so it is lazy to avoid unnecessary calculation which may affect performance, at least in the Analysis API mode
     *
     * Issue: KT-73900
     * @see computeOriginalExpression
     */
    protected val originalExpression: FirExpression by lazy(LazyThreadSafetyMode.PUBLICATION, ::computeOriginalExpression)

    private var isSmartCasted: Boolean = type != originalType

    /**
     * The idea of expression for implicit values is the following:
     *   - Implicit values are mutable because of smartcasts
     *   - The expression of the implicit value may be used during call resolution and then stored for later.
     *     This implies the necessity to keep value expressions independent of the state of the corresponding implicit value.
     * All those statements lead to the current implementation:
     *   - original expression (without smartcast) is always stored inside [originalExpression] and cannot be changed
     *   - we keep track if there is a smartcast in [isSmartCasted]
     *   - if the type of the implicit value changes, [isSmartCasted] is reset
     */
    fun computeExpression(): FirExpression {
        return if (isSmartCasted) {
            buildSmartCastExpression {
                this.originalExpression = this@ImplicitValue.originalExpression
                smartcastType = buildResolvedTypeRef {
                    source = this@ImplicitValue.originalExpression.source?.fakeElement(KtFakeSourceElementKind.SmartCastedTypeRef)
                    coneType = this@ImplicitValue.type
                }
                upperTypesFromSmartCast = listOf(this@ImplicitValue.type)
                smartcastStability = SmartcastStability.STABLE_VALUE
                coneTypeOrNull = this@ImplicitValue.type
                lowerTypesFromSmartCast = emptyList()
            }
        } else {
            originalExpression
        }
    }

    fun isSameImplicitReceiverInstance(other: FirExpression): Boolean {
        val otherBoundSymbol = when (val otherOriginal = other.unwrapSmartcastExpression()) {
            is FirThisReceiverExpression -> otherOriginal.calleeReference.boundSymbol
            is FirPropertyAccessExpression -> otherOriginal.calleeReference.toResolvedSymbol<FirBasedSymbol<*>>()
            else -> null
        }

        return boundSymbol === otherBoundSymbol
    }

    @RequiresOptIn
    annotation class ImplicitValueInternals

    /**
     * Should be called only in ImplicitReceiverStack
     */
    @ImplicitValueInternals
    open fun updateTypeFromSmartcast(type: ConeKotlinType) {
        if (type == this.type) return
        if (!mutable) error("Cannot mutate an immutable ImplicitReceiverValue")
        this.type = type
        isSmartCasted = type != this.originalType
    }

    abstract fun createSnapshot(keepMutable: Boolean): ImplicitValue<S>
}

fun FirExpression.copyImplicitValueExpression(): FirExpression {
    return when (this) {
        is FirPropertyAccessExpression -> buildPropertyAccessExpressionCopy(this) {}
        is FirThisReceiverExpression -> buildThisReceiverExpressionCopy(this) {}
        is FirInaccessibleReceiverExpression -> buildInaccessibleReceiverExpressionCopy(this) {}
        is FirSmartCastExpression -> buildSmartCastExpressionCopy(this) {}
        else -> error("Unexpected expression type '${this.javaClass.simpleName}'")
    }
}

class ImplicitContextParameterValue private constructor(
    override val boundSymbol: FirValueParameterSymbol,
    type: ConeKotlinType,
    originalType: ConeKotlinType,
    mutable: Boolean,
) : ImplicitValue<FirValueParameterSymbol>(type, originalType, mutable) {
    constructor(boundSymbol: FirValueParameterSymbol, type: ConeKotlinType)
            : this(boundSymbol, type, originalType = type, mutable = true)

    override fun computeOriginalExpression(): FirExpression = buildPropertyAccessExpression {
        source = boundSymbol.source?.fakeElement(KtFakeSourceElementKind.ImplicitContextParameterArgument)
        calleeReference = buildResolvedNamedReference {
            name = boundSymbol.name
            resolvedSymbol = boundSymbol
        }
        coneTypeOrNull = originalType
    }

    override fun createSnapshot(keepMutable: Boolean): ImplicitContextParameterValue {
        return ImplicitContextParameterValue(boundSymbol, type, originalType, keepMutable)
    }
}
