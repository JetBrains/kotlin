/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.snippetScopesConfigurators
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name

class FirSnippetFromReplScope(
    private val useSiteSession: FirSession,
) : FirContainingNamesAwareScope() {

    override fun getCallableNames(): Set<Name> {
        return emptySet()
    }

    override fun getClassifierNames(): Set<Name> {
        return emptySet()
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        for (configurator in useSiteSession.extensionService.snippetScopesConfigurators) {
            // TODO: unclear semantics
            configurator.contributeVariablesToReplScope(name, processor)
        }
    }
}

class FirSnippetDeclarationsScope(
    private val useSiteSession: FirSession,
) : FirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> {
        return emptySet()
    }

    override fun getClassifierNames(): Set<Name> {
        return emptySet()
    }

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        for (configurator in useSiteSession.extensionService.snippetScopesConfigurators) {
            configurator.contributeClassifiersToReplScope(name) { processor(it, ConeSubstitutor.Empty) }
        }
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        for (configurator in useSiteSession.extensionService.snippetScopesConfigurators) {
            configurator.contributeFunctionsToReplScope(name) { processor(it) }
        }
    }
}

