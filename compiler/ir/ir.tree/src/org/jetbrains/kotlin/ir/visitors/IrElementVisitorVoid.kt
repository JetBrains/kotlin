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

package org.jetbrains.kotlin.ir.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*

interface IrElementVisitorVoid : IrElementVisitor<Unit, Nothing?> {
    fun visitElement(element: IrElement)
    fun visitDeclaration(declaration: IrDeclaration) = visitElement(declaration)
    fun visitCompoundDeclaration(declaration: IrCompoundDeclaration) = visitDeclaration(declaration)
    fun visitModule(declaration: IrModule) = visitCompoundDeclaration(declaration)
    fun visitFile(declaration: IrFile) = visitCompoundDeclaration(declaration)
    fun visitClass(declaration: IrClass) = visitCompoundDeclaration(declaration)
    fun visitFunction(declaration: IrFunction) = visitDeclaration(declaration)
    fun visitPropertyGetter(declaration: IrPropertyGetter) = visitFunction(declaration)
    fun visitPropertySetter(declaration: IrPropertySetter) = visitFunction(declaration)
    fun visitProperty(declaration: IrProperty) = visitDeclaration(declaration)

    // Delegating methods from IrDeclarationVisitor
    override fun visitElement(element: IrElement, data: Nothing?) = visitElement(element)
    override fun visitDeclaration(declaration: IrDeclaration, data: Nothing?) = visitDeclaration(declaration)
    override fun visitCompoundDeclaration(declaration: IrCompoundDeclaration, data: Nothing?) = visitCompoundDeclaration(declaration)
    override fun visitModule(declaration: IrModule, data: Nothing?) = visitModule(declaration)
    override fun visitFile(declaration: IrFile, data: Nothing?) = visitFile(declaration)
    override fun visitClass(declaration: IrClass, data: Nothing?) = visitClass(declaration)
    override fun visitFunction(declaration: IrFunction, data: Nothing?) = visitFunction(declaration)
    override fun visitPropertyGetter(declaration: IrPropertyGetter, data: Nothing?) = visitPropertyGetter(declaration)
    override fun visitPropertySetter(declaration: IrPropertySetter, data: Nothing?) = visitPropertySetter(declaration)
    override fun visitProperty(declaration: IrProperty, data: Nothing?) = visitProperty(declaration)
}