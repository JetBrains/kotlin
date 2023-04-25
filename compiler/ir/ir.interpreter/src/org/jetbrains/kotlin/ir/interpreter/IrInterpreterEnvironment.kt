/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.properties

class IrInterpreterEnvironment(
    val irBuiltIns: IrBuiltIns,
    val configuration: IrInterpreterConfiguration = IrInterpreterConfiguration(),
) {
    internal val callStack: CallStack = CallStack()
    internal val irExceptions = mutableListOf<IrClass>()
    internal var mapOfEnums = mutableMapOf<IrSymbol, Complex>()
    internal var mapOfObjects = mutableMapOf<IrSymbol, Complex>()
    internal var javaClassToIrClass = mutableMapOf<Class<*>, IrClass>()
    private var functionCache = mutableMapOf<CacheFunctionSignature, IrFunctionSymbol>()

    internal val kTypeParameterClass by lazy { irBuiltIns.kClassClass.getIrClassOfReflectionFromList("typeParameters")!! }
    internal val kParameterClass by lazy { irBuiltIns.kFunctionClass.getIrClassOfReflectionFromList("parameters")!! }
    internal val kTypeProjectionClass by lazy { kTypeClass.getIrClassOfReflectionFromList("arguments")!! }
    internal val kTypeClass: IrClassSymbol by lazy {
        // here we use fallback to `Any` because `KType` cannot be found on JS/Native by this way
        // but still this class is used to represent type arguments in interpreter
        irBuiltIns.kClassClass.getIrClassOfReflectionFromList("supertypes") ?: irBuiltIns.anyClass
    }

    init {
        mapOfObjects[irBuiltIns.unitClass] = Common(irBuiltIns.unitClass.owner)
    }

    private data class CacheFunctionSignature(
        val symbol: IrFunctionSymbol,

        // must create different invoke function for function expression with and without receivers
        val hasDispatchReceiver: Boolean,
        val hasExtensionReceiver: Boolean,

        // must create different default functions for constructor call and delegating call;
        // their symbols are the same but calls are different, so default function must return different calls
        val fromDelegatingCall: Boolean
    )

    private constructor(environment: IrInterpreterEnvironment) : this(environment.irBuiltIns, configuration = environment.configuration) {
        irExceptions.addAll(environment.irExceptions)
        mapOfEnums = environment.mapOfEnums
        mapOfObjects = environment.mapOfObjects
    }

    constructor(irModule: IrModuleFragment) : this(irModule.irBuiltins) {
        irExceptions.addAll(
            irModule.files
                .flatMap { it.declarations }
                .filterIsInstance<IrClass>()
                .filter { it.isSubclassOf(irBuiltIns.throwableClass.owner) }
        )
    }

    fun copyWithNewCallStack(): IrInterpreterEnvironment {
        return IrInterpreterEnvironment(this)
    }

    internal fun getCachedFunction(
        symbol: IrFunctionSymbol,
        hasDispatchReceiver: Boolean = false,
        hasExtensionReceiver: Boolean = false,
        fromDelegatingCall: Boolean = false
    ): IrFunctionSymbol? {
        return functionCache[CacheFunctionSignature(symbol, hasDispatchReceiver, hasExtensionReceiver, fromDelegatingCall)]
    }

    internal fun setCachedFunction(
        symbol: IrFunctionSymbol,
        hasDispatchReceiver: Boolean = false,
        hasExtensionReceiver: Boolean = false,
        fromDelegatingCall: Boolean = false,
        newFunction: IrFunctionSymbol
    ): IrFunctionSymbol {
        functionCache[CacheFunctionSignature(symbol, hasDispatchReceiver, hasExtensionReceiver, fromDelegatingCall)] = newFunction
        return newFunction
    }

    /**
     * Convert object from outer world to state
     */
    internal fun convertToState(value: Any?, irType: IrType): State {
        return when (value) {
            is Proxy -> value.state
            is State -> value
            is Boolean, is Char, is Byte, is Short, is Int, is Long, is String, is Float, is Double, is Array<*>, is ByteArray,
            is CharArray, is ShortArray, is IntArray, is LongArray, is FloatArray, is DoubleArray, is BooleanArray -> Primitive(value, irType)
            null -> Primitive.nullStateOfType(irType)
            else -> irType.classOrNull?.owner?.let { Wrapper(value, it, this) }
                ?: Wrapper(value, this.javaClassToIrClass[value::class.java]!!, this)
        }
    }

    internal fun stateToIrExpression(state: State, original: IrExpression): IrExpression {
        val start = original.startOffset
        val end = original.endOffset
        val type = original.type.makeNotNull()
        return when (state) {
            is Primitive<*> ->
                when {
                    configuration.treatFloatInSpecialWay && state.value is Float -> IrConstImpl.float(start, end, type, state.value)
                    configuration.treatFloatInSpecialWay && state.value is Double -> IrConstImpl.double(start, end, type, state.value)
                    state.value == null || type.isPrimitiveType() || type.isString() -> state.value.toIrConst(type, start, end)
                    else -> original // TODO support for arrays
                }
            is ExceptionState -> {
                val message = if (configuration.printOnlyExceptionMessage) state.getShortDescription() else "\n" + state.getFullDescription()
                IrErrorExpressionImpl(original.startOffset, original.endOffset, original.type, message)
            }
            is Complex -> {
                val stateType = state.irClass.defaultType
                when {
                    stateType.isUnsignedType() -> (state.fields.values.single() as Primitive<*>).value.toIrConst(type, start, end)
                    else -> original
                }
            }
            else -> original // TODO support
        }
    }

    private fun IrClassSymbol.getIrClassOfReflectionFromList(name: String): IrClassSymbol? {
        val property = this.owner.properties.singleOrNull { it.name.asString() == name }
        val list = property?.getter?.returnType as? IrSimpleType
        return list?.arguments?.single()?.typeOrNull?.classOrNull
    }
}
