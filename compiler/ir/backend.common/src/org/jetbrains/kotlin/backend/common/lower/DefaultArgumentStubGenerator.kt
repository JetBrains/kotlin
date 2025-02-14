/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedString
import org.jetbrains.kotlin.backend.common.ir.ValueRemapper
import org.jetbrains.kotlin.backend.common.lower.canHaveDefaultValue
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.copyAttributes
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.backend.common.lower.canHaveDefaultValue as canHaveDefaultValueImpl

// TODO: fix expect/actual default parameters

/**
 * Generates synthetic stubs for functions with default parameter values.
 */
open class DefaultArgumentStubGenerator<TContext : CommonBackendContext>(
    val context: TContext,
    private val factory: DefaultArgumentFunctionFactory,
    private val skipInlineMethods: Boolean = true,
    private val skipExternalMethods: Boolean = false,
    private val forceSetOverrideSymbols: Boolean = true
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction) {
            return lower(declaration)
        }

        return null
    }

    protected open fun IrFunction.resolveAnnotations(): List<IrConstructorCall> = copyAnnotations()

    protected open fun IrFunction.generateDefaultStubBody(originalDeclaration: IrFunction): IrBody {
        val newIrFunction = this
        val builder = context.createIrBuilder(newIrFunction.symbol)
        log { "$originalDeclaration -> $newIrFunction" }

        return context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += builder.irBlockBody(newIrFunction) {
                val variables = mutableMapOf<IrValueSymbol, IrValueSymbol>()

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
                originalDeclaration.parameters.forEachIndexed { index, parameter ->
                    variables[parameter.symbol] = newIrFunction.parameters[index].symbol
                }

                generateSuperCallHandlerCheckIfNeeded(originalDeclaration, newIrFunction)

                val intAnd = this@DefaultArgumentStubGenerator.context.symbols.getBinaryOperator(
                    OperatorNameConventions.AND, context.irBuiltIns.intType, context.irBuiltIns.intType
                )
                var defaultableParameterIndex = -1
                val variablesForDefaultParameters = originalDeclaration.parameters.map { valueParameter ->
                    val canHaveDefaultValue = valueParameter.canHaveDefaultValue()
                    if (canHaveDefaultValue) {
                        defaultableParameterIndex++
                    }
                    val parameter = newIrFunction.parameters[valueParameter.indexInParameters]
                    val remapped = valueParameter.defaultValue?.let { defaultValue ->
                        require(canHaveDefaultValue) { "Parameter ${valueParameter.render()} cannot have a default value" }
                        val mask = irGet(newIrFunction.parameters[originalDeclaration.parameters.size + defaultableParameterIndex / 32])
                        val bit = irInt(1 shl (defaultableParameterIndex % 32))
                        val defaultFlag =
                            irCallOp(intAnd, context.irBuiltIns.intType, mask, bit)

                        val expression = defaultValue.expression
                            .prepareToBeUsedIn(newIrFunction)
                            .transform(ValueRemapper(variables), null)

                        selectArgumentOrDefault(defaultFlag, parameter, expression)
                    }

                    variables[valueParameter.symbol] = (remapped ?: parameter).symbol
                    remapped
                }

                when (originalDeclaration) {
                    is IrConstructor -> +irDelegatingConstructorCall(originalDeclaration).apply {
                        passTypeArgumentsFrom(newIrFunction.parentAsClass)
                        // This is for Kotlin/Native, which differs from the other backends in that constructors
                        // apparently do have dispatch receivers (though *probably* not type arguments, but copy
                        // those as well just in case):
                        passTypeArgumentsFrom(newIrFunction, offset = newIrFunction.parentAsClass.typeParameters.size)
                        arguments.assignFrom(variablesForDefaultParameters zip newIrFunction.parameters) { irGet(it.first ?: it.second) }
                    }
                    is IrSimpleFunction -> +irReturn(dispatchToImplementation(originalDeclaration, newIrFunction, variablesForDefaultParameters))
                }
            }.statements
        }
    }

    private fun lower(irFunction: IrFunction): List<IrFunction>? {
        val newIrFunction =
            factory.generateDefaultsFunction(
                irFunction,
                skipInlineMethods,
                skipExternalMethods,
                forceSetOverrideSymbols,
                defaultArgumentStubVisibility(irFunction),
                useConstructorMarker(irFunction),
                irFunction.resolveAnnotations(),
            ) ?: return null

        return listOf(irFunction, newIrFunction).also {
            if (!newIrFunction.isFakeOverride) {
                newIrFunction.body = newIrFunction.generateDefaultStubBody(irFunction)
            }
        }
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
        // For the JVM backend, we have to generate precisely this code because that results in the
        // bytecode the inliner expects see `expandMaskConditionsAndUpdateVariableNodes`. In short,
        // the bytecode sequence should be
        //
        //     -- no loads of the parameter here, as after inlining its value will be uninitialized
        //     ILOAD <mask>
        //     ICONST <bit>
        //     IAND
        //     IFEQ Lx
        //     -- any code inserted here is removed if the call site specifies the parameter
        //     STORE <n>
        //     -- no jumps here
        //   Lx
        //
        // This control flow limits us to an if-then (without an else), and this together with the
        // restriction on loading the parameter in the default case means we cannot create any temporaries.
        +irIfThen(irNotEquals(defaultFlag, irInt(0)), irSet(parameter.symbol, default))
        return parameter
    }

    protected open fun getOriginForCallToImplementation(): IrStatementOrigin? = null

    private fun IrBlockBodyBuilder.dispatchToImplementation(
        irFunction: IrSimpleFunction,
        newIrFunction: IrFunction,
        variablesForDefaultParameters: List<IrValueDeclaration?>
    ): IrExpression =
        irCall(irFunction, origin = getOriginForCallToImplementation()).apply {
            passTypeArgumentsFrom(newIrFunction)
            symbol.owner.parameters.forEachIndexed { index, parameter ->
                val variable = variablesForDefaultParameters[index]
                if (variable == null) {
                    arguments[index] = irGet(newIrFunction.parameters[index])
                } else {
                    val paramType = parameter.type
                    // The JVM backend doesn't introduce new variables, and hence may have incompatible types here.
                    val value = if (!paramType.isNullable() && variable.type.isNullable()) {
                        irImplicitCast(irGet(variable), paramType)
                    } else {
                        irGet(variable)
                    }
                    arguments[index] = value
                }
            }
        }

    protected open fun IrBlockBodyBuilder.generateSuperCallHandlerCheckIfNeeded(
        irFunction: IrFunction,
        newIrFunction: IrFunction
    ) {
        //NO-OP Stub
    }

    protected open fun defaultArgumentStubVisibility(function: IrFunction) = DescriptorVisibilities.PUBLIC

    protected open fun useConstructorMarker(function: IrFunction) = function is IrConstructor

    private fun log(msg: () -> String) = context.log { "DEFAULT-REPLACER: ${msg()}" }
}

/**
 * Replaces call site with default parameters with the corresponding stub function.
 */
open class DefaultParameterInjector<TContext : CommonBackendContext>(
    protected val context: TContext,
    protected val factory: DefaultArgumentFunctionFactory,
    protected val skipInline: Boolean = true,
    protected val skipExternalMethods: Boolean = false,
    protected val forceSetOverrideSymbols: Boolean = true,
) : IrElementTransformerVoid(), BodyLoweringPass {

    private val declarationStack = mutableListOf<IrDeclaration>()
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        declarationStack.push(container)
        irBody.transformChildrenVoid(this)
        declarationStack.pop()
    }

    protected open fun shouldReplaceWithSyntheticFunction(functionAccess: IrFunctionAccessExpression): Boolean =
        functionAccess.arguments.any { it == null }

    private fun <T : IrFunctionAccessExpression> visitFunctionAccessExpression(expression: T, builder: (IrFunctionSymbol) -> T): IrExpression {
        if (!shouldReplaceWithSyntheticFunction(expression))
            return expression

        val symbol = createStubFunction(expression) ?: return expression
        for (i in expression.typeArguments.indices) {
            log { "$symbol[$i]: ${expression.typeArguments[i]}" }
        }
        val stubFunction = symbol.owner
        stubFunction.typeParameters.forEach { log { "$stubFunction[${it.index}] : $it" } }

        val declaration = expression.symbol.owner
        val currentDeclaration = declarationStack.last()
        return context.createIrBuilder(
            currentDeclaration.symbol, startOffset = expression.startOffset, endOffset = expression.endOffset
        ).irBlock {
            val newCall = builder(symbol).apply {
                val offset = if (needsTypeArgumentOffset(declaration)) declaration.parentAsClass.typeParameters.size else 0
                for (i in typeArguments.indices) {
                    typeArguments[i] = expression.typeArguments[i + offset]
                }
                val parameter2arguments = argumentsForCall(expression, stubFunction)

                for ((parameter, argument) in parameter2arguments) {
                    log { "call::params@$${parameter.indexInParameters}/${parameter.name}: ${ir2string(argument)}" }
                    if (argument != null) {
                        arguments[parameter.indexInParameters] = argument
                    }
                }
            }

            +irCastIfNeeded(newCall, expression.type)
        }.unwrapBlock()
    }

    private fun needsTypeArgumentOffset(declaration: IrFunction) =
        isStatic(declaration) && declaration.parentAsClass.isMultiFieldValueClass && declaration is IrSimpleFunction

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrDelegatingConstructorCallImpl(
                    startOffset, endOffset, type, it as IrConstructorSymbol,
                    typeArgumentsCount = typeArguments.size,
                )
            }
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrConstructorCallImpl.fromSymbolOwner(
                    startOffset,
                    endOffset,
                    type,
                    it as IrConstructorSymbol,
                    IrStatementOrigin.DEFAULT_DISPATCH_CALL
                )
            }
        }
    }

    override fun visitEnumConstructorCall(expression: IrEnumConstructorCall): IrExpression {
        expression.transformChildrenVoid()
        return visitFunctionAccessExpression(expression) {
            with(expression) {
                IrEnumConstructorCallImpl(
                    startOffset, endOffset, type, it as IrConstructorSymbol,
                    typeArgumentsCount = typeArguments.size,
                )
            }
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid()
        val declaration = expression.symbol.owner
        val typeParametersToRemove = if (needsTypeArgumentOffset(declaration)) declaration.parentAsClass.typeParameters.size else 0
        with(expression) {
            return visitFunctionAccessExpression(expression) {
                IrCallImpl(
                    startOffset, endOffset, (it as IrSimpleFunctionSymbol).owner.returnType, it,
                    typeArgumentsCount = typeArguments.size - typeParametersToRemove,
                    origin = IrStatementOrigin.DEFAULT_DISPATCH_CALL,
                    superQualifierSymbol = superQualifierSymbol
                )
            }
        }
    }

    private fun createStubFunction(expression: IrFunctionAccessExpression): IrFunctionSymbol? {
        val declaration = expression.symbol.owner

        // We *have* to find the actual function here since on the JVM, a default stub for a function implemented
        // in an interface does not leave an abstract method after being moved to DefaultImpls (see InterfaceLowering).
        // Calling the fake override on an implementation of that interface would then result in a call to a method
        // that does not actually exist as DefaultImpls is not part of the inheritance hierarchy.
        val baseFunction = factory.findBaseFunctionWithDefaultArgumentsFor(declaration, skipInline, skipExternalMethods)
        val stubFunction = baseFunction?.let {
            factory.generateDefaultsFunction(
                it,
                skipInline,
                skipExternalMethods,
                forceSetOverrideSymbols,
                defaultArgumentStubVisibility(declaration),
                useConstructorMarker(declaration),
                baseFunction.copyAnnotations(),
            )
        } ?: return null
        log { "$declaration -> $stubFunction" }
        return stubFunction.symbol
    }

    protected open fun IrBlockBuilder.argumentsForCall(expression: IrFunctionAccessExpression, stubFunction: IrFunction): Map<IrValueParameter, IrExpression?> {
        val startOffset = expression.startOffset
        val endOffset = expression.endOffset
        val declaration = expression.symbol.owner

        val defaultableParametersSize = declaration.parameters.count { it.canHaveDefaultValue() }
        val maskValues = IntArray((defaultableParametersSize + 31) / 32)

        assert(stubFunction.parameters.size - (declaration.parameters.size + maskValues.size) in 0..1) {
            "argument count mismatch: expected $defaultableParametersSize arguments + ${maskValues.size} masks + optional handler/marker, " +
                    "got ${stubFunction.parameters.size} total in ${stubFunction.render()}"
        }

        var sourceParameterIndex = -1
        return buildMap {
            for (parameter in stubFunction.parameters) {
                if (parameter.canHaveDefaultValue()) {
                    ++sourceParameterIndex
                }
                val newArgument = when {
                    sourceParameterIndex >= defaultableParametersSize + maskValues.size -> IrConstImpl.constNull(
                        startOffset,
                        endOffset,
                        parameter.type
                    )
                    sourceParameterIndex >= defaultableParametersSize -> IrConstImpl.int(
                        startOffset,
                        endOffset,
                        parameter.type,
                        maskValues[sourceParameterIndex - defaultableParametersSize]
                    )
                    else -> {
                        val valueArgument = expression.arguments[parameter.indexInParameters]
                        if (valueArgument == null) {
                            maskValues[sourceParameterIndex / 32] =
                                maskValues[sourceParameterIndex / 32] or (1 shl (sourceParameterIndex % 32))
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
                put(parameter, newArgument)
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

    protected open fun isStatic(function: IrFunction): Boolean = false

    private fun log(msg: () -> String) = context.log { "DEFAULT-INJECTOR: ${msg()}" }

    protected fun IrValueParameter.canHaveDefaultValue() = canHaveDefaultValueImpl()
}

/**
 * Removes default argument initializers.
 */
open class DefaultParameterCleaner(
    val context: CommonBackendContext,
    val replaceDefaultValuesWithStubs: Boolean = false
) : DeclarationTransformer {
    override val withLocalDeclarations: Boolean get() = true

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrValueParameter && declaration.defaultValue != null) {
            if (replaceDefaultValuesWithStubs) {
                if ((declaration.parent as IrFunction).defaultArgumentsOriginalFunction == null) {
                    declaration.defaultValue = context.irFactory.createExpressionBody(
                        IrErrorExpressionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, declaration.type, "Default Stub").apply {
                            attributeOwnerId = declaration.defaultValue!!.expression
                        }
                    )
                }
            } else {
                declaration.defaultValue = null
            }
        }
        return null
    }
}

/**
 * Patch overrides for fake override dispatch functions. Should be used in case `forceSetOverrideSymbols = false`.
 */
class DefaultParameterPatchOverridenSymbolsLowering(
    val context: CommonBackendContext
) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction) {
            (declaration.defaultArgumentsOriginalFunction as? IrSimpleFunction)?.run {
                declaration.overriddenSymbols = declaration.overriddenSymbols memoryOptimizedPlus overriddenSymbols.mapNotNull {
                    (it.owner.defaultArgumentsDispatchFunction as? IrSimpleFunction)?.symbol
                }
            }
        }

        return null
    }
}

open class MaskedDefaultArgumentFunctionFactory(context: CommonBackendContext, copyOriginalFunctionLocation: Boolean = true) : DefaultArgumentFunctionFactory(context, copyOriginalFunctionLocation) {
    final override fun IrFunction.generateDefaultArgumentStubFrom(original: IrFunction, useConstructorMarker: Boolean) {
        copyAttributes(original)
        copyTypeParametersFrom(original)
        copyReturnTypeFrom(original)
        copyValueParametersFrom(original)

        val originalDefaultableParametersCount =
            original.parameters.count { it.canHaveDefaultValue() }
        for (i in 0 until (originalDefaultableParametersCount + 31) / 32) {
            addValueParameter(
                "mask$i".synthesizedString,
                context.irBuiltIns.intType,
                IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION
            )
        }

        if (useConstructorMarker) {
            val markerType = context.symbols.defaultConstructorMarker.defaultType.makeNullable()
            addValueParameter("marker".synthesizedString, markerType, IrDeclarationOrigin.DEFAULT_CONSTRUCTOR_MARKER)
        } else if (context.shouldGenerateHandlerParameterForDefaultBodyFun) {
            addValueParameter(
                "handler".synthesizedString,
                context.irBuiltIns.anyNType,
                IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION
            )
        }
        context.remapMultiFieldValueClassStructure(
            original, this, parametersMappingOrNull = original.parameters.zip(parameters).toMap()
        )
    }
}

private fun IrValueParameter.canHaveDefaultValue() =
    kind != IrParameterKind.DispatchReceiver && kind != IrParameterKind.ExtensionReceiver &&
    origin != IrDeclarationOrigin.MOVED_DISPATCH_RECEIVER && origin != IrDeclarationOrigin.MOVED_EXTENSION_RECEIVER
