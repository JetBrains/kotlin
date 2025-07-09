/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirModuleResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLEmptyKotlinSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinSourceSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLKotlinSymbolProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.LLContainingClassCalculator
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirSymbolEntry
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@ThreadSafeMutableState
internal class LLFirProvider(
    val session: LLFirSession,
    private val moduleComponents: LLFirModuleResolveComponents,
    canContainKotlinPackage: Boolean,
    disregardSelfDeclarations: Boolean = false,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
) : FirProvider() {
    override val symbolProvider: LLKotlinSymbolProvider =
        if (!disregardSelfDeclarations) {
            LLKotlinSourceSymbolProvider(session, moduleComponents, canContainKotlinPackage, declarationProviderFactory)
        } else {
            LLEmptyKotlinSymbolProvider(session)
        }

    override val isPhasedFirAllowed: Boolean get() = true

    override fun getFirClassifierByFqName(classId: ClassId): FirClassLikeDeclaration? =
        symbolProvider.getClassLikeSymbolByClassId(classId)?.fir

    /**
     * @param classLikeDeclaration The [KtClassLikeDeclaration] must be contained in the module associated with this [LLFirProvider]. See
     *  [LLModuleSpecificSymbolProviderAccess] for details.
     */
    fun getFirClassifierByDeclaration(classLikeDeclaration: KtClassLikeDeclaration): FirClassLikeDeclaration? {
        val classId = classLikeDeclaration.getClassId() ?: return null

        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return symbolProvider.getClassLikeSymbolByPsi(classId, classLikeDeclaration)?.fir
    }

    override fun getFirClassifierContainerFile(fqName: ClassId): FirFile {
        return getFirClassifierContainerFileIfAny(fqName)
            ?: errorWithAttachment("Couldn't find container") {
                withEntry("classId", fqName.asString())
            }
    }

    override fun getFirClassifierContainerFileIfAny(fqName: ClassId): FirFile? {
        return getFirClassifierByFqName(fqName)?.let { moduleComponents.cache.getContainerFirFile(it) }
    }

    override fun getFirClassifierContainerFile(symbol: FirClassLikeSymbol<*>): FirFile {
        return getFirClassifierContainerFileIfAny(symbol)
            ?: errorWithAttachment("Couldn't find container") {
                withFirSymbolEntry("symbol", symbol)
            }
    }

    override fun getFirClassifierContainerFileIfAny(symbol: FirClassLikeSymbol<*>): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirCallableContainerFile(symbol: FirCallableSymbol<*>): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirScriptContainerFile(symbol: FirScriptSymbol): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    // TODO: implement
    override fun getFirScriptByFilePath(path: String): FirScriptSymbol? = null

    override fun getFirReplSnippetContainerFile(symbol: FirReplSnippetSymbol): FirFile? {
        return moduleComponents.cache.getContainerFirFile(symbol.fir)
    }

    override fun getFirFilesByPackage(fqName: FqName): List<FirFile> = error("Should not be called in FIR IDE")

    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName)
            ?: errorWithAttachment("Cannot compute the set of class names in the given package") {
                withEntry("packageFqName", fqName.asString())
            }

    override fun getContainingClass(symbol: FirBasedSymbol<*>): FirClassLikeSymbol<*>? {
        val psiResult = LLContainingClassCalculator.getContainingClassSymbol(symbol)
        if (psiResult != null) {
            return psiResult
        }

        return super.getContainingClass(symbol)
    }
}
