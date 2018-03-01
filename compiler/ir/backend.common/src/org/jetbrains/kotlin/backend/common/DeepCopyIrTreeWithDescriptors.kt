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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class DeepCopyIrTreeWithDescriptors(val targetDescriptor: FunctionDescriptor,
                                             val context: CommonBackendContext) {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
    private var typeSubstitutor: TypeSubstitutor? = null
    private var nameIndex = 0

    //-------------------------------------------------------------------------//

    fun copy(irElement: IrElement, typeSubstitutor: TypeSubstitutor?): IrElement {
        this.typeSubstitutor = typeSubstitutor
        irElement.acceptChildrenVoid(DescriptorCollector())
        return irElement.accept(InlineCopyIr(), null)
    }

    //-------------------------------------------------------------------------//

    inner class DescriptorCollector: IrElementVisitorVoid {

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitClass(declaration: IrClass) {

            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            descriptorSubstituteMap[oldDescriptor.thisAsReceiverParameter] = newDescriptor.thisAsReceiverParameter

            super.visitClass(declaration)

            val constructors = oldDescriptor.constructors.map { oldConstructorDescriptor ->
                descriptorSubstituteMap[oldConstructorDescriptor] as ClassConstructorDescriptor
            }.toSet()

            val oldPrimaryConstructor = oldDescriptor.unsubstitutedPrimaryConstructor
            val primaryConstructor = oldPrimaryConstructor?.let { descriptorSubstituteMap[it] as ClassConstructorDescriptor }

            val contributedDescriptors = oldDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map {
                        descriptorSubstituteMap[it]!!
                    }
            newDescriptor.initialize(
                    SimpleMemberScope(contributedDescriptors),
                    constructors,
                    primaryConstructor
            )
        }

        //---------------------------------------------------------------------//

        override fun visitProperty(declaration: IrProperty) {

            copyPropertyOrField(declaration.descriptor)
            super.visitProperty(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitField(declaration: IrField) {

            val oldDescriptor = declaration.descriptor
            if (descriptorSubstituteMap[oldDescriptor] == null) {
                copyPropertyOrField(oldDescriptor)                                          // A field without a property or a field of a delegated property.
            }
            super.visitField(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction) {

            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) {                             // Property accessors are copied along with their property.
                val newDescriptor = copyFunctionDescriptor(oldDescriptor)
                descriptorSubstituteMap[oldDescriptor] = newDescriptor
                oldDescriptor.extensionReceiverParameter?.let{
                    descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
                }
            }
            super.visitFunction(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable) {

            val oldDescriptor = declaration.descriptor
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            val newDescriptor = IrTemporaryVariableDescriptorImpl(
                containingDeclaration = newContainingDeclaration,
                name                  = generateCopyName(oldDescriptor.name),
                outType               = substituteType(oldDescriptor.type)!!,
                isMutable             = oldDescriptor.isVar)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor

            super.visitVariable(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitCatch(aCatch: IrCatch) {
            val oldDescriptor = aCatch.parameter
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            val newDescriptor = IrTemporaryVariableDescriptorImpl(
                    containingDeclaration = newContainingDeclaration,
                    name                  = generateCopyName(oldDescriptor.name),
                    outType               = substituteType(oldDescriptor.type)!!,
                    isMutable             = oldDescriptor.isVar)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor

            super.visitCatch(aCatch)
        }

        //--- Copy descriptors ------------------------------------------------//

        private fun generateCopyName(name: Name): Name {

            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(declarationName + "_" + indexStr)
        }

        //---------------------------------------------------------------------//

        private fun copyFunctionDescriptor(oldDescriptor: CallableDescriptor): CallableDescriptor {

            return when (oldDescriptor) {
                is ConstructorDescriptor    -> copyConstructorDescriptor(oldDescriptor)
                is SimpleFunctionDescriptor -> copySimpleFunctionDescriptor(oldDescriptor)
                else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
            }
        }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor) : FunctionDescriptor {

            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* name                  = */ generateCopyName(oldDescriptor.name),
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source
            ).apply {

                val oldDispatchReceiverParameter = oldDescriptor.dispatchReceiverParameter
                val newDispatchReceiverParameter = oldDispatchReceiverParameter?.let { descriptorSubstituteMap.getOrDefault(it, it) as ReceiverParameterDescriptor }
                val newTypeParameters            = oldDescriptor.typeParameters        // TODO substitute types
                val newValueParameters           = copyValueParameters(oldDescriptor.valueParameters, this)
                val newReceiverParameterType     = substituteType(oldDescriptor.extensionReceiverParameter?.type)
                val newReturnType                = substituteType(oldDescriptor.returnType)

                initialize(
                    /* receiverParameterType        = */ newReceiverParameterType,
                    /* dispatchReceiverParameter    = */ newDispatchReceiverParameter,
                    /* typeParameters               = */ newTypeParameters,
                    /* unsubstitutedValueParameters = */ newValueParameters,
                    /* unsubstitutedReturnType      = */ newReturnType,
                    /* modality                     = */ oldDescriptor.modality,
                    /* visibility                   = */ oldDescriptor.visibility
                )
                isTailrec             =  oldDescriptor.isTailrec
                isSuspend             =  oldDescriptor.isSuspend
                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }
        }

        //---------------------------------------------------------------------//

        private fun copyConstructorDescriptor(oldDescriptor: ConstructorDescriptor) : FunctionDescriptor {

            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            return ClassConstructorDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration as ClassDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* isPrimary             = */ oldDescriptor.isPrimary,
                /* source                = */ oldDescriptor.source
            ).apply {

                val newTypeParameters     = oldDescriptor.typeParameters
                val newValueParameters    = copyValueParameters(oldDescriptor.valueParameters, this)
                val receiverParameterType = substituteType(oldDescriptor.dispatchReceiverParameter?.type)
                val returnType            = substituteType(oldDescriptor.returnType)

                initialize(
                    /* receiverParameterType        = */ receiverParameterType,
                    /* dispatchReceiverParameter    = */ null,                              //  For constructor there is no explicit dispatch receiver.
                    /* typeParameters               = */ newTypeParameters,
                    /* unsubstitutedValueParameters = */ newValueParameters,
                    /* unsubstitutedReturnType      = */ returnType,
                    /* modality                     = */ oldDescriptor.modality,
                    /* visibility                   = */ oldDescriptor.visibility
                )
            }
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
            oldDescriptor.extensionReceiverParameter?.let{
                descriptorSubstituteMap[it] = newDescriptor.extensionReceiverParameter!!
            }

        }

        //---------------------------------------------------------------------//

        private fun copyPropertyDescriptor(oldDescriptor: PropertyDescriptor): PropertyDescriptor {

            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration) as ClassDescriptor
            return PropertyDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isVar                 = */ oldDescriptor.isVar,
                /* name                  = */ oldDescriptor.name,
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source,
                /* lateInit              = */ oldDescriptor.isLateInit,
                /* isConst               = */ oldDescriptor.isConst,
                /* isExpect              = */ oldDescriptor.isExpect,
                /* isActual                = */ oldDescriptor.isActual,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isDelegated           = */ oldDescriptor.isDelegated
            ).apply {

                setType(
                    /* outType                   = */ oldDescriptor.type,
                    /* typeParameters            = */ oldDescriptor.typeParameters,
                    /* dispatchReceiverParameter = */ newContainingDeclaration.thisAsReceiverParameter,
                    /* receiverType              = */ oldDescriptor.extensionReceiverParameter?.type)

                initialize(
                    /* getter = */ oldDescriptor.getter?.let { copyPropertyGetterDescriptor(it, this) },
                    /* setter = */ oldDescriptor.setter?.let { copyPropertySetterDescriptor(it, this) })

                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }
        }

        //---------------------------------------------------------------------//

        private fun copyPropertyGetterDescriptor(oldDescriptor: PropertyGetterDescriptor, newPropertyDescriptor: PropertyDescriptor)
                : PropertyGetterDescriptorImpl {

            return PropertyGetterDescriptorImpl(
                /* correspondingProperty = */ newPropertyDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isDefault             = */ oldDescriptor.isDefault,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isInline              = */ oldDescriptor.isInline,
                /* kind                  = */ oldDescriptor.kind,
                /* original              = */ null,
                /* source                = */ oldDescriptor.source).apply {
                initialize(oldDescriptor.returnType)
            }
        }

        //---------------------------------------------------------------------//

        private fun copyPropertySetterDescriptor(oldDescriptor: PropertySetterDescriptor, newPropertyDescriptor: PropertyDescriptor)
                : PropertySetterDescriptorImpl {

            return PropertySetterDescriptorImpl(
                /* correspondingProperty = */ newPropertyDescriptor,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isDefault             = */ oldDescriptor.isDefault,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isInline              = */ oldDescriptor.isInline,
                /* kind                  = */ oldDescriptor.kind,
                /* original              = */ null,
                /* source                = */ oldDescriptor.source).apply {
                initialize(copyValueParameters(oldDescriptor.valueParameters, this).single())
            }
        }
        
        //---------------------------------------------------------------------//

        private fun copyClassDescriptor(oldDescriptor: ClassDescriptor): ClassDescriptorImpl {

            val oldSuperClass = oldDescriptor.getSuperClassOrAny()
            val newSuperClass = descriptorSubstituteMap.getOrDefault(oldSuperClass, oldSuperClass) as ClassDescriptor
            val oldContainingDeclaration = oldDescriptor.containingDeclaration
            val newContainingDeclaration = descriptorSubstituteMap.getOrDefault(oldContainingDeclaration, oldContainingDeclaration)
            val newName = if (DescriptorUtils.isAnonymousObject(oldDescriptor))      // Anonymous objects are identified by their name.
                oldDescriptor.name                                                   // We need to preserve it for LocalDeclarationsLowering.
            else
                generateCopyName(oldDescriptor.name)
            return ClassDescriptorImpl(
                /* containingDeclaration = */ newContainingDeclaration,
                /* name                  = */ newName,
                /* modality              = */ oldDescriptor.modality,
                /* kind                  = */ oldDescriptor.kind,
                /* supertypes            = */ listOf(newSuperClass.defaultType),
                /* source                = */ oldDescriptor.source,
                /* isExternal            = */ oldDescriptor.isExternal
            )
        }
    }

//-----------------------------------------------------------------------------//

    inner class InlineCopyIr : DeepCopyIrTree() {

        override fun mapClassDeclaration            (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapTypeAliasDeclaration        (descriptor: TypeAliasDescriptor)             = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as TypeAliasDescriptor
        override fun mapFunctionDeclaration         (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapConstructorDeclaration      (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapPropertyDeclaration         (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapLocalPropertyDeclaration    (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapEnumEntryDeclaration        (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapVariableDeclaration         (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapErrorDeclaration            (descriptor: DeclarationDescriptor)           = descriptorSubstituteMap.getOrDefault(descriptor, descriptor)

        override fun mapClassReference              (descriptor: ClassDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassDescriptor
        override fun mapValueReference              (descriptor: ValueDescriptor)                 = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ValueDescriptor
        override fun mapVariableReference           (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
        override fun mapPropertyReference           (descriptor: PropertyDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as PropertyDescriptor
        override fun mapCallee                      (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor
        override fun mapDelegatedConstructorCallee  (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapEnumConstructorCallee       (descriptor: ClassConstructorDescriptor)      = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassConstructorDescriptor
        override fun mapLocalPropertyReference      (descriptor: VariableDescriptorWithAccessors) = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptorWithAccessors
        override fun mapClassifierReference         (descriptor: ClassifierDescriptor)            = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as ClassifierDescriptor
        override fun mapReturnTarget                (descriptor: FunctionDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as FunctionDescriptor

        //---------------------------------------------------------------------//

        override fun mapSuperQualifier(qualifier: ClassDescriptor?): ClassDescriptor? {
            if (qualifier == null) return null
            return descriptorSubstituteMap.getOrDefault(qualifier,  qualifier) as ClassDescriptor
        }

        //--- Visits ----------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrCall {
            if (expression !is IrCallImpl) return super.visitCall(expression)
            val newDescriptor = mapCallee(expression.descriptor)
            return IrCallImpl(
                startOffset    = expression.startOffset,
                endOffset      = expression.endOffset,
                type           = newDescriptor.returnType!!,
                calleeDescriptor = newDescriptor,
                typeArguments  = substituteTypeArguments(expression.transformTypeArguments(newDescriptor)),
                origin         = expression.origin,
                superQualifierDescriptor = mapSuperQualifier(expression.superQualifier)
            ).transformValueArguments(expression)
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction): IrFunction =
            IrFunctionImpl(
                startOffset = declaration.startOffset,
                endOffset   = declaration.endOffset,
                origin      = mapDeclarationOrigin(declaration.origin),
                descriptor  = mapFunctionDeclaration(declaration.descriptor),
                body        = declaration.body?.transform(this, null)
            ).transformParameters(declaration)

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
                startOffset = expression.startOffset,
                endOffset   = expression.endOffset,
                type        = returnType,
                operator    = expression.operator,
                typeOperand = typeOperand,
                argument    = expression.argument.transform(this, null)
            )
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrReturn =
            IrReturnImpl(
                startOffset  = expression.startOffset,
                endOffset    = expression.endOffset,
                type         = substituteType(expression.type)!!,
                returnTargetDescriptor = mapReturnTarget(expression.returnTarget),
                value        = expression.value.transform(this, null)
            )

        //---------------------------------------------------------------------//

        override fun visitBlock(expression: IrBlock): IrBlock {
            return if (expression is IrReturnableBlock) {
                IrReturnableBlockImpl(
                    startOffset    = expression.startOffset,
                    endOffset      = expression.endOffset,
                    type           = expression.type,
                    descriptor     = expression.descriptor,
                    origin         = mapStatementOrigin(expression.origin),
                    statements     = expression.statements.map { it.transform(this, null) },
                    sourceFileName = expression.sourceFileName
                )
            } else {
                super.visitBlock(expression)
            }
        }

        override fun getNonTransformedLoop(irLoop: IrLoop): IrLoop {
            return irLoop
        }
    }

    //-------------------------------------------------------------------------//

    private fun copyValueParameters(oldValueParameters: List <ValueParameterDescriptor>, containingDeclaration: CallableDescriptor): List <ValueParameterDescriptor> {

        return oldValueParameters.map { oldDescriptor ->
            val newDescriptor = ValueParameterDescriptorImpl(
                containingDeclaration = containingDeclaration,
                original              = oldDescriptor.original,
                index                 = oldDescriptor.index,
                annotations           = oldDescriptor.annotations,
                name                  = oldDescriptor.name,
                outType               = substituteType(oldDescriptor.type)!!,
                declaresDefaultValue  = oldDescriptor.declaresDefaultValue(),
                isCrossinline         = oldDescriptor.isCrossinline,
                isNoinline            = oldDescriptor.isNoinline,
                varargElementType     = substituteType(oldDescriptor.varargElementType),
                source                = oldDescriptor.source
            )
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            newDescriptor
        }
    }

    //-------------------------------------------------------------------------//

    private fun substituteType(oldType: KotlinType?): KotlinType? {
        if (typeSubstitutor == null) return oldType
        if (oldType == null)         return oldType
        return typeSubstitutor!!.substitute(oldType, Variance.INVARIANT) ?: oldType
    }

    //-------------------------------------------------------------------------//

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

    //-------------------------------------------------------------------------//

    fun addCurrentSubstituteMap(globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>) {
        descriptorSubstituteMap.forEach { t, u ->
            globalSubstituteMap.put(t, SubstitutedDescriptor(targetDescriptor, u))
        }
    }

}

class SubstitutedDescriptor(val inlinedFunction: FunctionDescriptor, val descriptor: DeclarationDescriptor)

class DescriptorSubstitutorForExternalScope(val globalSubstituteMap: MutableMap<DeclarationDescriptor, SubstitutedDescriptor>)
    : IrElementTransformerVoidWithContext() {

    override fun visitCall(expression: IrCall): IrExpression {
        val oldExpression = super.visitCall(expression) as IrCall

        val substitutedDescriptor = globalSubstituteMap[expression.descriptor.original]
                ?: return oldExpression
        if (allScopes.any { it.scope.scopeOwner == substitutedDescriptor.inlinedFunction })
            return oldExpression
        return when (oldExpression) {
            is IrCallImpl -> copyIrCallImpl(oldExpression, substitutedDescriptor)
            is IrCallWithShallowCopy -> copyIrCallWithShallowCopy(oldExpression, substitutedDescriptor)
            else -> oldExpression
        }
    }

    //-------------------------------------------------------------------------//

    private fun copyIrCallImpl(oldExpression: IrCallImpl, substitutedDescriptor: SubstitutedDescriptor): IrCallImpl {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        val newExpression = IrCallImpl(
                startOffset              = oldExpression.startOffset,
                endOffset                = oldExpression.endOffset,
                type                     = oldExpression.type,
                calleeDescriptor         = newDescriptor,
                typeArguments            = oldExpression.typeArguments,
                origin                   = oldExpression.origin,
                superQualifierDescriptor = oldExpression.superQualifier
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

    //-------------------------------------------------------------------------//

    private fun copyIrCallWithShallowCopy(oldExpression: IrCallWithShallowCopy, substitutedDescriptor: SubstitutedDescriptor): IrCall {

        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = substitutedDescriptor.descriptor as FunctionDescriptor

        if (newDescriptor == oldDescriptor)
            return oldExpression

        return oldExpression.shallowCopy(oldExpression.origin, newDescriptor, oldExpression.superQualifier)
    }
}

