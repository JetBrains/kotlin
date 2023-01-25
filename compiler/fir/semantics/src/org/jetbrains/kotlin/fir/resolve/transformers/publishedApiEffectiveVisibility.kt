/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

private object PublishedApiEffectiveVisibilityKey : FirDeclarationDataKey()
var FirDeclaration.publishedApiEffectiveVisibility: EffectiveVisibility? by FirDeclarationDataRegistry.data(PublishedApiEffectiveVisibilityKey)

inline val FirBasedSymbol<*>.publishedApiEffectiveVisibility: EffectiveVisibility?
    get() {
        lazyResolveToPhase(FirResolvePhase.STATUS)
        return fir.publishedApiEffectiveVisibility
    }
