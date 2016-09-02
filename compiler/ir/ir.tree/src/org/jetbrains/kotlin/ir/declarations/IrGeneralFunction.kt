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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

interface IrGeneralFunction : IrDeclaration {
    override val descriptor: FunctionDescriptor
    var body: IrBody?

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.FUNCTION
}

interface IrFunction : IrGeneralFunction {
    fun putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody)
    fun getDefault(parameter: ValueParameterDescriptor): IrExpressionBody?
}

abstract class IrGeneralFunctionBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrDeclarationBase(startOffset, endOffset, origin), IrGeneralFunction {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            body: IrBody
    ) : this(startOffset, endOffset, origin) {
        this.body = body
    }

    final override var body: IrBody? = null
        set(newValue) {
            newValue?.assertDetached()
            field?.detach()
            field = newValue
            newValue?.setTreeLocation(this, FUNCTION_BODY_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                FUNCTION_BODY_SLOT -> body
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            FUNCTION_BODY_SLOT -> body = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body?.accept(visitor, data)
    }
}

abstract class IrFunctionBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrGeneralFunctionBase(startOffset, endOffset, origin), IrFunction {
    private val defaults = LinkedHashMap<ValueParameterDescriptor, IrExpressionBody>()

    override fun getDefault(parameter: ValueParameterDescriptor): IrExpressionBody? =
            defaults[parameter]

    override fun putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody) {
        expressionBody.assertDetached()
        defaults[parameter]?.detach()
        defaults[parameter] = expressionBody
        expressionBody.setTreeLocation(this, parameter.index)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaults.values.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }
}

class IrFunctionImpl(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin,
        override val descriptor: FunctionDescriptor
) : IrFunctionBase(startOffset, endOffset, origin) {
    constructor(
            startOffset: Int,
            endOffset: Int,
            origin: IrDeclarationOrigin,
            descriptor: FunctionDescriptor,
            body: IrBody?
    ) : this(startOffset, endOffset, origin, descriptor) {
        this.body = body
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitFunction(this, data)
}

