/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.state.Common
import org.jetbrains.kotlin.ir.interpreter.state.Complex
import org.jetbrains.kotlin.ir.interpreter.state.Primitive
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.Wrapper
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isSubclassOf

class IrInterpreterEnvironment(
    val irBuiltIns: IrBuiltIns,
    val configuration: IrInterpreterConfiguration = IrInterpreterConfiguration(),
) {
    internal val callStack: CallStack = CallStack()
    internal val irExceptions = mutableListOf<IrClass>()
    internal var mapOfEnums = mutableMapOf<IrSymbol, Complex>()
    internal var mapOfObjects = mutableMapOf<IrSymbol, Complex>()
    internal var javaClassToIrClass = mutableMapOf<Class<*>, IrClass>()

    private data class CacheFunctionSignature(
        val symbol: IrFunctionSymbol,

        // must create different invoke function for function expression with and without receivers
        val hasDispatchReceiver: Boolean,
        val hasExtensionReceiver: Boolean,

        // must create different default functions for constructor call and delegating call;
        // their symbols are the same but calls are different, so default function must return different calls
        val fromDelegatingCall: Boolean
    )

    private var functionCache = mutableMapOf<CacheFunctionSignature, IrFunctionSymbol>()

    init {
        mapOfObjects[irBuiltIns.unitClass] = Common(irBuiltIns.unitClass.owner)
    }

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
}
