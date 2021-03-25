/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileDiagnosticRetriever
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FileStructureElementDiagnostics
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.SingleNonLocalDeclarationDiagnosticRetriever
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.FirTowerDataContextCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.hasFqName
import org.jetbrains.kotlin.psi.*

internal sealed class FileStructureElement(val firFile: FirFile) {
    abstract val psi: KtAnnotated
    abstract val mappings: Map<KtElement, FirElement>
    abstract val diagnostics: FileStructureElementDiagnostics
}

internal sealed class ReanalyzableStructureElement<KT : KtDeclaration, S: AbstractFirBasedSymbol<*>>(
    firFile: FirFile,
    protected val firSymbol: S,
) : FileStructureElement(firFile) {
    abstract override val psi: KtDeclaration
    abstract val timestamp: Long

    /**
     * Creates new declaration by [newKtDeclaration] which will serve as replacement of [firSymbol]
     * Also, modify [firFile] & replace old version of declaration with a new one
     */
    abstract fun reanalyze(
        newKtDeclaration: KT,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
        towerDataContextCollector: FirTowerDataContextCollector,
    ): ReanalyzableStructureElement<KT, S>

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics = FileStructureElementDiagnostics(firFile, SingleNonLocalDeclarationDiagnosticRetriever(firSymbol.fir as FirDeclaration))

    companion object {
        val recorder = FirElementsRecorder()
    }
}

internal class ReanalyzableFunctionStructureElement(
    firFile: FirFile,
    override val psi: KtNamedFunction,
    firSymbol: FirFunctionSymbol<*>,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtNamedFunction, FirFunctionSymbol<*>>(firFile, firSymbol) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtNamedFunction,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
        towerDataContextCollector: FirTowerDataContextCollector,
    ): ReanalyzableFunctionStructureElement {
        val originalFunction = firSymbol.fir as FirSimpleFunction
        val newFunction = firIdeProvider.buildFunctionWithBody(newKtDeclaration, originalFunction) as FirSimpleFunction

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalFunction, newFunction) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newFunction,
                cache,
                FirResolvePhase.BODY_RESOLVE,
                towerDataContextCollector,
                checkPCE = true,
                reresolveFile = true,
            )
            cache.firFileLockProvider.withReadLock(firFile) {
                ReanalyzableFunctionStructureElement(
                    firFile,
                    newKtDeclaration,
                    newFunction.symbol,
                    newKtDeclaration.modificationStamp,
                )
            }
        }
    }
}

internal class ReanalyzablePropertyStructureElement(
    firFile: FirFile,
    override val psi: KtProperty,
    firSymbol: FirPropertySymbol,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtProperty, FirPropertySymbol>(firFile, firSymbol) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtProperty,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
        towerDataContextCollector: FirTowerDataContextCollector,
    ): ReanalyzablePropertyStructureElement {
        val originalProperty = firSymbol.fir
        val newProperty = firIdeProvider.buildPropertyWithBody(newKtDeclaration, originalProperty)

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalProperty, newProperty) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newProperty,
                cache,
                FirResolvePhase.BODY_RESOLVE,
                towerDataContextCollector,
                checkPCE = true,
                reresolveFile = true,
            )
            cache.firFileLockProvider.withReadLock(firFile) {
                ReanalyzablePropertyStructureElement(
                    firFile,
                    newKtDeclaration,
                    newProperty.symbol,
                    newKtDeclaration.modificationStamp,
                )
            }
        }
    }
}

internal class NonReanalyzableDeclarationStructureElement(
    firFile: FirFile,
    fir: FirDeclaration,
    override val psi: KtDeclaration,
) : FileStructureElement(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(fir, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, SingleNonLocalDeclarationDiagnosticRetriever(fir))


    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
                val psi = property.psi as? KtProperty ?: return super.visitProperty(property, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || !psi.hasFqName()) {
                    super.visitProperty(property, data)
                }
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
                val psi = simpleFunction.psi as? KtNamedFunction ?: return super.visitSimpleFunction(simpleFunction, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || !psi.hasFqName()) {
                    super.visitSimpleFunction(simpleFunction, data)
                }
            }
        }
    }
}


internal class RootStructureElement(
    firFile: FirFile,
    override val psi: KtFile,
) : FileStructureElement(firFile) {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firFile, recorder)

    override val diagnostics = FileStructureElementDiagnostics(firFile, FileDiagnosticRetriever)

    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitElement(element: FirElement, data: MutableMap<KtElement, FirElement>) {
                if (element !is FirDeclaration || element is FirFile) {
                    super.visitElement(element, data)
                }
            }
        }
    }
}
