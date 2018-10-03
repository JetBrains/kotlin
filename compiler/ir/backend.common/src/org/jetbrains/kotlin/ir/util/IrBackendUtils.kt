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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.name.Name

fun IrClassSymbol.getPropertyDeclaration(name: String) =
    this.owner.declarations.filterIsInstance<IrProperty>()
        .atMostOne { it.descriptor.name == Name.identifier(name) }

fun IrClassSymbol.getPropertyGetter(name: String): IrFunctionSymbol? =
    this.getPropertyDeclaration(name)?.getter?.symbol

fun IrClassSymbol.getPropertySetter(name: String): IrFunctionSymbol? =
    this.getPropertyDeclaration(name)?.setter?.symbol

fun IrClassSymbol.getPropertyField(name: String): IrFieldSymbol? =
    this.getPropertyDeclaration(name)?.backingField?.symbol
