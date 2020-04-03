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

import org.jetbrains.kotlin.ir.symbols.*

interface IrCallableReference : IrMemberAccessExpression

interface IrFunctionReference : IrCallableReference, IrFunctionAccessExpression {
    val reflectionTarget: IrFunctionSymbol?
}

val IrFunctionReference.isWithReflection: Boolean
    get() = reflectionTarget != null

val IrFunctionReference.isAdapterWithReflection: Boolean
    get() = reflectionTarget != null && reflectionTarget != symbol

interface IrPropertyReference : IrCallableReference {
    override val symbol: IrPropertySymbol
    val field: IrFieldSymbol?
    val getter: IrSimpleFunctionSymbol?
    val setter: IrSimpleFunctionSymbol?
}

interface IrLocalDelegatedPropertyReference : IrCallableReference {
    override val symbol: IrLocalDelegatedPropertySymbol
    val delegate: IrVariableSymbol
    val getter: IrSimpleFunctionSymbol
    val setter: IrSimpleFunctionSymbol?
}
