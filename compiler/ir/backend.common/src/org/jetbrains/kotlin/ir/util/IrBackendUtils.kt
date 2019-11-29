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

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol

private fun IrClass.getPropertyDeclaration(name: String): IrProperty? {
    val properties = declarations.filterIsInstance<IrProperty>().filter { it.name.asString() == name }
    if (properties.size > 1) {
        error(
            "More than one property with name $name in class $fqNameWhenAvailable:\n" +
                    properties.joinToString("\n", transform = IrProperty::render)
        )
    }
    return properties.firstOrNull()
}

private fun IrClass.getSimpleFunction(name: String): IrSimpleFunctionSymbol? =
    findDeclaration<IrSimpleFunction> { it.name.asString() == name }?.symbol

fun IrClass.getPropertyGetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.getter?.symbol
        ?: getSimpleFunction("<get-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

fun IrClass.getPropertySetter(name: String): IrSimpleFunctionSymbol? =
    getPropertyDeclaration(name)?.setter?.symbol
        ?: getSimpleFunction("<set-$name>").also { assert(it?.owner?.correspondingPropertySymbol?.owner?.name?.asString() == name) }

fun IrClassSymbol.getSimpleFunction(name: String): IrSimpleFunctionSymbol? = owner.getSimpleFunction(name)
fun IrClassSymbol.getPropertyGetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertyGetter(name)
fun IrClassSymbol.getPropertySetter(name: String): IrSimpleFunctionSymbol? = owner.getPropertySetter(name)
