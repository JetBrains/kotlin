/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.getNonLocalContainingOrThisDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.FirFileBuilder
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.firIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.findSourceNonLocalFirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.hasExplicitTypeOrUnit
import org.jetbrains.kotlin.idea.fir.low.level.api.util.replaceFirst
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import java.util.concurrent.ConcurrentHashMap


internal class FileStructure(
    private val ktFile: KtFile,
    private val firFile: FirFile,
    private val firLazyDeclarationResolver: FirLazyDeclarationResolver,
    private val firFileBuilder: FirFileBuilder,
    private val moduleFileCache: ModuleFileCache
) {
    private val firIdeProvider = firFile.session.firIdeProvider

    private val structureElements = ConcurrentHashMap<KtAnnotated, FileStructureElement>()

    fun getStructureElementFor(element: KtElement): FileStructureElement {
        val container: KtAnnotated = element.getNonLocalContainingOrThisDeclaration() ?: element.containingKtFile
        return getStructureElementForDeclaration(container)
    }

    private fun getStructureElementForDeclaration(declaration: KtAnnotated): FileStructureElement {
        val structureElement = structureElements.compute(declaration) { _, structureElement ->
            when {
                structureElement == null -> createStructureElement(declaration)
                structureElement is WithInBlockModificationFileStructureElement && !structureElement.isUpToDate() -> {
                    createMappingsCopy(structureElement, declaration as KtNamedFunction)
                }
                else -> structureElement
            }
        }
        return structureElement
            ?: error("FileStructureElement for was not defined for \n${declaration.getElementTextInContext()}")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun getAllDiagnosticsForFile(): Collection<Diagnostic> {
        val containersForStructureElement = buildList {
            add(ktFile)
            ktFile.forEachDescendantOfType<KtDeclaration>(
                canGoInside = { psi -> psi !is KtFunction && psi !is KtValVarKeywordOwner }
            ) { declaration ->
                if (declaration.isStructureElementContainer()) {
                    add(declaration)
                }
            }
        }
        val structureElements = containersForStructureElement.map(::getStructureElementFor)
        return buildList {
            structureElements.forEach { it.diagnostics.forEach { diagnostics -> addAll(diagnostics) } }
        }
    }

    private fun replaceFunction(from: FirSimpleFunction, to: FirSimpleFunction) {
        val declarations = if (from.symbol.callableId.className == null) {
            firFile.declarations as MutableList<FirDeclaration>
        } else {
            val classLikeLookupTag = from.containingClass()
                ?: error("Class name should not be null for non-top-level & non-local declarations")
            val containingClass = classLikeLookupTag.toSymbol(firFile.session)?.fir as FirRegularClass
            containingClass.declarations as MutableList<FirDeclaration>
        }
        declarations.replaceFirst(from, to)
    }

    private fun createMappingsCopy(
        original: WithInBlockModificationFileStructureElement,
        containerKtFunction: KtNamedFunction
    ): WithInBlockModificationFileStructureElement {
        val newFunction = firIdeProvider.buildFunctionWithBody(containerKtFunction) as FirSimpleFunction
        val originalFunction = original.firSymbol.fir as FirSimpleFunction
        replaceFunction(originalFunction, newFunction)

        try {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newFunction,
                moduleFileCache,
                FirResolvePhase.BODY_RESOLVE,
                checkPCE = true,
                reresolveFile = true,
            )
            return WithInBlockModificationFileStructureElement(
                firFile,
                containerKtFunction,
                newFunction.symbol,
                containerKtFunction.modificationStamp,
            )
        } catch (e: Throwable) {
            replaceFunction(newFunction, originalFunction)
            throw e
        }
    }

    private fun createDeclarationStructure(declaration: KtDeclaration): FileStructureElement {
        val firDeclaration = declaration.findSourceNonLocalFirDeclaration(firFileBuilder, firIdeProvider.symbolProvider, moduleFileCache)
        firLazyDeclarationResolver.lazyResolveDeclaration(
            firDeclaration,
            moduleFileCache,
            FirResolvePhase.BODY_RESOLVE,
            checkPCE = true
        )
        return when {
            declaration is KtNamedFunction && declaration.hasExplicitTypeOrUnit -> {
                WithInBlockModificationFileStructureElement(
                    firFile,
                    declaration,
                    (firDeclaration as FirSimpleFunction).symbol,
                    declaration.modificationStamp,
                )
            }
            else -> {
                NonLocalDeclarationFileStructureElement(
                    firFile,
                    firDeclaration,
                    declaration,
                )
            }
        }
    }

    private fun createStructureElement(container: KtAnnotated): FileStructureElement = when (container) {
        is KtFile -> {
            val firFile = firFileBuilder.getFirFileResolvedToPhaseWithCaching(
                container,
                moduleFileCache,
                FirResolvePhase.IMPORTS,
                checkPCE = true
            )
            FileWithoutDeclarationsFileStructureElement(
                firFile,
                container,
            )
        }
        is KtDeclaration -> createDeclarationStructure(container)
        else -> error("Invalid container $container")
    }
}

private fun KtDeclaration.isStructureElementContainer(): Boolean {
    if (this !is KtClassOrObject && this !is KtDeclarationWithBody && this !is KtProperty && this !is KtTypeAlias) return false
    if (this is KtEnumEntry) return false
    if (containingClassOrObject is KtEnumEntry) return false
    return !KtPsiUtil.isLocal(this)
}


