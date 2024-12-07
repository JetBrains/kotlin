/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.resolve.inference.FirInferenceSession
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol

class FirSpecialTowerDataContexts {
    private val contextForAnonymousFunctions: MutableMap<FirAnonymousFunctionSymbol, PostponedAtomsResolutionContext> = mutableMapOf()
    private val contextForCallableReferences: MutableMap<FirCallableReferenceAccess, PostponedAtomsResolutionContext> = mutableMapOf()

    fun getAnonymousFunctionContext(symbol: FirAnonymousFunctionSymbol): PostponedAtomsResolutionContext? {
        return contextForAnonymousFunctions[symbol]
    }

    fun getCallableReferenceContext(access: FirCallableReferenceAccess): PostponedAtomsResolutionContext? {
        return contextForCallableReferences[access]
    }

    fun storeAnonymousFunctionContext(
        symbol: FirAnonymousFunctionSymbol,
        context: FirTowerDataContext,
        inferenceSession: FirInferenceSession,
    ) {
        contextForAnonymousFunctions[symbol] = Pair(context, inferenceSession)
    }

    fun dropAnonymousFunctionContext(symbol: FirAnonymousFunctionSymbol) {
        contextForAnonymousFunctions.remove(symbol)
    }

    fun storeCallableReferenceContext(
        access: FirCallableReferenceAccess,
        context: FirTowerDataContext,
        inferenceSession: FirInferenceSession
    ) {
        contextForCallableReferences[access] = Pair(context, inferenceSession)
    }

    fun dropCallableReferenceContext(access: FirCallableReferenceAccess) {
        contextForCallableReferences.remove(access)
    }

    fun putAll(contexts: FirSpecialTowerDataContexts) {
        contextForCallableReferences.putAll(contexts.contextForCallableReferences)
        contextForAnonymousFunctions.putAll(contexts.contextForAnonymousFunctions)
    }

    fun clear() {
        contextForAnonymousFunctions.clear()
        contextForCallableReferences.clear()
    }
}

typealias PostponedAtomsResolutionContext = Pair<FirTowerDataContext, FirInferenceSession>