/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.getContainingClassLookupTag
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirProvider : FirSessionComponent {
    /**
     * [symbolProvider] for [FirProvider] may provide only symbols from sources of current module
     */
    abstract val symbolProvider: FirSymbolProvider

    open val isPhasedFirAllowed: Boolean get() = false

    abstract fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration?

    abstract fun getFirClassifierContainerFile(fqName: ClassId): FirFile

    abstract fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile?

    open fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile =
        getFirClassifierContainerFile(symbol.classId)

    open fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? =
        getFirClassifierContainerFileIfAny(symbol.classId)

    abstract fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile?

    abstract fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile?

    abstract fun getFirScriptByFilePath(path: String): FirScriptSymbol?

    abstract fun getFirReplSnippetContainerFile(symbol: FirReplSnippetSymbol): FirFile?

    abstract fun getFirFilesByPackage(fqName: FqName): List<FirFile>

    abstract fun getClassNamesInPackage(fqName: FqName): Set<Name>

    /**
     * Returns a containing class symbol for the given [symbol].
     * Returns `null` for top-level and local declarations.
     *
     * Note that the behavior of [getContainingClass] is different in the IDE (see the `LLFirProvider`).
     * There, a containing class is first looked up using the source PSI to deal with class redeclarations.
     */
    open fun getContainingClass(symbol: FirBasedSymbol<*>): FirClassLikeSymbol<*>? {
        return when (symbol) {
            is FirCallableSymbol<*> -> symbol.containingClassLookupTag()?.toSymbol(symbol.moduleData.session)
            is FirClassLikeSymbol<*> -> symbol.getContainingClassLookupTag()?.toSymbol(symbol.moduleData.session)
            is FirAnonymousInitializerSymbol -> symbol.containingDeclarationSymbol as? FirClassLikeSymbol<*>
            is FirDanglingModifierSymbol -> symbol.containingClassLookupTag()?.toSymbol(symbol.moduleData.session)
            else -> null
        }
    }
}

val FirSession.firProvider: FirProvider by FirSession.sessionComponentAccessor()
