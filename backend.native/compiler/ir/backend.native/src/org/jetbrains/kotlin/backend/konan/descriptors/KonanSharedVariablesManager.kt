/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.descriptors


import org.jetbrains.kotlin.backend.common.descriptors.SharedVariablesManager
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.types.*

internal class KonanSharedVariablesManager(val context: KonanBackendContext) : SharedVariablesManager {

    private val refClass = context.ir.symbols.refClass

    private val refClassConstructor = refClass.constructors.single()

    private val elementProperty = refClass.owner.declarations.filterIsInstance<IrProperty>().single()

    // TODO: refactor `SharedVariablesManager` interface instead of tracking the mapping:
    private val sharedVariableDescriptorToSymbol = mutableMapOf<VariableDescriptor, IrVariableSymbol>()

    private fun refConstructor(elementType: KotlinType): ClassConstructorDescriptor {
        val typeParameter = refClassConstructor.descriptor.typeParameters[0]

        return refClassConstructor.descriptor.substitute(TypeSubstitutor.create(
                mapOf(typeParameter.typeConstructor to TypeProjectionImpl(Variance.INVARIANT, elementType))
        ))!!
    }

    private fun refType(elementType: KotlinType): KotlinType {
        return refClass.owner.defaultType.replace(listOf(TypeProjectionImpl(elementType)))
    }

    override fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor {
        return LocalVariableDescriptor(
                variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
                refType(variableDescriptor.type),
                false, false, variableDescriptor.source
        ).also {
            sharedVariableDescriptorToSymbol[it] = IrVariableSymbolImpl(it)
        }
    }

    private fun getElementPropertyDescriptor(sharedVariableDescriptor: VariableDescriptor): PropertyDescriptor {
        return sharedVariableDescriptor.type.memberScope.getContributedDescriptors()
                .filterIsInstance<PropertyDescriptor>()
                .single {
                    it.name.asString() == "element"
                }
    }

    override fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor,
                                   originalDeclaration: IrVariable): IrStatement {

        val sharedVariableSymbol = sharedVariableDescriptorToSymbol[sharedVariableDescriptor]!!

        val valueType = originalDeclaration.descriptor.type

        val refConstructorTypeArguments = mapOf(refClassConstructor.descriptor.typeParameters[0] to valueType)

        val refConstructorCall = IrCallImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset,
                refClassConstructor, refConstructor(valueType), refConstructorTypeArguments
        )
        val sharedVariableDeclaration = IrVariableImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
                sharedVariableSymbol
        ).apply {
            initializer = refConstructorCall
        }

        val initializer = originalDeclaration.initializer ?:
                return sharedVariableDeclaration

        val elementPropertyDescriptor = getElementPropertyDescriptor(sharedVariableDescriptor)

        val sharedVariableInitialization =
                IrCallImpl(initializer.startOffset, initializer.endOffset, elementProperty.setter!!.symbol,
                        elementPropertyDescriptor.setter!!)

        sharedVariableInitialization.dispatchReceiver =
                IrGetValueImpl(initializer.startOffset, initializer.endOffset, sharedVariableSymbol)

        sharedVariableInitialization.putValueArgument(0, initializer)

        return IrCompositeImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, context.builtIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    override fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression {
        val sharedVariableSymbol = sharedVariableDescriptorToSymbol[sharedVariableDescriptor]!!

        val elementPropertyDescriptor = getElementPropertyDescriptor(sharedVariableDescriptor)

        return IrCallImpl(originalGet.startOffset, originalGet.endOffset, elementProperty.getter!!.symbol,
                elementPropertyDescriptor.getter!!).apply {
            dispatchReceiver = IrGetValueImpl(originalGet.startOffset, originalGet.endOffset, sharedVariableSymbol)
        }
    }

    override fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression {
        val sharedVariableSymbol = sharedVariableDescriptorToSymbol[sharedVariableDescriptor]!!

        val elementPropertyDescriptor = getElementPropertyDescriptor(sharedVariableDescriptor)

        return IrCallImpl(originalSet.startOffset, originalSet.endOffset, elementProperty.setter!!.symbol,
                elementPropertyDescriptor.setter!!).apply {
            dispatchReceiver = IrGetValueImpl(originalSet.startOffset, originalSet.endOffset, sharedVariableSymbol)
            putValueArgument(0, originalSet.value)
        }
    }

}
