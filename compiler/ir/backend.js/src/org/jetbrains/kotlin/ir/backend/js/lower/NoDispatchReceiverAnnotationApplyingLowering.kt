/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.ModuleLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.JsStandardClassIds

object NoDispatchReceiverAnnotationApplyingLowering : ModuleLoweringPass {
    override fun lower(irModule: IrModuleFragment) {
        RemoveDispatchArgumentPass.lower(irModule)
        RemoveDispatchParameterPass.lower(irModule)
    }

    private object RemoveDispatchArgumentPass : IrVisitorVoid(), BodyLoweringPass {
        override fun lower(irBody: IrBody, container: IrDeclaration) {
            irBody.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    val callee = expression.symbol.owner
                    if (callee.hasAnnotation(JsStandardClassIds.Annotations.JsNoDispatchReceiver)) {
                        // Has to be called before the corresponding parameter is removed.
                        expression.removeDispatchReceiver()
                    }
                    super.visitCall(expression)
                }
            })
        }
    }

    private object RemoveDispatchParameterPass : IrVisitorVoid(), BodyLoweringPass {
        override fun lower(irBody: IrBody, container: IrDeclaration) {
            irBody.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    val callee = expression.symbol.owner
                    if (callee.hasAnnotation(JsStandardClassIds.Annotations.JsNoDispatchReceiver)) {
                        callee.parameters = callee.parameters.filter { it.kind != IrParameterKind.DispatchReceiver }
                    }
                    super.visitCall(expression)
                }
            })
        }
    }
}