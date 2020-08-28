/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

// TODO merge with common `InitializersLowering` - needs some modifications for persistent IR
//   (a body lowering cannot create a new method)
class InstanceInitializersLowering(context: CommonBackendContext) : InitializersLoweringBase(context), ClassLoweringPass {
    object COMMON_INIT_STATEMENTS : IrDeclarationOriginImpl("COMMON_INIT_STATEMENTS", isSynthetic = true)

    override fun lower(irClass: IrClass) {
        val initializers = extractInitializers(irClass) {
            (it is IrField && !it.isStatic) || (it is IrAnonymousInitializer && !it.isStatic)
        }

        var copies = 0
        for (constructor in irClass.constructors) {
            constructor.body?.acceptChildrenVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) = element.acceptChildren(this, null)

                override fun visitDeclaration(declaration: IrDeclarationBase) = Unit

                override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall) {
                    copies++
                }
            })
        }

        val method = if (initializers.isNotEmpty() && copies > 1) {
            irClass.addFunction {
                name = Name.identifier("\$init")
                returnType = context.irBuiltIns.unitType
                origin = COMMON_INIT_STATEMENTS
                modality = Modality.FINAL
                visibility = Visibilities.PRIVATE
            }.apply {
                dispatchReceiverParameter = irClass.thisReceiver!!.copyTo(this, type = irClass.defaultType)
                body = context.irFactory.createBlockBody(irClass.startOffset, irClass.endOffset, initializers)
                    .patchDeclarationParents(this)
                    .transform(VariableRemapper(mapOf(irClass.thisReceiver!! to dispatchReceiverParameter!!)), null)
            }
        } else null
        for (constructor in irClass.constructors) {
            constructor.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement = declaration

                override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression =
                    if (method != null) {
                        IrCallImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, method.symbol, 0, 0).apply {
                            dispatchReceiver = IrGetValueImpl(expression.startOffset, expression.endOffset, irClass.thisReceiver!!.symbol)
                        }
                    } else {
                        IrBlockImpl(expression.startOffset, expression.endOffset, context.irBuiltIns.unitType, null, initializers)
                            .patchDeclarationParents(constructor)
                    }
            })
        }
    }
}
