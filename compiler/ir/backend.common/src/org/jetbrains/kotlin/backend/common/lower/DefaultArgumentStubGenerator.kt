/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

// TODO: fix expect/actual default parameters

open class DefaultArgumentStubGenerator(
    open val context: CommonBackendContext,
    private val skipInlineMethods: Boolean = true,
    private val skipExternalMethods: Boolean = false,
    private val forceSetOverrideSymbols: Boolean = true
) : DeclarationTransformer {

    override fun lower(irFile: IrFile) {
        runPostfix(true).toFileLoweringPass().lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction) {
            return lower(declaration)
        }

        return null
    }

    private fun lower(irFunction: IrFunction): List<IrFunction>? {
        val newIrFunction =
            irFunction.generateDefaultsFunction(
                context,
                skipInlineMethods,
                skipExternalMethods,
                forceSetOverrideSymbols,
                defaultArgumentStubVisibility(irFunction),
                useConstructorMarker(irFunction)
            ) ?: return null
        if (newIrFunction.isFakeOverride) {
            return listOf(irFunction, newIrFunction)
        }

        log { "$irFunction -> $newIrFunction" }
        val builder = context.createIrBuilder(newIrFunction.symbol)

        newIrFunction.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += builder.irBlockBody(newIrFunction) {
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

                generateSuperCallHandlerCheckIfNeeded(irFunction, newIrFunction)

                val intAnd = this@DefaultArgumentStubGenerator.context.ir.symbols.getBinaryOperator(
                    OperatorNameConventions.AND, context.irBuiltIns.intType, context.irBuiltIns.intType
                )
                var sourceParameterIndex = -1
                for (valueParameter in irFunction.valueParameters) {
                    if (!valueParameter.isMovedReceiver()) {
                        ++sourceParameterIndex
                    }
                    val parameter = newIrFunction.valueParameters[valueParameter.index]
                    val remapped = valueParameter.defaultValue?.let { defaultValue ->
                        val mask = irGet(newIrFunction.valueParameters[irFunction.valueParameters.size + valueParameter.index / 32])
                        val bit = irInt(1 shl (sourceParameterIndex % 32))
                        val defaultFlag =
                            irCallOp(intAnd, context.irBuiltIns.intType, mask, bit)

                        val expression = defaultValue.expression
                            .prepareToBeUsedIn(newIrFunction)
                            .transform(object : IrElementTransformerVoid() {
                                override fun visitGetValue(expression: IrGetValue): IrExpression {
                                    log { "GetValue: ${expression.symbol.owner}" }
                                    val valueSymbol = variables[expression.symbol.owner] ?: return expression
                                    return irGet(valueSymbol)
                                }
                            }, null)

                        selectArgumentOrDefault(defaultFlag, parameter, expression)
                    } ?: parameter

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
            }.statements
        }
        return listOf(irFunction, newIrFunction)
    }

    /**
     * Prepares the default value to be used inside the `function` body by patching the parents.
     * In K/JS it also copies the expression in order to avoid duplicate declarations after this lowering.
     *
     * In K/JVM copying doesn't preserve metadata, so the following case won't work:
     *
     * ```
     *   import kotlin.reflect.jvm.reflect
     *
     *   fun foo(x: Function<*> = {}) {
     *       // Will print "null" if lambda is copied
     *       println(x.reflect())
     *   }
     * ```
     *
     * Thus the duplicate declarations during the lowering pipeline is considered to be a lesser evil.
     */
    protected open fun IrExpression.prepareToBeUsedIn(function: IrFunction): IrExpression {
        return patchDeclarationParents(function)
    }


    protected open fun IrBlockBodyBuilder.selectArgumentOrDefault(
        defaultFlag: IrExpression,
        parameter: IrValueParameter,
        default: IrExpression
    ): IrValueDeclaration {
        val value = irIfThenElse(parameter.type, irNotEquals(defaultFlag, irInt(0)), default, irGet(parameter))
        return createTmpVariable(value, nameHint = parameter.name.asString())
    }

    protected open fun getOriginForCallToImplementation(): IrStatementOrigin? = null

    private fun IrBlockBodyBuilder.dispatchToImplementation(
        irFunction: IrSimpleFunction,
        newIrFunction: IrFunction,
        params: MutableList<IrValueDeclaration>
    ): IrExpression {
        val dispatchCall = irCall(irFunction, origin = getOriginForCallToImplementation()).apply {
            passTypeArgumentsFrom(newIrFunction)
            dispatchReceiver = newIrFunction.dispatchReceiverParameter?.let { irGet(it) }
            extensionReceiver = newIrFunction.extensionReceiverParameter?.let { irGet(it) }

            for ((i, variable) in params.withIndex()) {
                val paramType = irFunction.valueParameters[i].type
                // The JVM backend doesn't introduce new variables, and hence may have incompatible types here.
                val value = if (!paramType.isNullable() && variable.type.isNullable()) {
                    irImplicitCast(irGet(variable), paramType)
                } else {
                    irGet(variable)
                }
                putValueArgument(i, value)
            }
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

    protected open fun IrBlockBodyBuilder.generateSuperCallHandlerCheckIfNeeded(
        irFunction: IrFunction,
        newIrFunction: IrFunction
    ) {
        //NO-OP Stub
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

    protected open fun defaultArgumentStubVisibility(function: IrFunction) = DescriptorVisibilities.PUBLIC

    protected open fun useConstructorMarker(function: IrFunction) = function is IrConstructor

    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

private fun IrFunction.findBaseFunctionWithDefaultArguments(skipInlineMethods: Boolean, skipExternalMethods: Boolean): IrFunction? {

    val visited = mutableSetOf<IrFunction>()

    fun IrFunction.dfsImpl(): IrFunction? {
        visited += this

        if (isInline && skipInlineMethods) return null
        if (skipExternalMethods && isExternalOrInheritedFromExternal()) return null

        if (this is IrSimpleFunction) {
            overriddenSymbols.forEach {
                val base = it.owner
                if (base !in visited) base.dfsImpl()?.let { return it }
            }
        }

        if (valueParameters.any { it.defaultValue != null }) return this

        return null
    }

    return dfsImpl()
}

val DEFAULT_DISPATCH_CALL = object : IrStatementOriginImpl("DEFAULT_DISPATCH_CALL") {}

open class DefaultParameterInjector(
    open val context: CommonBackendContext,
    private val skipInline: Boolean = true,
    private val skipExternalMethods: Boolean = false,
    private val forceSetOverrideSymbols: Boolean = true
) : IrElementTransformerVoid(), BodyLoweringPass {

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(this)
    }

    private fun <T : IrFunctionAccessExpression> visitFunctionAccessExpression(expression: T, builder: (IrFunctionSymbol) -> T): T {
        val argumentsCount = (0 until expression.valueArgumentsCount).count { expression.getValueArgument(it) != null }
        if (argumentsCount == expression.symbol.owner.valueParameters.size)
            return expression

        val (symbol, params) = parametersForCall(expression) ?: return expression
        for (i in 0 until expression.typeArgumentsCount) {
            log { "$symbol[$i]: ${expression.getTypeArgument(i)}" }
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
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, type, it as IrConstructorSymbol,
                    typeArgumentsCount = typeArgumentsCount,
                    valueArgumentsCount = it.owner.valueParameters.size
                )
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
                IrEnumConstructorCallImpl(
                    startOffset, endOffset, type, it as IrConstructorSymbol,
                    typeArgumentsCount = typeArgumentsCount,
                    valueArgumentsCount = it.owner.valueParameters.size
                )
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrCallImpl(
                    startOffset, endOffset, type, it as IrSimpleFunctionSymbol,
                    typeArgumentsCount = typeArgumentsCount,
                    valueArgumentsCount = it.owner.valueParameters.size,
                    origin = DEFAULT_DISPATCH_CALL,
                    superQualifierSymbol = superQualifierSymbol
                )
            }
        }
    }

    private fun parametersForCall(expression: IrFunctionAccessExpression): Pair<IrFunctionSymbol, List<IrExpression?>>? {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        val declaration = expression.symbol.owner

        // We *have* to find the actual function here since on the JVM, a default stub for a function implemented
        // in an interface does not leave an abstract method after being moved to DefaultImpls (see InterfaceLowering).
        // Calling the fake override on an implementation of that interface would then result in a call to a method
        // that does not actually exist as DefaultImpls is not part of the inheritance hierarchy.
        val stubFunction = declaration.findBaseFunctionWithDefaultArguments(skipInline, skipExternalMethods)
            ?.generateDefaultsFunction(
                context,
                skipInline,
                skipExternalMethods,
                forceSetOverrideSymbols,
                defaultArgumentStubVisibility(declaration),
                useConstructorMarker(declaration)
            ) ?: return null

        log { "$declaration -> $stubFunction" }

        val realArgumentsNumber = declaration.valueParameters.size
        val maskValues = IntArray((declaration.valueParameters.size + 31) / 32)
        assert((stubFunction.valueParameters.size - realArgumentsNumber - maskValues.size) in listOf(0, 1)) {
            "argument count mismatch: expected $realArgumentsNumber arguments + ${maskValues.size} masks + optional handler/marker, " +
                    "got ${stubFunction.valueParameters.size} total in ${stubFunction.render()}"
        }
        var sourceParameterIndex = -1
        return stubFunction.symbol to stubFunction.valueParameters.mapIndexed { i, parameter ->
            if (!parameter.isMovedReceiver()) {
                ++sourceParameterIndex
            }
            when {
                i >= realArgumentsNumber + maskValues.size -> IrConstImpl.constNull(startOffset, endOffset, parameter.type)
                i >= realArgumentsNumber -> IrConstImpl.int(startOffset, endOffset, parameter.type, maskValues[i - realArgumentsNumber])
                else -> {
                    val valueArgument = expression.getValueArgument(i)
                    if (valueArgument == null) {
                        maskValues[i / 32] = maskValues[i / 32] or (1 shl (sourceParameterIndex % 32))
                    }
                    valueArgument ?: nullConst(startOffset, endOffset, parameter)?.let {
                        IrCompositeImpl(
                            expression.startOffset,
                            expression.endOffset,
                            parameter.type,
                            IrStatementOrigin.DEFAULT_VALUE,
                            listOf(it)
                        )
                    }
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

    protected open fun defaultArgumentStubVisibility(function: IrFunction) = DescriptorVisibilities.PUBLIC

    protected open fun useConstructorMarker(function: IrFunction) = function is IrConstructor

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }
}

// Remove default argument initializers.
class DefaultParameterCleaner(
    val context: CommonBackendContext,
    val replaceDefaultValuesWithStubs: Boolean = false
) : DeclarationTransformer {
    override fun lower(irFile: IrFile) {
        runPostfix(true).toFileLoweringPass().lower(irFile)
    }

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrValueParameter && declaration.defaultValue != null) {
            if (replaceDefaultValuesWithStubs) {
                if (context.mapping.defaultArgumentsOriginalFunction[declaration.parent as IrFunction] == null) {
                    declaration.defaultValue = context.irFactory.createExpressionBody(
                        IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, declaration.type, "Default Stub")
                    )
                }
            } else {
                declaration.defaultValue = null
            }
        }
        return null
    }
}

// Sets overriden symbols. Should be used in case `forceSetOverrideSymbols = false`
class DefaultParameterPatchOverridenSymbolsLowering(
    val context: CommonBackendContext
) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            (context.mapping.defaultArgumentsOriginalFunction[declaration] as? IrSimpleFunction)?.run {
                declaration.overriddenSymbols += overriddenSymbols.mapNotNull {
                    (context.mapping.defaultArgumentsDispatchFunction[it.owner] as? IrSimpleFunction)?.symbol
                }
            }
        }

        return null
    }
}

private fun IrFunction.generateDefaultsFunction(
        context: CommonBackendContext,
        skipInlineMethods: Boolean,
        skipExternalMethods: Boolean,
        forceSetOverrideSymbols: Boolean,
        visibility: DescriptorVisibility,
        useConstructorMarker: Boolean
): IrFunction? {
    if (skipInlineMethods && isInline) return null
    if (skipExternalMethods && isExternalOrInheritedFromExternal()) return null
    if (context.mapping.defaultArgumentsOriginalFunction[this] != null) return null
    context.mapping.defaultArgumentsDispatchFunction[this]?.let { return it }
    if (this is IrSimpleFunction) {
        // If this is an override of a function with default arguments, produce a fake override of a default stub.
        if (overriddenSymbols.any { it.owner.findBaseFunctionWithDefaultArguments(skipInlineMethods, skipExternalMethods) != null })
            return generateDefaultsFunctionImpl(context, IrDeclarationOrigin.FAKE_OVERRIDE, visibility, true, useConstructorMarker).also {
                context.mapping.defaultArgumentsDispatchFunction[this] = it
                context.mapping.defaultArgumentsOriginalFunction[it] = this

                if (forceSetOverrideSymbols) {
                    (it as IrSimpleFunction).overriddenSymbols += overriddenSymbols.mapNotNull {
                        it.owner.generateDefaultsFunction(
                            context,
                            skipInlineMethods,
                            skipExternalMethods,
                            forceSetOverrideSymbols,
                            visibility,
                            useConstructorMarker
                        )?.symbol as IrSimpleFunctionSymbol?
                    }
                }
            }
    }
    // Note: this is intentionally done *after* checking for overrides. While normally `override fun`s
    // have no default parameters, there is an exception in case of interface delegation:
    //     interface I {
    //         fun f(x: Int = 1)
    //     }
    //     class C(val y: I) : I by y {
    //         // implicit `override fun f(x: Int) = y.f(x)` has a default value for `x`
    //     }
    // Since this bug causes the metadata serializer to write the "has default value" flag into compiled
    // binaries, it's way too late to fix it. Hence the workaround.
    if (valueParameters.any { it.defaultValue != null }) {
        return generateDefaultsFunctionImpl(
            context, IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER, visibility, false, useConstructorMarker
        ).also {
            context.mapping.defaultArgumentsDispatchFunction[this] = it
            context.mapping.defaultArgumentsOriginalFunction[it] = this
        }
    }
    return null
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrFunction.generateDefaultsFunctionImpl(
        context: CommonBackendContext,
        newOrigin: IrDeclarationOrigin,
        newVisibility: DescriptorVisibility,
        isFakeOverride: Boolean,
        useConstructorMarker: Boolean
): IrFunction {
    val newFunction = when (this) {
        is IrConstructor ->
            factory.buildConstructor {
                updateFrom(this@generateDefaultsFunctionImpl)
                origin = newOrigin
                isExternal = false
                isPrimary = false
                isExpect = false
                visibility = newVisibility
            }
        is IrSimpleFunction ->
            factory.buildFun {
                updateFrom(this@generateDefaultsFunctionImpl)
                name = Name.identifier("${this@generateDefaultsFunctionImpl.name}\$default")
                origin = newOrigin
                this.isFakeOverride = isFakeOverride
                modality = Modality.FINAL
                isExternal = false
                isTailrec = false
                visibility = newVisibility
            }
        else -> throw IllegalStateException("Unknown function type")
    }
    (newFunction as? IrAttributeContainer)?.copyAttributes(this@generateDefaultsFunctionImpl as? IrAttributeContainer)
    newFunction.copyTypeParametersFrom(this)
    newFunction.parent = parent
    newFunction.returnType = returnType.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
    newFunction.dispatchReceiverParameter = dispatchReceiverParameter?.copyTo(newFunction)
    newFunction.extensionReceiverParameter = extensionReceiverParameter?.copyTo(newFunction)

    newFunction.valueParameters = valueParameters.map {
        val newType = it.type.remapTypeParameters(classIfConstructor, newFunction.classIfConstructor)
        val makeNullable = it.defaultValue != null &&
                (context.ir.unfoldInlineClassType(it.type) ?: it.type) !in context.irBuiltIns.primitiveIrTypes
        it.copyTo(
            newFunction,
            type = if (makeNullable) newType.makeNullable() else newType,
            defaultValue = if (it.defaultValue != null) {
                factory.createExpressionBody(IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, "Default Stub"))
            } else null
        )
    }

    for (i in 0 until (valueParameters.size + 31) / 32) {
        newFunction.addValueParameter("mask$i".synthesizedString, context.irBuiltIns.intType, IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION)
    }
    if (useConstructorMarker) {
        val markerType = context.ir.symbols.defaultConstructorMarker.defaultType.makeNullable()
        newFunction.addValueParameter("marker".synthesizedString, markerType, IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER)
    } else if (context.ir.shouldGenerateHandlerParameterForDefaultBodyFun()) {
        newFunction.addValueParameter(
            "handler".synthesizedString,
            context.irBuiltIns.anyNType,
            IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION
        )
    }

    // TODO some annotations are needed (e.g. @JvmStatic), others need different values (e.g. @JvmName), the rest are redundant.
    newFunction.annotations = annotations.map { it.deepCopyWithSymbols() }
    return newFunction
}

private fun IrValueParameter.isMovedReceiver() =
    origin == IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER || origin == IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
