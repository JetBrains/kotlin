/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class IrConstDeclarationAnnotationTransformer(context: IrConstEvaluationContext) : IrConstAnnotationTransformer(context) {
    override fun visitAnnotations(element: IrElement) {
        element.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFile(declaration: IrFile) {
                transformAnnotations(declaration)
                super.visitFile(declaration)
            }

            override fun visitDeclaration(declaration: IrDeclarationBase) {
                return handleAsFakeOverrideIf(declaration is IrOverridableDeclaration<*> && declaration.isFakeOverride) {
                    transformAnnotations(declaration)
                    super.visitDeclaration(declaration)
                }
            }
        })
    }
}
