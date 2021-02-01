/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.setDeclarationsParent
import org.jetbrains.kotlin.backend.common.lower.LocalClassPopupLowering
import org.jetbrains.kotlin.backend.common.lower.LocalDeclarationsLowering
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.*

/*
 Here we're extracting some local classes from inline bodies.
 The mental model of inlining is as following:
  - for inline lambdas, since we don't see the keyword `inline` at a callsite,
    it is logical to think that the lambda won't be copied but will be embedded as is at the callsite,
    so all local classes declared in those inline lambdas are NEVER COPIED.
  - as for the bodies of inline functions, then it is the opposite - we see the `inline` keyword,
    so it is only logical to think that this is a macro substitution, so the bodies of inline functions
    are copied. But the compiler could optimize the usage of some local classes and not copy them.
    So in this case all local classes MIGHT BE COPIED.
 */

class LocalClassesInInlineLambdasLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, allowDeclarationModification = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (!expression.symbol.owner.isInline)
                    return super.visitCall(expression)

                val localClasses = mutableSetOf<IrClass>()
                expression.acceptChildrenVoid(object : IrElementVisitorVoid {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitClass(declaration: IrClass) {
                        localClasses.add(declaration)
                    }
                })
                if (localClasses.isEmpty())
                    return expression

                LocalDeclarationsLowering(context).lower(expression, container, localClasses)

                expression.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitClass(declaration: IrClass): IrStatement {
                        return IrCompositeImpl(
                            declaration.startOffset, declaration.endOffset,
                            context.irBuiltIns.unitType
                        )
                    }
                })
                localClasses.forEach {
                    it.setDeclarationsParent(
                        currentDeclarationParent
                            ?: (container as? IrDeclarationParent)
                            ?: container.parent
                    )
                }
                return IrBlockImpl(expression.startOffset, expression.endOffset, expression.type).apply {
                    statements += localClasses
                    statements += expression
                }
            }
        })
    }
}

class LocalClassesInInlineFunctionsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, allowDeclarationModification = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return
        if (!function.isInline) return
        // Conservatively assume that functions with reified type parameters must be copied.
        if (function.typeParameters.any { it.isReified }) return

        val crossinlineParameters = function.valueParameters.filter { it.isCrossinline }.toSet()
        val classesToExtract = mutableSetOf<IrClass>()
        function.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                var canExtract = true
                if (crossinlineParameters.isNotEmpty()) {
                    declaration.acceptVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitGetValue(expression: IrGetValue) {
                            if (expression.symbol.owner in crossinlineParameters)
                                canExtract = false
                        }
                    })
                }
                if (canExtract)
                    classesToExtract.add(declaration)
            }
        })
        if (classesToExtract.isEmpty())
            return

        LocalDeclarationsLowering(context).lower(function, function, classesToExtract)
    }
}

class LocalClassesExtractionFromInlineFunctionsLowering(
    context: CommonBackendContext,
    recordExtractedLocalClasses: BackendContext.(IrClass) -> Unit = {},
) : LocalClassPopupLowering(context, recordExtractedLocalClasses) {
    private val classesToExtract = mutableSetOf<IrClass>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val function = container as? IrFunction ?: return
        if (!function.isInline) return
        // Conservatively assume that functions with reified type parameters must be copied.
        if (function.typeParameters.any { it.isReified }) return

        val crossinlineParameters = function.valueParameters.filter { it.isCrossinline }.toSet()

        function.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                var canExtract = true
                if (crossinlineParameters.isNotEmpty()) {
                    declaration.acceptVoid(object : IrElementVisitorVoid {
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitGetValue(expression: IrGetValue) {
                            if (expression.symbol.owner in crossinlineParameters)
                                canExtract = false
                        }
                    })
                }
                if (canExtract)
                    classesToExtract.add(declaration)
            }
        })
        if (classesToExtract.isEmpty())
            return

        super.lower(irBody, container)

        classesToExtract.clear()
    }

    override fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean {
        return classesToExtract.contains(klass)
    }
}