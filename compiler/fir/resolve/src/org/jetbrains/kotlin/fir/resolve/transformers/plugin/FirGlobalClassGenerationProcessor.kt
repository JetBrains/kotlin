/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor

class FirGlobalClassGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {
    override fun process(files: Collection<FirFile>) {
        val extensions = session.extensionService.declarationGenerators
        if (extensions.isEmpty()) return
        val provider = session.predicateBasedProvider
        for (extension in extensions) {
            var annotatedDeclarations = provider.getSymbolsWithOwnersByPredicate(extension.predicate)
            while (annotatedDeclarations.isNotEmpty()) {
                val newClasses = generateClasses(annotatedDeclarations, extension)
                annotatedDeclarations = provider.getSymbolsWithOwnersByPredicate(newClasses, extension.predicate)
            }
        }
    }

    private fun generateClasses(
        declarations: List<Pair<FirAnnotatedDeclaration<*>, List<FirAnnotatedDeclaration<*>>>>,
        extension: FirDeclarationGenerationExtension,
    ): List<FirRegularClass> {
        val newClasses = mutableListOf<FirRegularClass>()
        for ((declaration, owners) in declarations) {
            generateClass(extension, declaration, owners, newClasses)
        }
        return newClasses
    }

    @OptIn(FirProviderInternals::class)
    private fun generateClass(
        extension: FirDeclarationGenerationExtension,
        declaration: FirAnnotatedDeclaration<*>,
        owners: List<FirAnnotatedDeclaration<*>>,
        newClasses: MutableList<FirRegularClass>
    ) {
        val generatedClasses = extension.generateClasses(declaration, owners)
        for ((klass, owner) in generatedClasses) {
            when (owner) {
                is FirRegularClass -> owner.addDeclaration(klass)
                is FirFile -> owner.addDeclaration(klass)
            }
            session.generatedClassIndex.registerClass(klass, owner)
            session.predicateBasedProvider.registerGeneratedDeclaration(klass, owner)
            session.firProvider.recordGeneratedClass(owner, klass)
            newClasses += klass
        }
    }
}
