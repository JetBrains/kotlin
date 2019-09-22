/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    override val target: IrConstructor,
    override val descriptor: ClassConstructorDescriptor,
    typeArgumentsCount: Int,
    override val constructorTypeArgumentsCount: Int,
    valueArgumentsCount: Int,
    origin: IrStatementOrigin? = null
) :
    IrCallWithIndexedArgumentsBase(startOffset, endOffset, type, typeArgumentsCount, valueArgumentsCount, origin),
    IrConstructorCall {

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitConstructorCall(this, data)

    companion object {
        fun fromSubstitutedDescriptor(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            target: IrConstructor,
            constructorDescriptor: ClassConstructorDescriptor,
            origin: IrStatementOrigin? = null
        ): IrConstructorCallImpl {
            val classTypeParametersCount = constructorDescriptor.constructedClass.original.declaredTypeParameters.size
            val totalTypeParametersCount = constructorDescriptor.typeParameters.size
            val valueParametersCount = constructorDescriptor.valueParameters.size

            return IrConstructorCallImpl(
                startOffset, endOffset,
                type,
                target,
                constructorDescriptor,
                totalTypeParametersCount,
                totalTypeParametersCount - classTypeParametersCount,
                valueParametersCount,
                origin
            )
        }

        fun fromSymbolDescriptor(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            target: IrConstructor,
            origin: IrStatementOrigin? = null
        ): IrConstructorCallImpl =
            fromSubstitutedDescriptor(startOffset, endOffset, type, target, target.descriptor, origin)

        fun fromSymbolOwner(
            startOffset: Int,
            endOffset: Int,
            type: IrType,
            target: IrConstructor,
            origin: IrStatementOrigin? = null
        ): IrConstructorCallImpl {
            val constructedClass = target.parentAsClass
            val classTypeParametersCount = constructedClass.typeParameters.size
            val constructorTypeParametersCount = target.typeParameters.size
            val totalTypeParametersCount = classTypeParametersCount + constructorTypeParametersCount
            val valueParametersCount = target.valueParameters.size

            return IrConstructorCallImpl(
                startOffset, endOffset,
                type,
                target,
                target.descriptor,
                totalTypeParametersCount,
                constructorTypeParametersCount,
                valueParametersCount,
                origin
            )
        }

        fun fromSymbolOwner(
            type: IrType,
            target: IrConstructor,
            origin: IrStatementOrigin? = null
        ) =
            fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, target, origin)
    }
}

