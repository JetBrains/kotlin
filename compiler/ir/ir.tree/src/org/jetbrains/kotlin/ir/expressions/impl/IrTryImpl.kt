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

import org.jetbrains.kotlin.ir.expressions.IrCatch
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTry
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.utils.SmartList

class IrTryImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
) : IrTry() {
    constructor(
        startOffset: Int,
        endOffset: Int,
        type: IrType,
        tryResult: IrExpression,
        catches: List<IrCatch>,
        finallyExpression: IrExpression?
    ) : this(startOffset, endOffset, type) {
        this.tryResult = tryResult
        this.catches.addAll(catches)
        this.finallyExpression = finallyExpression
    }

    override lateinit var tryResult: IrExpression

    override val catches: MutableList<IrCatch> = SmartList()

    override var finallyExpression: IrExpression? = null
}
