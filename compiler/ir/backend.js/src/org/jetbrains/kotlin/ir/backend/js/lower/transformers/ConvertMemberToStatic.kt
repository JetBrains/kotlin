/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.transformers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSyntheticBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isOriginallyLocal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.Name
import kotlin.collections.get
import kotlin.getValue

private val STATIC_THIS_PARAMETER by IrDeclarationOriginImpl.Regular

var IrFunction.correspondingStatic: IrSimpleFunction? by irAttribute(copyByDefault = false)

context(context: CommonBackendContext)
fun transformMemberToStaticFunction(function: IrSimpleFunction): IrSimpleFunction {
    val newVisibility = if (function.isOriginallyLocal) DescriptorVisibilities.LOCAL else function.visibility

    val staticFunction = context.irFactory.buildFun {
        updateFrom(function)
        name = function.name
        returnType = function.returnType
        visibility = newVisibility
    }.also {
        it.parent = function.parent
        it.annotations = function.annotations
        it.copyAttributes(function)
    }

    function.correspondingStatic = staticFunction

    val allTypeParameters = function.parentAsClass.typeParameters + function.typeParameters

    staticFunction.typeParameters = allTypeParameters.map { it.deepCopyWithSymbols(staticFunction) }

    val typeMapping = allTypeParameters.zip(staticFunction.typeParameters).toMap()

    staticFunction.parameters = function.parameters.map { originalParameter ->
        if (originalParameter.kind == IrParameterKind.DispatchReceiver) {
            originalParameter.copyTo(
                staticFunction,
                origin = STATIC_THIS_PARAMETER,
                name = Name.identifier("\$this"),
                kind = IrParameterKind.Regular,
                remapTypeMap = typeMapping
            )
        } else {
            // TODO better way to avoid copying default value
            originalParameter.copyTo(
                staticFunction,
                defaultValue = null,
                kind = IrParameterKind.Regular,
                remapTypeMap = typeMapping
            )
        }
    }

    val parameterMapping = function.parameters.zip(staticFunction.parameters).toMap()

    val parameterTransformer = object : IrElementTransformerVoid() {
        override fun visitGetValue(expression: IrGetValue): IrGetValue = parameterMapping[expression.symbol.owner]?.let {
            expression.run { IrGetValueImpl(startOffset, endOffset, type, it.symbol, origin) }
        } ?: expression
    }

    fun IrBody.copyWithParameters(): IrBody {
        return deepCopyWithSymbols(staticFunction).also {
            it.transform(parameterTransformer, null)
        }
    }

    function.parameters.forEach {
        // TODO better way to avoid copying default value

        parameterMapping[it]?.apply {
            it.defaultValue?.let { originalDefault ->
                defaultValue = context.irFactory.createExpressionBody(
                    startOffset = it.startOffset,
                    endOffset = it.endOffset,
                    expression = (originalDefault.copyWithParameters() as IrExpressionBody).expression,
                )
            }
        }
    }

    function.body?.let {
        staticFunction.body = when (it) {
            is IrBlockBody -> context.irFactory.createBlockBody(it.startOffset, it.endOffset) {
                statements += (it.copyWithParameters() as IrBlockBody).statements
            }
            is IrExpressionBody -> context.irFactory.createExpressionBody(
                startOffset = it.startOffset,
                endOffset = it.endOffset,
                expression = (it.copyWithParameters() as IrExpressionBody).expression,
            )
            is IrSyntheticBody -> it
        }
    }

    return staticFunction
}
