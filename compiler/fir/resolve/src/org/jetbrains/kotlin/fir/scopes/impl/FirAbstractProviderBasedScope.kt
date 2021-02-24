/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.symbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirScope

abstract class FirAbstractProviderBasedScope(val session: FirSession, lookupInFir: Boolean = true) :
    FirScope() {
    val provider = when (val symbolProvider = session.symbolProvider) {
        is FirCompositeSymbolProvider -> symbolProvider.takeIf { !lookupInFir }?.providers?.find {
            it is FirDependenciesSymbolProviderImpl
        } ?: symbolProvider
        else -> symbolProvider
    }
}
