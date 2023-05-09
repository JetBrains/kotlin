/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.extensions.FirSwitchableExtensionDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.extensions.generatedDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.scopes.impl.nestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.processClassifiersByName
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.fir.withFileAnalysisExceptionWrapping
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

class FirCompanionGenerationProcessor(
    session: FirSession,
    scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor(session, scopeSession, FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS) {
    override val transformer: FirTransformer<Nothing?> = FirCompanionGenerationTransformer(session)
}

class FirCompanionGenerationTransformer(val session: FirSession) : FirTransformer<Nothing?>() {
    private val generatedDeclarationProvider: FirSwitchableExtensionDeclarationsSymbolProvider? =
        session.generatedDeclarationsSymbolProvider

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        return element
    }

    override fun transformFile(file: FirFile, data: Nothing?): FirFile {
        // I don't want to use laziness here to prevent possible multi-threading problems
        if (generatedDeclarationProvider == null) return file
        return withFileAnalysisExceptionWrapping(file) {
            file.transformDeclarations(this, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): FirStatement {
        generateAndUpdateCompanion(regularClass)
        return regularClass.transformDeclarations(this, data)
    }

    fun generateAndUpdateCompanion(regularClass: FirRegularClass) {
        val companionSymbol = generateCompanion(regularClass)
        if (companionSymbol != null) {
            regularClass.replaceCompanionObjectSymbol(companionSymbol)
        }
    }

    private fun generateCompanion(regularClass: FirRegularClass): FirRegularClassSymbol? {
        if (generatedDeclarationProvider == null) return null
        val generatedCompanion = if (regularClass.isLocal) {
            var result: FirClassLikeSymbol<*>? = null
            session.nestedClassifierScope(regularClass)?.processClassifiersByName(DEFAULT_NAME_FOR_COMPANION_OBJECT) {
                if (it is FirClassLikeSymbol<*> && it.origin.generated) {
                    result = it
                }
            }

            result
        } else {
            val companionClassId = regularClass.classId.createNestedClassId(DEFAULT_NAME_FOR_COMPANION_OBJECT)
            generatedDeclarationProvider.getClassLikeSymbolByClassId(companionClassId)?.takeIf { it.origin.generated }
        }

        return when (generatedCompanion) {
            null -> null
            is FirRegularClassSymbol -> when {
                regularClass.companionObjectSymbol != null -> error("Plugin generated companion object for class $regularClass, but it is already present in class")
                else -> generatedCompanion
            }
            else -> error("Plugin generated non regular class as companion object")
        }
    }
}

fun <F : FirClassLikeDeclaration> F.runCompanionGenerationPhaseForLocalClass(session: FirSession): F {
    val transformer = FirCompanionGenerationTransformer(session)
    return this.transformSingle(transformer, null)
}
