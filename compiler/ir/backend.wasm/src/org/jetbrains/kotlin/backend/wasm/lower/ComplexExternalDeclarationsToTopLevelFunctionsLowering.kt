/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name
import java.lang.StringBuilder

/**
 * Lower complex external declarations to top-level functions:
 *   - Property accessors
 *   - Member functions
 *   - Class constructors
 *   - Object declarations
 *   - Class instance checks
 */
class ComplexExternalDeclarationsToTopLevelFunctionsLowering(val context: WasmBackendContext) : FileLoweringPass {
    lateinit var currentFile: IrFile
    val addedDeclarations = mutableListOf<IrDeclaration>()

    val externalFunToTopLevelMapping =
        context.mapping.wasmNestedExternalToNewTopLevelFunction

    val externalObjectToGetInstanceFunction =
        context.mapping.wasmExternalObjectToGetInstanceFunction

    override fun lower(irFile: IrFile) {
        currentFile = irFile
        for (declaration in irFile.declarations) {
            if (declaration.isEffectivelyExternal()) {
                processExternalDeclaration(declaration)
            }
        }
        irFile.declarations += addedDeclarations
        addedDeclarations.clear()
    }

    fun processExternalDeclaration(declaration: IrDeclaration) {
        declaration.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                error("Unknown external element ${element::class}")
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                lowerExternalClass(declaration)
            }

            override fun visitProperty(declaration: IrProperty) {
                processExternalProperty(declaration)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                processExternalConstructor(declaration)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                processExternalSimpleFunction(declaration)
            }
        })
    }

    fun lowerExternalClass(klass: IrClass) {
        if (klass.kind == ClassKind.OBJECT)
            generateExternalObjectInstanceGetter(klass)

        if (klass.kind != ClassKind.INTERFACE)
            generateInstanceCheckForExternalClass(klass)
    }

    fun processExternalProperty(property: IrProperty) {
        if (property.isFakeOverride)
            return

        val propName: String =
            property.getJsNameOrKotlinName().identifier

        property.getter?.let { getter ->
            val dispatchReceiver = getter.dispatchReceiverParameter
            val jsCode =
                if (dispatchReceiver == null)
                    "() => $propName"
                else
                    "(_this) => _this.$propName"

            val res = createExternalJsFunction(
                property.name,
                "_\$external_prop_getter",
                resultType = getter.returnType,
                jsCode = jsCode
            )

            if (dispatchReceiver != null) {
                res.addValueParameter("_this", dispatchReceiver.type)
            }

            externalFunToTopLevelMapping[getter] = res
        }

        property.setter?.let { setter ->
            val dispatchReceiver = setter.dispatchReceiverParameter
            val jsCode =
                if (dispatchReceiver == null)
                    "(v) => $propName = v"
                else
                    "(_this, v) => _this.$propName = v"

            val res = createExternalJsFunction(
                property.name,
                "_\$external_prop_setter",
                resultType = setter.returnType,
                jsCode = jsCode
            )

            if (dispatchReceiver != null) {
                res.addValueParameter("_this", dispatchReceiver.type)
            }
            res.addValueParameter("v", setter.valueParameters[0].type)

            externalFunToTopLevelMapping[setter] = res
        }
    }

    private fun StringBuilder.appendExternalClassReference(klass: IrClass) {
        val parent = klass.parent
        if (parent is IrClass) {
            appendExternalClassReference(parent)
            if (klass.isCompanion) {
                // Reference to external companion object is reference to its parent class
                return
            }
            append('.')
        }
        append(klass.getJsNameOrKotlinName())
    }

    fun processExternalConstructor(constructor: IrConstructor) {
        val klass = constructor.constructedClass
        val jsCode = buildString {
            append("(")
            appendParameterList(constructor.valueParameters.size)
            append(") => new ")
            appendExternalClassReference(klass)
            append("(")
            appendParameterList(constructor.valueParameters.size)
            append(")")
        }

        val res = createExternalJsFunction(
            klass.name,
            "_\$external_constructor",
            resultType = klass.defaultType,
            jsCode = jsCode
        )

        constructor.valueParameters.forEach { res.addValueParameter(it.name, it.type) }
        externalFunToTopLevelMapping[constructor] = res
    }

    fun processExternalSimpleFunction(function: IrSimpleFunction) {
        if (function.parent is IrPackageFragment) return
        val jsName = function.getJsNameOrKotlinName()
        val dispatchReceiver = function.dispatchReceiverParameter
        require(dispatchReceiver != null) {
            "Non top-level external function w/o dispatchReceiverParameter: ${function.fqNameWhenAvailable}"
        }

        val jsCode = buildString {
            append("(_this, ")
            appendParameterList(function.valueParameters.size)
            append(") => _this.")
            append(jsName)
            append("(")
            appendParameterList(function.valueParameters.size)
            append(")")
        }
        val res = createExternalJsFunction(
            function.name,
            "_\$external_member_fun",
            resultType = function.returnType,
            jsCode = jsCode
        )
        res.addValueParameter("_this", dispatchReceiver.type)
        function.valueParameters.forEach { res.addValueParameter(it.name, it.type) }

        externalFunToTopLevelMapping[function] = res
    }

    fun generateExternalObjectInstanceGetter(obj: IrClass) {
        context.mapping.wasmExternalObjectToGetInstanceFunction[obj] = createExternalJsFunction(
            obj.name,
            "_\$external_object_getInstance",
            resultType = obj.defaultType,
            jsCode = buildString {
                append("() => ")
                appendExternalClassReference(obj)
            }
        )
    }

    fun generateInstanceCheckForExternalClass(klass: IrClass) {
        context.mapping.wasmExternalClassToInstanceCheck[klass] = createExternalJsFunction(
            klass.name,
            "_\$external_class_instanceof",
            resultType = context.irBuiltIns.booleanType,
            jsCode = buildString {
                append("(x) => x instanceof ")
                appendExternalClassReference(klass)
            }
        ).also {
            it.addValueParameter("x", context.irBuiltIns.anyType)
        }
    }

    private fun createExternalJsFunction(
        originalName: Name,
        suffix: String,
        resultType: IrType,
        jsCode: String,
    ): IrSimpleFunction {
        val res = context.irFactory.buildFun {
            name = Name.identifier(originalName.asStringStripSpecialMarkers() + suffix)
            returnType = resultType
            isExternal = true
        }
        val builder = context.createIrBuilder(res.symbol)
        res.annotations += builder.irCallConstructor(context.wasmSymbols.jsFunConstructor, typeArguments = emptyList()).also {
            it.putValueArgument(0, builder.irString(jsCode))
        }
        res.parent = currentFile
        addedDeclarations += res
        return res
    }
}

/**
 * Redirect usages of complex declarations to top-level functions
 */
class ComplexExternalDeclarationsUsageLowering(val context: WasmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid()
                return transformCall(expression)
            }

            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                expression.transformChildrenVoid()
                return transformCall(expression)
            }

            override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
                val externalGetInstance = context.mapping.wasmExternalObjectToGetInstanceFunction[expression.symbol.owner]
                return if (externalGetInstance != null) {
                    IrCallImpl(
                        expression.startOffset,
                        expression.endOffset,
                        expression.type,
                        externalGetInstance.symbol,
                        valueArgumentsCount = 0,
                        typeArgumentsCount = 0
                    )
                } else {
                    expression
                }
            }

            fun transformCall(call: IrFunctionAccessExpression): IrExpression {
                val newFun: IrSimpleFunction? =
                    context.mapping.wasmNestedExternalToNewTopLevelFunction[call.symbol.owner.realOverrideTarget]

                return if (newFun != null) {
                    irCall(call, newFun, receiversAsArguments = true)
                } else {
                    call
                }
            }
        })
    }
}


