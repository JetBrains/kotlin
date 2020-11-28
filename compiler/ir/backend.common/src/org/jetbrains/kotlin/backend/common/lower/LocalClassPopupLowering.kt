/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.setDeclarationsParent
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

//This lower takes part of old LocalDeclarationLowering job to pop up local classes from functions
open class LocalClassPopupLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true, allowDeclarationModification = true)
    }

    private data class ExtractedLocalClass(
        val local: IrClass, val newContainer: IrDeclarationParent, val extractedUnder: IrStatement?
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val extractedLocalClasses = arrayListOf<ExtractedLocalClass>()

        irBody.transform(object : IrElementTransformerVoidWithContext() {

            override fun visitClassNew(declaration: IrClass): IrStatement {
                val currentScope =
                    if (allScopes.size > 1) allScopes[allScopes.lastIndex - 1] else createScope(container as IrSymbolOwner)
                if (!shouldPopUp(declaration, currentScope)) return declaration

                var extractedUnder: IrStatement? = declaration
                var newContainer = declaration.parent
                while (newContainer is IrDeclaration && newContainer !is IrClass && newContainer !is IrScript) {
                    extractedUnder = newContainer
                    newContainer = newContainer.parent
                }
                when (newContainer) {
                    is IrStatementContainer -> {
                        // TODO: check if it is the correct behavior
                        if (extractedUnder == declaration) {
                            extractedUnder = (newContainer.statements.indexOf(extractedUnder) + 1)
                                .takeIf { it > 0 && it < newContainer.statements.size }
                                ?.let { newContainer.statements[it] }
                        }
                        extractedLocalClasses.add(ExtractedLocalClass(declaration, newContainer, extractedUnder))
                    }
                    is IrDeclarationContainer -> extractedLocalClasses.add(ExtractedLocalClass(declaration, newContainer, extractedUnder))
                    else -> error("Inexpected container type $newContainer")
                }

                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        for ((local, newContainer, extractedUnder) in extractedLocalClasses) {
            when (newContainer) {
                is IrStatementContainer -> {
                    val insertIndex = extractedUnder?.let { newContainer.statements.indexOf(it) } ?: -1
                    if (insertIndex >= 0) {
                        newContainer.statements.add(insertIndex, local)
                    } else {
                        newContainer.statements.add(local)
                    }
                    local.setDeclarationsParent(newContainer)
                }
                is IrDeclarationContainer -> {
                    newContainer.addChild(local)
                }
                else -> error("Inexpected container type $newContainer")
            }

            local.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitBody(body: IrBody) {
                }

                override fun visitClass(declaration: IrClass) {
                    super.visitClass(declaration)
                    context.extractedLocalClasses += declaration
                }
            })
        }
    }

    protected open fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean =
        klass.isLocalNotInner()
}
