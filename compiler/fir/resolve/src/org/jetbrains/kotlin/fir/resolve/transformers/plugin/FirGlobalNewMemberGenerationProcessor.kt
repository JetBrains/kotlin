/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.addDeclaration
import org.jetbrains.kotlin.fir.declarations.validate
import org.jetbrains.kotlin.fir.extensions.FirExistingClassModificationExtension
import org.jetbrains.kotlin.fir.extensions.existingClassModifiers
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor

class FirGlobalNewMemberGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirGlobalResolveProcessor(session, scopeSession) {
    private val index = session.generatedNestedClassIndex
    private val provider = session.predicateBasedProvider

    override fun process() {
        val extensions = session.extensionService.existingClassModifiers
        if (extensions.isEmpty()) return
        for (extension in extensions) {
            generateNewMembers(extension)
            fillGeneratedNestedClasses(extension)
        }
    }

    private fun generateNewMembers(extension: FirExistingClassModificationExtension) {
        val declarations = provider.getSymbolsWithOwnersByPredicate(extension.predicate)
        for ((declaration, owners) in declarations) {
            val newMembers = extension.generateMembers(declaration, owners)
            for ((newMember, owner) in newMembers) {
                newMember.validate()
                owner.addDeclaration(newMember)
            }
        }
    }

    private fun fillGeneratedNestedClasses(extension: FirExistingClassModificationExtension) {
        for (generatedNestedClass in index[extension.key]) {
            val nestedClass = generatedNestedClass.nestedClass
            val newMembers = extension.generateMembersForNestedClasses(generatedNestedClass)
            for (newMember in newMembers) {
                newMember.validate()
                nestedClass.addDeclaration(newMember)
            }
        }
    }
}