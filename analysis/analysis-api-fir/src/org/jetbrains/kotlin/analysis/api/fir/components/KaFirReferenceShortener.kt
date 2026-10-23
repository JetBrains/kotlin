/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.util.TextRange
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.createSmartPointer
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.shortening.*
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsReferenceShortener
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class KaFirReferenceShortener(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsReferenceShortener {
    private val context by lazy { FirShorteningContext(analysisSession) }

    override fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy
    ): ShortenCommand = withPsiValidityAssertion(element) {
        collectPossibleReferenceShortenings(
            element.containingKtFile,
            element.textRange,
            shortenOptions,
            classShortenStrategy,
            callableShortenStrategy
        )
    }

    override fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy
    ): ShortenCommand = withPsiValidityAssertion(file) {
        require(!file.isCompiled) { "No sense to collect references for shortening in compiled file $file" }

        val declarationToVisit = file.findSmallestElementOfTypeContainingSelection<KtDeclaration>(selection)
            ?: file

        val firDeclaration = declarationToVisit.getCorrespondingFirElement() ?: return ShortenCommandImpl(
            file.createSmartPointer(),
            importsToAdd = emptySet(),
            starImportsToAdd = emptySet(),
            listOfTypeToShortenInfo = emptyList(),
            listOfQualifierToShortenInfo = emptyList(),
            thisLabelsToShorten = emptyList(),
            kDocQualifiersToShorten = emptyList(),
        )

        val towerContext = FirTowerDataContextProvider.create(resolutionFacade, declarationToVisit)

        //TODO: collect all usages of available symbols in the file and prevent importing symbols that could introduce name clashes, which
        // may alter the meaning of existing code.
        val collector = ElementsToShortenCollector(
            shortenOptions,
            context,
            towerContext,
            file,
            selection,
            classShortenStrategy = { classShortenStrategy(buildSymbol(it) as KaClassLikeSymbol) },
            callableShortenStrategy = { callableShortenStrategy(buildSymbol(it) as KaCallableSymbol) },
            resolutionFacade,
        )
        firDeclaration.accept(CollectingVisitor(collector))

        val additionalImports = AdditionalImports(
            collector.getNamesToImport(starImport = false).toSet(),
            collector.getNamesToImport(starImport = true).toSet(),
        )

        val kDocCollector = KDocQualifiersToShortenCollector(
            analysisSession,
            selection,
            additionalImports,
            classShortenStrategy = {
                minOf(classShortenStrategy(buildSymbol(it) as KaClassLikeSymbol), ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED)
            },
            callableShortenStrategy = {
                minOf(callableShortenStrategy(buildSymbol(it) as KaCallableSymbol), ShortenStrategy.SHORTEN_IF_ALREADY_IMPORTED)
            },
        )
        kDocCollector.visitElement(declarationToVisit)

        return ShortenCommandImpl(
            file.createSmartPointer(),
            additionalImports.simpleImports,
            additionalImports.starImports,
            collector.typesToShorten.distinctBy { it.element }.map { TypeToShortenInfo(it.element.createSmartPointer(), it.shortenedRef) },
            collector.qualifiersToShorten.distinctBy { it.element }.map { QualifierToShortenInfo(it.element.createSmartPointer(), it.shortenedRef) },
            collector.labelsToShorten.distinctBy { it.element }.map { ThisLabelToShortenInfo(it.element.createSmartPointer()) },
            kDocCollector.kDocQualifiersToShorten.distinctBy { it.element }.map { it.element.createSmartPointer() },
        )
    }

    private fun KtElement.getCorrespondingFirElement(): FirElement? {
        require(this is KtFile || this is KtDeclaration)

        val firElement = getOrBuildFir(resolutionFacade)

        return when (firElement) {
            is FirDeclaration -> firElement
            is FirAnonymousFunctionExpression -> firElement.anonymousFunction
            is FirFunctionTypeParameter -> firElement
            else -> null
        }
    }

    private fun buildSymbol(firSymbol: FirBasedSymbol<*>): KaSymbol = analysisSession.firSymbolBuilder.buildSymbol(firSymbol)
}

private inline fun <reified T : KtElement> KtFile.findSmallestElementOfTypeContainingSelection(selection: TextRange): T? =
    findElementAt(selection.startOffset)
        ?.parentsOfType<T>(withSelf = true)
        ?.firstOrNull { selection in it.textRange }

private class ShortenCommandImpl(
    override val targetFile: SmartPsiElementPointer<KtFile>,
    override val importsToAdd: Set<FqName>,
    override val starImportsToAdd: Set<FqName>,
    override val listOfTypeToShortenInfo: List<TypeToShortenInfo>,
    override val listOfQualifierToShortenInfo: List<QualifierToShortenInfo>,
    override val thisLabelsToShorten: List<ThisLabelToShortenInfo>,
    override val kDocQualifiersToShorten: List<SmartPsiElementPointer<KDocName>>,
) : ShortenCommand
