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

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

interface IrTry : IrExpression {
    var tryResult: IrExpression

    val catches: List<IrCatch>

    var finallyExpression: IrExpression?
}

interface IrCatch : IrElement {
    val parameter: VariableDescriptor
    var catchParameter: IrVariable
    var result: IrExpression

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrCatch =
        super.transform(transformer, data) as IrCatch
}
