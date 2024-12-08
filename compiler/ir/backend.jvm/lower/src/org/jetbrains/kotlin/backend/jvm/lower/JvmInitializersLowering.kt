/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.InitializersCleanupLowering
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.transformStatement
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Merges init blocks and field initializers into constructors.
 */
@PhaseDescription(
    name = "Initializers",
    // Depends on local class extraction, because otherwise local classes in initializers will be copied into each constructor.
    prerequisite = [JvmLocalClassPopupLowering::class]
)
internal class JvmInitializersLowering(context: JvmBackendContext) : InitializersLowering(context) {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (container !is IrConstructor) return
        val constructorBody = container.body
        if (container.constructedClass.isValhallaValueClass && container.isPrimary && constructorBody != null) {
            val fieldInit = makeInitializerBlock(container) { it is IrField && !it.isStatic }
            val restInit = makeInitializerBlock(container) { it is IrAnonymousInitializer && !it.isStatic }
            container.body = context.irFactory.createBlockBody(container.startOffset, container.endOffset) {
                statements += fieldInit.statements
                for (statement in constructorBody.statements) {
                    statements += statement
                        .patchDeclarationParents(container)
                        .transformStatement(object : IrElementTransformerVoid() {
                            override fun visitInstanceInitializerCall(expression: IrInstanceInitializerCall): IrExpression =
                                IrBlockImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, context.irBuiltIns.unitType)
                        })
                }
                statements += restInit.statements
            }
        } else {
            super.lower(irBody, container)
        }
    }
}

/**
 * Removes non-static anonymous initializers and non-constant non-static field init expressions.
 */
@PhaseDescription(
    name = "InitializersCleanup",
    prerequisite = [JvmInitializersLowering::class]
)
internal class JvmInitializersCleanupLowering(context: JvmBackendContext) : InitializersCleanupLowering(
    context,
    {
        it.constantValue() == null && (!it.isStatic || it.correspondingPropertySymbol?.owner?.isConst != true)
    }
)
