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

interface IrConst<T> : IrExpression, IrExpressionWithCopy {
    val kind: IrConstKind<T>
    val value: T

    override fun copy(): IrConst<T>
}

sealed class IrConstKind<T>(val asString: kotlin.String) {
    @Suppress("UNCHECKED_CAST")
    fun valueOf(aConst: IrConst<*>) =
        (aConst as IrConst<T>).value

    object Null : IrConstKind<Nothing?>("Null")
    object Boolean : IrConstKind<kotlin.Boolean>("Boolean")
    object Char : IrConstKind<kotlin.Char>("Char")
    object Byte : IrConstKind<kotlin.Byte>("Byte")
    object Short : IrConstKind<kotlin.Short>("Short")
    object Int : IrConstKind<kotlin.Int>("Int")
    object Long : IrConstKind<kotlin.Long>("Long")
    object String : IrConstKind<kotlin.String>("String")
    object Float : IrConstKind<kotlin.Float>("Float")
    object Double : IrConstKind<kotlin.Double>("Double")

    override fun toString() = asString
}

