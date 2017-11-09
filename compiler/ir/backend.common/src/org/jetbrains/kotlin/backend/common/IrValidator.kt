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
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

fun validateIrFile(context: CommonBackendContext, irFile: IrFile) {
    val visitor = IrValidator(context, false)
    irFile.acceptVoid(visitor)
}

fun validateIrModule(context: CommonBackendContext, irModule: IrModuleFragment) {
    val visitor = IrValidator(context, true) // TODO: consider taking the boolean from settings.
    irModule.acceptVoid(visitor)

    // TODO: also check that all referenced symbol targets are reachable.
}

private fun CommonBackendContext.reportIrValidationError(message: String, irFile: IrFile, irElement: IrElement) {
    try {
        this.reportWarning("[IR VALIDATION] $message", irFile, irElement)
    } catch (e: Throwable) {
        println("an error trying to print a warning message: $e")
        e.printStackTrace()
    }
    // TODO: throw an exception after fixing bugs leading to invalid IR.
}

private class IrValidator(val context: CommonBackendContext, performHeavyValidations: Boolean) : IrElementVisitorVoid {

    val builtIns = context.builtIns
    lateinit var currentFile: IrFile

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
    }

    private fun error(element: IrElement, message: String) {
        // TODO: render all element's parents.
        context.reportIrValidationError(
                "$message\n" +
                        element.render(),
                currentFile, element)
    }

    private val elementChecker = CheckIrElementVisitor(builtIns, this::error, performHeavyValidations)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }
}
