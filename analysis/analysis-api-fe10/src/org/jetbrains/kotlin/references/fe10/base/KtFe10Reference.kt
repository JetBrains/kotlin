/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10.base

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisFacade.AnalysisMode
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaResolvableReferenceBridge
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.resolve.BindingContext

@KaImplementationDetail
@OptIn(KtImplementationDetail::class)
interface KtFe10Reference : KtReference, KaResolvableReferenceBridge {
    fun resolveToDescriptors(bindingContext: BindingContext): Collection<DeclarationDescriptor> = getTargetDescriptors(bindingContext)

    fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>

    override fun KaSession.resolveToSymbols(): Collection<KaSymbol> {
        this as KaFe10Session

        val bindingContext = analysisContext.analyze(element, AnalysisMode.PARTIAL)
        return getTargetDescriptors(bindingContext).mapNotNull { descriptor ->
            descriptor.toKtSymbol(analysisContext)
        }
    }

    fun isReferenceToImportAlias(alias: KtImportAlias): Boolean = false
}
