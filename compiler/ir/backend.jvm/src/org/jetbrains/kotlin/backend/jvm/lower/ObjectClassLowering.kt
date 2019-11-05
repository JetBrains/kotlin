/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*

internal val objectClassPhase = makeIrFilePhase(
    ::ObjectClassLowering,
    name = "ObjectClass",
    description = "Handle object classes"
)

private class ObjectClassLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

    private var pendingTransformations = mutableListOf<Function0<Unit>>()

    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)

        pendingTransformations.forEach { it() }
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        process(declaration)
        return super.visitClassNew(declaration)
    }


    private fun process(irClass: IrClass) {
        if (!irClass.isObject) return

        val publicInstanceField = context.declarationFactory.getFieldForObjectInstance(irClass)
        val privateInstanceField = context.declarationFactory.getPrivateFieldForObjectInstance(irClass)

        val constructor = irClass.constructors.find { it.isPrimary }
            ?: throw AssertionError("Object should have a primary constructor: ${irClass.name}")

        if (privateInstanceField != publicInstanceField) {
            with(context.createIrBuilder(privateInstanceField.symbol)) {
                privateInstanceField.initializer = irExprBody(irCall(constructor.symbol))
            }
            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irGetField(null, privateInstanceField))
            }
            pendingTransformations.add {
                (privateInstanceField.parent as IrDeclarationContainer).declarations.add(0, privateInstanceField)
            }
        } else {
            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
            }
        }

        pendingTransformations.add {
            (publicInstanceField.parent as IrDeclarationContainer).declarations.add(0, publicInstanceField)
        }
    }
}