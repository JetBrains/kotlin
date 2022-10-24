/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.backend.js.utils.getVoid
import org.jetbrains.kotlin.ir.backend.js.utils.isInnerClassSuperType
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irEqeqeqWithoutBox
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ES6InnerClassesLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    private val preinitHandlerDecl get() = context.irBuiltIns.functionN(2)
    private val IrConstructor.preinitHandlerType
        get() = preinitHandlerDecl.typeWith(
            parentAsClass.defaultType,
            context.dynamicType,
            context.irBuiltIns.unitType
        )

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (!context.es6mode || declaration !is IrConstructor) return null

        val parentClass = declaration.parentAsClass
        val superClass = parentClass.superClass
        val isInnerClassSuperType = parentClass.isInnerClassSuperType()

        return when {
            parentClass.isInner && superClass != null -> declaration.patchInnerClassConstructor()
            isInnerClassSuperType && superClass != null -> declaration.patchInBetweenConstructor()
            isInnerClassSuperType && superClass == null -> declaration.patchTopInClassHierarchyConstructor()
            else -> null
        }
    }

    private fun IrConstructor.patchTopInClassHierarchyConstructor(): List<IrDeclaration>? {
        val preinitHandler = generatePreinitHandler().also { valueParameters += it }
        val preinitParam = generatePreinitParam().also { valueParameters += it }

        (body as IrBlockBody).statements.add(0, generatePreinitHandlerCall(preinitHandler, preinitParam))

        return null
    }

    private fun IrConstructor.patchInBetweenConstructor(): List<IrDeclaration>? {
        val preinitHandler = generatePreinitHandler().also { valueParameters += it }
        val preinitParam = generatePreinitParam().also { valueParameters += it }

        patchDelegatedSuperCalls(
            JsIrBuilder.buildGetValue(preinitHandler.symbol),
            JsIrBuilder.buildGetValue(preinitParam.symbol)
        )

        return null
    }

    private fun IrConstructor.patchInnerClassConstructor(): List<IrDeclaration>? {
        val parent = parentAsClass
        val outerParam = valueParameters.first()
        val outerField = context.innerClassesSupport.getOuterThisField(parent)
        val statements = (body as IrBlockBody).statements

        val firstStatement = statements.firstOrNull()

        if (firstStatement != null && firstStatement is IrSetField && firstStatement.symbol == outerField.symbol) {
            statements.removeAt(0)
        }

        val preinitFn = generatePreinitFunction(outerField.symbol)

        patchDelegatedSuperCalls(
            JsIrBuilder.buildRawReference(preinitFn.symbol, preinitHandlerType),
            JsIrBuilder.buildGetValue(outerParam.symbol)
        )

        return listOf(this, preinitFn)
    }

    private fun IrConstructor.generatePreinitHandlerCall(preinitFn: IrValueParameter, preinitParam: IrValueParameter): IrStatement {
        return with(context.createIrBuilder(symbol, startOffset, endOffset)) {
            irIfThen(
                irNot(irEqeqeqWithoutBox(irGet(preinitFn), this@ES6InnerClassesLowering.context.getVoid())),
                JsIrBuilder.buildCall(preinitHandlerDecl.invokeFun!!.symbol)
                    .apply {
                        dispatchReceiver = irGet(preinitFn)
                        putValueArgument(0, irGet(parentAsClass.thisReceiver!!))
                        putValueArgument(1, irGet(preinitParam))
                    }
            )
        }
    }

    private fun IrConstructor.generatePreinitParam(i: Int = valueParameters.size): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.PREINIT_PARAM)
            isHidden = true
            type = context.dynamicType
            origin = ES6_UTILITY_PARAMETER_ORIGIN
            index = i
        }
    }

    private fun IrConstructor.generatePreinitHandler(i: Int = valueParameters.size): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.PREINIT)
            isHidden = true
            type = preinitHandlerType
            origin = ES6_UTILITY_PARAMETER_ORIGIN
            index = i
        }
    }

    private fun IrSimpleFunction.generateThisParam(irClass: IrClass): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.SYNTHETIC_RECEIVER_NAME)
            type = irClass.defaultType
            origin = ES6_THIS_VARIABLE_ORIGIN
            index = valueParameters.size
        }
    }

    private fun IrSimpleFunction.generateOuterParam(): IrValueParameter {
        return buildValueParameter(this) {
            name = Name.identifier(Namer.OUTER_NAME)
            type = context.dynamicType
            origin = ES6_THIS_VARIABLE_ORIGIN
            index = valueParameters.size
        }
    }

    private fun IrConstructor.generatePreinitFunction(outerField: IrFieldSymbol): IrSimpleFunction {
        val irClass = parentAsClass
        val constructorName = "${irClass.name}_init"
        val functionName = "${constructorName}_\$Preinit\$"

        return factory.buildFun {
            updateFrom(this@generatePreinitFunction)
            name = Name.identifier(functionName)
            returnType = context.irBuiltIns.unitType
            origin = JsIrBuilder.SYNTHESIZED_DECLARATION
        }.also {
            it.parent = parent

            val thisParam = it.generateThisParam(irClass)
            it.valueParameters += thisParam

            val outerParam = it.generateOuterParam()
            it.valueParameters += outerParam

            it.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                statements += JsIrBuilder.buildSetField(
                    outerField,
                    JsIrBuilder.buildGetValue(thisParam.symbol),
                    JsIrBuilder.buildGetValue(outerParam.symbol),
                    context.dynamicType
                )
            }
        }
    }

    private fun IrConstructor.patchDelegatedSuperCalls(preinitFn: IrExpression, preinitParam: IrExpression) {
        transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                    val symbol = expression.symbol
                    val valueArgumentsCount = expression.valueArgumentsCount;
                    return IrDelegatingConstructorCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        symbol,
                        expression.typeArgumentsCount,
                        valueArgumentsCount + 2
                    ).apply {
                        copyTypeAndValueArgumentsFrom(expression)
                        putValueArgument(valueArgumentsCount, preinitFn)
                        putValueArgument(valueArgumentsCount + 1, preinitParam)
                    }
                }
            }
        )
    }

}
