/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class StripTypeAliasDeclarationsLowering :
    IrElementVisitorVoid,
    FileLoweringPass {

    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(this)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFile(declaration: IrFile) {
        declaration.acceptChildrenVoid(this)
        declaration.declarations.removeAll { it is IrTypeAlias }
    }

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildrenVoid(this)
        declaration.declarations.removeAll { it is IrTypeAlias }
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        expression.acceptChildrenVoid(this)
        expression.statements.removeAll { it is IrTypeAlias }
    }
}