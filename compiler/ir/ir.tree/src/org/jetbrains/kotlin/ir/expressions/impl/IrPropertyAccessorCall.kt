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
import org.jetbrains.kotlin.ir.symbols.impl.createClassSymbolOrNull
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.lang.AssertionError
import java.lang.UnsupportedOperationException

abstract class IrPropertyAccessorCallBase(
    startOffset: Int, endOffset: Int,
    override val symbol: IrFunctionSymbol,
    override val descriptor: FunctionDescriptor,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    override val origin: IrStatementOrigin? = null,
    override val superQualifierSymbol: IrClassSymbol? = null
) : IrMemberAccessExpressionBase(startOffset, endOffset, descriptor.returnType!!, typeArguments), IrCall {
    override val superQualifier: ClassDescriptor? get() = superQualifierSymbol?.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitCall(this, data)
    }

    companion object {
        const val SETTER_ARGUMENT_INDEX = 0
    }
}

class IrGetterCallImpl(
    startOffset: Int, endOffset: Int,
    symbol: IrFunctionSymbol,
    descriptor: FunctionDescriptor,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null
) : IrPropertyAccessorCallBase(startOffset, endOffset, symbol, descriptor, typeArguments, origin, superQualifierSymbol),
    IrCallWithShallowCopy {
    constructor(
        startOffset: Int, endOffset: Int,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        origin: IrStatementOrigin? = null,
        superQualifierSymbol: IrClassSymbol? = null
    ) : this(startOffset, endOffset, symbol, descriptor, typeArguments, origin, superQualifierSymbol) {
        this.dispatchReceiver = dispatchReceiver
        this.extensionReceiver = extensionReceiver
    }

    override fun getValueArgument(index: Int): IrExpression? = null

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        throw UnsupportedOperationException("Property setter call has no arguments")
    }

    override fun removeValueArgument(index: Int) {
        throw UnsupportedOperationException("Property getter call has no arguments")
    }

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: IrFunctionSymbol, newSuperQualifier: IrClassSymbol?): IrCall =
        IrGetterCallImpl(
            startOffset, endOffset, newCallee,
            descriptor, // TODO substitute descriptor for new callee?
            typeArguments, dispatchReceiver, extensionReceiver, newOrigin, newSuperQualifier
        )

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: FunctionDescriptor, newSuperQualifier: ClassDescriptor?): IrCall =
        IrGetterCallImpl(
            startOffset, endOffset,
            createFunctionSymbol(newCallee),
            newCallee,
            typeArguments, dispatchReceiver, extensionReceiver,
            newOrigin,
            createClassSymbolOrNull(newSuperQualifier)
        )
}

class IrSetterCallImpl(
    startOffset: Int, endOffset: Int,
    symbol: IrFunctionSymbol,
    descriptor: FunctionDescriptor,
    typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null
) : IrPropertyAccessorCallBase(startOffset, endOffset, symbol, descriptor, typeArguments, origin, superQualifierSymbol),
    IrCallWithShallowCopy {
    constructor(
        startOffset: Int, endOffset: Int,
        symbol: IrFunctionSymbol,
        descriptor: FunctionDescriptor,
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        dispatchReceiver: IrExpression?,
        extensionReceiver: IrExpression?,
        argument: IrExpression,
        origin: IrStatementOrigin? = null,
        superQualifierSymbol: IrClassSymbol? = null
    ) : this(startOffset, endOffset, symbol, descriptor, typeArguments, origin, superQualifierSymbol) {
        this.dispatchReceiver = dispatchReceiver
        this.extensionReceiver = extensionReceiver
        putValueArgument(SETTER_ARGUMENT_INDEX, argument)
    }

    private var argumentImpl: IrExpression? = null

    override fun getValueArgument(index: Int): IrExpression? =
        if (index == SETTER_ARGUMENT_INDEX) argumentImpl!! else null

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index != SETTER_ARGUMENT_INDEX) throw AssertionError("Property setter call $descriptor has no argument $index")
        argumentImpl = valueArgument
    }

    override fun removeValueArgument(index: Int) {
        if (index != SETTER_ARGUMENT_INDEX) throw AssertionError("Property setter call $descriptor has no argument $index")
        argumentImpl = null
    }

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: IrFunctionSymbol, newSuperQualifier: IrClassSymbol?): IrCall =
        IrSetterCallImpl(
            startOffset, endOffset, newCallee,
            descriptor, // TODO substitute newCallee.descriptor?
            typeArguments, newOrigin, newSuperQualifier
        )

    override fun shallowCopy(newOrigin: IrStatementOrigin?, newCallee: FunctionDescriptor, newSuperQualifier: ClassDescriptor?): IrCall =
        IrSetterCallImpl(
            startOffset, endOffset,
            createFunctionSymbol(newCallee),
            newCallee,
            typeArguments,
            newOrigin,
            createClassSymbolOrNull(newSuperQualifier)
        )

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        super.transformChildren(transformer, data)
        argumentImpl = argumentImpl?.transform(transformer, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super.acceptChildren(visitor, data)
        argumentImpl?.accept(visitor, data)
    }
}
