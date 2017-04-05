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

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.lower.SimpleMemberScope
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionInvoke
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNullable

internal class DeepCopyIrTreeWithDescriptors(val targetFunction: IrFunction, typeArgsMap: Map <TypeParameterDescriptor, KotlinType>?, val context: Context) {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
    private var typeSubstitutor = createTypeSubstitutor(typeArgsMap)
    private var inlinedFunctionName = ""
    private var nameIndex = 0

    //-------------------------------------------------------------------------//

    fun copy(irElement: IrElement, functionName: String) {

        inlinedFunctionName = functionName
        descriptorSubstituteMap.clear()
        irElement.acceptChildrenVoid(descriptorCollector)
        irElement.transformChildrenVoid(descriptorSubstitutor)
    }

    //-------------------------------------------------------------------------//

    private val descriptorCollector = object : IrElementVisitorVoid {

        override fun visitClass(declaration: IrClass) {

            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            descriptorSubstituteMap[oldDescriptor.thisAsReceiverParameter] = newDescriptor.thisAsReceiverParameter
            super.visitClass(declaration)

            val constructors = oldDescriptor.constructors.map { oldConstructorDescriptor ->
                descriptorSubstituteMap[oldConstructorDescriptor] as ClassConstructorDescriptor
            }.toSet()

            var primaryConstructor: ClassConstructorDescriptor? = null
            val oldPrimaryConstructor = oldDescriptor.unsubstitutedPrimaryConstructor
            if (oldPrimaryConstructor != null) {
                primaryConstructor = descriptorSubstituteMap[oldPrimaryConstructor] as ClassConstructorDescriptor
            }

            val contributedDescriptors = oldDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map {
                        if (it is CallableMemberDescriptor && it.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                            it
                        else descriptorSubstituteMap[it]!!
                    }
            newDescriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),
                    constructors,
                    primaryConstructor
            )
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyOrField(oldDescriptor: PropertyDescriptor) {
            val newDescriptor = copyPropertyDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            oldDescriptor.getter?.let {
                descriptorSubstituteMap[it] = newDescriptor.getter!!
            }
            oldDescriptor.setter?.let {
                descriptorSubstituteMap[it] = newDescriptor.setter!!
            }
        }

        override fun visitProperty(declaration: IrProperty) {
            copyPropertyOrField(declaration.descriptor)
            super.visitProperty(declaration)
        }

        override fun visitField(declaration: IrField) {
            val oldDescriptor = declaration.descriptor
            if (descriptorSubstituteMap[oldDescriptor] == null) {
                // A field without a property or a field of a delegated property.
                copyPropertyOrField(oldDescriptor)
            }
            super.visitField(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction) {

            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) { // Property accessors are copied along with their property.
                val newDescriptor = copyFunctionDescriptor(oldDescriptor)
                descriptorSubstituteMap[oldDescriptor] = newDescriptor
            }
            super.visitFunction(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall) {

            val descriptor = expression.descriptor as FunctionDescriptor
            if (descriptor.isFunctionInvoke) {
                val oldDescriptor = descriptor as SimpleFunctionDescriptor
                val containingDeclaration = targetFunction.descriptor
                val newReturnType = substituteType(oldDescriptor.returnType)!!
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, containingDeclaration)
                val newDescriptor = oldDescriptor.newCopyBuilder().apply {
                    setReturnType(newReturnType)
                    setValueParameters(newValueParameters)
                }.build()
                descriptorSubstituteMap[oldDescriptor] = newDescriptor!!
            }

            super.visitCall(expression)
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable) {

            val oldDescriptor = declaration.descriptor
            val newDescriptor = IrTemporaryVariableDescriptorImpl(
                targetFunction.descriptor,
                generateName(oldDescriptor.name),
                substituteType(oldDescriptor.type)!!,
                oldDescriptor.isVar)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            super.visitVariable(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateName(name: Name): Name {

            val containingName  = targetFunction.descriptor.name.toString()                 // Name of inline target (function we inline in)
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(containingName + "_" + inlinedFunctionName + "_" + declarationName + "_" + indexStr)
        }

        //---------------------------------------------------------------------//

        private fun copyFunctionDescriptor(oldDescriptor: CallableDescriptor): CallableDescriptor {

            return when (oldDescriptor) {
                is ConstructorDescriptor       -> copyConstructorDescriptor(oldDescriptor)
                is SimpleFunctionDescriptor    -> copySimpleFunctionDescriptor(oldDescriptor)
                else -> TODO("Unsupported FunctionDescriptor subtype")
            }
        }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor) : FunctionDescriptor {

            val oldContainingDeclaration = oldDescriptor.containingDeclaration         // TODO should target function to be containing declaration
            val newContainingDeclaration = descriptorSubstituteMap[oldContainingDeclaration] ?: targetFunction.descriptor

            val newDescriptor = SimpleFunctionDescriptorImpl.create(
                newContainingDeclaration,
                oldDescriptor.annotations,
                generateName(oldDescriptor.name),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                oldDescriptor.source
            ).apply { isTailrec = oldDescriptor.isTailrec }

            val oldDispatchReceiverParameter = oldDescriptor.dispatchReceiverParameter
            val newDispatchReceiverParameter =
                    if (oldDispatchReceiverParameter == null) null
                    else descriptorSubstituteMap[oldDispatchReceiverParameter]
            val newTypeParameters     = oldDescriptor.typeParameters        // TODO substitute types
            val newValueParameters    = copyValueParameters(oldDescriptor.valueParameters, newDescriptor)
            val receiverParameterType = substituteType(oldDescriptor.extensionReceiverParameter?.type)
            val newReturnType         = substituteType(oldDescriptor.returnType)

            newDescriptor.initialize(
                receiverParameterType,
                newDispatchReceiverParameter as? ReceiverParameterDescriptor,
                newTypeParameters,
                newValueParameters,
                newReturnType,
                oldDescriptor.modality,
                oldDescriptor.visibility
            )
            newDescriptor.overriddenDescriptors += oldDescriptor.overriddenDescriptors
            return newDescriptor
        }

        //---------------------------------------------------------------------//

        private fun copyConstructorDescriptor(oldDescriptor: ConstructorDescriptor) : FunctionDescriptor {

            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val containingDeclaration = descriptorSubstituteMap[oldContainingDeclaration]
            val newDescriptor = ClassConstructorDescriptorImpl.create(
                containingDeclaration as ClassDescriptor,
                oldDescriptor.annotations,
                oldDescriptor.isPrimary,
                oldDescriptor.source
            )

            val newTypeParameters     = oldDescriptor.typeParameters
            val newValueParameters    = copyValueParameters(oldDescriptor.valueParameters, newDescriptor)
            val receiverParameterType = substituteType(oldDescriptor.dispatchReceiverParameter?.type)
            val returnType            = substituteType(oldDescriptor.returnType)
            assert(newTypeParameters.isEmpty())

            newDescriptor.initialize(
                receiverParameterType,
                null,                                               //  TODO @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
                newTypeParameters,
                newValueParameters,
                returnType,
                oldDescriptor.modality,
                oldDescriptor.visibility
            )
            return newDescriptor
        }


        //---------------------------------------------------------------------//

        private fun copyPropertyDescriptor(oldDescriptor: PropertyDescriptor): PropertyDescriptor {
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val memberOwner = descriptorSubstituteMap[oldContainingDeclaration] as ClassDescriptor
            val newDescriptor = PropertyDescriptorImpl.create(
                    memberOwner,
                    oldDescriptor.annotations,
                    oldDescriptor.modality,
                    oldDescriptor.visibility,
                    oldDescriptor.isVar,
                    oldDescriptor.name,
                    oldDescriptor.kind,
                    oldDescriptor.source,
                    oldDescriptor.isLateInit,
                    oldDescriptor.isConst,
                    oldDescriptor.isHeader,
                    oldDescriptor.isImpl,
                    oldDescriptor.isExternal,
                    oldDescriptor.isDelegated)

            newDescriptor.setType(
                    oldDescriptor.type,
                    oldDescriptor.typeParameters,
                    memberOwner.thisAsReceiverParameter,
                    oldDescriptor.extensionReceiverParameter?.type)

            newDescriptor.initialize(
                    oldDescriptor.getter?.let { copyPropertyGetterDescriptor(it, newDescriptor) },
                    oldDescriptor.setter?.let { copyPropertySetterDescriptor(it, newDescriptor) })

            newDescriptor.overriddenDescriptors += oldDescriptor.overriddenDescriptors

            return newDescriptor
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyGetterDescriptor(oldDescriptor: PropertyGetterDescriptor, newPropertyDescriptor: PropertyDescriptor)
                : PropertyGetterDescriptorImpl {

            return PropertyGetterDescriptorImpl(
                    newPropertyDescriptor,
                    oldDescriptor.annotations,
                    oldDescriptor.modality,
                    oldDescriptor.visibility,
                    oldDescriptor.isDefault,
                    oldDescriptor.isExternal,
                    oldDescriptor.isInline,
                    oldDescriptor.kind,
                    null,
                    oldDescriptor.source).apply {
                initialize(oldDescriptor.returnType)
            }
        }

        //---------------------------------------------------------------------//

        private fun copyPropertySetterDescriptor(oldDescriptor: PropertySetterDescriptor, newPropertyDescriptor: PropertyDescriptor)
                : PropertySetterDescriptorImpl {

            return PropertySetterDescriptorImpl(
                    newPropertyDescriptor,
                    oldDescriptor.annotations,
                    oldDescriptor.modality,
                    oldDescriptor.visibility,
                    oldDescriptor.isDefault,
                    oldDescriptor.isExternal,
                    oldDescriptor.isInline,
                    oldDescriptor.kind,
                    null,
                    oldDescriptor.source).apply {
                initialize(copyValueParameters(oldDescriptor.valueParameters, this).single())
            }
        }
        
        //---------------------------------------------------------------------//

        private fun copyClassDescriptor(oldDescriptor: ClassDescriptor): ClassDescriptorImpl {

            val oldSuperClass = oldDescriptor.getSuperClassOrAny()
            val newSuperClass = descriptorSubstituteMap.getOrDefault(oldSuperClass, oldSuperClass) as ClassDescriptor
            return ClassDescriptorImpl(
                targetFunction.descriptor,
                generateName(oldDescriptor.name),
                oldDescriptor.modality,
                oldDescriptor.kind,
                listOf(newSuperClass.defaultType),
                oldDescriptor.source,
                oldDescriptor.isExternal
            )
        }
    }

    //-------------------------------------------------------------------------//

    val descriptorSubstitutor = object : IrElementTransformerVoid() {

        override fun visitElement(element: IrElement): IrElement {
            return super.visitElement(element)
        }

        //---------------------------------------------------------------------//

        override fun visitClass(declaration: IrClass): IrStatement {
            val oldDeclaration = super.visitClass(declaration) as IrClass
            val newDescriptor = descriptorSubstituteMap[oldDeclaration.descriptor]
            if (newDescriptor == null) return oldDeclaration

            val newDeclaration = IrClassImpl(
                oldDeclaration.startOffset,
                oldDeclaration.endOffset,
                oldDeclaration.origin,
                newDescriptor as ClassDescriptor,
                oldDeclaration.declarations
            )

            return newDeclaration
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction): IrStatement {

            val oldDeclaration = super.visitFunction(declaration) as IrFunction
            val newDescriptor = descriptorSubstituteMap[oldDeclaration.descriptor]
            if (newDescriptor == null) return oldDeclaration

            return when (oldDeclaration) {
                is IrFunctionImpl    -> copyIrFunctionImpl(oldDeclaration, newDescriptor)
                is IrConstructorImpl -> copyIrConstructorImpl(oldDeclaration, newDescriptor)
                else -> TODO("Unsupported IrFunction subtype")
            }
        }

        //---------------------------------------------------------------------//

        override fun visitField(declaration: IrField): IrStatement {
            declaration.transformChildrenVoid(this)

            val oldDescriptor = declaration.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor] as PropertyDescriptor
            return IrFieldImpl(declaration.startOffset, declaration.endOffset, declaration.origin,
                    newDescriptor, declaration.initializer)
        }

        //---------------------------------------------------------------------//

        override fun visitProperty(declaration: IrProperty): IrStatement {
            declaration.transformChildrenVoid(this)

            val oldDescriptor = declaration.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor] as PropertyDescriptor
            return IrPropertyImpl(declaration.startOffset, declaration.endOffset, declaration.origin,
                    declaration.isDelegated, newDescriptor, declaration.backingField, declaration.getter, declaration.setter)
        }

        //---------------------------------------------------------------------//

        override fun visitGetField(expression: IrGetField): IrExpression {
            expression.transformChildrenVoid(this)

            val oldDescriptor = expression.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor] ?: oldDescriptor
            val oldSuperQualifier = expression.superQualifier
            val newSuperQualifier = oldSuperQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }
            if (newDescriptor == oldDescriptor && newSuperQualifier == oldSuperQualifier) return expression
            return IrGetFieldImpl(expression.startOffset, expression.endOffset, newDescriptor as PropertyDescriptor,
                    expression.receiver, expression.origin, newSuperQualifier)
        }

        //---------------------------------------------------------------------//

        override fun visitSetField(expression: IrSetField): IrExpression {
            expression.transformChildrenVoid(this)

            val oldDescriptor = expression.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor] ?: oldDescriptor
            val oldSuperQualifier = expression.superQualifier
            val newSuperQualifier = oldSuperQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }
            if (newDescriptor == oldDescriptor && newSuperQualifier == oldSuperQualifier) return expression
            return IrSetFieldImpl(expression.startOffset, expression.endOffset, newDescriptor as PropertyDescriptor,
                    expression.receiver, expression.value, expression.origin, newSuperQualifier)
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrExpression {
            val oldExpression = super.visitCall(expression) as IrCall

            return when (oldExpression) {
                is IrCallImpl -> transformIrCallImpl(oldExpression)
                is IrGetterCallImpl -> transformIrGetterCallImpl(oldExpression)
                is IrSetterCallImpl -> transformIrSetterCallImpl(oldExpression)
                else -> oldExpression
            }
        }

        private fun transformIrCallImpl(oldExpression: IrCallImpl): IrExpression {
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap.getOrDefault(oldDescriptor.original,
                    oldDescriptor) as FunctionDescriptor

            val oldSuperQualifier = oldExpression.superQualifier
            val newSuperQualifier = oldSuperQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }

            val newExpression = IrCallImpl(
                    oldExpression.startOffset,
                    oldExpression.endOffset,
                    substituteType(oldExpression.type)!!,
                    newDescriptor,
                    substituteTypeArguments(oldExpression.typeArguments),
                    oldExpression.origin,
                    newSuperQualifier
            ).apply {
                oldExpression.descriptor.valueParameters.forEach {
                    val valueArgument = oldExpression.getValueArgument(it)
                    putValueArgument(it.index, valueArgument)
                }
                extensionReceiver = oldExpression.extensionReceiver
                dispatchReceiver  = oldExpression.dispatchReceiver
            }

            return newExpression
        }

        private fun transformIrGetterCallImpl(oldExpression: IrGetterCallImpl): IrExpression {
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap.getOrDefault(oldDescriptor.original,
                    oldDescriptor) as FunctionDescriptor

            val oldSuperQualifier = oldExpression.superQualifier

            val newSuperQualifier = oldSuperQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }

            val newExpression = IrGetterCallImpl(
                    oldExpression.startOffset,
                    oldExpression.endOffset,
                    newDescriptor,
                    substituteTypeArguments(oldExpression.typeArguments),
                    oldExpression.origin,
                    newSuperQualifier
            ).apply {
                oldExpression.descriptor.valueParameters.forEach {
                    val valueArgument = oldExpression.getValueArgument(it)
                    putValueArgument(it.index, valueArgument)
                }
                extensionReceiver = oldExpression.extensionReceiver
                dispatchReceiver  = oldExpression.dispatchReceiver
            }

            return newExpression
        }

        private fun transformIrSetterCallImpl(oldExpression: IrSetterCallImpl): IrExpression {
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap.getOrDefault(oldDescriptor.original,
                    oldDescriptor) as FunctionDescriptor

            val oldSuperQualifier = oldExpression.superQualifier

            val newSuperQualifier = oldSuperQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }

            val newExpression = IrSetterCallImpl(
                    oldExpression.startOffset,
                    oldExpression.endOffset,
                    newDescriptor,
                    substituteTypeArguments(oldExpression.typeArguments),
                    oldExpression.origin,
                    newSuperQualifier
            ).apply {
                oldExpression.descriptor.valueParameters.forEach {
                    val valueArgument = oldExpression.getValueArgument(it)
                    putValueArgument(it.index, valueArgument)
                }
                extensionReceiver = oldExpression.extensionReceiver
                dispatchReceiver  = oldExpression.dispatchReceiver
            }

            return newExpression
        }

        //---------------------------------------------------------------------//

        override fun visitCallableReference(expression: IrCallableReference): IrExpression {

            val oldReference = super.visitCallableReference(expression) as IrCallableReference
            val oldDescriptor = oldReference.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldReference

            val oldTypeArguments = (oldReference as IrMemberAccessExpressionBase).typeArguments
            val newTypeArguments = substituteTypeArguments(oldTypeArguments)
            val newReference = IrCallableReferenceImpl(
                expression.startOffset,
                oldReference.endOffset,
                substituteType(oldReference.type)!!,
                newDescriptor as CallableDescriptor,
                newTypeArguments,
                oldReference.origin
            )
            return newReference
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrExpression {

            val oldReturn = super.visitReturn(expression) as IrReturn
            val oldDescriptor = oldReturn.returnTarget
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldReturn

            val newReturn = IrReturnImpl(
                oldReturn.startOffset,
                oldReturn.endOffset,
                substituteType(oldReturn.type)!!,
                newDescriptor as CallableDescriptor,
                oldReturn.value
            )
            return newReturn
        }

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrExpression {

            val oldExpression = super.visitGetValue(expression) as IrGetValue
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldExpression

            val newExpression = IrGetValueImpl(
                oldExpression.startOffset,
                oldExpression.endOffset,
                newDescriptor as ValueDescriptor,
                oldExpression.origin
            )
            return newExpression
        }

        //---------------------------------------------------------------------//

        override fun visitSetVariable(expression: IrSetVariable): IrExpression {

            val oldExpression = super.visitSetVariable(expression) as IrSetVariable
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldExpression

            val newExpression = IrSetVariableImpl(
                oldExpression.startOffset,
                oldExpression.endOffset,
                newDescriptor as VariableDescriptor,
                oldExpression.value,
                oldExpression.origin
            )
            return newExpression
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable): IrStatement {
            val oldDeclaration = super.visitVariable(declaration) as IrVariable
            val newDescriptor = descriptorSubstituteMap[oldDeclaration.descriptor]
            val newDeclaration = IrVariableImpl(
                oldDeclaration.startOffset,
                oldDeclaration.endOffset,
                oldDeclaration.origin,
                newDescriptor as VariableDescriptor,
                oldDeclaration.initializer
            )
            return newDeclaration
        }

        //---------------------------------------------------------------------//

        fun getTypeOperatorReturnType(operator: IrTypeOperator, type: KotlinType) : KotlinType {
            return when (operator) {
                IrTypeOperator.CAST,
                IrTypeOperator.IMPLICIT_CAST,
                IrTypeOperator.IMPLICIT_NOTNULL,
                IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                IrTypeOperator.IMPLICIT_INTEGER_COERCION    -> type
                IrTypeOperator.SAFE_CAST                    -> type.makeNullable()
                IrTypeOperator.INSTANCEOF,
                IrTypeOperator.NOT_INSTANCEOF               -> context.builtIns.booleanType
            }
        }

        //---------------------------------------------------------------------//

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {

            val oldExpression = super.visitTypeOperator(expression) as IrTypeOperatorCall
            if (typeArgsMap == null) return oldExpression

            if (oldExpression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {          // Nothing to do for IMPLICIT_COERCION_TO_UNIT
                return oldExpression
            }

            val typeOperand = oldExpression.typeOperand
            val operandTypeDescriptor = typeOperand.constructor.declarationDescriptor
            if (operandTypeDescriptor !is TypeParameterDescriptor) return oldExpression        // It is not TypeParameter - do nothing

            var newType = typeArgsMap[operandTypeDescriptor] ?: return expression
            if (typeOperand.isMarkedNullable) newType = newType.makeNullable()
            val operator        = oldExpression.operator
            val returnType      = getTypeOperatorReturnType(operator, newType)

            return IrTypeOperatorCallImpl(
                oldExpression.startOffset,
                oldExpression.endOffset,
                returnType,
                oldExpression.operator,
                newType,
                oldExpression.argument
            )
        }

        //--- Copy declarations -----------------------------------------------//

        private fun copyIrFunctionImpl(oldDeclaration: IrFunction, newDescriptor: DeclarationDescriptor): IrFunction {
            return IrFunctionImpl(
                oldDeclaration.startOffset, oldDeclaration.endOffset, oldDeclaration.origin,
                newDescriptor as FunctionDescriptor, oldDeclaration.body
            )
        }

        //---------------------------------------------------------------------//

        private fun copyIrConstructorImpl(oldDeclaration: IrConstructor, newDescriptor: DeclarationDescriptor): IrFunction {
            return IrConstructorImpl(
                oldDeclaration.startOffset, oldDeclaration.endOffset, oldDeclaration.origin,
                newDescriptor as ClassConstructorDescriptor, oldDeclaration.body!!
            )
        }
    }

    //-------------------------------------------------------------------------//

    private fun substituteType(oldType: KotlinType?): KotlinType? {
        if (typeSubstitutor == null) return oldType
        if (oldType == null)         return oldType
        return typeSubstitutor!!.substitute(oldType, Variance.INVARIANT) ?: oldType
    }

    //---------------------------------------------------------------------//

    private fun substituteTypeArguments(oldTypeArguments: Map <TypeParameterDescriptor, KotlinType>?): Map <TypeParameterDescriptor, KotlinType>? {

        if (oldTypeArguments == null) return null
        if (typeSubstitutor  == null) return oldTypeArguments

        val newTypeArguments = oldTypeArguments.entries.associate {
            val typeParameterDescriptor = it.key
            val oldTypeArgument         = it.value
            val newTypeArgument         = substituteType(oldTypeArgument)!!
            typeParameterDescriptor to newTypeArgument
        }
        return newTypeArguments
    }

    //---------------------------------------------------------------------//

    private fun copyValueParameters(oldValueParameters: List <ValueParameterDescriptor>, containingDeclaration: CallableDescriptor): List <ValueParameterDescriptor> {

        return oldValueParameters.map { oldDescriptor ->
            val newDescriptor = ValueParameterDescriptorImpl(
                containingDeclaration,
                oldDescriptor.original,
                oldDescriptor.index,
                oldDescriptor.annotations,
                oldDescriptor.name,
                substituteType(oldDescriptor.type)!!,
                oldDescriptor.declaresDefaultValue(),
                oldDescriptor.isCrossinline,
                oldDescriptor.isNoinline,
                substituteType(oldDescriptor.varargElementType),
                oldDescriptor.source
            )
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            newDescriptor
        }
    }

    //-------------------------------------------------------------------------//

    private fun createTypeSubstitutor(typeArgsMap: Map <TypeParameterDescriptor, KotlinType>?): TypeSubstitutor? {

        if (typeArgsMap == null) return null
        val substitutionContext = typeArgsMap.entries.associate {
            (typeParameter, typeArgument) ->
            typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
        }
        return TypeSubstitutor.create(substitutionContext)
    }
}