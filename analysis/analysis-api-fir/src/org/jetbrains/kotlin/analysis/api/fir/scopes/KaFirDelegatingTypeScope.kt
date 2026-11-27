/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.signatures.KaCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.name.Name

internal open class KaFirDelegatingTypeScope(
    val firScope: FirContainingNamesAwareScope,
    override val analysisSession: KaFirSession,
) : KaTypeScope, KaFirSessionBoundOwner {

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

    override fun getCallableSignatures(nameFilter: (Name) -> Boolean): Sequence<KaCallableSignature<*>> = entryPointImplementation {
        firScope.getCallableSignatures(getPossibleCallableNames().filter(nameFilter))
    }

    override fun getCallableSignatures(names: Collection<Name>): Sequence<KaCallableSignature<*>> = entryPointImplementation {
        firScope.getCallableSignatures(names)
    }

    override fun getClassifierSymbols(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = entryPointImplementation {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter))
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = entryPointImplementation {
        firScope.getClassifierSymbols(names)
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = entryPointImplementation {
        firScope.getConstructors()
    }

    override fun mayContainName(name: Name): Boolean = entryPointImplementation {
        name in getAllPossibleNames()
    }
}
