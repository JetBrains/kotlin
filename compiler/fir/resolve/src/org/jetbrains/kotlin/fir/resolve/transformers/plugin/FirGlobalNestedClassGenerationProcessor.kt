/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.extensions.existingClassModifiers
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor

class FirGlobalNestedClassGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {
    @OptIn(FirProviderInternals::class)
    override fun process() {
        val extensions = session.extensionService.existingClassModifiers
        if (extensions.isEmpty()) return
        val provider = session.predicateBasedProvider
        val index = session.generatedNestedClassIndex
        for (extension in extensions) {
            val declarations = provider.getSymbolsWithOwnersByPredicate(extension.predicate)
            for ((declaration, owners) in declarations) {
                val nestedClasses = extension.generateNestedClasses(declaration, owners)
                for ((nestedClass, owner) in nestedClasses) {
                    owner.addDeclaration(nestedClass)
                    index.registerNestedClass(nestedClass, owner)
                    session.firProvider.recordNestedClass(owner, nestedClass)
                }
            }
        }
    }
}

