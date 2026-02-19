/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.transformer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrOverridableDeclaration
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrTypeVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class IrConstTypeAnnotationTransformer(context: IrConstEvaluationContext) : IrConstAnnotationTransformer(context) {
    override fun visitAnnotations(element: IrElement) {
        element.acceptVoid(
            object : IrTypeVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    return handleAsFakeOverrideIf(element is IrOverridableDeclaration<*> && element.isFakeOverride) {
                        element.acceptChildrenVoid(this)
                    }
                }

                override fun visitType(container: IrElement, type: IrType) {
                    transformAnnotations(type)
                }
            }
        )
    }
}
