/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirProviderInternals
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor

class FirGlobalNewMemberGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {
    private val index = session.generatedClassIndex
    private val provider = session.predicateBasedProvider

    override fun process() {
        val extensions = session.extensionService.declarationGenerators
        if (extensions.isEmpty()) return
        for (extension in extensions) {
            generateNewMembers(extension)
            fillGeneratedClasses(extension)
        }
    }

    @OptIn(FirProviderInternals::class)
    private fun generateNewMembers(extension: FirDeclarationGenerationExtension) {
        val declarations = provider.getSymbolsWithOwnersByPredicate(extension.predicate)
        for ((declaration, owners) in declarations) {
            val newMembers = extension.generateMembers(declaration, owners)
            for ((newMember, owner) in newMembers) {
                newMember.validate()
                when (owner) {
                    is FirRegularClass -> owner.addDeclaration(newMember)
                    is FirFile -> owner.addDeclaration(newMember)
                    else -> error("Should not be here")
                }
                session.firProvider.recordGeneratedMember(owner, newMember)
            }
        }
    }

    @OptIn(FirProviderInternals::class)
    private fun fillGeneratedClasses(extension: FirDeclarationGenerationExtension) {
        for (generatedClass in index[extension.key]) {
            val klass = generatedClass.klass
            val newMembers = extension.generateMembersForGeneratedClass(generatedClass)
            for (newMember in newMembers) {
                newMember.validate()
                klass.addDeclaration(newMember)
                session.firProvider.recordGeneratedMember(klass, newMember)
            }
        }
    }
}