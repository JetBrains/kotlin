/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.constructorFactory
import org.jetbrains.kotlin.ir.backend.js.defaultConstructorForReflection
import org.jetbrains.kotlin.ir.backend.js.needsBoxParameter
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.irEmpty
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.isOriginallyLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.superClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

private var IrSimpleFunction.replacementWithoutBoxParameter: IrSimpleFunction? by irAttribute(copyByDefault = false)

/**
 * Optimization: replaces constructors with the `box` parameter with constructors without the `box` parameter where possible.
 *
 * For example, transforms this:
 * ```kotlin
 * open class K {
 *   fun new_K_r03577_k$(x: Int, $box: K?): K {
 *     val $this: K = kotlin.js.createThis(this, $box)
 *     return $this
 *   }
 * }
 *
 * class A {
 *   fun new_A_3zfso4_k$(x: Int, $box: A?): A {
 *     val $this: A = this.new_K_r03577_k$(x, $box)
 *     return $this
 *   }
 * }
 * ```
 *
 * into this:
 * ```kotlin
 * open class K {
 *   fun new_K_r03577_k$(x: Int): K {
 *     val $this: K = kotlin.js.createThis(this, kotlin.js.VOID)
 *     return $this
 *   }
 * }
 *
 * class A {
 *   fun new_A_3zfso4_k$(x: Int): A {
 *     val $this: A = this.new_K_r03577_k$(x)
 *     return $this
 *   }
 * }
 * ```
 */
class ES6ConstructorBoxParameterOptimizationLowering(private val context: JsIrBackendContext) : FileLoweringPass {

    override fun lower(irFile: IrFile) {
        if (!context.es6mode) return
        irFile.transformChildren(
            object : IrElementTransformerVoid() {
                override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                    return super.visitSimpleFunction(declaration.getOrCreateReplacementWithoutBoxParameter() ?: declaration)
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    return super.visitCall(expression.replaceCalleeIfNeeded())
                }
            },
            null
        )
    }

    private fun IrSimpleFunction.getOrCreateReplacementWithoutBoxParameter(): IrSimpleFunction? {
        replacementWithoutBoxParameter?.let { return it }
        if (!isEs6ConstructorReplacement) return null
        val constructedClass = parentAsClass
        if (constructedClass.needsBoxParameter || valueParameters.none { it.isBoxParameter }) return null

        val original = this
        val newReplacement = factory.buildFun {
            updateFrom(original)
            name = original.name
            returnType = original.returnType
        }.apply {
            parent = original.parent
            copyAttributes(original)
            annotations = original.annotations
            typeParameters = original.typeParameters
            dispatchReceiverParameter = original.dispatchReceiverParameter
            extensionReceiverParameter = original.extensionReceiverParameter
            valueParameters = original.valueParameters.dropLastWhile { it.isBoxParameter }.compactIfPossible()
            body = original.body
        }

        original.replacementWithoutBoxParameter = newReplacement

        newReplacement.body?.transformChildren(
            object : IrElementTransformerVoid() {
                override fun visitWhen(expression: IrWhen): IrExpression =
                    if (expression.isBoxParameterDefaultResolution) {
                        irEmpty(context)
                    } else {
                        super.visitWhen(expression)
                    }

                override fun visitReturn(expression: IrReturn): IrExpression =
                    if (expression.returnTargetSymbol == original.symbol) {
                        IrReturnImpl(expression.startOffset, expression.endOffset, expression.type, newReplacement.symbol, expression.value)
                    } else {
                        super.visitReturn(expression)
                    }

                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.isSuperCallWithBoxParameter) {
                        expression.putValueArgument(expression.valueArgumentsCount - 1, context.getVoid())
                    }
                    return super.visitCall(expression)
                }
            },
            null
        )

        // Don't forget to update the cached default constructor for use in `KClass<*>.createInstance`.
        val defaultConstructor = constructedClass.defaultConstructorForReflection
        if (defaultConstructor != null && defaultConstructor.constructorFactory == original) {
            defaultConstructor.constructorFactory = newReplacement
        }

        return newReplacement
    }

    private val IrCall.isSuperCallWithBoxParameter: Boolean
        get() = symbol == context.intrinsics.jsCreateThisSymbol ||
                symbol == context.intrinsics.jsCreateExternalThisSymbol ||
                symbol.owner.isEs6ConstructorReplacement && symbol.owner.boxParameter != null

    private fun IrCall.replaceCalleeIfNeeded(): IrCall {
        val replacementWithBoxParameter = symbol.owner
        val replacementWithoutBoxParameter = replacementWithBoxParameter.getOrCreateReplacementWithoutBoxParameter()?.symbol
            ?: return this
        val original = this
        return IrCallImpl(
            startOffset,
            endOffset,
            type,
            replacementWithoutBoxParameter,
            typeArguments.size,
            origin,
            superQualifierSymbol,
        ).apply {
            copyAttributes(original)
            copyTypeArgumentsFrom(original)
            dispatchReceiver = original.dispatchReceiver
            extensionReceiver = original.extensionReceiver
            // Don't copy the `box` argument
            for (i in 0..<original.valueArgumentsCount - 1) {
                putValueArgument(i, original.getValueArgument(i))
            }
        }
    }
}

/**
 * Optimization: collects all constructors which require a box parameter.
 */
class ES6CollectConstructorsWhichNeedBoxParameters(private val context: JsIrBackendContext) : DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrClass) return null

        val hasSuperClass = declaration.superClass != null

        if (hasSuperClass && declaration.isInner) {
            declaration.markAsNeedsBoxParameter()
        } else if (hasSuperClass && declaration.isOriginallyLocal && declaration.containsCapturedValues()) {
            declaration.markAsNeedsBoxParameter()
        }

        return null
    }

    private fun IrClass.containsCapturedValues(): Boolean {
        if (superClass == null) return false

        declarations
            .filterIsInstanceAnd<IrFunction> { it.isEs6ConstructorReplacement }
            .forEach {
                var meetCapturing = false
                val boxParameter = it.boxParameter

                it.body?.acceptChildrenVoid(object : IrVisitorVoid() {
                    override fun visitSetField(expression: IrSetField) {
                        val receiver = expression.receiver as? IrGetValue
                        if (receiver != null && receiver.symbol == boxParameter?.symbol) {
                            meetCapturing = true
                        }
                        super.visitSetField(expression)
                    }
                })

                if (meetCapturing) return true
            }

        return false
    }

    private fun IrClass.markAsNeedsBoxParameter() {
        if (isExternal) return
        needsBoxParameter = true
        superClass?.markAsNeedsBoxParameter()
    }
}
