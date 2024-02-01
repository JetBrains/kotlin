/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.dispatchReceiverClassLookupTagOrNull
import org.jetbrains.kotlin.fir.java.scopes.ClassicBuiltinSpecialProperties.getBuiltinSpecialPropertyGetterName
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmSignature
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_VALUE_PARAMETERS_SHORT_NAMES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_VALUE_PARAMETERS_SIGNATURES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.REMOVE_AT_NAME_AND_SIGNATURE
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.SIGNATURE_TO_JVM_REPRESENTATION_NAME
import org.jetbrains.kotlin.name.Name

fun FirCallableSymbol<*>.doesOverrideBuiltinWithDifferentJvmName(containingScope: FirTypeScope, session: FirSession): Boolean {
    return getOverriddenBuiltinWithDifferentJvmName(containingScope, session) != null
}

fun <T : FirCallableSymbol<*>> T.getOverriddenBuiltinWithDifferentJvmName(containingScope: FirTypeScope, session: FirSession): T? {
    if (
        name !in SpecialGenericSignatures.ORIGINAL_SHORT_NAMES && name !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES
    ) return null

    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is FirNamedFunctionSymbol -> firstOverriddenFunction(containingScope) {
            BuiltinMethodsWithDifferentJvmName.isBuiltinFunctionWithDifferentNameInJvm(it, session)
        } as T?

        is FirPropertySymbol -> ClassicBuiltinSpecialProperties.findBuiltinSpecialPropertyFqName(this, containingScope) as T?

        else -> null
    }
}

fun FirCallableSymbol<*>.getJvmMethodNameIfSpecial(containingScope: FirTypeScope, session: FirSession): Name? {
    val overriddenBuiltin = getOverriddenBuiltinWithDifferentJvmName(containingScope, session)
        ?: return null
    val name = when (overriddenBuiltin) {
        is FirPropertySymbol -> overriddenBuiltin.getBuiltinSpecialPropertyGetterName(containingScope)
        is FirNamedFunctionSymbol -> BuiltinMethodsWithDifferentJvmName.getJvmName(overriddenBuiltin)?.asString()
        else -> null
    } ?: return null
    return Name.identifier(name)
}


object BuiltinMethodsWithSpecialGenericSignature {
    private val FirNamedFunctionSymbol.hasErasedValueParametersInJava: Boolean
        get() = fir.computeJvmSignature() in ERASED_VALUE_PARAMETERS_SIGNATURES

    @JvmStatic
    fun getOverriddenBuiltinFunctionWithErasedValueParametersInJava(
        functionSymbol: FirNamedFunctionSymbol,
        containingScope: FirTypeScope
    ): FirNamedFunctionSymbol? {
        if (!functionSymbol.name.sameAsBuiltinMethodWithErasedValueParameters) return null
        return functionSymbol.firstOverriddenFunction(containingScope) { it.hasErasedValueParametersInJava }
    }

    val Name.sameAsBuiltinMethodWithErasedValueParameters: Boolean
        get() = this in ERASED_VALUE_PARAMETERS_SHORT_NAMES
}

object BuiltinMethodsWithDifferentJvmName {
    fun getJvmName(functionSymbol: FirNamedFunctionSymbol): Name? {
        return SIGNATURE_TO_JVM_REPRESENTATION_NAME[functionSymbol.fir.computeJvmSignature() ?: return null]
    }

    fun isBuiltinFunctionWithDifferentNameInJvm(functionSymbol: FirNamedFunctionSymbol, session: FirSession): Boolean {
        return functionSymbol.isFromBuiltinClass(session) && SIGNATURE_TO_JVM_REPRESENTATION_NAME.containsKey(functionSymbol.fir.computeJvmSignature())
    }

    val FirNamedFunctionSymbol.isRemoveAtByIndex: Boolean
        get() = name.asString() == "removeAt" && fir.computeJvmSignature() == REMOVE_AT_NAME_AND_SIGNATURE.signature
}

object ClassicBuiltinSpecialProperties {
    fun FirCallableSymbol<*>.getBuiltinSpecialPropertyGetterName(containingScope: FirTypeScope): String? {
        val overridden = findBuiltinSpecialPropertyFqName(this, containingScope) ?: return null
        return BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[overridden.callableId.asSingleFqName()]?.asString()
    }

    fun findBuiltinSpecialPropertyFqName(symbol: FirCallableSymbol<*>, containingScope: FirTypeScope): FirCallableSymbol<*>? {
        if (symbol.name !in BuiltinSpecialProperties.SPECIAL_SHORT_NAMES) return null

        return symbol.hasBuiltinSpecialPropertyFqNameImpl(containingScope)
    }

    private fun FirCallableSymbol<*>.hasBuiltinSpecialPropertyFqNameImpl(containingScope: FirTypeScope): FirCallableSymbol<*>? {
        if (callableId.asSingleFqName() in BuiltinSpecialProperties.SPECIAL_FQ_NAMES && valueParametersAreEmpty()) return this
        // if (!KotlinBuiltIns.isBuiltIn(this)) return false
        var result: FirCallableSymbol<*>? = null

        fun process(overridden: FirCallableSymbol<*>, scope: FirTypeScope): ProcessorAction {
            val foundSymbol = findBuiltinSpecialPropertyFqName(overridden, scope)
            return if (foundSymbol != null) {
                result = foundSymbol
                ProcessorAction.STOP
            } else {
                ProcessorAction.NEXT
            }
        }

        when (this) {
            is FirNamedFunctionSymbol -> containingScope.processDirectOverriddenFunctionsWithBaseScope(this) { overridden, scope ->
                process(overridden, scope)
            }

            is FirPropertySymbol -> containingScope.processDirectOverriddenPropertiesWithBaseScope(this) { overridden, scope ->
                process(overridden, scope)
            }
        }

        return result
    }

    private fun FirCallableSymbol<*>.valueParametersAreEmpty(): Boolean {
        return when (this) {
            is FirNamedFunctionSymbol -> fir.valueParameters.isEmpty()
            else -> true
        }
    }
}

private fun FirCallableSymbol<*>.isFromBuiltinClass(session: FirSession): Boolean {
    return dispatchReceiverClassLookupTagOrNull()?.toSymbol(session)?.fir?.origin == FirDeclarationOrigin.BuiltIns
}

private fun FirNamedFunctionSymbol.firstOverriddenFunction(
    containingScope: FirTypeScope,
    predicate: (FirNamedFunctionSymbol) -> Boolean
): FirNamedFunctionSymbol? {
    return firstOverriddenCallable(containingScope, FirTypeScope::processOverriddenFunctionsAndSelf, predicate)
}

private inline fun <T : FirCallableSymbol<*>> T.firstOverriddenCallable(
    containingScope: FirTypeScope,
    processFunction: FirTypeScope.(T, (T) -> ProcessorAction) -> ProcessorAction,
    noinline predicate: (T) -> Boolean,
): T? {
    var result: T? = null
    containingScope.processFunction(this) { symbol ->
        if (predicate(symbol)) {
            result = symbol
            ProcessorAction.STOP
        } else {
            ProcessorAction.NEXT
        }
    }
    return result
}
