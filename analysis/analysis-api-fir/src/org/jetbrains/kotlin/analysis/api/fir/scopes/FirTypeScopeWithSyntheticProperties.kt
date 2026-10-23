/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.FirSyntheticPropertiesScope
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirDelegatingTypeScope
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

internal class FirTypeScopeWithSyntheticProperties(
    val typeScope: FirTypeScope,
    val syntheticPropertiesScope: FirSyntheticPropertiesScope,
) : FirDelegatingTypeScope(typeScope) {
    override fun getCallableNames(): Set<Name> = typeScope.getCallableNames() + syntheticPropertiesScope.getCallableNames()
    override fun mayContainName(name: Name): Boolean = typeScope.mayContainName(name) || syntheticPropertiesScope.mayContainName(name)

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        typeScope.processPropertiesByName(name, processor)
        syntheticPropertiesScope.processPropertiesByName(name, processor)
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirTypeScopeWithSyntheticProperties? {
        val newTypeScope = typeScope.withReplacedSessionOrNull(newSession, newScopeSession)
        val newSyntheticPropertiesScope = syntheticPropertiesScope.withReplacedSessionOrNull(newSession, newScopeSession)
        if (newTypeScope == null && newSyntheticPropertiesScope == null) return null
        return FirTypeScopeWithSyntheticProperties(
            newTypeScope ?: typeScope,
            newSyntheticPropertiesScope ?: syntheticPropertiesScope,
        )
    }
}
