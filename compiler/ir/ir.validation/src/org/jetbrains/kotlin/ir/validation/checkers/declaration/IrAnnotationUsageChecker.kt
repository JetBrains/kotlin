/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.checkAllAnnotationsAreIrAnnotations
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

/**
 * Makes sure that all annotations of declarations are [IrAnnotation]s, not plain [IrConstructorCall]s.
 * This includes annotations passed as arguments of other annotations.
 *
 * See also [IrAnnotationParameterDefaultValueChecker].
 */
object IrAnnotationUsageChecker : IrElementChecker<IrDeclaration>(IrDeclaration::class) {
    override fun check(element: IrDeclaration, context: CheckerContext) {
        element.annotations.forEach { checkAllAnnotationsAreIrAnnotations(root = it, declaration = element, context) }
    }
}
