/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FirIdeStructureElementDiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.providers.FirIdeProvider
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isGeneratedDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.ktDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.util.replaceFirst
import org.jetbrains.kotlin.psi.*

internal class FileStructureElementDiagnostics(
    private val map: Map<KtElement, List<Diagnostic>>
) {
    fun diagnosticsFor(element: KtElement): List<Diagnostic> = map[element] ?: emptyList()

    inline fun forEach(action: (List<Diagnostic>) -> Unit) = map.values.forEach(action)
}

internal sealed class FileStructureElement {
    abstract val firFile: FirFile
    abstract val psi: KtAnnotated
    abstract val mappings: Map<KtElement, FirElement>
    abstract val diagnostics: FileStructureElementDiagnostics
}

internal sealed class ReanalyzableStructureElement<KT : KtDeclaration> : FileStructureElement() {
    abstract override val psi: KtDeclaration
    abstract val firSymbol: AbstractFirBasedSymbol<*>
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
    ): ReanalyzableStructureElement<KT>

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics: FileStructureElementDiagnostics by lazy {
        FirIdeStructureElementDiagnosticsCollector.collectForSingleDeclaration(firFile, firSymbol.fir as FirDeclaration)
    }

    companion object {
        val recorder = FirElementsRecorder()
    }
}

internal class ReanalyzableFunctionStructureElement(
    override val firFile: FirFile,
    override val psi: KtNamedFunction,
    override val firSymbol: FirFunctionSymbol<*>,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtNamedFunction>() {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtNamedFunction,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzableFunctionStructureElement {
        val originalFunction = firSymbol.fir as FirSimpleFunction
        val newFunction = firIdeProvider.buildFunctionWithBody(newKtDeclaration, originalFunction) as FirSimpleFunction

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalFunction, newFunction) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newFunction,
                cache,
                FirResolvePhase.BODY_RESOLVE,
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
    override val firFile: FirFile,
    override val psi: KtProperty,
    override val firSymbol: FirPropertySymbol,
    override val timestamp: Long
) : ReanalyzableStructureElement<KtProperty>() {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    override fun reanalyze(
        newKtDeclaration: KtProperty,
        cache: ModuleFileCache,
        firLazyDeclarationResolver: FirLazyDeclarationResolver,
        firIdeProvider: FirIdeProvider,
    ): ReanalyzablePropertyStructureElement {
        val originalProperty = firSymbol.fir
        val newProperty = firIdeProvider.buildPropertyWithBody(newKtDeclaration, originalProperty)

        return FileStructureUtil.withDeclarationReplaced(firFile, cache, originalProperty, newProperty) {
            firLazyDeclarationResolver.lazyResolveDeclaration(
                newProperty,
                cache,
                FirResolvePhase.BODY_RESOLVE,
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
    override val firFile: FirFile,
    fir: FirDeclaration,
    override val psi: KtDeclaration,
) : FileStructureElement() {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(fir, recorder)

    override val diagnostics: FileStructureElementDiagnostics by lazy {
        var inCurrentDeclaration = false
        FirIdeStructureElementDiagnosticsCollector.collectForStructureElement(
            firFile,
            onDeclarationEnter = { firDeclaration ->
                when {
                    firDeclaration.isGeneratedDeclaration -> DiagnosticCollectorDeclarationAction.SKIP
                    firDeclaration is FirFile -> DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    firDeclaration == fir -> {
                        inCurrentDeclaration = true
                        DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    }
                    FileElementFactory.isReanalyzableContainer(firDeclaration.ktDeclaration) -> {
                        DiagnosticCollectorDeclarationAction.SKIP
                    }
                    inCurrentDeclaration -> {
                        DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    }
                    else -> DiagnosticCollectorDeclarationAction.SKIP_CURRENT_DECLARATION_AND_CHECK_NESTED
                }
            },
            onDeclarationExit = { firDeclaration ->
                if (firDeclaration == fir) {
                    inCurrentDeclaration = false
                }
            },
        )
    }

    companion object {
        private val recorder = object : FirElementsRecorder() {
            override fun visitProperty(property: FirProperty, data: MutableMap<KtElement, FirElement>) {
                val psi = property.psi as? KtProperty ?: return super.visitProperty(property, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || KtPsiUtil.isLocal(psi)) {
                    super.visitProperty(property, data)
                }
            }

            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
                val psi = simpleFunction.psi as? KtNamedFunction ?: return super.visitSimpleFunction(simpleFunction, data)
                if (!FileElementFactory.isReanalyzableContainer(psi) || KtPsiUtil.isLocal(psi)) {
                    super.visitSimpleFunction(simpleFunction, data)
                }
            }
        }
    }
}


internal data class RootStructureElement(
    override val firFile: FirFile,
    override val psi: KtFile,
) : FileStructureElement() {
    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firFile, recorder)

    override val diagnostics: FileStructureElementDiagnostics by lazy {
        FirIdeStructureElementDiagnosticsCollector.collectForStructureElement(firFile) { firDeclaration ->
            if (firDeclaration is FirFile) DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_SKIP_NESTED
            else DiagnosticCollectorDeclarationAction.SKIP
        }
    }

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
