/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationDiagnosticRenderers.CONFLICTING_KLIB_SIGNATURES_DATA
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.strongWarningWithoutSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBasedDeclarationDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.MemberComparator

object SerializationErrors : KtDiagnosticsContainer() {
    val CONFLICTING_KLIB_SIGNATURES_ERROR by error1<PsiElement, ConflictingKlibSignaturesData>()
    val KLIB_LOADING_ERROR by errorWithoutSource()
    val KLIB_LOADING_WARNING by strongWarningWithoutSource()
    val KLIB_LOADING_INFO = KtSourcelessDiagnosticFactory("KLIB_LOADING_INFO", Severity.INFO, getRendererFactory())

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultSerializationErrorMessages
    }
}

internal object KtDefaultSerializationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            SerializationErrors.CONFLICTING_KLIB_SIGNATURES_ERROR,
            "Platform declaration clash: {0}",
            CONFLICTING_KLIB_SIGNATURES_DATA,
        )
        map.put(SerializationErrors.KLIB_LOADING_ERROR, "{0}")
        map.put(SerializationErrors.KLIB_LOADING_WARNING, "{0}")
        map.put(SerializationErrors.KLIB_LOADING_INFO, "{0}")
    }
}

internal object SerializationDiagnosticRenderers {
    val CONFLICTING_KLIB_SIGNATURES_DATA =
        CommonRenderers.renderConflictingSignatureData<DeclarationDescriptor, ConflictingKlibSignaturesData>(
            signatureKind = "IR",
            sortUsing = MemberComparator.INSTANCE,
            declarationRenderer = Renderer { descriptor ->
                getDeclarationSymbol(descriptor)?.let {
                    FirDiagnosticRenderers.SYMBOL_WITH_LOCATION.render(it).ifEmpty { null }
                } ?: DescriptorRenderer.WITHOUT_MODIFIERS.render(descriptor)
            },
            renderSignature = { append(it.signature.render()) },
            declarations = { it.declarations.map(IrDeclaration::toIrBasedDescriptor) },
            declarationKind = { data ->
                when {
                    data.declarations.all { it is IrSimpleFunction } -> "functions"
                    data.declarations.all { it is IrProperty } -> "properties"
                    data.declarations.all { it is IrField } -> "fields"
                    else -> "declarations"
                }
            },
        )

    private fun getDeclarationSymbol(descriptor: DeclarationDescriptor): DeclarationSymbolMarker? {
        val irDeclaration = (descriptor as? IrBasedDeclarationDescriptor<*>)?.owner
        val metadata = (irDeclaration as? IrMetadataSourceOwner)?.metadata
        return (metadata as? DeclarationSymbolOwner)?.symbol
    }
}
