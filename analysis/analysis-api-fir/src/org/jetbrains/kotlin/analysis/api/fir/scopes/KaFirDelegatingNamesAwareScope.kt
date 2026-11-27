/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal open class KaFirDelegatingNamesAwareScope(
    firScope: FirContainingNamesAwareScope,
    analysisSession: KaFirSession,
) : KaFirBasedScope<FirContainingNamesAwareScope>(firScope, analysisSession) {
    private val allNamesCached by cached {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    override fun getAllPossibleNames(): Set<Name> = entryPointImplementation { allNamesCached }

    override fun getPossibleCallableNames(): Set<Name> = entryPointImplementation {
        firScope.getCallableNames()
    }

    override fun getPossibleClassifierNames(): Set<Name> = entryPointImplementation {
        firScope.getClassifierNames()
    }

    override fun mayContainName(name: Name): Boolean = entryPointImplementation {
        name in getAllPossibleNames()
    }
}
