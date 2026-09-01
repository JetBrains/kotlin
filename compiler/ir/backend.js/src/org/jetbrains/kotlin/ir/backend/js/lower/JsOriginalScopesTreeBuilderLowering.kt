/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.metadata.JsOriginalScopeNode
import java.util.Stack

var IrElement.originalScope: JsOriginalScopeNode? by irAttribute<IrElement, JsOriginalScopeNode>(copyByDefault = true)

class JsOriginalScopesTreeBuilderLowering(private val context: JsIrBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val scopeStack = Stack<JsOriginalScopeNode>()
        val file = irFile.fileEntry

        irFile.originalScope = file.createFileScope(irFile)
        scopeStack.push(irFile.originalScope)

        irFile.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                if (declaration.hasSyntheticOrUndefinedLocation) return super.visitClass(declaration)
                val classScope = file.createClassScope(declaration)
                declaration.originalScope = classScope
                scopeStack.peek().children += classScope

                scopeStack.push(classScope)
                super.visitClass(declaration)
                scopeStack.pop()
            }

            override fun visitFunction(declaration: IrFunction) {
                if (declaration.hasSyntheticOrUndefinedLocation) return super.visitFunction(declaration)
                val functionScope = file.createFunctionScope(declaration)
                declaration.originalScope = functionScope
                scopeStack.peek().children += functionScope

                scopeStack.push(functionScope)
                super.visitFunction(declaration)
                scopeStack.pop()
            }
        })

        context.originalFileScopes[file.name] = scopeStack.peek()
        scopeStack.pop()
    }

    private fun IrFileEntry.createFileScope(irFile: IrFile): JsOriginalScopeNode {
        // maxOffset can't relatively tell us the real max offset, falling back to last declaration endOffset.
        val endOffset = maxOffset.takeIf { it >= 0 }
            ?: irFile.declarations.maxOfOrNull { it.endOffset } ?: 0

        return JsOriginalScopeNode(
            name = name,
            kind = "Global",
            startLine = getLineNumber(irFile.startOffset),
            startColumn = getColumnNumber(irFile.startOffset),
            endLine = getLineNumber(endOffset),
            endColumn = getColumnNumber(endOffset),
            variables = irFile.declarations.filterIsInstance<IrDeclarationWithName>().map { it.name.asString() }.toMutableList(),
            children = [],
            isStackFrame = false
        )
    }

    private fun IrFileEntry.createClassScope(irClass: IrClass): JsOriginalScopeNode {
        return JsOriginalScopeNode(
            name = irClass.name.asString(),
            kind = "Class",
            startLine = getLineNumber(irClass.startOffset),
            startColumn = getColumnNumber(irClass.startOffset),
            endLine = getLineNumber(irClass.endOffset),
            endColumn = getColumnNumber(irClass.endOffset),
            variables = irClass.declarations.filterIsInstance<IrDeclarationWithName>().map { it.name.asString() }.toMutableList(),
            children = [],
            isStackFrame = true
        )
    }

    private fun IrFileEntry.createFunctionScope(irFunction: IrFunction): JsOriginalScopeNode {
        return JsOriginalScopeNode(
            name = irFunction.name.asString(),
            kind = "Function",
            startLine = getLineNumber(irFunction.startOffset),
            startColumn = getColumnNumber(irFunction.startOffset),
            endLine = getLineNumber(irFunction.endOffset),
            endColumn = getColumnNumber(irFunction.endOffset),
            variables = irFunction.parameters.filter { it.kind == IrParameterKind.Regular }.map { it.name.asString() }.toMutableList(),
            children = [],
            isStackFrame = true
        )
    }

    private val IrElement.hasSyntheticOrUndefinedLocation: Boolean
        get() = startOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET ||
                endOffset in SYNTHETIC_OFFSET..UNDEFINED_OFFSET
}
