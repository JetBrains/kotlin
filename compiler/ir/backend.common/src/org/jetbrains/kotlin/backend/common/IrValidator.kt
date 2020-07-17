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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun validateIrFile(context: CommonBackendContext, irFile: IrFile) {
    val visitor = IrValidator(context, IrValidatorConfig(abortOnError = false, ensureAllNodesAreDifferent = false))
    irFile.acceptVoid(visitor)
}

fun validateIrModule(context: CommonBackendContext, irModule: IrModuleFragment) {
    val visitor = IrValidator(
        context,
        IrValidatorConfig(abortOnError = false, ensureAllNodesAreDifferent = true)
    ) // TODO: consider taking the boolean from settings.
    irModule.acceptVoid(visitor)

    // TODO: also check that all referenced symbol targets are reachable.
}

private fun CommonBackendContext.reportIrValidationError(message: String, irFile: IrFile?, irElement: IrElement) {
    try {
        this.reportWarning("[IR VALIDATION] $message", irFile, irElement)
    } catch (e: Throwable) {
        println("an error trying to print a warning message: $e")
        e.printStackTrace()
    }
    // TODO: throw an exception after fixing bugs leading to invalid IR.
}

data class IrValidatorConfig(
    val abortOnError: Boolean,
    val ensureAllNodesAreDifferent: Boolean,
    val checkTypes: Boolean = true,
    val checkDescriptors: Boolean = true,
    val checkProperties: Boolean = false,
)

class IrValidator(val context: CommonBackendContext, val config: IrValidatorConfig) : IrElementVisitorVoid {

    val irBuiltIns = context.irBuiltIns
    var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
    }

    private fun error(element: IrElement, message: String) {
        // TODO: render all element's parents.
        context.reportIrValidationError(
            "$message\n" + element.render(),
            currentFile,
            element
        )

        if (config.abortOnError) {
            error("Validation failed")
        }
    }

    private val elementChecker = CheckIrElementVisitor(irBuiltIns, this::error, config)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }
}

fun IrModuleFragment.checkDeclarationParents() {
    this.accept(CheckDeclarationParentsVisitor, null)
}

object CheckDeclarationParentsVisitor : IrElementVisitor<Unit, IrDeclarationParent?> {

    override fun visitElement(element: IrElement, data: IrDeclarationParent?) {
        element.acceptChildren(this, element as? IrDeclarationParent ?: data)
    }

    override fun visitDeclaration(declaration: IrDeclaration, data: IrDeclarationParent?) {
        checkParent(declaration, data)
        super.visitDeclaration(declaration, data)
    }

    private fun checkParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?) {
        val parent = try {
            declaration.parent
        } catch (e: Throwable) {
            error("$declaration for ${declaration.render()} has no parent")
        }

        if (parent != expectedParent) {
            error("$declaration for ${declaration.render()} has unexpected parent $parent")
        }
    }
}

