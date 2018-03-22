/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

interface IrValueParameter : IrSymbolDeclaration<IrValueParameterSymbol> {
    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.VALUE_PARAMETER

    override val descriptor: ParameterDescriptor

    val name: Name
    val index: Int
    val type: KotlinType
    val varargElementType: KotlinType?
    val isCrossinline: Boolean
    val isNoinline: Boolean

    var defaultValue: IrExpressionBody?

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrValueParameter
}