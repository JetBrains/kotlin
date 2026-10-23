/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.shortening

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.getTagIfSubject
import org.jetbrains.kotlin.analysis.api.fir.references.KDocReferenceResolver
import org.jetbrains.kotlin.analysis.api.fir.utils.firSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.resolve.ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE

internal class KDocQualifiersToShortenCollector(
    private val analysisSession: KaFirSession,
    private val selection: TextRange,
    private val additionalImports: AdditionalImports,
    private val classShortenStrategy: (FirClassLikeSymbol<*>) -> ShortenStrategy,
    private val callableShortenStrategy: (FirCallableSymbol<*>) -> ShortenStrategy,
) : KtVisitorVoid() {
    val kDocQualifiersToShorten: MutableList<ShortenKDocQualifier> = mutableListOf()
    override fun visitElement(element: PsiElement) {
        if (!element.textRange.intersects(selection)) return

        if (!selection.contains(element.textRange) || element !is KDocName) {
            element.acceptChildren(this)
            return
        }

        if (element.getQualifier() == null) return

        val shouldShortenKDocQualifier = shouldShortenKDocQualifier(
            element,
            additionalImports,
            classShortenStrategy = { classShortenStrategy(it) },
            callableShortenStrategy = { callableShortenStrategy(it) },
        )
        if (shouldShortenKDocQualifier) {
            addKDocQualifierToShorten(element)
        } else {
            element.acceptChildren(this)

            if (element.getQualifier()?.getNameText() == ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE) {
                addKDocQualifierToShorten(element)
            }
        }
    }

    private fun shouldShortenKDocQualifier(
        kDocName: KDocName,
        additionalImports: AdditionalImports,
        classShortenStrategy: (FirClassLikeSymbol<*>) -> ShortenStrategy,
        callableShortenStrategy: (FirCallableSymbol<*>) -> ShortenStrategy,
    ): Boolean {
        val fqName = kDocName.getQualifiedNameAsFqName().dropFakeRootPrefixIfPresent()

        // KDocs are only shortened if they are available without imports, so `additionalImports` contain all the imports to add
        if (fqName.isInNewImports(additionalImports)) return true

        with(analysisSession) {
            val shortFqName = FqName.topLevel(fqName.shortName())
            val owner = kDocName.getContainingDoc().owner

            val contextElement = owner ?: kDocName.containingKtFile
            // There we have to calculate the resolution results for FQN and short name to then ensure that these sets of symbols are the same

            val containedTagSectionIfSubject = kDocName.getTagIfSubject()?.knownTag

            val resolvedSymbolsByShortName =
                KDocReferenceResolver.resolveKdocFqName(
                    useSiteSession,
                    selectedFqName = shortFqName,
                    fullFqName = shortFqName,
                    contextElement,
                    containedTagSectionIfSubject
                )
            val resolvedSymbolsByFQN =
                KDocReferenceResolver.resolveKdocFqName(
                    useSiteSession,
                    selectedFqName = fqName,
                    fullFqName = fqName,
                    contextElement,
                    containedTagSectionIfSubject
                )

            if (resolvedSymbolsByShortName != resolvedSymbolsByFQN) {
                // Shortening led to another set of symbols being available, so we cannot shorten the KDoc
                return false
            }

            val shortenStrategies = resolvedSymbolsByFQN.map { symbol ->
                when (symbol) {
                    is KaCallableSymbol -> callableShortenStrategy(symbol.firSymbol)
                    is KaClassLikeSymbol -> classShortenStrategy(symbol.firSymbol)
                    else -> ShortenStrategy.DO_NOT_SHORTEN
                }
            }

            val singleStrategy = shortenStrategies.distinct().singleOrNull() ?: return false
            return singleStrategy != ShortenStrategy.DO_NOT_SHORTEN
        }
    }

    private fun FqName.isInNewImports(additionalImports: AdditionalImports): Boolean =
        this in additionalImports.simpleImports || this.parent() in additionalImports.starImports

    private fun addKDocQualifierToShorten(kDocName: KDocName) {
        kDocQualifiersToShorten.add(ShortenKDocQualifier(kDocName))
    }
}

private fun FqName.dropFakeRootPrefixIfPresent(): FqName =
    tail(FqName(ROOT_PREFIX_FOR_IDE_RESOLUTION_MODE))
