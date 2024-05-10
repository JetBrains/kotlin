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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.IrVerificationMode
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.DeclarationParentsVisitor
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

data class IrValidatorConfig(
    val checkTypes: Boolean = true,
    val checkProperties: Boolean = false,
    val checkScopes: Boolean = false, // TODO: Consider setting to true by default and deleting
)

class IrValidator(
    val irBuiltIns: IrBuiltIns,
    val config: IrValidatorConfig,
    val reportError: (IrFile?, IrElement, String) -> Unit
) : IrElementVisitorVoid {

    var currentFile: IrFile? = null

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
        if (config.checkScopes) {
            ScopeValidator(this::error).check(declaration)
        }
    }

    private fun error(element: IrElement, message: String) {
        reportError(currentFile, element, message)
    }

    private val elementChecker = CheckIrElementVisitor(irBuiltIns, this::error, config)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }
}

fun IrElement.checkDeclarationParents() {
    val checker = CheckDeclarationParentsVisitor()
    accept(checker, null)
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

    override fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent) {
        try {
            val assignedParent = declaration.parent
            if (assignedParent != actualParent) {
                errors.add(Error(declaration, assignedParent, actualParent))
            }
        } catch (e: Exception) {
            errors.add(Error(declaration, actualParent, null))
        }
    }
}

open class IrValidationError(message: String? = null) : IllegalStateException(message)

class DuplicateIrNodeError(element: IrElement) : IrValidationError(element.render())

/**
 * Verifies common IR invariants that should hold in all the backends.
 */
fun performBasicIrValidation(
    element: IrElement,
    irBuiltIns: IrBuiltIns,
    checkProperties: Boolean = false,
    checkTypes: Boolean = false,
    reportError: (IrFile?, IrElement, String) -> Unit,
) {
    val validatorConfig = IrValidatorConfig(
        checkTypes = checkTypes,
        checkProperties = checkProperties,
    )
    val validator = IrValidator(irBuiltIns, validatorConfig, reportError)
    try {
        element.acceptVoid(validator)
    } catch (e: DuplicateIrNodeError) {
        // Performing other checks may cause e.g. infinite recursion.
        return
    }
    element.checkDeclarationParents()
}

/**
 * Verifies common IR invariants that should hold in all the backends.
 *
 * Reports errors to [CommonBackendContext.messageCollector].
 *
 * **Note:** this method does **not** throw [IrValidationError]. Use [throwValidationErrorIfNeeded] for checking for errors and throwing
 * [IrValidationError]. This gives the caller the opportunity to perform additional (for example, backend-specific) validation before
 * aborting. The caller decides when it's time to abort.
 */
fun performBasicIrValidation(
    context: CommonBackendContext,
    fragment: IrElement,
    mode: IrVerificationMode,
    phaseName: String,
    checkProperties: Boolean = false,
    checkTypes: Boolean = false,
) {
    if (mode == IrVerificationMode.NONE) return
    performBasicIrValidation(
        fragment,
        context.irBuiltIns,
        checkProperties,
        checkTypes,
    ) { file, element, message ->
        context.reportIrValidationError(mode, file, element, message, phaseName)
    }
}

fun LoggingContext.reportIrValidationError(
    mode: IrVerificationMode,
    file: IrFile?,
    element: IrElement,
    message: String,
    phaseName: String,
) {
    val severity = when (mode) {
        IrVerificationMode.WARNING -> CompilerMessageSeverity.WARNING
        IrVerificationMode.ERROR -> CompilerMessageSeverity.ERROR
        IrVerificationMode.NONE -> return
    }
    val phaseMessage = if (phaseName.isNotEmpty()) "$phaseName: " else ""
    // TODO: render all element's parents.
    report(
        severity,
        element,
        file,
        "[IR VALIDATION] ${phaseMessage}${"$message\n" + element.render()}",
    )
}

/**
 * Allows to abort the compilation process if after validating the IR there were errors and [mode] is [IrVerificationMode.ERROR].
 */
fun LoggingContext.throwValidationErrorIfNeeded(mode: IrVerificationMode) {
    if (messageCollector.hasErrors() && mode == IrVerificationMode.ERROR) {
        throw IrValidationError()
    }
}
