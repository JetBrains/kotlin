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

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType

class IrBlockImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var origin: IrStatementOrigin? = null,
) : IrBlock() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        origin: IrStatementOrigin?,
        statements: List<IrStatement>
    ) : this(startOffset, endOffset, type, origin) {
        this.statements.addAll(statements)
    }
}

fun IrBlockImpl.addIfNotNull(statement: IrStatement?) {
    if (statement != null) statements.add(statement)
}

fun IrBlockImpl.inlineStatement(statement: IrStatement) {
    if (statement is IrBlock) {
        statements.addAll(statement.statements)
    } else {
        statements.add(statement)
    }
}
