/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.SpecialNames

class FirCompanionGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession) {
    override val transformer: FirTransformer<Nothing?> = FirCompanionGenerationTransformer(session)
}

class FirCompanionGenerationTransformer(val session: FirSession) : FirTransformer<Nothing?>() {

    lateinit var generatedDeclarationProvider: FirSwitchableExtensionDeclarationsSymbolProvider

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        generatedDeclarationProvider = session.generatedDeclarationsSymbolProvider ?: return file
        return file.transformDeclarations(this, data)
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        generateCompanion(regularClass)
        return regularClass.transformDeclarations(this, data)
    }

    private fun generateCompanion(regularClass: FirRegularClass) {
        // TODO: add proper error reporting
        generatedDeclarationProvider = session.generatedDeclarationsSymbolProvider ?: return
        val companionClassId = regularClass.classId.createNestedClassId(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
        when (val generatedCompanion = generatedDeclarationProvider.getClassLikeSymbolByClassId(companionClassId)) {
            null -> {}
            is FirRegularClassSymbol -> when {
                regularClass.companionObjectSymbol != null -> error("Plugin generated companion object for class $regularClass, but it is already present in class")
                else -> regularClass.replaceCompanionObjectSymbol(generatedCompanion)
            }
            else -> error("Plugin generated non regular class as companion object")
        }
    }
}
