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
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedFieldDescriptor
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
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

        val constructor = irClass.constructors.find { it.isPrimary }
            ?: throw AssertionError("Object should have a primary constructor: ${irClass.name}")

        val publicInstanceOwner = if (irClass.isCompanion) parentScope!!.irElement as IrDeclarationContainer else irClass
        if (irClass.isCompanion && irClass.parentAsClass.isJvmInterface) {
            // TODO rename to $$INSTANCE
            // TODO: mark as synthesized
            val privateFieldDescriptor = WrappedFieldDescriptor()
            val privateField = IrFieldImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE,
                IrFieldSymbolImpl(privateFieldDescriptor),
                publicInstanceField.name,
                irClass.defaultType,
                Visibilities.PROTECTED/* TODO package local */,
                isFinal = true,
                isExternal = false,
                isStatic = true,
                isFakeOverride = false
            ).apply {
                privateFieldDescriptor.bind(this)
                parent = irClass
                with(context.createIrBuilder(symbol)) {
                    initializer = irExprBody(
                        irCall(constructor)
                    )
                }
                pendingTransformations.add { parentAsClass.declarations.add(this) }
            }

            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irGetField(null, privateField))
            }
        } else {
            with(context.createIrBuilder(publicInstanceField.symbol)) {
                publicInstanceField.initializer = irExprBody(irCall(constructor.symbol))
            }
        }

        publicInstanceField.parent = publicInstanceOwner
        pendingTransformations.add {
            publicInstanceOwner.declarations.add(publicInstanceField)
        }
    }
}