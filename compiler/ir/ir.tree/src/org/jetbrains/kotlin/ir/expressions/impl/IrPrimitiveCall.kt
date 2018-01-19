/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallWithShallowCopy
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.lang.UnsupportedOperationException

abstract class IrPrimitiveCallBase(
    startOffset: Int,
    endOffset: Int,
    override val origin: IrStatementOrigin?,
    override val symbol: IrFunctionSymbol
) : IrExpressionBase(startOffset, endOffset, symbol.descriptor.returnType!!), IrCall {
    override val descriptor: FunctionDescriptor get() = symbol.descriptor
    override val superQualifier: ClassDescriptor? get() = null
    override val superQualifierSymbol: IrClassSymbol? get() = null

    override var dispatchReceiver: IrExpression?
        get() = null
        set(value) {
            if (value != null)
                throw UnsupportedOperationException("Operator call expression can't have a receiver")
        }

    override var extensionReceiver: IrExpression?
        get() = null
        set(value) {
            if (value != null)
                throw UnsupportedOperationException("Operator call expression can't have a receiver")
        }

    override fun getTypeArgument(typeParameterDescriptor: TypeParameterDescriptor): KotlinType? =
        null // IR primitives have no type parameters

    override fun removeValueArgument(index: Int) {
        throw AssertionError("Operator call expression can't have a default argument")
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitCall(this, data)
    }

    companion object {
        const val ARGUMENT0 = 0
        const val ARGUMENT1 = 1
    }
}

class IrNullaryPrimitiveImpl(startOffset: Int, endOffset: Int, origin: IrStatementOrigin?, symbol: IrFunctionSymbol) :
    IrPrimitiveCallBase(startOffset, endOffset, origin, symbol), IrCallWithShallowCopy {
    override fun getValueArgument(index: Int): IrExpression? = null

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        throw UnsupportedOperationException("Nullary operator $descriptor doesn't have arguments")
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // no children
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // no children
    }

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: IrFunctionSymbol, newSuperQualifier: IrClassSymbol?): IrCall =
        IrNullaryPrimitiveImpl(startOffset, endOffset, newOrigin, newCallee)

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: FunctionDescriptor, newSuperQualifier: ClassDescriptor?): IrCall =
        IrNullaryPrimitiveImpl(startOffset, endOffset, newOrigin, createFunctionSymbol(newCallee))
}

class IrUnaryPrimitiveImpl(startOffset: Int, endOffset: Int, origin: IrStatementOrigin?, symbol: IrFunctionSymbol) :
    IrPrimitiveCallBase(startOffset, endOffset, origin, symbol), IrCallWithShallowCopy {
    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int, endOffset: Int,
        origin: IrStatementOrigin?,
        functionDescriptor: FunctionDescriptor
    ) : this(
        startOffset, endOffset, origin,
        createFunctionSymbol(functionDescriptor)
    )


    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int, endOffset: Int,
        origin: IrStatementOrigin?,
        functionDescriptor: FunctionDescriptor,
        argument: IrExpression
    ) : this(
        startOffset, endOffset, origin,
        createFunctionSymbol(functionDescriptor),
        argument
    )

    constructor(
        startOffset: Int, endOffset: Int,
        origin: IrStatementOrigin?,
        symbol: IrFunctionSymbol,
        argument: IrExpression
    ) : this(startOffset, endOffset, origin, symbol) {
        this.argument = argument
    }

    lateinit var argument: IrExpression

    override fun getValueArgument(index: Int): IrExpression? {
        return when (index) {
            ARGUMENT0 -> argument
            else -> null
        }
    }

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        when (index) {
            ARGUMENT0 -> argument = valueArgument ?: throw AssertionError("Primitive call $descriptor argument is null")
            else -> throw AssertionError("Primitive call $descriptor: no such argument index $index")
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        argument = argument.transform(transformer, data)
    }

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: IrFunctionSymbol, newSuperQualifier: IrClassSymbol?): IrCall =
        IrUnaryPrimitiveImpl(startOffset, endOffset, newOrigin, newCallee)

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: FunctionDescriptor, newSuperQualifier: ClassDescriptor?): IrCall =
        IrUnaryPrimitiveImpl(startOffset, endOffset, newOrigin, newCallee)
}

class IrBinaryPrimitiveImpl(startOffset: Int, endOffset: Int, origin: IrStatementOrigin?, symbol: IrFunctionSymbol) :
    IrPrimitiveCallBase(startOffset, endOffset, origin, symbol), IrCallWithShallowCopy {
    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int, endOffset: Int, origin: IrStatementOrigin?,
        descriptor: FunctionDescriptor
    ) : this(
        startOffset, endOffset, origin,
        createFunctionSymbol(descriptor)
    )

    @Deprecated("Creates unbound symbol")
    constructor(
        startOffset: Int, endOffset: Int, origin: IrStatementOrigin?,
        descriptor: FunctionDescriptor,
        argument0: IrExpression, argument1: IrExpression
    ) : this(
        startOffset, endOffset, origin,
        createFunctionSymbol(descriptor),
        argument0, argument1
    )

    constructor(
        startOffset: Int, endOffset: Int, origin: IrStatementOrigin?,
        symbol: IrFunctionSymbol,
        argument0: IrExpression, argument1: IrExpression
    ) : this(startOffset, endOffset, origin, symbol) {
        this.argument0 = argument0
        this.argument1 = argument1
    }

    lateinit var argument0: IrExpression
    lateinit var argument1: IrExpression

    override fun getValueArgument(index: Int): IrExpression? {
        return when (index) {
            ARGUMENT0 -> argument0
            ARGUMENT1 -> argument1
            else -> null
        }
    }

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        val argument = valueArgument ?: throw AssertionError("Primitive call $descriptor argument is null")
        when (index) {
            ARGUMENT0 -> argument0 = argument
            ARGUMENT1 -> argument1 = argument
            else -> throw AssertionError("Primitive call $descriptor: no such argument index $index")
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        argument0.accept(visitor, data)
        argument1.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        argument0 = argument0.transform(transformer, data)
        argument1 = argument1.transform(transformer, data)
    }

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: IrFunctionSymbol, newSuperQualifier: IrClassSymbol?): IrCall =
        IrBinaryPrimitiveImpl(startOffset, endOffset, newOrigin, newCallee)

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: FunctionDescriptor, newSuperQualifier: ClassDescriptor?): IrCall =
        IrBinaryPrimitiveImpl(startOffset, endOffset, newOrigin, newCallee)
}
