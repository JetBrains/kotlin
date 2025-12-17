/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementContainer
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.isOriginallyLocalDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.util.setDeclarationsParent
import org.jetbrains.kotlin.ir.util.transformInPlace

/**
 * Moves local declarations into nearest declaration container.
 */
@PhasePrerequisites(LocalDeclarationsLowering::class)
open class LocalDeclarationPopupLowering(
    val context: LoweringContext,
) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true)
    }

    private data class ExtractedLocalDeclaration(
        val local: IrDeclaration, val newContainer: IrDeclarationParent, val extractedUnder: IrStatement?,
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val extractedLocalDeclarations = arrayListOf<ExtractedLocalDeclaration>()

        irBody.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitLocalDelegatedProperty(declaration: IrLocalDelegatedProperty): IrStatement {
                // Note: This code is not accessible in KLIB-based backends now because `LocalDeclarationPopupLowering` runs
                // after `LocalDelegatedPropertiesLowering`, which reshapes `IrLocalDelegatedProperty`s to simpler declarations.
                // While it is still used in Kotlin/JVM.
                declaration.getter.transformStatement(this)
                declaration.setter?.transformStatement(this)

                val delegate = declaration.delegate
                requireNotNull(delegate) { "Local delegated property ${declaration.render()} has no delegate" }
                return delegate.transformStatement(this)
            }

            override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
                expression.boundValues.transformInPlace(this, null)
                expression.invokeFunction.transformChildrenVoid(this)
                return expression
            }

            override fun visitClassNew(declaration: IrClass): IrStatement = visitClassOrFunction(declaration)

            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement = visitClassOrFunction(declaration)

            private fun visitClassOrFunction(declaration: IrDeclaration): IrStatement {
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
                        extractedLocalDeclarations += ExtractedLocalDeclaration(declaration, newContainer, extractedUnder)
                    }

                    is IrDeclarationContainer ->
                        extractedLocalDeclarations += ExtractedLocalDeclaration(declaration, newContainer, extractedUnder)

                    else -> error("Unexpected container type $newContainer")
                }

                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        for ((local, newContainer, extractedUnder) in extractedLocalDeclarations) {
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
                else -> error("Unexpected container type $newContainer")
            }
        }
    }

    protected open fun shouldPopUp(declaration: IrDeclaration, currentScope: ScopeWithIr?): Boolean =
        declaration.isOriginallyLocalDeclaration || (declaration as? IrClass)?.isLocalNotInner() == true

}
