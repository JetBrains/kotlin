/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.validation.checkers.declaration

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isAnnotationClass
import org.jetbrains.kotlin.ir.validation.checkers.IrElementChecker
import org.jetbrains.kotlin.ir.validation.checkers.checkAllAnnotationsAreIrAnnotations
import org.jetbrains.kotlin.ir.validation.checkers.context.CheckerContext

/**
 * Makes sure that all annotations in default values of annotation parameters (which are effectively annotation arguments)
 * are [IrAnnotation]s, not plain [IrConstructorCall]s.
 *
 * See also [IrAnnotationUsageChecker].
 */
object IrAnnotationParameterDefaultValueChecker : IrElementChecker<IrValueParameter>(IrValueParameter::class) {
    override fun check(element: IrValueParameter, context: CheckerContext) {
        if (!element.isAnnotationParameter) return
        element.defaultValue?.let { checkAllAnnotationsAreIrAnnotations(root = it, declaration = element, context) }
    }

    private val IrValueParameter.isAnnotationParameter: Boolean
        get() = kind == IrParameterKind.Regular && (parent as? IrConstructor)?.constructedClass?.isAnnotationClass == true
}
