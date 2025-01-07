/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error3
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer

internal object IrInlinerErrors {
    val IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION by error3<PsiElement, IrDeclarationWithName, IrDeclarationWithName, IrDeclaration>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultSerializationErrorMessages)
    }
}

internal object KtDefaultSerializationErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            IrInlinerErrors.IR_PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION,
            "Inline {0} `{1}` accesses, possibly transitively, a declaration with narrower visibility: {2}",
            IrDiagnosticRenderers.DECLARATION_KIND,
            IrDiagnosticRenderers.DECLARATION_NAME,
            Renderer<IrDeclaration> { declaration ->
                DescriptorRenderer.FQ_NAMES_IN_TYPES.render(declaration.toIrBasedDescriptor())
            }
        )
    }
}
