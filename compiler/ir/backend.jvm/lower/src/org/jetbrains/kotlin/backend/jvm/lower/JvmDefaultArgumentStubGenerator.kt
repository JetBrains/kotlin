/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration

class JvmDefaultArgumentStubGenerator(context: JvmBackendContext) : DefaultArgumentStubGenerator<JvmBackendContext>(
    context = context,
    factory = JvmDefaultArgumentFunctionFactory(context),
    skipInlineMethods = false,
    skipExternalMethods = false
) {
    override fun defaultArgumentStubVisibility(function: IrFunction) = function.getJvmVisibilityOfDefaultArgumentStub()

    override fun useConstructorMarker(function: IrFunction): Boolean =
        function is IrConstructor ||
                function.origin == JvmLoweredDeclarationOrigin.STATIC_INLINE_CLASS_CONSTRUCTOR ||
                function.origin == JvmLoweredDeclarationOrigin.STATIC_MULTI_FIELD_VALUE_CLASS_CONSTRUCTOR

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

    // Since the call to the underlying implementation in a default stub has different inlining behavior we need to mark it.
    override fun getOriginForCallToImplementation() = JvmLoweredStatementOrigin.DEFAULT_STUB_CALL_TO_IMPLEMENTATION
}
