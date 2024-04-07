/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType

class IrCompositeImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin? = null,
) : IrComposite() {
    override val statements: MutableList<IrStatement> = ArrayList(2)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null

    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        origin: IrStatementOrigin?,
        statements: List<IrStatement>,
    ) : this(startOffset, endOffset, type, origin) {
        this.statements.addAll(statements)
    }
}
