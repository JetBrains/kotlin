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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

interface IrVariable : IrSymbolDeclaration<IrVariableSymbol> {
    override val descriptor: VariableDescriptor

    val name: Name
    val type: KotlinType
    val isVar: Boolean
    val isConst: Boolean
    val isLateinit: Boolean

    override val declarationKind: IrDeclarationKind
        get() = IrDeclarationKind.VARIABLE

    var initializer: IrExpression?
}

