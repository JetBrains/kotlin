/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

//This lower takes part of old LocalDeclarationLowering job to pop up local classes from functions
open class LocalClassPopupLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irFile: IrFile) {
        runOnFilePostfix(irFile, withLocalDeclarations = true, allowDeclarationModification = true)
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val extractedLocalClasses = arrayListOf<Pair<IrClass, IrDeclarationContainer>>()

        irBody.transform(object : IrElementTransformerVoidWithContext() {

            override fun visitClassNew(declaration: IrClass): IrStatement {
                val currentScope =
                    if (allScopes.size > 1) allScopes[allScopes.lastIndex - 1] else createScope(container as IrSymbolOwner)
                if (!shouldPopUp(declaration, currentScope)) return declaration

                val newContainer = run {
                    var currentParent = declaration.parent
                    while (currentParent is IrDeclaration && currentParent !is IrClass && currentParent !is IrScript) {
                        currentParent = currentParent.parent
                    }

                    currentParent as IrDeclarationContainer // IrClass or IrScript or IrPackageFragment
                }

                extractedLocalClasses.add(declaration to newContainer)
                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        for ((local, newContainer) in extractedLocalClasses) {
            newContainer.addChild(local)

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
