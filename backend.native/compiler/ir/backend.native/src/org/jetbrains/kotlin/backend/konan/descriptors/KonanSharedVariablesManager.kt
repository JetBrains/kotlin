package org.jetbrains.kotlin.backend.konan.descriptors


import org.jetbrains.kotlin.backend.common.descriptors.SharedVariablesManager
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.types.*

internal class KonanSharedVariablesManager(val builtIns: KonanBuiltIns) : SharedVariablesManager {

    private val refClass = builtIns.getKonanInternalClass("Ref")

    private val refClassConstructor = refClass.unsubstitutedPrimaryConstructor!!

    private fun refConstructor(elementType: KotlinType): ClassConstructorDescriptor {
        val typeParameter = refClassConstructor.typeParameters[0]

        return refClassConstructor.substitute(TypeSubstitutor.create(
                mapOf(typeParameter.typeConstructor to TypeProjectionImpl(Variance.INVARIANT, elementType))
        ))!!
    }

    private fun refType(elementType: KotlinType): KotlinType {
        return refClass.defaultType.replace(listOf(TypeProjectionImpl(elementType)))
    }

    override fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor {
        return LocalVariableDescriptor(
                variableDescriptor.containingDeclaration, variableDescriptor.annotations, variableDescriptor.name,
                refType(variableDescriptor.type),
                false, false, variableDescriptor.source
        )
    }

    private fun getElementProperty(sharedVariableDescriptor: VariableDescriptor): PropertyDescriptor {
        return sharedVariableDescriptor.type.memberScope.getContributedDescriptors()
                .filterIsInstance<PropertyDescriptor>()
                .single {
                    it.name.asString() == "element"
                }
    }

    override fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor,
                                   originalDeclaration: IrVariable): IrStatement {

        val valueType = originalDeclaration.descriptor.type

        val refConstructorTypeArguments = mapOf(refClassConstructor.typeParameters[0] to valueType)

        val refConstructorCall = IrCallImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset,
                refConstructor(valueType), refConstructorTypeArguments
        )
        val sharedVariableDeclaration = IrVariableImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, originalDeclaration.origin,
                sharedVariableDescriptor, refConstructorCall
        )

        val initializer = originalDeclaration.initializer ?:
                return sharedVariableDeclaration

        val elementProperty = getElementProperty(sharedVariableDescriptor)

        val sharedVariableInitialization =
                IrCallImpl(initializer.startOffset, initializer.endOffset, elementProperty.setter!!)

        sharedVariableInitialization.dispatchReceiver =
                IrGetValueImpl(initializer.startOffset, initializer.endOffset, sharedVariableDescriptor)

        sharedVariableInitialization.putValueArgument(0, initializer)

        return IrCompositeImpl(
                originalDeclaration.startOffset, originalDeclaration.endOffset, builtIns.unitType, null,
                listOf(sharedVariableDeclaration, sharedVariableInitialization)
        )
    }

    override fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression {
        val elementProperty = getElementProperty(sharedVariableDescriptor)
        return IrCallImpl(originalGet.startOffset, originalGet.endOffset, elementProperty.getter!!).apply {
            dispatchReceiver = IrGetValueImpl(originalGet.startOffset, originalGet.endOffset, sharedVariableDescriptor)
        }
    }

    override fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression {
        val elementProperty = getElementProperty(sharedVariableDescriptor)
        return IrCallImpl(originalSet.startOffset, originalSet.endOffset, elementProperty.setter!!).apply {
            dispatchReceiver = IrGetValueImpl(originalSet.startOffset, originalSet.endOffset, sharedVariableDescriptor)
            putValueArgument(0, originalSet.value)
        }
    }

}
