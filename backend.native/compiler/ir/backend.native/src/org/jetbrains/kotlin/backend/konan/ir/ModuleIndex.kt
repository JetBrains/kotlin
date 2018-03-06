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

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class ModuleIndex(val module: IrModuleFragment) {

    var currentFile: IrFile? = null
    /**
     * Contains all classes declared in [module]
     */
    val classes: Map<ClassDescriptor, IrClass>

    val enumEntries: Map<ClassDescriptor, IrEnumEntry>

    /**
     * Contains all functions declared in [module]
     */
    val functions = mutableMapOf<FunctionDescriptor, IrFunction>()
    val declarationToFile = mutableMapOf<DeclarationDescriptor, String>()

    init {
        val map = mutableMapOf<ClassDescriptor, IrClass>()
        enumEntries = mutableMapOf()

        module.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFile(declaration: IrFile) {
                currentFile = declaration
                super.visitFile(declaration)
            }

            override fun visitClass(declaration: IrClass) {
                super.visitClass(declaration)

                map[declaration.descriptor] = declaration
            }

            override fun visitEnumEntry(declaration: IrEnumEntry) {
                super.visitEnumEntry(declaration)

                enumEntries[declaration.descriptor] = declaration
            }

            override fun visitFunction(declaration: IrFunction) {
                super.visitFunction(declaration)
                functions[declaration.descriptor] = declaration
            }

            override fun visitDeclaration(declaration: IrDeclaration) {
                super.visitDeclaration(declaration)
                declarationToFile[declaration.descriptor] = currentFile!!.name
            }
        })

        classes = map
    }
}
