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
import org.jetbrains.kotlin.diagnostics.*

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
            "Public-API {0} inline {1} accesses a non Public-API {2}. This can happen as a result of cascaded inlining of the following functions:\n{3}\n",
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
        buildString {
            inlinedFunctionBlocks.reversed().forEach { inlinedFunctionBlock ->
                val renderedFunction = inlinedFunctionBlock.inlinedFunctionSymbol?.owner
                    ?.dumpKotlinLike(
                        KotlinLikeDumpOptions(
                            bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES
                        )
                    )
                    ?.trim()
                    ?: "<unknown>"
                appendLine(renderedFunction)
            }
        }
}
