/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.isFinalClass
import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration

class JvmDefaultArgumentStubGenerator(override val context: JvmBackendContext) : DefaultArgumentStubGenerator(context, false, false) {
    override fun IrBlockBodyBuilder.selectArgumentOrDefault(
        defaultFlag: IrExpression,
        parameter: IrValueParameter,
        default: IrExpression
    ): IrValueDeclaration {
        // We have to generate precisely this code because that results in the bytecode the inliner expects;
        // see `expandMaskConditionsAndUpdateVariableNodes`. In short, the bytecode sequence should be
        //
        //     -- no loads of the parameter here, as after inlining its value will be uninitialized
        //     ILOAD <mask>
        //     ICONST <bit>
        //     IAND
        //     IFEQ Lx
        //     -- any code inserted here is removed if the call site specifies the parameter
        //     STORE <n>
        //     -- no jumps here
        //   Lx
        //
        // This control flow limits us to an if-then (without an else), and this together with the
        // restriction on loading the parameter in the default case means we cannot create any temporaries.
        +irIfThen(defaultFlag, irSet(parameter.symbol, default))
        return parameter
    }

    override fun defaultArgumentStubVisibility(function: IrFunction) = function.getJvmVisibilityOfDefaultArgumentStub()

    override fun useConstructorMarker(function: IrFunction): Boolean =
        function is IrConstructor || function.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR

    override fun IrBlockBodyBuilder.generateSuperCallHandlerCheckIfNeeded(
        irFunction: IrFunction,
        newIrFunction: IrFunction
    ) {
        if (irFunction !is IrSimpleFunction
            || !this@JvmDefaultArgumentStubGenerator.context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()
            || irFunction.isTopLevelDeclaration
            || (irFunction.parent as? IrClass)?.isFinalClass == true
        )
            return

        val handlerDeclaration = newIrFunction.valueParameters.last()
        +irIfThen(
            context.irBuiltIns.unitType,
            irNot(irEqualsNull(irGet(handlerDeclaration))),
            irCall(this@JvmDefaultArgumentStubGenerator.context.ir.symbols.throwUnsupportedOperationException).apply {
                putValueArgument(
                    0,
                    irString("Super calls with default arguments not supported in this target, function: ${irFunction.name.asString()}")
                )
            }
        )
    }
}
