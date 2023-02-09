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
import org.jetbrains.kotlin.ir.util.DeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

@Suppress("unused")
fun validateIrFile(context: CommonBackendContext, irFile: IrFile) {
    val visitor = IrValidator(context, IrValidatorConfig(abortOnError = false, ensureAllNodesAreDifferent = false))
    irFile.acceptVoid(visitor)
}

@Suppress("unused")
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
    val checkScopes: Boolean = false,
)

class IrValidator(val context: CommonBackendContext, val config: IrValidatorConfig) : IrElementVisitorVoid {

    val irBuiltIns = context.irBuiltIns
    var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
        if (config.checkScopes) {
            ScopeValidator(this::error).check(declaration)
        }
    }

    private fun error(element: IrElement, message: String) {
        // TODO: render all element's parents.
        context.reportIrValidationError(
            "$message\n" + element.render(),
            currentFile,
            element
        )

        if (config.abortOnError) {
            error("Validation failed in file ${currentFile?.name ?: "???"} : ${message}\n${element.render()}")
        }
    }

    private val elementChecker = CheckIrElementVisitor(irBuiltIns, this::error, config)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }
}

fun IrElement.checkDeclarationParents() {
    val checker = CheckDeclarationParentsVisitor()
    acceptVoid(checker)
    if (checker.errors.isNotEmpty()) {
        val expectedParents = LinkedHashSet<IrDeclarationParent>()
        throw AssertionError(
            buildString {
                append("Declarations with wrong parent: ")
                append(checker.errors.size)
                append("\n")
                checker.errors.forEach {
                    append("declaration: ")
                    append(it.declaration.render())
                    append("\n\t")
                    append(it.declaration)
                    append("\nexpectedParent: ")
                    append(it.expectedParent.render())
                    append("\nactualParent: ")
                    append(it.actualParent?.render())
                    append("\n")
                    expectedParents.add(it.expectedParent)
                }
                append("\nExpected parents:\n")
                expectedParents.forEach {
                    append(it.dump())
                }
            }
        )
    }
}

class CheckDeclarationParentsVisitor : DeclarationParentsVisitor() {
    class Error(val declaration: IrDeclaration, val expectedParent: IrDeclarationParent, val actualParent: IrDeclarationParent?)

    val errors = ArrayList<Error>()

    override fun handleParent(declaration: IrDeclaration, parent: IrDeclarationParent) {
        try {
            val actualParent = declaration.parent
            if (actualParent != parent) {
                errors.add(Error(declaration, parent, actualParent))
            }
        } catch (e: Exception) {
            errors.add(Error(declaration, parent, null))
        }
    }
}
