/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl

//This lower takes part of old LocalDeclarationLowering job to pop up local classes from functions
open class LocalClassPopupLowering(val context: BackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val extractedLocalClasses = arrayListOf<Pair<IrClass, IrDeclarationContainer>>()

        irFile.transform(object : IrElementTransformerVoidWithContext() {

            override fun visitClassNew(declaration: IrClass): IrStatement {
                val newDeclaration = super.visitClassNew(declaration)
                if (newDeclaration !is IrClass) return newDeclaration

                val currentScope = allScopes[allScopes.lastIndex - 1]
                if (!shouldPopUp(declaration, currentScope)) return newDeclaration

                val newContainer = allScopes.asReversed().drop(1/*skip self*/).firstOrNull {
                    //find first class local or not;
                    // to reproduce original LocalDeclarationLowering behaviour add: '&& !it.irElement.isLocal' condition
                    it.irElement is IrClass || it.irElement is IrScript
                }?.irElement?.let { it as? IrClass ?: it as? IrScript } ?: currentFile
                extractedLocalClasses.add(newDeclaration to newContainer)
                return IrCompositeImpl(declaration.startOffset, declaration.endOffset, context.irBuiltIns.unitType)
            }
        }, null)

        for ((local, newContainer) in extractedLocalClasses) {
            newContainer.addChild(local)
        }
    }

    protected open fun shouldPopUp(klass: IrClass, currentScope: ScopeWithIr?): Boolean =
        klass.isLocalNotInner()
}
