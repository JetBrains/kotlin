/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors


import org.jetbrains.kotlin.backend.common.ir.SharedVariablesManager
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.types.*

internal class KonanSharedVariablesManager(val context: KonanBackendContext) : SharedVariablesManager {

    private val refClass = context.ir.symbols.refClass

    private val refClassConstructor = refClass.constructors.single()

    private val elementProperty = refClass.owner.declarations.filterIsInstance<IrProperty>().single()

    private fun refType(elementType: KotlinType): KotlinType {
        return refClass.descriptor.defaultType.replace(listOf(TypeProjectionImpl(elementType)))
    }

    override fun declareSharedVariable(originalDeclaration: IrVariable): IrVariable {
        val variableDescriptor = originalDeclaration.descriptor
        val sharedVariableDescriptor = LocalVariableDescriptor(
                variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
                refType(variableDescriptor.type),
                false, false, variableDescriptor.source
        )

        val valueType = originalDeclaration.type

        val refConstructorCall = IrConstructorCallImpl.fromSymbolOwner(
                originalDeclaration.startOffset, originalDeclaration.endOffset,
                refClass.typeWith(valueType),
                refClassConstructor
        ).apply {
            putTypeArgument(0, valueType)
        }

        return IrVariableImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
                sharedVariableDescriptor, refConstructorCall.type
        ).apply {
            initializer = refConstructorCall
        }
    }

    override fun defineSharedValue(originalDeclaration: IrVariable, sharedVariableDeclaration: IrVariable): IrStatement {
        val initializer = originalDeclaration.initializer ?: return sharedVariableDeclaration

        val sharedVariableInitialization =
                IrCallImpl(initializer.startOffset, initializer.endOffset,
                        context.irBuiltIns.unitType, elementProperty.setter!!.symbol)

        sharedVariableInitialization.dispatchReceiver =
                IrGetValueImpl(initializer.startOffset, initializer.endOffset,
                        sharedVariableDeclaration.type, sharedVariableDeclaration.symbol)

        sharedVariableInitialization.putValueArgument(0, initializer)

        return IrCompositeImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, context.irBuiltIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    override fun getSharedValue(sharedVariableSymbol: IrVariableSymbol, originalGet: IrGetValue) =
            IrCallImpl(originalGet.startOffset, originalGet.endOffset,
                    originalGet.type, elementProperty.getter!!.symbol).apply {
                dispatchReceiver = IrGetValueImpl(
                        originalGet.startOffset, originalGet.endOffset,
                        sharedVariableSymbol.owner.type, sharedVariableSymbol
                )
            }

    override fun setSharedValue(sharedVariableSymbol: IrVariableSymbol, originalSet: IrSetVariable) =
            IrCallImpl(originalSet.startOffset, originalSet.endOffset, context.irBuiltIns.unitType,
                    elementProperty.setter!!.symbol).apply {
                dispatchReceiver = IrGetValueImpl(
                        originalSet.startOffset, originalSet.endOffset,
                        sharedVariableSymbol.owner.type, sharedVariableSymbol
                )
                putValueArgument(0, originalSet.value)
            }

}
