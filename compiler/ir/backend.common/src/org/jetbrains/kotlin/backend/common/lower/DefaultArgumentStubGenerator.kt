/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFunWithDescriptorForInlining
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

// TODO: fix expect/actual default parameters

open class DefaultArgumentStubGenerator(
    open val context: CommonBackendContext,
    private val skipInlineMethods: Boolean = true,
    private val skipExternalMethods: Boolean = false
) : DeclarationContainerLoweringPass {

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.transformDeclarationsFlat { memberDeclaration ->
            if (memberDeclaration is IrFunction)
                lower(memberDeclaration)
            else
                null
        }
    }

    private fun lower(irFunction: IrFunction): List<IrFunction> {
        val newIrFunction = irFunction.generateDefaultsFunction(context, skipInlineMethods, skipExternalMethods)
            ?: return listOf(irFunction)
        if (newIrFunction.origin == IrDeclarationOrigin.FAKE_OVERRIDE) {
            return listOf(irFunction, newIrFunction)
        }

        log { "$irFunction -> $newIrFunction" }
        val builder = context.createIrBuilder(newIrFunction.symbol)

        newIrFunction.body = builder.irBlockBody(newIrFunction) {
            val params = mutableListOf<IrValueDeclaration>()
            val variables = mutableMapOf<IrValueDeclaration, IrValueDeclaration>()

            irFunction.dispatchReceiverParameter?.let {
                variables[it] = newIrFunction.dispatchReceiverParameter!!
            }

            irFunction.extensionReceiverParameter?.let {
                variables[it] = newIrFunction.extensionReceiverParameter!!
            }

            // In order to deal with forward references in default value lambdas,
            // accesses to the parameter before it has been determined if there is
            // a default value or not is redirected to the actual parameter of the
            // $default function. This is to ensure that examples such as:
            //
            // fun f(f1: () -> String = { f2() },
            //       f2: () -> String = { "OK" }) = f1()
            //
            // works correctly so that `f() { "OK" }` returns "OK" and
            // `f()` throws a NullPointerException.
            irFunction.valueParameters.associateWithTo(variables) {
                newIrFunction.valueParameters[it.index]
            }

            for (valueParameter in irFunction.valueParameters) {
                val parameter = newIrFunction.valueParameters[valueParameter.index]
                val remapped = if (valueParameter.defaultValue != null) {
                    val mask = irGet(newIrFunction.valueParameters[irFunction.valueParameters.size + valueParameter.index / 32])
                    val bit = irInt(1 shl (valueParameter.index % 32))
                    val test = irCallOp(this@DefaultArgumentStubGenerator.context.ir.symbols.intAnd, context.irBuiltIns.intType, mask, bit)

                    val expressionBody = valueParameter.defaultValue!!
                    expressionBody.patchDeclarationParents(newIrFunction)
                    expressionBody.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            log { "GetValue: ${expression.symbol.owner}" }
                            val valueSymbol = variables[expression.symbol.owner] ?: return expression
                            return irGet(valueSymbol)
                        }
                    })

                    selectArgumentOrDefault(irNotEquals(test, irInt(0)), parameter, expressionBody.expression)
                } else {
                    parameter
                }
                params.add(remapped)
                variables[valueParameter] = remapped
            }

            when (irFunction) {
                is IrConstructor -> +irDelegatingConstructorCall(irFunction).apply {
                    passTypeArgumentsFrom(newIrFunction.parentAsClass)
                    // This is for Kotlin/Native, which differs from the other backends in that constructors
                    // apparently do have dispatch receivers (though *probably* not type arguments, but copy
                    // those as well just in case):
                    passTypeArgumentsFrom(newIrFunction, offset = newIrFunction.parentAsClass.typeParameters.size)
                    dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
                    params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
                }
                is IrSimpleFunction -> +irReturn(dispatchToImplementation(irFunction, newIrFunction, params))
                else -> error("Unknown function declaration")
            }
        }
        // Remove default argument initializers.
        irFunction.valueParameters.forEach {
            if (it.defaultValue != null) {
                it.defaultValue = IrExpressionBodyImpl(IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub"))
            }
        }
        return listOf(irFunction, newIrFunction)
    }

    protected open fun IrBlockBodyBuilder.selectArgumentOrDefault(
        shouldUseDefault: IrExpression,
        parameter: IrValueParameter,
        default: IrExpression
    ): IrValueDeclaration {
        val value = irIfThenElse(parameter.type, shouldUseDefault, default, irGet(parameter))
        return createTmpVariable(value, nameHint = parameter.name.asString())
    }

    private fun IrBlockBodyBuilder.dispatchToImplementation(
        irFunction: IrSimpleFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        val dispatchCall = irCall(irFunction.symbol).apply {
            passTypeArgumentsFrom(newIrFunction)
            dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
            extensionReceiver = newIrFunction.extensionReceiverParameter?.let { irGet(it) }

            params.forEachIndexed { i, variable -> putValueArgument(i, irGet(variable)) }
        }
        return if (needSpecialDispatch(irFunction)) {
            val handlerDeclaration = newIrFunction.valueParameters.last()
            // if $handler != null $handler(a, b, c) else foo(a, b, c)
            irIfThenElse(
                irFunction.returnType,
                irEqualsNull(irGet(handlerDeclaration)),
                dispatchCall,
                generateHandleCall(handlerDeclaration, irFunction, newIrFunction, params)
            )
        } else dispatchCall
    }

    protected open fun needSpecialDispatch(irFunction: IrSimpleFunction) = false
    protected open fun IrBlockBodyBuilder.generateHandleCall(
        handlerDeclaration: IrValueParameter,
        oldIrFunction: IrFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        assert(needSpecialDispatch(oldIrFunction as IrSimpleFunction))
        error("This method should be overridden")
    }

    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

val DEFAULT_DISPATCH_CALL = object : IrStatementOriginImpl("DEFAULT_DISPATCH_CALL") {}

open class DefaultParameterInjector(
    open val context: CommonBackendContext,
    private val skipInline: Boolean = true,
    private val skipExternalMethods: Boolean = false
) : IrElementTransformerVoid(), BodyLoweringPass, FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    private fun <T : IrFunctionAccessExpression> visitFunctionAccessExpression(expression: T, builder: (IrFunctionSymbol) -> T): T {
        val argumentsCount = (0 until expression.valueArgumentsCount).count { expression.getValueArgument(it) != null }
        if (argumentsCount == expression.symbol.owner.valueParameters.size)
            return expression

        val (symbol, params) = parametersForCall(expression) ?: return expression
        for (i in 0 until expression.typeArgumentsCount) {
            log { "${symbol.descriptor}[$i]: ${expression.getTypeArgument(i)}" }
        }
        symbol.owner.typeParameters.forEach { log { "${symbol.owner}[${it.index}] : $it" } }

        return builder(symbol).apply {
            copyTypeArgumentsFrom(expression)

            params.forEachIndexed { i, value ->
                log { "call::params@$i/${symbol.owner.valueParameters[i].name}: ${ir2string(value)}" }
                putValueArgument(i, value)
            }

            dispatchReceiver = expression.dispatchReceiver
            extensionReceiver = expression.extensionReceiver

            log { "call::extension@: ${ir2string(expression.extensionReceiver)}" }
            log { "call::dispatch@: ${ir2string(expression.dispatchReceiver)}" }
        }
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrDelegatingConstructorCallImpl(startOffset, endOffset, type, it as IrConstructorSymbol, typeArgumentsCount)
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrConstructorCallImpl.fromSymbolOwner(startOffset, endOffset, type, it as IrConstructorSymbol, DEFAULT_DISPATCH_CALL)
            }
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrEnumConstructorCallImpl(startOffset, endOffset, type, it as IrConstructorSymbol)
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrCallImpl(startOffset, endOffset, type, it, typeArgumentsCount, DEFAULT_DISPATCH_CALL, superQualifierSymbol)
            }
        }
    }

    private fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<IrExpression?>>? {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        val declaration = expression.symbol.owner

        val stubOverride = declaration.generateDefaultsFunction(context, skipInline, skipExternalMethods) ?: return null
        // We *have* to resolve the fake override here since on the JVM, a default stub for a function implemented
        // in an interface does not leave an abstract method after being moved to DefaultImpls (see InterfaceLowering).
        // Calling the fake override on an implementation of that interface would then result in a call to a method
        // that does not actually exist as DefaultImpls is not part of the inheritance hierarchy.
        val stubFunction = if (stubOverride is IrSimpleFunction) stubOverride.resolveFakeOverride()!! else stubOverride
        log { "$declaration -> $stubFunction" }

        val realArgumentsNumber = declaration.valueParameters.size
        val maskValues = IntArray((declaration.valueParameters.size + 31) / 32)
        assert((stubFunction.valueParameters.size - realArgumentsNumber - maskValues.size) in listOf(0, 1)) {
            "argument count mismatch: expected $realArgumentsNumber arguments + ${maskValues.size} masks + optional handler/marker, " +
                    "got ${stubFunction.valueParameters.size} total in ${stubFunction.render()}"
        }
        return stubFunction.symbol to stubFunction.valueParameters.mapIndexed { i, parameter ->
            when {
                i >= realArgumentsNumber + maskValues.size -> IrConstImpl.constNull(startOffset, endOffset, parameter.type)
                i >= realArgumentsNumber -> IrConstImpl.int(startOffset, endOffset, parameter.type, maskValues[i - realArgumentsNumber])
                else -> {
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        maskValues[i / 32] = maskValues[i / 32] or (1 shl (i % 32))
                    }
                    valueArgument ?: nullConst(startOffset, endOffset, parameter)
                }
            }
        }
    }

    protected open fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? =
        if (irParameter.varargElementType != null) {
            null
        } else {
            nullConst(startOffset, endOffset, irParameter.type)
        }

    protected open fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression =
        IrConstImpl.defaultValueForType(startOffset, endOffset, type)

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

class DefaultParameterCleaner constructor(val context: CommonBackendContext) : FunctionLoweringPass {
    override fun lower(irFunction: IrFunction) {
        if (!context.scriptMode) {
            irFunction.valueParameters.forEach { it.defaultValue = null }
        }
    }
}

private fun IrFunction.generateDefaultsFunction(
    context: CommonBackendContext,
    skipInlineMethods: Boolean,
    skipExternalMethods: Boolean
): IrFunction? = context.ir.defaultParameterDeclarationsCache[this] ?: when {
    skipInlineMethods && isInline -> null
    skipExternalMethods && isExternalOrInheritedFromExternal() -> null
    valueParameters.any { it.defaultValue != null } ->
        generateDefaultsFunctionImpl(context, IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER).also {
            context.ir.defaultParameterDeclarationsCache[this] = it
        }
    this is IrSimpleFunction -> {
        // If this is an override of a function with default arguments, produce a fake override of a default stub.
        val overriddenStubs = overriddenSymbols.mapNotNull {
            it.owner.generateDefaultsFunction(context, skipInlineMethods, skipExternalMethods)?.symbol as IrSimpleFunctionSymbol?
        }
        if (overriddenStubs.isNotEmpty())
            generateDefaultsFunctionImpl(context, IrDeclarationOrigin.FAKE_OVERRIDE).also {
                (it as IrSimpleFunction).overriddenSymbols.addAll(overriddenStubs)
                context.ir.defaultParameterDeclarationsCache[this] = it
            }
        else
            null
    }
    else -> null
}

private fun IrFunction.generateDefaultsFunctionImpl(context: CommonBackendContext, newOrigin: IrDeclarationOrigin): IrFunction {
    val newFunction = when (this) {
        is IrConstructor ->
            buildConstructor {
                updateFrom(this@generateDefaultsFunctionImpl)
                origin = newOrigin
                isExternal = false
                isPrimary = false
                isExpect = false
            }
        is IrSimpleFunction ->
            buildFunWithDescriptorForInlining(descriptor) {
                updateFrom(this@generateDefaultsFunctionImpl)
                name = Name.identifier("${this@generateDefaultsFunctionImpl.name}\$default")
                origin = newOrigin
                modality = Modality.FINAL
                isExternal = false
                isTailrec = false
            }
        else -> throw IllegalStateException("Unknown function type")
    }
    newFunction.copyTypeParametersFrom(this)
    newFunction.parent = parent
    newFunction.returnType = returnType.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
    newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(newFunction)
    newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)

    valueParameters.mapTo(newFunction.valueParameters) {
        val newType = it.type.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
        val makeNullable = it.defaultValue != null &&
                (context.ir.unfoldInlineClassType(it.type) ?: it.type) !in context.irBuiltIns.primitiveIrTypes
        it.copyTo(newFunction, type = if (makeNullable) newType.makeNullable() else newType, defaultValue = null)
    }

    for (i in 0 until (valueParameters.size + 31) / 32) {
        newFunction.addValueParameter("mask$i".synthesizedString, context.irBuiltIns.intType)
    }
    if (this is IrConstructor) {
        val markerType = context.ir.symbols.defaultConstructorMarker.owner.defaultType.makeNullable()
        newFunction.addValueParameter("marker".synthesizedString, markerType)
    } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
        newFunction.addValueParameter("handler".synthesizedString, context.irBuiltIns.anyNType)
    }

    // TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
    annotations.mapTo(newFunction.annotations) { it.deepCopyWithSymbols() }
    return newFunction
}
