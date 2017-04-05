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
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
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

    fun copy(irElement: IrElement, functionName: String): IrElement {

        inlinedFunctionName = functionName
        descriptorSubstituteMap.clear()
        irElement.acceptChildrenVoid(descriptorCollector)
        val newElement = irElement.accept(InlineCopyIr(), null)
        val newElement1 = newElement.accept(descriptorSubstitutor, null)
        return newElement1
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
                    setOriginal(oldDescriptor.original)
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
    }

    //-------------------------------------------------------------------------//

    private fun substituteType(oldType: KotlinType?): KotlinType? {
        if (typeSubstitutor == null) return oldType
        if (oldType == null)         return oldType
        return typeSubstitutor!!.substitute(oldType, Variance.INVARIANT) ?: oldType
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

    private fun createTypeSubstitutor(typeArgsMap: Map <TypeParameterDescriptor, KotlinType>?): TypeSubstitutor? {

        if (typeArgsMap == null) return null
        val substitutionContext = typeArgsMap.entries.associate {
            (typeParameter, typeArgument) ->
            typeParameter.typeConstructor to TypeProjectionImpl(typeArgument)
        }
        return TypeSubstitutor.create(substitutionContext)
    }

//-----------------------------------------------------------------------------//

    inner class InlineCopyIr() : DeepCopyIrTree() {

        override fun mapFunctionDeclaration         (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapConstructorDeclaration      (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapPropertyDeclaration         (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapLocalPropertyDeclaration    (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapEnumEntryDeclaration        (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapVariableDeclaration         (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapErrorDeclaration            (descriptor: DeclarationDescriptor)           = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as DeclarationDescriptor
        override fun mapClassReference              (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapValueReference              (descriptor: ValueDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ValueDescriptor
        override fun mapVariableReference           (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapPropertyReference           (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapCallee                      (descriptor: CallableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as CallableDescriptor
        override fun mapDelegatedConstructorCallee  (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapEnumConstructorCallee       (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapCallableReference           (descriptor: CallableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as CallableDescriptor
        override fun mapClassifierReference         (descriptor: ClassifierDescriptor)            = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassifierDescriptor

        //---------------------------------------------------------------------//

        override fun mapSuperQualifier(qualifier: ClassDescriptor?): ClassDescriptor? {
            if (qualifier == null) return null
            return descriptorSubstituteMap.getOrDefault(qualifier,  qualifier) as ClassDescriptor
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrCall =
            copyCall(expression).transformValueArguments(expression)

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction): IrFunction =
            IrFunctionImpl(
                declaration.startOffset, declaration.endOffset,
                mapDeclarationOrigin(declaration.origin),
                mapFunctionDeclaration(declaration.descriptor),
                declaration.body?.transform(this, null)
            ).transformDefaults(declaration)

        //---------------------------------------------------------------------//

        private fun <T : IrFunction> T.transformDefaults(original: T): T {
            for (originalValueParameter in original.descriptor.valueParameters) {
                val valueParameter = descriptor.valueParameters[originalValueParameter.index]
                original.getDefault(originalValueParameter)?.let { irDefaultParameterValue ->
                    putDefault(valueParameter, irDefaultParameterValue.transform(this@InlineCopyIr, null))
                }
            }
            return this
        }

        //---------------------------------------------------------------------//

        private fun copyCall(expression: IrCall) =
            when (expression) {
                is IrCallWithShallowCopy ->
                    expression.shallowCopy(
                        mapStatementOrigin(expression.origin),
                        mapCallee(expression.descriptor),
                        mapSuperQualifier(expression.superQualifier)
                    )
                else ->
                    IrCallImpl(
                        expression.startOffset, expression.endOffset,
                        substituteType(expression.type)!!,
                        mapCallee(expression.descriptor),
                        expression.getTypeArgumentsMap(),
                        mapStatementOrigin(expression.origin),
                        mapSuperQualifier(expression.superQualifier)
                    )
            }

        //---------------------------------------------------------------------//

        override fun visitCallableReference(expression: IrCallableReference): IrCallableReference =
            IrCallableReferenceImpl(
                expression.startOffset, expression.endOffset,
                substituteType(expression.type)!!,
                mapCallableReference(expression.descriptor),
                expression.getTypeArgumentsMap(),
                mapStatementOrigin(expression.origin)
            ).transformValueArguments(expression)

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

        override fun visitTypeOperator(expression: IrTypeOperatorCall): IrTypeOperatorCall {

            val typeOperand = substituteType(expression.typeOperand)!!
            val returnType = getTypeOperatorReturnType(expression.operator, typeOperand)
            return IrTypeOperatorCallImpl(
                expression.startOffset, expression.endOffset,
                returnType,
                expression.operator,
                typeOperand,
                expression.argument.transform(this, null)
            )
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                expression.startOffset, expression.endOffset,
                substituteType(expression.type)!!,
                mapReturnTarget(expression.returnTarget),
                expression.value.transform(this, null)
            )

        //---------------------------------------------------------------------//

        override fun visitBlock(expression: IrBlock): IrBlock {
            return if (expression is IrInlineFunctionBody) {
                IrInlineFunctionBody(
                    expression.startOffset, expression.endOffset,
                    expression.type,
                    expression.descriptor,
                    mapStatementOrigin(expression.origin),
                    expression.statements.map { it.transform(this, null) }
                )
            } else {
                super.visitBlock(expression)
            }
        }
    }
}


