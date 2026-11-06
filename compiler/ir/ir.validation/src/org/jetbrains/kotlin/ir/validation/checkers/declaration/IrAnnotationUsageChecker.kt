/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Makes sure that all annotations of declarations are [IrAnnotation]s, not plain [IrConstructorCall]s.
 * This includes annotations passed as arguments of other annotations.
 */
object IrAnnotationUsageChecker : IrElementChecker<IrAnnotation>(IrAnnotation::class) {
    override fun check(element: IrAnnotation, context: CheckerContext) {
        element.acceptChildrenVoid(PlainConstructorCallFinder(element, context))
    }
}

private class PlainConstructorCallFinder(
    private val annotation: IrAnnotation,
    private val context: CheckerContext,
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitConstructorCall(expression: IrConstructorCall) {
        if (expression !is IrAnnotation) {
            context.error(
                annotation,
                IrAnnotationUsageChecker,
                "Annotation must be represented by IrAnnotation, not by a plain IrConstructorCall: ${expression.render()}"
            )
        }
        super.visitConstructorCall(expression)
    }
}
