/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

// backend.native/compiler/ir/backend.native/src/org/jetbrains/kotlin/backend/konan/ir/ModuleIndex.kt
class ModuleIndex(val module: IrModuleFragment) {

    var currentFile: IrFile? = null

    /**
     * Contains all classes declared in [module]
     */
    val classes = mutableMapOf<ClassDescriptor, IrClass>()

    val enumEntries = mutableMapOf<ClassDescriptor, IrEnumEntry>()

    /**
     * Contains all functions declared in [module]
     */
    val functions = mutableMapOf<FunctionDescriptor, IrFunction>()
    val declarationToFile = mutableMapOf<DeclarationDescriptor, String>()

    init {
        addModule(module)
    }

    fun addModule(module: IrModuleFragment) {
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

                classes[declaration.descriptor] = declaration
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
                declarationToFile[declaration.descriptor] = currentFile!!.path
            }
        })
    }
}
