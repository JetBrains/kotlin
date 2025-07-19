/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers

import org.jetbrains.kotlin.backend.common.IrValidatorConfig
import org.jetbrains.kotlin.backend.common.ReportIrValidationError
import org.jetbrains.kotlin.backend.common.temporarilyPushing
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.IrTreeSymbolsVisitor
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

private class CheckTreeConsistencyVisitor(val reportError: ReportIrValidationError, val config: IrValidatorConfig) :
    IrTreeSymbolsVisitor() {
    var hasInconsistency = false

    private val visitedElements = hashSetOf<IrElement>()
    private val parentChain: MutableList<IrElement> = mutableListOf()
    private var currentActualParent: IrDeclarationParent? = null

    override fun visitElement(element: IrElement) {
        checkDuplicateNode(element)
        parentChain.temporarilyPushing(element) {
            element.acceptChildrenVoid(this)
        }
    }

    override fun visitTypeRecursively(container: IrElement, type: IrType) {
        // Skip `type.annotations` to avoid visiting the same annotation nodes multiple times,
        // since `IrType` instances can be shared across the IR tree and are not guaranteed to be unique.
        visitType(container, type)
        if (type is IrSimpleType) {
            type.arguments.forEach {
                if (it is IrTypeProjection) {
                    visitTypeRecursively(container, it.type)
                }
            }
        }
    }

    override fun visitDeclaration(declaration: IrDeclarationBase) {
        checkDuplicateNode(declaration)
        parentChain.temporarilyPushing(declaration) {
            handleParent(declaration, currentActualParent)
            val previousActualParent = currentActualParent
            currentActualParent = declaration as? IrDeclarationParent ?: currentActualParent
            declaration.acceptChildrenVoid(this)
            currentActualParent = previousActualParent
        }
    }

    override fun visitPackageFragment(declaration: IrPackageFragment) {
        currentActualParent = declaration
        visitElement(declaration)
    }

    override fun visitSymbol(container: IrElement, symbol: IrSymbol) {
        if (config.checkUnboundSymbols && !symbol.isBound) {
            hasInconsistency = true
            reportError(null, container, "Unexpected unbound symbol", parentChain)
        }
    }

    private fun handleParent(declaration: IrDeclaration, actualParent: IrDeclarationParent?) {
        if (actualParent == null) return
        try {
            val assignedParent = declaration.parent
            if (assignedParent != actualParent) {
                reportWrongParent(declaration, assignedParent, actualParent)
            }
        } catch (_: Exception) {
            reportWrongParent(declaration, null, actualParent)
        }
    }

    private fun reportWrongParent(declaration: IrDeclaration, expectedParent: IrDeclarationParent?, actualParent: IrDeclarationParent) {
        hasInconsistency = true
        reportError(
            null,
            declaration,
            buildString {
                appendLine("Declaration with wrong parent:")
                appendLine("declaration: ${declaration.render()}")
                appendLine("expectedParent: ${expectedParent?.render()}")
                appendLine("actualParent: ${actualParent.render()}")
            },
            parentChain,
        )
    }

    private fun checkDuplicateNode(element: IrElement) {
        if (!visitedElements.add(element)) {
            val renderString = if (element is IrTypeParameter) element.render() + " of " + element.parent.render() else element.render()
            reportError(null, element, "Duplicate IR node: $renderString", parentChain)

            // The IR tree is completely messed up if it includes one element twice. It may not be a tree at all, there may be cycles.
            // Give up early to avoid stack overflow.
            throw TreeConsistencyError(element)
        }
    }
}

internal fun IrElement.checkTreeConsistency(reportError: ReportIrValidationError, config: IrValidatorConfig) {
    val checker = CheckTreeConsistencyVisitor(reportError, config)
    accept(checker, null)
    if (checker.hasInconsistency) throw TreeConsistencyError(this)
}

open class IrValidationError(message: String? = null, cause: Throwable? = null) : IllegalStateException(message, cause)

class TreeConsistencyError(element: IrElement) : IrValidationError(element.render())