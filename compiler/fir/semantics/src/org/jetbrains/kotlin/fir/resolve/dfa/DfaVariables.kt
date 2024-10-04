/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.SmartcastStability
import java.util.*

sealed class DataFlowVariable {
    companion object {
        /**
         * Retrieve a [DataFlowVariable] representing the given [FirExpression]. If [fir] is a property reference,
         * return a [RealVariable], otherwise a [SyntheticVariable].
         *
         * @param unwrapReceiver Used when looking up [RealVariable]s for receivers of [fir]
         * if it is a qualified access expression.
         */
        fun of(
            fir: FirExpression,
            session: FirSession,
            unwrapReceiver: (RealVariable) -> RealVariable?,
        ): DataFlowVariable? {
            fun DataFlowVariable.unwrap(): DataFlowVariable? = when (this) {
                is RealVariable -> unwrapReceiver(this)
                is SyntheticVariable -> this
            }

            val unwrapped = fir.unwrapElement() ?: return null
            val isReceiver = unwrapped is FirThisReceiverExpression
            val symbol = when (unwrapped) {
                is FirWhenSubjectExpression -> unwrapped.whenRef.value.subjectVariable?.symbol
                is FirResolvedQualifier -> unwrapped.symbol?.fullyExpandedClass(session)
                is FirResolvable -> unwrapped.calleeReference.symbol
                else -> null
            }?.takeIf {
                isReceiver || it is FirClassSymbol || (it is FirVariableSymbol && it !is FirSyntheticPropertySymbol)
            }?.unwrapFakeOverridesIfNecessary() ?: return SyntheticVariable(unwrapped)

            val qualifiedAccess = unwrapped as? FirQualifiedAccessExpression
            val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
                (of(it, session, unwrapReceiver)?.unwrap() ?: return null) as? RealVariable ?: return SyntheticVariable(unwrapped)
            }
            val extensionReceiverVar = qualifiedAccess?.extensionReceiver?.let {
                (of(it, session, unwrapReceiver)?.unwrap() ?: return null) as? RealVariable ?: return SyntheticVariable(unwrapped)
            }
            return RealVariable(symbol, isReceiver, dispatchReceiverVar, extensionReceiverVar, unwrapped.resolvedType)
        }
    }
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
    val isReceiver: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?,
    val originalType: ConeKotlinType,
) : DataFlowVariable() {
    companion object {
        fun local(symbol: FirVariableSymbol<*>): RealVariable =
            RealVariable(symbol, isReceiver = false, dispatchReceiver = null, extensionReceiver = null, symbol.resolvedReturnType)

        fun receiver(symbol: FirBasedSymbol<*>, type: ConeKotlinType): RealVariable =
            RealVariable(symbol, isReceiver = true, dispatchReceiver = null, extensionReceiver = null, type)
    }

    // `originalType` cannot be included into equality comparisons because it can be a captured type.
    // Those are normally not equal to each other, but if this variable is stable, then it is in fact the same type.
    override fun equals(other: Any?): Boolean =
        other is RealVariable && symbol == other.symbol && isReceiver == other.isReceiver &&
                dispatchReceiver == other.dispatchReceiver && extensionReceiver == other.extensionReceiver

    override fun hashCode(): Int =
        Objects.hash(symbol, isReceiver, dispatchReceiver, extensionReceiver)

    override fun toString(): String =
        (if (isReceiver) "this@" else "") + when (symbol) {
            is FirClassSymbol<*> -> "${symbol.classId}"
            is FirCallableSymbol<*> -> "${symbol.callableId}"
            else -> "$symbol"
        } + when {
            dispatchReceiver != null && extensionReceiver != null -> "(${dispatchReceiver}, ${extensionReceiver})"
            dispatchReceiver != null || extensionReceiver != null -> "(${dispatchReceiver ?: extensionReceiver})"
            else -> ""
        }

    fun getStability(flow: Flow, session: FirSession): SmartcastStability {
        if (!isReceiver) {
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
        originalType.isFinal(session) || flow.getTypeStatement(this)?.exactType?.any { it.isFinal(session) } == true

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
                fir.isFinal -> PropertyStability.PUBLIC_FINAL_VAL
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

data class SyntheticVariable(val fir: FirExpression) : DataFlowVariable()

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

private tailrec fun FirExpression.unwrapElement(): FirExpression? {
    return when (this) {
        is FirWhenSubjectExpression -> (whenRef.value.takeIf { it.subjectVariable == null }?.subject ?: return this).unwrapElement()
        is FirSmartCastExpression -> originalExpression.unwrapElement()
        // Safe assignments (a?.x = b) have non-expression selectors. In this case the entire safe call
        // is not really an expression either, so we shouldn't produce any kinds of statements on it.
        // For example, saying that `(a?.x = b) != null => a != null` makes no sense, since an assignment
        // has no value in the first place, null or otherwise.
        is FirSafeCallExpression -> (selector as? FirExpression ?: return null).unwrapElement()
        is FirCheckedSafeCallSubject -> originalReceiverRef.value.unwrapElement()
        is FirCheckNotNullCall -> argument.unwrapElement()
        is FirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.unwrapElement()
        else -> this
    }
}

private fun FirBasedSymbol<*>.unwrapFakeOverridesIfNecessary(): FirBasedSymbol<*> {
    if (this !is FirCallableSymbol) return this
    // This is necessary only for sake of optimizations necessary because this is a really hot place.
    // Not having `dispatchReceiverType` means that this is a local/top-level property that can't be a fake override.
    // And at the same time, checking a field is much faster than a couple of attributes (0.3 secs at MT Full Kotlin)
    if (this.dispatchReceiverType == null) return this

    return this.unwrapFakeOverrides()
}
