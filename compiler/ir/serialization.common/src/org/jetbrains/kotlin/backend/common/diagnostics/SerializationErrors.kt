/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.diagnostics.SerializationDiagnosticRenderers.CONFLICTING_KLIB_SIGNATURES_DATA
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.deprecationError2
import org.jetbrains.kotlin.diagnostics.deprecationError3
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.expressions.IrInlinedFunctionBlock
import org.jetbrains.kotlin.ir.util.BodyPrintingStrategy
import org.jetbrains.kotlin.ir.util.KotlinLikeDumpOptions
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.MemberComparator

internal object SerializationErrors {
    val CONFLICTING_KLIB_SIGNATURES_ERROR by error1<PsiElement, ConflictingKlibSignaturesData>()

    val IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION by deprecationError2<PsiElement, IrDeclaration, IrDeclaration>(
        LanguageFeature.ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs,
    )

    val IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING by
    deprecationError3<PsiElement, IrDeclaration, IrDeclaration, List<IrInlinedFunctionBlock>>(
        LanguageFeature.ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs,
    )

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultSerializationErrorMessages)
    }
}

internal object KtDefaultSerializationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            SerializationErrors.CONFLICTING_KLIB_SIGNATURES_ERROR,
            "Platform declaration clash: {0}",
            CONFLICTING_KLIB_SIGNATURES_DATA,
        )
        map.put(
            SerializationErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
            "Public-API inline {0} accesses a non Public-API {1}",
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND,
        )
        map.put(
            SerializationErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION_CASCADING,
            "Public-API inline {0} accesses a non Public-API {1}. This could happen as a result of cascaded inlining of the following functions:\n{2}\n",
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_KIND_AND_NAME,
            Renderer<List<IrInlinedFunctionBlock>> { inlinedFunctionBlocks ->
                buildString {
                    inlinedFunctionBlocks.reversed().forEach { inlinedFunctionBlock ->
                        appendLine(
                            inlinedFunctionBlock.inlinedFunctionSymbol!!.owner.dumpKotlinLike(
                                KotlinLikeDumpOptions(
                                    bodyPrintingStrategy = BodyPrintingStrategy.NO_BODIES
                                )
                            ).trim()
                        )
                    }
                }
            }
        )
    }
}

internal object SerializationDiagnosticRenderers {
    val CONFLICTING_KLIB_SIGNATURES_DATA =
        CommonRenderers.renderConflictingSignatureData<DeclarationDescriptor, ConflictingKlibSignaturesData>(
            signatureKind = "IR",
            sortUsing = MemberComparator.INSTANCE,
            declarationRenderer = Renderer {
                DescriptorRenderer.WITHOUT_MODIFIERS.render(it)
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
}