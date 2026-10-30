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
    override fun lower(irFile: IrFile) { TODO() }

    private data class ExtractedLocalDeclaration(
        val local: IrDeclaration, val newContainer: IrDeclarationParent, val extractedUnder: IrStatement?,
    )

    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    protected open fun shouldPopUp(declaration: IrDeclaration, currentScope: ScopeWithIr?): Boolean = TODO()

}
