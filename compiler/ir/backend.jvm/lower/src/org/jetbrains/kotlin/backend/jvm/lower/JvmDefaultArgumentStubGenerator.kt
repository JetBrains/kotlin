/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultArgumentStubGenerator
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.getJvmVisibilityOfDefaultArgumentStub
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.isTopLevelDeclaration
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds

@PhasePrerequisites(
    JvmLocalDeclarationsLowering::class,
    // @JvmExposeBoxed should already be processed
    JvmInlineClassLowering::class,
)
internal class JvmDefaultArgumentStubGenerator(context: JvmBackendContext) : DefaultArgumentStubGenerator<JvmBackendContext>(
    context = context,
    factory = JvmDefaultArgumentFunctionFactory(context),
    skipInlineMethods = false,
    skipExternalMethods = false
) {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction && declaration.hasAnnotation(JvmStandardClassIds.JVM_EXPOSE_BOXED_ANNOTATION_FQ_NAME)) return null
        val lowered = super.transformFlat(declaration) ?: return null
        if (lowered.size != 2) return lowered
        val stub = lowered[1] as? IrFunction ?: return lowered

        stub.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.origin == JvmLoweredStatementOrigin.DEFAULT_STUB_CALL_TO_IMPLEMENTATION) {
                    val callee = expression.symbol.owner
                    for (i in 0 until expression.arguments.size) {
                        val argument = expression.arguments[i] ?: continue
                        val parameter = callee.parameters[i]
                        if (!argument.type.isSubtypeOf(parameter.type, context.typeSystem) || argument.type.isInlineClassType()) {
                            // KT-78051: When an argument has an inline class type, IrInlineDefaultCodegen cannot be used
                            // because it inlines the body verbatim without type coercion. The callee may expect
                            // the underlying type (unboxed) while the argument remains boxed, causing VerifyError.
                            // Clearing the origin forces IrInlineCodegen to handle unboxing.
                            // The subtype check handles other potential type mismatches.
                            expression.origin = null
                            break
                        }
                    }
                }
                return super.visitCall(expression)
            }
        })

        return lowered
    }

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
            || !this@JvmDefaultArgumentStubGenerator.context.shouldGenerateHandlerParameterForDefaultBodyFun
            || irFunction.isTopLevelDeclaration
            || (irFunction.parent as? IrClass)?.isFinalClass == true
        )
            return

        val handlerDeclaration = newIrFunction.parameters.last()
        +irIfThen(
            context.irBuiltIns.unitType,
            irNot(irEqualsNull(irGet(handlerDeclaration))),
            irCall(this@JvmDefaultArgumentStubGenerator.context.symbols.throwUnsupportedOperationException).apply {
                arguments[0] = irString(
                    "Super calls with default arguments not supported in this target, function: ${irFunction.name.asString()}"
                )
            }
        )
    }

    // Since the call to the underlying implementation in a default stub has different inlining behavior we need to mark it.
    override fun getOriginForCallToImplementation() = JvmLoweredStatementOrigin.DEFAULT_STUB_CALL_TO_IMPLEMENTATION
}
