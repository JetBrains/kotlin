/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsCommonBackendContext
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import org.jetbrains.kotlin.utils.toSmartList

/**
 * Constructs bridges for inherited generic functions
 *
 *  Example: for given class hierarchy
 *
 *          class C<T>  {
 *            fun foo(t: T) = ...
 *          }
 *
 *          class D : C<Int> {
 *            override fun foo(t: Int) = impl
 *          }
 *
 *  it adds method D that delegates generic calls to implementation:
 *
 *          class D : C<Int> {
 *            override fun foo(t: Int) = impl
 *            fun foo(t: Any?) = foo(t as Int)  // Constructed bridge
 *          }
 */
abstract class BridgesConstruction(private val context: JsCommonBackendContext) : DeclarationTransformer {

    private val specialBridgeMethods = SpecialBridgeMethods(context)

    abstract fun getFunctionSignature(function: IrSimpleFunction): Any

    /**
     * Given a concrete function, finds an implementation (a concrete declaration) of this function in the supertypes.
     * The implementation is guaranteed to exist because if it wouldn't, the given function would've been abstract
     */
    abstract fun findConcreteSuperDeclaration(function: IrSimpleFunction): IrSimpleFunction

    /**
     * Usually just returns [irFunction]'s parameters, but special transformations may be required if,
     * for example, we're dealing with an external function, and that function contains a vararg,
     * which we must extract and convert to an array.
     */
    protected open fun extractParameters(
        blockBodyBuilder: IrBlockBodyBuilder,
        irFunction: IrSimpleFunction,
        bridge: IrSimpleFunction
    ): List<IrValueDeclaration> = irFunction.parameters

    // Should dispatch receiver type be casted inside a bridge.
    open val shouldCastDispatchReceiver: Boolean = false

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrSimpleFunction || declaration.isStaticMethodOfClass || declaration.parent !is IrClass) return null

        return generateBridges(declaration)?.let { listOf(declaration) + it }
    }

    private fun dfsForOverrides(currentFunction: IrSimpleFunction, afterChild: (IrSimpleFunction) -> Unit) {
        dfsForOverrides(currentFunction.overriddenSymbols, afterChild)
        if (currentFunction.isRealOrOverridesInterface) {
            afterChild(currentFunction)
        }
    }

    private fun dfsForOverrides(functions: List<IrSimpleFunctionSymbol>, afterChild: (IrSimpleFunction) -> Unit) {
        for (currentFunction in functions) {
            dfsForOverrides(currentFunction.owner, afterChild)
        }
    }

    private fun generateBridges(function: IrSimpleFunction): List<IrDeclaration>? {
        // If it's an abstract function, no bridges are needed: when an implementation will appear in some concrete subclass, all necessary
        // bridges will be generated there
        if (function.modality == Modality.ABSTRACT) return null

        val (bridgesDfsRoots, implementedDfsRoots) =
            if (function.isRealOrOverridesInterface) function.overriddenSymbols to emptyList()
            else function.overriddenSymbols.partition { it.owner.modality == Modality.ABSTRACT }

        // If it's a concrete fake override and all of its super-functions are concrete, then every possible bridge is already generated
        // into some of the super-classes and will be inherited in this class
        if (bridgesDfsRoots.isEmpty()) return null

        val implementation = findConcreteSuperDeclaration(function)
        val implementationSignature = getFunctionSignature(implementation)

        val implementedBridges = mutableSetOf<Any>()
        dfsForOverrides(implementedDfsRoots) { override ->
            implementedBridges.add(getFunctionSignature(override))
        }

        val bridgesToGenerate = mutableMapOf<Any, IrSimpleFunction>()
        dfsForOverrides(bridgesDfsRoots) { override ->
            val functionSignature = getFunctionSignature(override)
            if (functionSignature != implementationSignature && functionSignature !in implementedBridges) {
                bridgesToGenerate.putIfAbsent(functionSignature, override)
            }
        }

        if (bridgesToGenerate.isEmpty()) return null

        val (specialOverride: IrSimpleFunction?, specialOverrideInfo) =
            specialBridgeMethods.findSpecialWithOverride(function) ?: Pair(null, null)
        val specialOverrideSignature = specialOverride?.let(::getFunctionSignature)

        val result = mutableListOf<IrDeclaration>()
        for ((bridgeSignature, bridgeMethod) in bridgesToGenerate) {
            result += createBridge(
                function = function,
                bridge = bridgeMethod,
                delegateTo = implementation,
                specialMethodInfo = specialOverrideInfo.takeIf { specialOverrideSignature == bridgeSignature }
            )
        }
        return result
    }

    private fun createBridge(
        function: IrSimpleFunction,
        bridge: IrSimpleFunction,
        delegateTo: IrSimpleFunction,
        specialMethodInfo: SpecialMethodWithDefaultInfo?
    ): IrFunction {

        val origin = getBridgeOrigin(bridge)

        // TODO: Support offsets for debug info
        val irFunction = context.irFactory.buildFun {
            updateFrom(bridge)
            this.startOffset = UNDEFINED_OFFSET
            this.endOffset = UNDEFINED_OFFSET
            this.name = bridge.name
            this.returnType = bridge.returnType
            this.modality = Modality.FINAL
            this.isFakeOverride = false
            this.origin = origin
        }.apply {
            parent = function.parent
            copyTypeParametersFrom(bridge)
            val substitutionMap = makeTypeParameterSubstitutionMap(bridge, this)
            copyValueParametersFrom(bridge, substitutionMap)
            annotations = annotations memoryOptimizedPlus bridge.annotations
            // the js function signature building process (jsFunctionSignature()) uses dfs throught overriddenSymbols for getting js name,
            // therefore it is very important to put bridge symbol at the beginning, it allows to get correct js function name
            overriddenSymbols = mutableSetOf(bridge.symbol).also {
                it.addAll(overriddenSymbols)
                it.addAll(delegateTo.overriddenSymbols)
            }.toSmartList()
        }

        irFunction.body = context.irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
            statements += context.createIrBuilder(irFunction.symbol).irBlockBody(irFunction) {
                val extractedParameters = extractParameters(this, irFunction, bridge)
                if (specialMethodInfo != null) {
                    extractedParameters
                        .map { it as? IrValueParameter ?: compilationException("Expected a value parameter", it) }
                        .filter { it.kind != IrParameterKind.DispatchReceiver }
                        .take(specialMethodInfo.argumentsToCheck)
                        .forEach { parameter ->
                            +irIfThen(
                                context.irBuiltIns.unitType,
                                irNot(irIs(irGet(parameter), delegateTo.parameters[parameter.indexInParameters].type)),
                                irReturn(specialMethodInfo.defaultValueGenerator(irFunction))
                            )
                        }
                }

                val call = irCall(delegateTo.symbol)
                for (delegateParam in delegateTo.parameters) {
                    if ((delegateTo.isSuspend xor irFunction.isSuspend) && delegateParam.indexInParameters == extractedParameters.lastIndex) {
                        break
                    }

                    var argument: IrExpression = irGet(extractedParameters[delegateParam.indexInParameters])
                    if (delegateParam.kind != IrParameterKind.DispatchReceiver || shouldCastDispatchReceiver) {
                        argument = irCastIfNeeded(argument, delegateParam.type)
                    }
                    call.arguments[delegateParam] = argument
                }

                +irReturn(call)
            }.statements
        }

        return irFunction
    }

    /**
     * Copies the value parameters from [bridge] to [this]. If [bridge] is external and contains a vararg parameter,
     * only copies the parameters before the vararg.
     * The rest parameters are expected to be obtained later using the `arguments` object in JS.
     */
    private fun IrSimpleFunction.copyValueParametersFrom(bridge: IrSimpleFunction, substitutionMap: Map<IrTypeParameterSymbol, IrType>) {
        var valueParametersToCopy = bridge.parameters
        if (bridge.isEffectivelyExternal()) {
            valueParametersToCopy = valueParametersToCopy.takeWhile { it.varargElementType == null }
        }
        parameters = parameters memoryOptimizedPlus valueParametersToCopy.map { p ->
            p.copyTo(
                startOffset = UNDEFINED_OFFSET, // The offsets must be UNDEFINED because the bridge could come from another file
                endOffset = UNDEFINED_OFFSET,
                irFunction = this,
                type = p.type.substitute(substitutionMap)
            )
        }
    }

    abstract fun getBridgeOrigin(bridge: IrSimpleFunction): IrDeclarationOrigin

    // TODO: get rid of Unit check
    private fun IrBlockBodyBuilder.irCastIfNeeded(argument: IrExpression, type: IrType): IrExpression =
        if (argument.type.classifierOrNull == type.classifierOrNull) argument else irAs(argument, type)

    protected val IrSimpleFunction.isRealOrOverridesInterface
        get() = isReal || resolveFakeOverride()?.parentAsClass?.isInterface == true
}