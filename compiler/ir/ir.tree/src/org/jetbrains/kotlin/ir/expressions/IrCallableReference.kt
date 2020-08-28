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
import org.jetbrains.kotlin.name.Name

abstract class IrCallableReference<S : IrSymbol>(typeArgumentsCount: Int) : IrMemberAccessExpression<S>(typeArgumentsCount) {
    abstract val referencedName: Name
}

abstract class IrFunctionReference(typeArgumentsCount: Int) : IrCallableReference<IrFunctionSymbol>(typeArgumentsCount) {
    abstract val reflectionTarget: IrFunctionSymbol?
}

val IrFunctionReference.isWithReflection: Boolean
    get() = reflectionTarget != null

val IrFunctionReference.isAdapterWithReflection: Boolean
    get() = reflectionTarget != null && reflectionTarget != symbol

abstract class IrPropertyReference(typeArgumentsCount: Int) : IrCallableReference<IrPropertySymbol>(typeArgumentsCount) {
    abstract val field: IrFieldSymbol?
    abstract val getter: IrSimpleFunctionSymbol?
    abstract val setter: IrSimpleFunctionSymbol?

    override val valueArgumentsCount: Int
        get() = 0
}

abstract class IrLocalDelegatedPropertyReference : IrCallableReference<IrLocalDelegatedPropertySymbol>(0) {
    abstract val delegate: IrVariableSymbol
    abstract val getter: IrSimpleFunctionSymbol
    abstract val setter: IrSimpleFunctionSymbol?

    override val valueArgumentsCount: Int
        get() = 0
}
