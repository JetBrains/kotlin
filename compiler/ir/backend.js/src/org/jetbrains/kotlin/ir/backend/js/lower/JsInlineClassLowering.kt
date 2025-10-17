/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.InlineClassLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.utils.memoryOptimizedFilter

class JsInlineClassLowering(context: JsIrBackendContext) : InlineClassLowering(context) {
    private val jsGeneratorAnnotationSymbol =
        context.symbols.jsGeneratorAnnotationSymbol.owner.primaryConstructor!!.symbol

    override fun processDelegatedInlineClassMember(declaration: IrDeclaration) {
        declaration.annotations = declaration.annotations.memoryOptimizedFilter {
            it.symbol != jsGeneratorAnnotationSymbol
        }
        super.processDelegatedInlineClassMember(declaration)
    }
}