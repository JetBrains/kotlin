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
import org.jetbrains.kotlin.backend.konan.ir.IrInlineFunctionBody
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrTemporaryVariableDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallableReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.DeepCopyIrTree
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.makeNullable

internal class DeepCopyIrTreeWithDescriptors(val targetScope: ScopeWithIr,
                                             val context: Context) {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
    private var typeSubstitutor: TypeSubstitutor? = null
    private var nameIndex = 0

    //-------------------------------------------------------------------------//

    fun copy(irElement: IrElement, typeSubstitutor: TypeSubstitutor?): IrElement {

        this.typeSubstitutor = typeSubstitutor
        descriptorSubstituteMap.clear()
        irElement.acceptChildrenVoid(DescriptorCollector())
        // Transform calls to object that might be returned from inline function call.
        targetScope.irElement.transformChildrenVoid(descriptorSubstitutorForExternalScope)
        return irElement.accept(InlineCopyIr(), null)
    }

    //-------------------------------------------------------------------------//

    val descriptorSubstitutorForExternalScope = object : IrElementTransformerVoid() {

        override fun visitCall(expression: IrCall): IrExpression {
            val oldExpression = super.visitCall(expression) as IrCall

            if (oldExpression is IrCallImpl)
                return copyIrCallImpl(oldExpression)
            return oldExpression
        }
    }

    //-------------------------------------------------------------------------//

    inner class DescriptorCollector: IrElementVisitorVoidWithContext() {

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitClassNew(declaration: IrClass) {

            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            descriptorSubstituteMap[oldDescriptor.thisAsReceiverParameter] = newDescriptor.thisAsReceiverParameter

            super.visitClassNew(declaration)

            val constructors = oldDescriptor.constructors.map { oldConstructorDescriptor ->
                descriptorSubstituteMap[oldConstructorDescriptor] as ClassConstructorDescriptor
            }.toSet()

            val oldPrimaryConstructor = oldDescriptor.unsubstitutedPrimaryConstructor
            val primaryConstructor = oldPrimaryConstructor?.let { descriptorSubstituteMap[it] as ClassConstructorDescriptor }

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

        override fun visitPropertyNew(declaration: IrProperty) {

            copyPropertyOrField(declaration.descriptor)

            super.visitPropertyNew(declaration)
        }

        override fun visitFieldNew(declaration: IrField) {

            val oldDescriptor = declaration.descriptor
            if (descriptorSubstituteMap[oldDescriptor] == null) {
                // A field without a property or a field of a delegated property.
                copyPropertyOrField(oldDescriptor)
            }

            super.visitFieldNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitFunctionNew(declaration: IrFunction) {

            val oldDescriptor = declaration.descriptor
            if (oldDescriptor !is PropertyAccessorDescriptor) { // Property accessors are copied along with their property.
                val newDescriptor = copyFunctionDescriptor(oldDescriptor)
                descriptorSubstituteMap[oldDescriptor] = newDescriptor
            }

            super.visitFunctionNew(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitCall(expression: IrCall) {

            val oldDescriptor = expression.descriptor as FunctionDescriptor
            if (oldDescriptor is SimpleFunctionDescriptor) {
                val containingDeclaration = (targetScope.scope.scopeOwner as? CallableDescriptor) ?: oldDescriptor
                val newReturnType = substituteType(oldDescriptor.returnType)!!
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, containingDeclaration)
                val newDescriptor = oldDescriptor
                        .newCopyBuilder()
                        .setReturnType(newReturnType)
                        .setValueParameters(newValueParameters)
                        .setOriginal(oldDescriptor.original)
                        .build()
                descriptorSubstituteMap[oldDescriptor] = newDescriptor!!
            }

            super.visitCall(expression)
        }

        //---------------------------------------------------------------------//

        override fun visitVariable(declaration: IrVariable) {

            val oldDescriptor = declaration.descriptor
            val newDescriptor = IrTemporaryVariableDescriptorImpl(
                containingDeclaration = targetScope.scope.scopeOwner,
                name                  = generateCopyName(oldDescriptor.name),
                outType               = substituteType(oldDescriptor.type)!!,
                isMutable             = oldDescriptor.isVar)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor

            super.visitVariable(declaration)
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
                is ConstructorDescriptor       -> copyConstructorDescriptor(oldDescriptor)
                is SimpleFunctionDescriptor    -> copySimpleFunctionDescriptor(oldDescriptor)
                else -> TODO("Unsupported FunctionDescriptor subtype: $oldDescriptor")
            }
        }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor) : FunctionDescriptor {

            val newContainingDeclaration = parentScope?.let { descriptorSubstituteMap[it.scope.scopeOwner] } ?: targetScope.scope.scopeOwner

            return SimpleFunctionDescriptorImpl.create(
                /* containingDeclaration = */ newContainingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* name                  = */ generateCopyName(oldDescriptor.name),
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source
            ).apply {

                val oldDispatchReceiverParameter = oldDescriptor.dispatchReceiverParameter
                val newDispatchReceiverParameter = oldDispatchReceiverParameter?.let { descriptorSubstituteMap[it] as ReceiverParameterDescriptor }
                val newTypeParameters = oldDescriptor.typeParameters        // TODO substitute types
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                val newReceiverParameterType = substituteType(oldDescriptor.extensionReceiverParameter?.type)
                val newReturnType = substituteType(oldDescriptor.returnType)

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
                overriddenDescriptors += oldDescriptor.overriddenDescriptors
            }
        }

        //---------------------------------------------------------------------//

        private fun copyConstructorDescriptor(oldDescriptor: ConstructorDescriptor) : FunctionDescriptor {

            val containingDeclaration = parentScope?.let { descriptorSubstituteMap[it.scope.scopeOwner] } as ClassDescriptor
            return ClassConstructorDescriptorImpl.create(
                /* containingDeclaration = */ containingDeclaration,
                /* annotations           = */ oldDescriptor.annotations,
                /* isPrimary             = */ oldDescriptor.isPrimary,
                /* source                = */ oldDescriptor.source
            ).apply {

                val newTypeParameters = oldDescriptor.typeParameters
                val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, this)
                val receiverParameterType = substituteType(oldDescriptor.dispatchReceiverParameter?.type)
                val returnType = substituteType(oldDescriptor.returnType)
                assert(newTypeParameters.isEmpty())

                initialize(
                    /* receiverParameterType        = */ receiverParameterType,
                    /* dispatchReceiverParameter    = */ null, //  For constructor there is no explicit dispatch receiver.
                    /* typeParameters               = */ newTypeParameters,
                    /* unsubstitutedValueParameters = */ newValueParameters,
                    /* unsubstitutedReturnType      = */ returnType,
                    /* modality                     = */ oldDescriptor.modality,
                    /* visibility                   = */ oldDescriptor.visibility
                )
            }
        }


        //---------------------------------------------------------------------//

        private fun copyPropertyDescriptor(oldDescriptor: PropertyDescriptor): PropertyDescriptor {

            val memberOwner = currentClass?.let { descriptorSubstituteMap[it.scope.scopeOwner] } as ClassDescriptor
            return PropertyDescriptorImpl.create(
                /* containingDeclaration = */ memberOwner,
                /* annotations           = */ oldDescriptor.annotations,
                /* modality              = */ oldDescriptor.modality,
                /* visibility            = */ oldDescriptor.visibility,
                /* isVar                 = */ oldDescriptor.isVar,
                /* name                  = */ oldDescriptor.name,
                /* kind                  = */ oldDescriptor.kind,
                /* source                = */ oldDescriptor.source,
                /* lateInit              = */ oldDescriptor.isLateInit,
                /* isConst               = */ oldDescriptor.isConst,
                /* isHeader              = */ oldDescriptor.isHeader,
                /* isImpl                = */ oldDescriptor.isImpl,
                /* isExternal            = */ oldDescriptor.isExternal,
                /* isDelegated           = */ oldDescriptor.isDelegated
            ).apply {

                setType(
                    /* outType                   = */ oldDescriptor.type,
                    /* typeParameters            = */ oldDescriptor.typeParameters,
                    /* dispatchReceiverParameter = */ memberOwner.thisAsReceiverParameter,
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
            val newContainingDeclaration = parentScope?.let { descriptorSubstituteMap[it.scope.scopeOwner] } ?: targetScope.scope.scopeOwner
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
        override fun mapCatchParameterDeclaration   (descriptor: VariableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as VariableDescriptor
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
        override fun mapReturnTarget                (descriptor: CallableDescriptor)              = descriptorSubstituteMap.getOrDefault(descriptor, descriptor) as CallableDescriptor

        //---------------------------------------------------------------------//

        override fun mapSuperQualifier(qualifier: ClassDescriptor?): ClassDescriptor? {
            if (qualifier == null) return null
            return descriptorSubstituteMap.getOrDefault(qualifier,  qualifier) as ClassDescriptor
        }

        //--- Visits ----------------------------------------------------------//

        override fun visitCall(expression: IrCall): IrCall {

            val oldExpression = super.visitCall(expression)
            if (oldExpression !is IrCallImpl) return oldExpression                                        // TODO what other kinds of call can we meet?

            return copyIrCallImpl(oldExpression)
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction): IrFunction =
            IrFunctionImpl(
                startOffset = declaration.startOffset,
                endOffset   = declaration.endOffset,
                origin      = mapDeclarationOrigin(declaration.origin),
                descriptor  = mapFunctionDeclaration(declaration.descriptor),
                body        = declaration.body?.transform(this, null)
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

        override fun visitCallableReference(expression: IrCallableReference): IrCallableReference =
            IrCallableReferenceImpl(
                startOffset   = expression.startOffset,
                endOffset     = expression.endOffset,
                type          = substituteType(expression.type)!!,
                descriptor    = mapCallableReference(expression.descriptor),
                typeArguments = expression.getTypeArgumentsMap(),
                origin        = mapStatementOrigin(expression.origin)
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
                returnTarget = mapReturnTarget(expression.returnTarget),
                value        = expression.value.transform(this, null)
            )

        //---------------------------------------------------------------------//

        override fun visitBlock(expression: IrBlock): IrBlock {
            return if (expression is IrInlineFunctionBody) {
                IrInlineFunctionBody(
                    startOffset = expression.startOffset,
                    endOffset   = expression.endOffset,
                    type        = expression.type,
                    descriptor  = expression.descriptor,
                    origin      = mapStatementOrigin(expression.origin),
                    statements  = expression.statements.map { it.transform(this, null) }
                )
            } else {
                super.visitBlock(expression)
            }
        }
    }

    //---------------------------------------------------------------------//

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

    private fun copyIrCallImpl(oldExpression: IrCallImpl): IrCallImpl {
        val oldDescriptor = oldExpression.descriptor
        val newDescriptor = descriptorSubstituteMap.getOrDefault(oldDescriptor.original,
            oldDescriptor) as FunctionDescriptor

        val newSuperQualifier = oldExpression.superQualifier?.let { (descriptorSubstituteMap[it] ?: it) as ClassDescriptor }

        val newExpression = IrCallImpl(
            startOffset    = oldExpression.startOffset,
            endOffset      = oldExpression.endOffset,
            type           = substituteType(oldExpression.type)!!,
            descriptor     = newDescriptor,
            typeArguments  = substituteTypeArguments(oldExpression.typeArguments),
            origin         = oldExpression.origin,
            superQualifier = newSuperQualifier
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
}


