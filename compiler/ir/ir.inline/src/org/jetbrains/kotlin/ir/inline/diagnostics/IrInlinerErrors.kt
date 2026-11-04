/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline.diagnostics

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.util.BodyPrintingStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import kotlin.getValue
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.DecorationHeader
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.LogicalLocation
import org.jetbrains.kotlin.fir.analysis.diagnostics.RenderedLineWithNumber
import org.jetbrains.kotlin.fir.analysis.diagnostics.renderLocationDecoration
import org.jetbrains.kotlin.fir.analysis.diagnostics.renderWithLocation
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.getSourceFile
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.sourceElement
import java.nio.file.Files.lines

object IrInlinerErrors : KtDiagnosticsContainer() {
    val IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION by
    deprecationError3<PsiElement, Visibility, IrDeclaration, IrDeclaration>(
        LanguageFeature.ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs,
    )

    val IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING by
    deprecationError4<PsiElement, Visibility, IrDeclaration, IrDeclaration, List<IrInlinedFunctionBlock>>(
        LanguageFeature.ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs,
    )

    val IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION by
    deprecationError3<PsiElement, Visibility, IrDeclaration, IrDeclaration>(
        LanguageFeature.ForbidExposingLessVisibleTypesInInline,
    )

    val IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION_CASCADING by
    deprecationError4<PsiElement, Visibility, IrDeclaration, IrDeclaration, List<IrInlinedFunctionBlock>>(
        LanguageFeature.ForbidExposingLessVisibleTypesInInline,
    )

    val IR_SYNTHETIC_ACCESSOR_LOWERING_WARNING by warning1<PsiElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultSerializationErrorMessages
    }
}

internal object KtDefaultSerializationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            IrInlinerErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
            "Public-API {0} inline {1} accesses a non Public-API {2}.",
            KtDiagnosticRenderers.VISIBILITY,
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND,
        )
        map.put(
            IrInlinerErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING,
            "Public-API {0} inline {1} accesses a non Public-API {2}. This can happen as a result of cascaded inlining of the following functions:\n{3}",
            KtDiagnosticRenderers.VISIBILITY,
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND_AND_NAME,
            Renderer<List<IrInlinedFunctionBlock>>(::renderCascadingInlining)
        )
        map.put(
            IrInlinerErrors.IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION,
            "Public-API {0} inline {1} references a non Public-API {2}.",
            KtDiagnosticRenderers.VISIBILITY,
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND,
        )
        map.put(
            IrInlinerErrors.IR_PRIVATE_CALLABLE_REFERENCED_BY_NON_PRIVATE_INLINE_FUNCTION_CASCADING,
            "Public-API {0} inline {1} references a non Public-API {2}. This can happen as a result of cascaded inlining of the following functions:\n{3}\n",
            KtDiagnosticRenderers.VISIBILITY,
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND_AND_NAME,
            Renderer<List<IrInlinedFunctionBlock>>(::renderCascadingInlining)
        )
        map.put(IrInlinerErrors.IR_SYNTHETIC_ACCESSOR_LOWERING_WARNING, "{0}", KtDiagnosticRenderers.TO_STRING)
    }

    private fun renderCascadingInlining(inlinedFunctionBlocks: List<IrInlinedFunctionBlock>) =
        inlinedFunctionBlocks.reversed().map { inlinedFunctionBlock ->
            val owner = inlinedFunctionBlock.inlinedFunctionSymbol?.owner
            val metadata = owner?.metadata
            when (metadata) {
                is FirMetadataSource -> metadata.fir.symbol.renderWithLocation(FirDiagnosticRenderers.SYMBOL_WITH_ALL_MODIFIERS::render)
                else -> inlinedFunctionBlock.inlinedFunctionSymbol?.owner
                    ?.dumpKotlinLike(
                        KotlinLikeDumpOptions(
                            bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES
                        )
                    )
                    ?.trim()
                    ?.let {
                        val location = owner?.getPackageFragment()
                            ?.let { LogicalLocation(it.packageFqName.asString(), it.module.name) }
                        // Likely `KtOffsetsOnlySourceElement`, so it's redundant?
                        val sourceElement = owner?.sourceElement() as? KtSourceElement
                        val startOffset = sourceElement?.let { getTightOffsetsFor(sourceElement).first }
                        val line = when {
                            owner != null && startOffset != null -> owner.getSourceFile()?.getLineNumber(startOffset)
                            else -> null
                        }
                        val indent = generateSequence(owner?.parentClassOrNull) { it.parentClassOrNull }.count()
                        renderLocationDecoration(
                            header = location?.let { DecorationHeader.Candidates(it, owner.file.path) }
                                ?: DecorationHeader.None,
                            renderedLines = it.lines().map { RenderedLineWithNumber(number = null, indent, renderedLine = it) }
                                .let { it.first().copy(number = line).let(::listOf) + it.drop(1) }
                        )
                    }
                    ?: "<unknown>"
            }
        }.joinToString("\n")
}
