/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.lower.PrimaryConstructorLowering.SYNTHETIC_PRIMARY_CONSTRUCTOR
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Add ES6_INIT_BOX_PARAMETER for each constructor (except constructors
 * of inline, external and primitive (string, arrays) classes).
 * Uses: Inline class's constructor initialize field of outer class before
 * call `super`, but its impossible in ES6. ES6_INIT_BOX_PARAMETER - object,
 * that collect these values and pass to super constructor. In the last
 * constructor of delegation chain ES6_INIT_BOX_PARAMETER will be open
 * with `Object.assign(this, box)`
 *
 * Add ES6_RESULT_TYPE PARAMETER for each secondary ctor (except
 * constructors of external and primitive (string, arrays) classes).
 * Uses: Pass information about type of new object to `Reflect.construct()`
 *
 * Transform SYNTHETIC_PRIMARY_CONSTRUCTOR:
 * constructor() {
 *   //statements
 * }
 * ==>
 * constructor() {
 *   init(this)
 * }
 * init($this$) {
 *   //statements from ctor
 * }
 */
class ES6AddInternalParametersToConstructorPhase(val context: JsIrBackendContext) : BodyLoweringPass {
    object ES6_SYNTHETIC_PRIMARY_INIT_FUNCTION : IrDeclarationOriginImpl("ES6_SYNTHETIC_PRIMARY_INIT_FUNCTION")
    object ES6_INIT_BOX_PARAMETER : IrDeclarationOriginImpl("ES6_INIT_BOX_PARAMETER")
    object ES6_RESULT_TYPE_PARAMETER : IrDeclarationOriginImpl("ES6_RESULT_TYPE_PARAMETER")

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (!context.es6mode) return

        container.transform(CallSiteTransformer(), null)

        if (container !is IrConstructor) return

        if (!container.hasStrictSignature()) container.addInternalValueParameters()

        if (container.origin === SYNTHETIC_PRIMARY_CONSTRUCTOR) {
            createInitFunction(container)
            openInitializerBox(container)
        }
    }

    private fun IrConstructor.addInternalValueParameters() {
        addValueParameter("box", context.dynamicType, ES6_INIT_BOX_PARAMETER)

        if (!isPrimary) {
            addValueParameter("resultType", context.dynamicType, ES6_RESULT_TYPE_PARAMETER)
        }
    }

    private fun createInitFunction(constructor: IrConstructor) {
        val irClass = constructor.parentAsClass
        val initFunction = buildInitFunction(constructor, irClass)
        irClass.declarations += initFunction

        context.mapping.constructorToInitFunction[constructor] = initFunction

        initFunction.transformChildren(object : IrElementTransformerVoid() {
            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                declaration.parent = initFunction
                return declaration
            }
        }, null)
    }

    private fun openInitializerBox(constructor: IrConstructor) = with(constructor.body as IrBlockBody) {
        statements.clear()
        statements += JsIrBuilder.buildCall(context.intrinsics.jsOpenInitializerBox).also {
            it.putValueArgument(0, JsIrBuilder.buildGetValue(constructor.parentAsClass.thisReceiver!!.symbol))
            it.putValueArgument(1, JsIrBuilder.buildGetValue(constructor.valueParameters.last().symbol))
        }
    }

    private fun buildInitFunction(constructor: IrConstructor, irClass: IrClass): IrSimpleFunction {
        val functionName = "${irClass.name}_init"

        return context.jsIrDeclarationBuilder.buildFunction(
            functionName,
            context.irBuiltIns.unitType,
            irClass,
            Visibilities.PROTECTED,
            Modality.FINAL,
            constructor.isInline,
            constructor.isExternal,
            origin = ES6_SYNTHETIC_PRIMARY_INIT_FUNCTION
        ).apply {
            addValueParameter("\$this\$", context.dynamicType)

            body = JsIrBuilder.buildBlockBody(constructor.body?.statements ?: emptyList())

            transformChildren(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (expression.symbol.owner === constructor.parentAsClass.thisReceiver!!) {
                        with(expression) { IrGetValueImpl(startOffset, endOffset, type, valueParameters.single().symbol) }
                    } else {
                        expression
                    }
                }
            }, null)
        }
    }

    /**
     * Pass `null` for ES6_INIT_BOX_PARAMETER and ES6_RESULT_TYPE_PARAMETER
     */
    inner class CallSiteTransformer : IrElementTransformerVoid() {
        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            val constructor = expression.symbol.owner
            val parent = constructor.parentAsClass

            if (constructor.hasStrictSignature() || parent.defaultType.isAny()) return expression

            val newArgsCount = if (constructor.isPrimary) 1 else 2
            return expression.run {
                IrConstructorCallImpl(
                    startOffset,
                    endOffset,
                    type,
                    symbol,
                    typeArgumentsCount,
                    constructorTypeArgumentsCount,
                    valueArgumentsCount + newArgsCount
                ).also {
                    for (i in 0 until valueArgumentsCount) {
                        it.putValueArgument(i, getValueArgument(i))
                    }

                    it.putValueArgument(
                        valueArgumentsCount,
                        JsIrBuilder.buildNull(context.dynamicType)
                    )

                    if (!constructor.isPrimary) {
                        it.putValueArgument(
                            valueArgumentsCount + 1,
                            JsIrBuilder.buildNull(context.dynamicType)
                        )
                    }
                }
            }
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
            val constructor = expression.symbol.owner
            val parent = constructor.parentAsClass

            if (constructor.hasStrictSignature() || parent.defaultType.isAny()) return expression

            val newArgsCount = if (constructor.isPrimary) 1 else 2

            return expression.run {
                IrDelegatingConstructorCallImpl(
                    startOffset,
                    endOffset,
                    type,
                    symbol,
                    typeArgumentsCount,
                    valueArgumentsCount + newArgsCount
                ).also {
                    for (i in 0 until valueArgumentsCount) {
                        it.putValueArgument(i, getValueArgument(i))
                    }

                    it.putValueArgument(
                        valueArgumentsCount,
                        JsIrBuilder.buildNull(context.dynamicType)
                    )

                    if (!constructor.isPrimary) {
                        it.putValueArgument(
                            valueArgumentsCount + 1,
                            JsIrBuilder.buildNull(context.dynamicType)
                        )
                    }
                }
            }
        }
    }

    private fun IrConstructor.hasStrictSignature(): Boolean {
        val primitives = with(context.irBuiltIns) { primitiveArrays + stringClass }
        return with(parentAsClass) { isExternal || isInline || symbol in primitives }
    }
}
