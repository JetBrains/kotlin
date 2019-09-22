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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.Name

fun IrClass.getPropertyDeclaration(name: String) =
    this.declarations.filterIsInstance<IrProperty>()
        .atMostOne { it.descriptor.name == Name.identifier(name) }

fun IrClass.getSimpleFunction(name: String): IrSimpleFunction? =
        findDeclaration<IrSimpleFunction> { it.name.asString() == name }

fun IrClass.getPropertyGetter(name: String): IrSimpleFunction? =
    this.getPropertyDeclaration(name)?.getter ?: this.getSimpleFunction("<get-$name>")

fun IrClass.getPropertySetter(name: String): IrSimpleFunction? =
    this.getPropertyDeclaration(name)?.setter ?: this.getSimpleFunction("<set-$name>")

fun IrClass.getPropertyField(name: String): IrField? =
    this.getPropertyDeclaration(name)?.backingField
