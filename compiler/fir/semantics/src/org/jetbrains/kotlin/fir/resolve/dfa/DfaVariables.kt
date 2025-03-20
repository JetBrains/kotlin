/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.SmartcastStability
import java.util.*

sealed class DataFlowVariable {
    abstract val originalType: ConeKotlinType
}

private enum class PropertyStability(
    val inherentInstability: SmartcastStability?,
    val checkModule: Boolean = false,
    val checkReceiver: Boolean = false,
) {
    // Private vals can only be accessed from the same scope, so they're always safe to smart cast.
    // Constant values (e.g. singleton objects) cannot be reassigned no matter what, so they're always safe
    // to smart cast as well, although this is not very useful.
    PRIVATE_OR_CONST_VAL(null),

    // Public final vals can be accessed from different modules, which are not necessarily recompiled
    // when the module declaring the property changes, so smart casting them there is unsafe.
    PUBLIC_FINAL_VAL(null, checkModule = true),

    // Public open vals can be overridden with custom getters, so smart casting them is only safe
    // if the receiver is known to be of a final type that doesn't do that.
    PUBLIC_OPEN_VAL(null, checkModule = true, checkReceiver = true),

    CAPTURED_VARIABLE(SmartcastStability.CAPTURED_VARIABLE),
    EXPECT_PROPERTY(SmartcastStability.EXPECT_PROPERTY),
    PROPERTY_WITH_GETTER(SmartcastStability.PROPERTY_WITH_GETTER),
    MUTABLE_PROPERTY(SmartcastStability.MUTABLE_PROPERTY),
    DELEGATED_PROPERTY(SmartcastStability.DELEGATED_PROPERTY);
}

class RealVariable(
    val symbol: FirBasedSymbol<*>,
    val isImplicit: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?,
    override val originalType: ConeKotlinType,
) : DataFlowVariable() {
    companion object {
        fun local(symbol: FirVariableSymbol<*>): RealVariable =
            RealVariable(symbol, isImplicit = false, dispatchReceiver = null, extensionReceiver = null, symbol.resolvedReturnType)

        fun implicit(symbol: FirBasedSymbol<*>, type: ConeKotlinType): RealVariable =
            RealVariable(symbol, isImplicit = true, dispatchReceiver = null, extensionReceiver = null, type)
    }

    // `originalType` cannot be included into equality comparisons because it can be a captured type.
    // Those are normally not equal to each other, but if this variable is stable, then it is in fact the same type.
    override fun equals(other: Any?): Boolean =
        other is RealVariable && symbol == other.symbol && isImplicit == other.isImplicit &&
                dispatchReceiver == other.dispatchReceiver && extensionReceiver == other.extensionReceiver

    override fun hashCode(): Int =
        Objects.hash(symbol, isImplicit, dispatchReceiver, extensionReceiver)

    override fun toString(): String = buildString {
        if (isImplicit) {
            append(if (symbol is FirValueParameterSymbol) "context@" else "this@")
        }

        append(
            when (symbol) {
                is FirClassSymbol<*> -> symbol.classId
                is FirCallableSymbol<*> -> symbol.callableId
                else -> symbol
            }
        )

        if (dispatchReceiver != null && extensionReceiver != null) {
            append("(${dispatchReceiver}, ${extensionReceiver})")
        } else if (dispatchReceiver != null || extensionReceiver != null) {
            append("(${dispatchReceiver ?: extensionReceiver})")
        }
    }

    fun getStability(flow: Flow, session: FirSession): SmartcastStability {
        if (!isImplicit) {
            val stability = propertyStability

            val isUnstableSmartcastOnDelegatedProperties =
                session.languageVersionSettings.supportsFeature(LanguageFeature.UnstableSmartcastOnDelegatedProperties)
            if (isUnstableSmartcastOnDelegatedProperties && (symbol.fir as? FirProperty)?.isDelegated == true) return SmartcastStability.DELEGATED_PROPERTY

            stability.inherentInstability?.let { return it }
            if (stability.checkReceiver && dispatchReceiver?.hasFinalType(flow, session) == false)
                return SmartcastStability.PROPERTY_WITH_GETTER
            if (stability.checkModule && !(symbol.fir as FirVariable).isInCurrentOrFriendModule(session))
                return SmartcastStability.ALIEN_PUBLIC_PROPERTY
            // Members of unstable values should always be unstable, as the receiver could've changed.
            dispatchReceiver?.getStability(flow, session)?.takeIf { it != SmartcastStability.STABLE_VALUE }?.let { return it }
            // No need to check extension receiver, as properties with one cannot be stable by symbol stability.
        }
        return SmartcastStability.STABLE_VALUE
    }

    private fun hasFinalType(flow: Flow, session: FirSession): Boolean =
        originalType.isFinal(session) || flow.getTypeStatement(this)?.upperTypes?.any { it.isFinal(session) } == true

    private val propertyStability: PropertyStability by lazy {
        when (val fir = symbol.fir) {
            !is FirVariable -> PropertyStability.PRIVATE_OR_CONST_VAL // named object or containing class for a static field reference
            is FirEnumEntry -> PropertyStability.PRIVATE_OR_CONST_VAL
            is FirErrorProperty -> PropertyStability.PRIVATE_OR_CONST_VAL
            is FirValueParameter -> PropertyStability.PRIVATE_OR_CONST_VAL
            is FirBackingField -> when {
                fir.isVal -> PropertyStability.PRIVATE_OR_CONST_VAL
                else -> PropertyStability.MUTABLE_PROPERTY
            }
            is FirField -> when {
                fir.isVal -> PropertyStability.PUBLIC_FINAL_VAL
                else -> PropertyStability.MUTABLE_PROPERTY
            }
            is FirProperty -> when {
                fir.isExpect -> PropertyStability.EXPECT_PROPERTY
                fir.delegate != null -> PropertyStability.DELEGATED_PROPERTY
                // Local vars are only *sometimes* unstable (when there are concurrent assignments). `FirDataFlowAnalyzer`
                // will check that at each use site individually and mark the access as stable when possible.
                fir.isLocal && fir.isVar -> PropertyStability.CAPTURED_VARIABLE
                fir.isLocal -> PropertyStability.PRIVATE_OR_CONST_VAL
                fir.isVar -> PropertyStability.MUTABLE_PROPERTY
                fir.receiverParameter != null -> PropertyStability.PROPERTY_WITH_GETTER
                fir.getter !is FirDefaultPropertyAccessor? -> PropertyStability.PROPERTY_WITH_GETTER
                fir.visibility == Visibilities.Private -> PropertyStability.PRIVATE_OR_CONST_VAL
                fir.isFinal -> PropertyStability.PUBLIC_FINAL_VAL
                else -> PropertyStability.PUBLIC_OPEN_VAL
            }
        }
    }
}

data class SyntheticVariable(val fir: FirExpression) : DataFlowVariable() {
    override val originalType: ConeKotlinType get() = fir.resolvedType
}

private fun ConeKotlinType.isFinal(session: FirSession): Boolean = when (this) {
    is ConeFlexibleType -> lowerBound.isFinal(session)
    is ConeDefinitelyNotNullType -> original.isFinal(session)
    is ConeClassLikeType -> toSymbol(session)?.fullyExpandedClass(session)?.isFinal == true
    is ConeIntersectionType -> intersectedTypes.any { it.isFinal(session) }
    else -> false
}

private fun FirVariable.isInCurrentOrFriendModule(session: FirSession): Boolean {
    val propertyModuleData = originalOrSelf().moduleData
    val currentModuleData = session.moduleData
    return propertyModuleData == currentModuleData ||
            propertyModuleData in currentModuleData.friendDependencies ||
            propertyModuleData in currentModuleData.allDependsOnDependencies
}
