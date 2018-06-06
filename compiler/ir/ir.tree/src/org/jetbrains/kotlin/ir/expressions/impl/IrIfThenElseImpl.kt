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

import org.jetbrains.kotlin.ir.expressions.IrBranch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.SmartList

class IrIfThenElseImpl(
    startOffset: Int, endOffset: Int, type: KotlinType,
    override val origin: IrStatementOrigin? = null
) : IrWhenBase(startOffset, endOffset, type) {
    override val branches: MutableList<IrBranch> = SmartList()

    constructor(
        startOffset: Int, endOffset: Int, type: KotlinType,
        condition: IrExpression,
        thenBranch: IrExpression,
        elseBranch: IrExpression? = null,
        origin: IrStatementOrigin? = null
    ) : this(startOffset, endOffset, type, origin) {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenBranch))
        if (elseBranch != null) {
            branches.add(IrBranchImpl.elseBranch(elseBranch))
        }
    }
}