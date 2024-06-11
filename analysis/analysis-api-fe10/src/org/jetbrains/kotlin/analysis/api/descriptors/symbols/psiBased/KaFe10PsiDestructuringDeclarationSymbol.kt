/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDestructuringDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.CanNotCreateSymbolPointerForLocalLibraryDeclarationException
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaPsiBasedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10PsiDestructuringDeclarationSymbol(
    override val psi: KtDestructuringDeclaration,
    private val analysisSession: KaFe10Session,
) : KaDestructuringDeclarationSymbol(), KaFe10PsiSymbol<KtDestructuringDeclaration, CallableDescriptor> {
    override val descriptor: CallableDescriptor? get() = null

    override val annotationsObject: Annotations by cached {
        val bindingContext = analysisContext.analyze(psi, AnalysisMode.PARTIAL)
        Annotations.create(
            psi.annotationEntries.mapNotNull { entry ->
                bindingContext[BindingContext.ANNOTATION, entry]
            }
        )
    }

    override val analysisContext: Fe10AnalysisContext get() = analysisSession.analysisContext

    override val entries: List<KaVariableSymbol>
        get() = withValidityAssertion {
            psi.entries.map { entry ->
                with(analysisSession) { entry.symbol }
            }
        }

    override fun createPointer(): KaSymbolPointer<KaDestructuringDeclarationSymbol> {
        KaPsiBasedSymbolPointer.createForSymbolFromSource<KaDestructuringDeclarationSymbol>(this)?.let { return it }
        throw CanNotCreateSymbolPointerForLocalLibraryDeclarationException(SpecialNames.DESTRUCT.asString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is KaFe10PsiDestructuringDeclarationSymbol && other.psi == this.psi
    }

    override fun hashCode(): Int = psi.hashCode()
}