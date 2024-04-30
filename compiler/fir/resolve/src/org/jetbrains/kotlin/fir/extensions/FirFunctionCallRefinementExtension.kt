/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.calls.CallInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import kotlin.reflect.KClass

/**
 * This extension integrates with call resolution mechanism:
 * resolution and completion of the receiver
 * resolution of arguments
 * resolution of the call itself
 *  - [intercept] is called
 * resolution of the outer call
 * completion of the call
 *  - [transform] is called
 * completion of the outer call
 *
 * !!!! This extension is highly unstable and not recommended to use !!!!
 */
@FirExtensionApiInternals
abstract class FirFunctionCallRefinementExtension(session: FirSession) : FirExtension(session) {
    companion object {
        val NAME: FirExtensionPointName = FirExtensionPointName("FunctionCallRefinementExtension")
    }

    final override val name: FirExtensionPointName
        get() = NAME


    final override val extensionType: KClass<out FirExtension> = FirFunctionCallRefinementExtension::class

    /**
     * Allows a call to be completed with a more specific type than the declared return type of function
     * ```
     * interface Container<out T> { }
     * fun Container<T>.add(item: String): Container<Any>
     * ```
     * at call site `Container<Any>` can be modified to become `Container<NewLocalType>`
     * ```
     * container.add("A")
     * ```
     * this `NewLocalType` can be created in [intercept]. It must be later saved into FIR tree in [transform]
     * Generated declarations should be local because this [FirExtension] works at body resolve stage and thus cannot create new top level declarations
     *
     * When [intercept] returns non-null value, a copy will be created from FirFunction that [symbol]
     * points to. Copy will be used in call completion instead of original function.
     *
     * @return null if plugin is not interested in a [symbol]
     */
    abstract fun intercept(callInfo: CallInfo, symbol: FirNamedFunctionSymbol): CallReturnType?

    /**
     *
     * Data can be associated with [FirNamedFunctionSymbol] in [callback]
     */
    class CallReturnType(val typeRef: FirResolvedTypeRef, val callback: ((FirNamedFunctionSymbol) -> Unit)? = null)

    /**
     * @param call to a function that was created with modified [FirResolvedTypeRef] as a result of [intercept].
     * This function doesn't exist in FIR, it is needed to complete the call.
     * @param originalSymbol [intercept] is called with symbol to a declaration that exists somewhere in FIR: library, project code.
     * The same symbol is [originalSymbol].
     * [transform] needs to generate call to [let] with the same return type as [call]
     * and put all generated declarations used in [FirResolvedTypeRef] in statements.
     */
    abstract fun transform(call: FirFunctionCall, originalSymbol: FirNamedFunctionSymbol): FirFunctionCall

    fun interface Factory : FirExtension.Factory<FirFunctionCallRefinementExtension>
}

@OptIn(FirExtensionApiInternals::class)
val FirExtensionService.callRefinementExtensions: List<FirFunctionCallRefinementExtension> by FirExtensionService.registeredExtensions()

@OptIn(FirExtensionApiInternals::class)
internal class OriginalCallData(val originalSymbol: FirNamedFunctionSymbol, val extension: FirFunctionCallRefinementExtension)

internal object OriginalCallDataKey : FirDeclarationDataKey()

internal var FirDeclaration.originalCallDataForPluginRefinedCall: OriginalCallData? by FirDeclarationDataRegistry.data(OriginalCallDataKey)
