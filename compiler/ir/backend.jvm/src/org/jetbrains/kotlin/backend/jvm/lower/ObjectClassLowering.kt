/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.isCompanionObjectInInterfaceNotIntrinsic
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.isObject

class ObjectClassLowering(val context: JvmBackendContext) : IrElementTransformerVoidWithContext(), FileLoweringPass {

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

        val constructor = irClass.descriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Object should have a primary constructor: ${irClass.descriptor}")

        val publicInstanceOwner = if (irClass.descriptor.isCompanionObject) parentScope!!.irElement as IrDeclarationContainer else irClass
        if (isCompanionObjectInInterfaceNotIntrinsic(irClass.descriptor)) {
            // TODO rename to $$INSTANCE
            val privateInstance = publicInstanceField.descriptor.copy(
                irClass.descriptor,
                Modality.FINAL,
                Visibilities.PROTECTED/*TODO package local*/,
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                false
            ) as PropertyDescriptor
            privateInstance.name
            val field = createInstanceFieldWithInitializer(IrFieldSymbolImpl(privateInstance), constructor, irClass, irClass.defaultType)
            publicInstanceField.initializer =
                    IrExpressionBodyImpl(IrGetFieldImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, field.symbol, irClass.defaultType))
        } else {
            val constructorCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irClass.defaultType, constructor, 0)
            publicInstanceField.initializer = IrExpressionBodyImpl(constructorCall)
        }

        publicInstanceField.parent = publicInstanceOwner
        pendingTransformations.add { publicInstanceOwner.declarations.add(publicInstanceField) }
    }

    private fun createInstanceFieldWithInitializer(
        fieldSymbol: IrFieldSymbol,
        constructor: ClassConstructorDescriptor,
        instanceOwner: IrDeclarationContainer,
        objectType: IrType
    ): IrField =
        createFieldWithCustomInitializer(
            fieldSymbol,
            IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, objectType, constructor, 0),
            instanceOwner,
            objectType
        )

    private fun createFieldWithCustomInitializer(
        fieldSymbol: IrFieldSymbol,
        instanceInitializer: IrExpression,
        instanceOwner: IrDeclarationContainer,
        objectType: IrType
    ): IrField =
        IrFieldImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE,
            fieldSymbol, objectType
        ).also {
            it.initializer = IrExpressionBodyImpl(instanceInitializer)
            it.parent = instanceOwner
            pendingTransformations.add { instanceOwner.declarations.add(it) }
        }
}