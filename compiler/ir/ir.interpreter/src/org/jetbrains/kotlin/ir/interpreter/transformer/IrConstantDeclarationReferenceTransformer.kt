/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.Companion.IR_EXTERNAL_DECLARATION_STUB
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

/**
 * Processes referenced external declarations, evaluating their annotations.
 * During ordinary command-line compilation, declarations from other modules come as binaries, so all annotation arguments are
 * already evaluated. However, in the IDE, a code fragment or its context may reference declarations from other modules.
 * For them, we build [IR_EXTERNAL_DECLARATION_STUB]s with lazy annotations.
 * Unless we process their arguments, '@JvmName' and other special annotations won't work.
 */
internal class IrConstantDeclarationReferenceTransformer(context: IrConstEvaluationContext) : IrConstAnnotationTransformer(context) {
    override fun visitAnnotations(element: IrElement) {
        element.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitDeclarationReference(expression: IrDeclarationReference) {
                val referencedDeclaration = expression.symbol.owner
                if (referencedDeclaration is IrDeclaration && referencedDeclaration.origin == IR_EXTERNAL_DECLARATION_STUB) {
                    transformAnnotations(referencedDeclaration)
                }

                super.visitDeclarationReference(expression)
            }
        })
    }
}