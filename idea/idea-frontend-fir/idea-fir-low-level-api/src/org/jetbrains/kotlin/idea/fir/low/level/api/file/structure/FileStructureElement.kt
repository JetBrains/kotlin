/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.file.structure

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.collectors.DiagnosticCollectorDeclarationAction
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics.FirIdeStructureElementDiagnosticsCollector
import org.jetbrains.kotlin.idea.fir.low.level.api.util.hasExplicitTypeOrUnit
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfTypeTo
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

internal class WithInBlockModificationFileStructureElement(
    override val firFile: FirFile,
    override val psi: KtFunction,
    val firSymbol: FirFunctionSymbol<*>,
    val timestamp: Long
) : FileStructureElement() {

    override val mappings: Map<KtElement, FirElement> =
        FirElementsRecorder.recordElementsFrom(firSymbol.fir, recorder)

    fun isUpToDate(): Boolean = psi.getModificationStamp() == timestamp

    override val diagnostics: FileStructureElementDiagnostics by lazy {
        var inCurrentDeclaration = false

        FirIdeStructureElementDiagnosticsCollector.collectForStructureElement(
            firFile,
            onDeclarationEnter = { firDeclaration ->
                when {
                    firDeclaration == firSymbol.fir -> {
                        inCurrentDeclaration = true
                        DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    }
                    inCurrentDeclaration -> DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    else -> DiagnosticCollectorDeclarationAction.SKIP_CURRENT_DECLARATION_AND_CHECK_NESTED
                }
            },
            onDeclarationExit = { declaration ->
                if (declaration == firSymbol.fir) {
                    inCurrentDeclaration = false
                }
            }
        )
    }

    companion object {
        private val recorder = FirElementsRecorder()
    }
}

internal class NonLocalDeclarationFileStructureElement(
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
                    firDeclaration == fir -> {
                        inCurrentDeclaration = true
                        DiagnosticCollectorDeclarationAction.CHECK_CURRENT_DECLARATION_AND_CHECK_NESTED
                    }
                    (firDeclaration.psi as? KtNamedFunction)?.hasExplicitTypeOrUnit == true -> {
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
            override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: MutableMap<KtElement, FirElement>) {
                val psi = simpleFunction.psi as? KtNamedFunction ?: return super.visitSimpleFunction(simpleFunction, data)
                if (!psi.hasExplicitTypeOrUnit || KtPsiUtil.isLocal(psi)) {
                    super.visitSimpleFunction(simpleFunction, data)
                }
            }
        }
    }
}


internal data class FileWithoutDeclarationsFileStructureElement(
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
