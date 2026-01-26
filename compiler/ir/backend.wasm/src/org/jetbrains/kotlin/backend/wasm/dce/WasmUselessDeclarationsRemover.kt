/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dce

import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.isObjectInstanceField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrSetField
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

class WasmUselessDeclarationsRemover(
    private val context: WasmBackendContext,
    private val usefulDeclarations: Set<IrDeclaration>
) : IrVisitorVoid() {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        val fileContext = context.fileContexts[declaration]
        val objectInstanceFieldInitializer = fileContext?.objectInstanceFieldInitializer

        process(declaration, objectInstanceFieldInitializer)

        if (objectInstanceFieldInitializer != null) {
            val statements = (objectInstanceFieldInitializer.body as IrBlockBody).statements
            statements.removeIf {
                val field = (it as? IrSetField)?.symbol?.owner ?: error("Expected IrSetField but got ${it::class.simpleName}")
                field !in usefulDeclarations
            }
            if (statements.isEmpty()) {
                declaration.declarations.remove(objectInstanceFieldInitializer)
                fileContext.objectInstanceFieldInitializer = null
            }
        }
    }

    override fun visitClass(declaration: IrClass) {
        process(declaration, null)
    }

    // TODO bring back the primary constructor fix
    private fun process(container: IrDeclarationContainer, objectInstanceFieldInitializer: IrSimpleFunction?) {
        container.declarations.transformFlat { member ->
            if (objectInstanceFieldInitializer != null && objectInstanceFieldInitializer == member) {
                return@transformFlat null
            }
            if (member !in usefulDeclarations) {
                emptyList()
            } else {
                member.acceptVoid(this)
                null
            }
        }
    }
}