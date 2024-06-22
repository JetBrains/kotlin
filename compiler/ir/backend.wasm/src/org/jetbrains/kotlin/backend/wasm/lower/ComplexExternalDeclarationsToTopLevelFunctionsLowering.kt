/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.JsModuleAndQualifierReference
import org.jetbrains.kotlin.backend.wasm.utils.getJsFunAnnotation
import org.jetbrains.kotlin.backend.wasm.utils.getJsPrimitiveType
import org.jetbrains.kotlin.backend.wasm.utils.getWasmImportDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

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

            override fun visitTypeParameter(declaration: IrTypeParameter) {
            }

            override fun visitValueParameter(declaration: IrValueParameter) {
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)
                declaration.factory.stageController.restrictTo(declaration) {
                    lowerExternalClass(declaration)
                }
            }

            override fun visitProperty(declaration: IrProperty) {
                declaration.factory.stageController.restrictTo(declaration) {
                    processExternalProperty(declaration)
                }
            }

            override fun visitConstructor(declaration: IrConstructor) {
                declaration.factory.stageController.restrictTo(declaration) {
                    processExternalConstructor(declaration)
                }
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                declaration.factory.stageController.restrictTo(declaration) {
                    processExternalSimpleFunction(declaration)
                }
            }
        })
    }

    fun lowerExternalClass(klass: IrClass) {
        if (klass.kind == ClassKind.OBJECT)
            generateExternalObjectInstanceGetter(klass)

        if (klass.kind != ClassKind.INTERFACE) {
            generateInstanceCheckForExternalClass(klass)
            generateGetClassForExternalClass(klass)
        }
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
                    "() => ${referenceTopLevelExternalDeclaration(property)}"
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
                    "(v) => ${referenceTopLevelExternalDeclaration(property)} = v"
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

            // This is hack to support Kotlin/JS like implementation of IDL string enums bindings
            // with error suppression:
            //
            //    @JsName("null")
            //    @Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
            //    public external interface CanvasFillRule {
            //        companion object
            //    }
            //    public inline val CanvasFillRule.Companion.NONZERO: CanvasFillRule get() = "nonzero".asDynamic().unsafeCast<CanvasFillRule>()
            //
            // Kotlin/JS translates access to CanvasFillRule.Companion as `null` due to @JsName("null"),
            // but Kotlin/Wasm fails to do this due to stricter null checks on interop boundary.
            //
            // Instead, as a temporary solution, we evaluate such companion object to an empty JS object.
            // TODO: Optimize (KT-60661)
            if (parent.isInterface) {
                append("({})")
                return
            }

            appendExternalClassReference(parent)
            if (klass.isCompanion) {
                // Reference to external companion object is reference to its parent class
                return
            }
            append('.')
            append(klass.getJsNameOrKotlinName())
        } else {
            append(referenceTopLevelExternalDeclaration(klass))
        }
    }

    fun processExternalConstructor(constructor: IrConstructor) {
        val klass = constructor.constructedClass

        // External interfaces can have synthetic primary constructors in K/JS
        if (klass.isInterface)
            return

        processFunctionOrConstructor(
            function = constructor,
            name = klass.name,
            returnType = klass.defaultType,
            isConstructor = true,
            jsFunctionReference = buildString { appendExternalClassReference(klass) }
        )
    }

    fun processExternalSimpleFunction(function: IrSimpleFunction) {
        // Skip JS interop adapters form WasmImport.
        // It needs to keep original signature to interop with other Wasm modules.
        if (function.getWasmImportDescriptor() != null)
            return

        val jsFun = function.getJsFunAnnotation()
        // Wrap external functions without @JsFun to lambdas `foo` -> `(a, b) => foo(a, b)`.
        // This way we wouldn't fail if we don't call them.
        if (jsFun != null &&
            function.valueParameters.all { it.defaultValue == null && it.varargElementType == null } &&
            currentFile.getJsQualifier() == null &&
            currentFile.getJsModule() == null
        ) {
            return
        }

        if (function.isFakeOverride) {
            return
        }

        val jsFunctionReference = when {
            jsFun != null -> "($jsFun)"
            function.isTopLevelDeclaration -> referenceTopLevelExternalDeclaration(function)
            else -> function.getJsNameOrKotlinName().identifier
        }

        processFunctionOrConstructor(
            function = function,
            name = function.name,
            returnType = function.returnType,
            isConstructor = false,
            jsFunctionReference = jsFunctionReference
        )
    }

    private val IrFunction.isSetOperator get() =
        (this is IrSimpleFunction) && isOperator && name.asString() == "set"

    private val IrFunction.isGetOperator get() =
        (this is IrSimpleFunction) && isOperator && name.asString() == "get"

    private fun createJsCodeForFunction(
        function: IrFunction,
        numDefaultParameters: Int,
        isConstructor: Boolean,
        jsFunctionReference: String
    ): String {
        val dispatchReceiver = function.dispatchReceiverParameter
        val numValueParameters = function.valueParameters.size

        return buildString {
            append("(")
            if (dispatchReceiver != null) {
                append("_this, ")
            }
            appendParameterList(numValueParameters, isEnd = numDefaultParameters == 0)

            // Parameters with default values are handled via adding additional flags to indicate that parameter is default .
            //
            //   external fun foo(x: Int, y: Int = definedExternally, z: Int = definedExternally)
            //
            //     =>
            //
            //   @JsCode("""(x, y, z, isDefault1, isDefault2) =>
            //      foo(
            //          x,
            //          isDefault1 ? undefined : y,
            //          isDefault2 ? undefined : z
            //      )
            //   """)
            //   external fun foo(x: Int, y: Int, z: Int, isDefault1: Int, isDefault: Int)

            appendParameterList(numDefaultParameters, "isDefault", isEnd = true)
            append(") => ")
            if (isConstructor) {
                append("new ")
            }
            if (dispatchReceiver != null) {
                append("_this.")
            }
            append(jsFunctionReference)
            append("(")

            val numNonDefaultParamters = numValueParameters - numDefaultParameters
            repeat(numNonDefaultParamters) {
                if (function.valueParameters[it].isVararg) {
                    append("...")
                }
                append("p$it")
                if (numDefaultParameters != 0 || it + 1 < numNonDefaultParamters)
                    append(", ")
            }
            repeat(numDefaultParameters) {
                if (function.valueParameters[numNonDefaultParamters + it].isVararg) {
                    append("...")
                } else {
                    append("isDefault$it ? undefined : ")
                }
                append("p${numNonDefaultParamters + it}, ")
            }
            append(")")
        }
    }

    fun processFunctionOrConstructor(
        function: IrFunction,
        name: Name,
        returnType: IrType,
        isConstructor: Boolean,
        jsFunctionReference: String
    ) {
        val dispatchReceiver = function.dispatchReceiverParameter

        val numDefaultParameters =
            numDefaultParametersForExternalFunction(function)

        val jsCode = when {
            function.isSetOperator -> "(_this, i, value) => _this[i] = value"
            function.isGetOperator -> "(_this, i) => _this[i]"
            else -> createJsCodeForFunction(function, numDefaultParameters, isConstructor, jsFunctionReference)
        }

        val res = createExternalJsFunction(
            name,
            "_\$external_fun",
            resultType = returnType,
            jsCode = jsCode
        )
        if (dispatchReceiver != null) {
            res.addValueParameter("_this", dispatchReceiver.type)
        }
        function.valueParameters.forEach {
            res.addValueParameter(
                name = it.name,
                type = if (it.type.isPrimitiveType(false)) it.type else it.type.makeNullable()
            ).apply {
                varargElementType = it.varargElementType
            }
        }
        // Using Int type with 0 and 1 values to prevent overhead of converting Boolean to true and false
        repeat(numDefaultParameters) { res.addValueParameter("isDefault$it", context.irBuiltIns.intType) }
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
                val jsPrimitiveType = klass.getJsPrimitiveType()
                if (jsPrimitiveType != null) {
                    append("(x) => typeof x === '$jsPrimitiveType'")
                } else {
                    append("(x) => x instanceof ")
                    appendExternalClassReference(klass)
                }
            }
        ).also {
            it.addValueParameter("x", context.irBuiltIns.anyType)
        }
    }

    fun generateGetClassForExternalClass(klass: IrClass) {
        context.mapping.wasmGetJsClass[klass] = createExternalJsFunction(
            klass.name,
            "_\$external_class_get",
            resultType = context.wasmSymbols.jsRelatedSymbols.jsAnyType.makeNullable(),
            jsCode = buildString {
                append("() => ")
                appendExternalClassReference(klass)
            }
        )
    }

    private fun createExternalJsFunction(
        originalName: Name,
        suffix: String,
        resultType: IrType,
        jsCode: String,
    ): IrSimpleFunction {
        val res = createExternalJsFunction(context, originalName, suffix, resultType, jsCode)
        res.parent = currentFile
        addedDeclarations += res
        return res
    }

    private fun referenceTopLevelExternalDeclaration(declaration: IrDeclarationWithName): String {
        var name: String? = declaration.getJsNameOrKotlinName().identifier

        val qualifier = currentFile.getJsQualifier()

        val module = currentFile.getJsModule()
            ?: declaration.getJsModule()?.also {
                name = if (declaration is IrClass && declaration.isObject) null else "default"
            }

        if (qualifier == null && module == null)
            return name!!

        val qualifieReference = JsModuleAndQualifierReference(module, qualifier)
        context.jsModuleAndQualifierReferences += qualifieReference
        return qualifieReference.jsVariableName + name?.let { ".$it" }.orEmpty()
    }
}

fun createExternalJsFunction(
    context: WasmBackendContext,
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
    res.annotations += builder.irCallConstructor(context.wasmSymbols.jsRelatedSymbols.jsFunConstructor, typeArguments = emptyList()).also {
        it.putValueArgument(0, builder.irString(jsCode))
    }
    return res
}

/**
 * Redirect usages of complex declarations to top-level functions
 */
class ComplexExternalDeclarationsUsageLowering(val context: WasmBackendContext) : FileLoweringPass {
    private val nestedExternalToNewTopLevelFunctions = context.mapping.wasmNestedExternalToNewTopLevelFunction
    private val objectToGetInstanceFunctions = context.mapping.wasmExternalObjectToGetInstanceFunction

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(declarationTransformer)
    }

    private val declarationTransformer = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitFile(declaration: IrFile) {
            process(declaration)
        }

        override fun visitClass(declaration: IrClass) {
            if (!declaration.isExternal) {
                process(declaration)
            }
        }

        private fun process(container: IrDeclarationContainer) {
            container.declarations.transformFlat { member ->
                if (member is IrFunction && nestedExternalToNewTopLevelFunctions[member] != null) {
                    emptyList()
                } else {
                    member.acceptVoid(this)
                    null
                }
            }
        }

        override fun visitBody(body: IrBody) {
            body.transformChildrenVoid(usagesTransformer)
        }
    }

    private val usagesTransformer = object : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            expression.transformChildrenVoid()
            return transformCall(expression)
        }

        override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
            expression.transformChildrenVoid()
            return transformCall(expression)
        }

        override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
            val externalGetInstance = objectToGetInstanceFunctions[expression.symbol.owner] ?: return expression
            return IrCallImpl(
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type,
                symbol = externalGetInstance.symbol,
                valueArgumentsCount = 0,
                typeArgumentsCount = 0
            )
        }

        fun transformCall(call: IrFunctionAccessExpression): IrExpression {
            val oldFun = call.symbol.owner.realOverrideTarget
            val newFun: IrSimpleFunction = nestedExternalToNewTopLevelFunctions[oldFun] ?: return call

            val newCall = irCall(call, newFun, receiversAsArguments = true)

            // Add default arguments flags if needed
            val numDefaultParameters = numDefaultParametersForExternalFunction(oldFun)
            val firstDefaultFlagArgumentIdx = newFun.valueParameters.size - numDefaultParameters
            val firstOldDefaultArgumentIdx = call.valueArgumentsCount - numDefaultParameters
            repeat(numDefaultParameters) {
                val value = if (call.getValueArgument(firstOldDefaultArgumentIdx + it) == null) 1 else 0
                newCall.putValueArgument(
                    firstDefaultFlagArgumentIdx + it,
                    IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.intType, value)
                )
            }

            // Add default argument values if needed
            for (i in 0 until newFun.valueParameters.size) {
                val parameter = newFun.valueParameters[i]
                if (parameter.isVararg) continue // Handled with WasmVarargExpressionLowering
                if (newCall.getValueArgument(i) != null) continue
                newCall.putValueArgument(
                    i,
                    IrConstImpl.defaultValueForType(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.type)
                )
            }

            return newCall
        }
    }
}

private fun numDefaultParametersForExternalFunction(function: IrFunction): Int {
    if (function is IrSimpleFunction) {
        // Default parameters can be in overridden external functions
        val numDefaultParametersInOverrides =
            function.overriddenSymbols.maxOfOrNull {
                numDefaultParametersForExternalFunction(it.owner)
            } ?: 0

        if (numDefaultParametersInOverrides > 0) {
            return numDefaultParametersInOverrides
        }
    }

    val firstDefaultParameterIndex: Int? =
        function.valueParameters.firstOrNull { it.defaultValue != null }?.index

    return if (firstDefaultParameterIndex == null)
        0
    else
        function.valueParameters.size - firstDefaultParameterIndex
}
