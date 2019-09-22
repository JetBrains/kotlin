/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrGetValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    target: IrValueDeclaration,
    override val origin: IrStatementOrigin? = null
) :
    IrTerminalDeclarationReferenceBase<IrValueDeclaration, ValueDescriptor>(
        startOffset,
        endOffset,
        type,
        target,
        target.descriptor
    ),
    IrGetValue {

    constructor(
        startOffset: Int,
        endOffset: Int,
        target: IrValueDeclaration,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, target.type, target, origin)

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitGetValue(this, data)

    override fun copy(): IrGetValue =
        IrGetValueImpl(startOffset, endOffset, type, target, origin)
}
