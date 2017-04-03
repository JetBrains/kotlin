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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal class DeepCopyIrTreeWithDescriptors(val targetFunction: IrFunction, val context: Context) {

    private val descriptorSubstituteMap: MutableMap<DeclarationDescriptor, DeclarationDescriptor> = mutableMapOf()
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

        override fun visitElement(element: IrElement) {
            element.acceptChildren(this, null)
        }

        //---------------------------------------------------------------------//

        override fun visitFunction(declaration: IrFunction) {
            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyFunctionDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            super.visitFunction(declaration)
        }

        //---------------------------------------------------------------------//

        override fun visitClass(declaration: IrClass) {
            val oldDescriptor = declaration.descriptor
            val newDescriptor = copyClassDescriptor(oldDescriptor)
            descriptorSubstituteMap[oldDescriptor] = newDescriptor
            super.visitClass(declaration)

            val constructors = oldDescriptor.constructors.map {
                descriptorSubstituteMap[it] as ClassConstructorDescriptor
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

        //--- Copy descriptors ------------------------------------------------//

        private fun generateName(name: Name): Name {
            val containingName  = targetFunction.descriptor.name.toString()                 // Name of inline target (function we inline in)
            val declarationName = name.toString()                                           // Name of declaration
            val indexStr        = (nameIndex++).toString()                                  // Unique for inline target index
            return Name.identifier(containingName + "_" + inlinedFunctionName + "_" + declarationName + "_" + indexStr)
        }

        //---------------------------------------------------------------------//

        private fun copyValueParameters(oldValueParameters: List <ValueParameterDescriptor>, owner: CallableDescriptor): List <ValueParameterDescriptor> {
            return oldValueParameters.map { oldDescriptor ->
                val newDescriptor = ValueParameterDescriptorImpl(
                    owner,
                    oldDescriptor.original,
                    oldDescriptor.index,
                    oldDescriptor.annotations,
                    oldDescriptor.name,
                    oldDescriptor.type,
                    oldDescriptor.declaresDefaultValue(),
                    oldDescriptor.isCrossinline,
                    oldDescriptor.isNoinline,
                    oldDescriptor.varargElementType,
                    oldDescriptor.source
                )
                descriptorSubstituteMap[oldDescriptor] = newDescriptor
                newDescriptor
            }
        }

        //---------------------------------------------------------------------//

        private fun copyFunctionDescriptor(oldDescriptor: FunctionDescriptor): FunctionDescriptor {

            return when (oldDescriptor) {
                is SimpleFunctionDescriptor -> copySimpleFunctionDescriptor(oldDescriptor)
                is ConstructorDescriptor    -> copyConstructorDescriptor(oldDescriptor)
                else -> TODO("Unsupported FunctionDescriptor subtype")
            }
        }

        //---------------------------------------------------------------------//

        private fun copySimpleFunctionDescriptor(oldDescriptor: SimpleFunctionDescriptor) : FunctionDescriptor {

            val memberOwner = targetFunction.descriptor
            val newDescriptor = SimpleFunctionDescriptorImpl.create(
                memberOwner,
                oldDescriptor.annotations,
                generateName(oldDescriptor.name),
                CallableMemberDescriptor.Kind.SYNTHESIZED,
                oldDescriptor.source
            ).apply { isTailrec = oldDescriptor.isTailrec }

            val newDispatchReceiverParameter = null                                         // TODO
            val newTypeParameters = oldDescriptor.typeParameters
            val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, memberOwner)

            newDescriptor.initialize(
                oldDescriptor.extensionReceiverParameter?.type,
                newDispatchReceiverParameter,
                newTypeParameters,
                newValueParameters,
                oldDescriptor.returnType,
                Modality.FINAL,
                Visibilities.LOCAL
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

            val newTypeParameters = oldDescriptor.typeParameters
            val newValueParameters = copyValueParameters(oldDescriptor.valueParameters, newDescriptor)
            newDescriptor.initialize(
                oldDescriptor.dispatchReceiverParameter?.type,
                null,                                               //  TODO @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
                newTypeParameters,
                newValueParameters,
                oldDescriptor.returnType,
                oldDescriptor.modality,
                oldDescriptor.visibility
            )
            return newDescriptor
        }

        //---------------------------------------------------------------------//

        private fun copyClassDescriptor(oldDescriptor: ClassDescriptor): ClassDescriptorImpl {

            return ClassDescriptorImpl(
                targetFunction.descriptor,
                generateName(oldDescriptor.name),
                oldDescriptor.modality,
                oldDescriptor.kind,
                listOf(context.builtIns.anyType),                                   // TODO get list of real supertypes
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

        override fun visitCall(expression: IrCall): IrExpression {

            val irCall = super.visitCall(expression) as IrCall
            if (irCall !is IrCallImpl) return irCall                                        // TODO what other kinds of call can we meet?

            val oldDescriptor = irCall.descriptor
            val newDescriptor = descriptorSubstituteMap.getOrDefault(oldDescriptor,
                oldDescriptor) as FunctionDescriptor

            val oldSuperQualifier = irCall.superQualifier
            var newSuperQualifier: ClassDescriptor? = oldSuperQualifier
            if (newSuperQualifier != null) {
                newSuperQualifier = descriptorSubstituteMap.getOrDefault(newSuperQualifier,
                    newSuperQualifier) as ClassDescriptor
            }

            return IrCallImpl(irCall.startOffset, irCall.endOffset, irCall.type, newDescriptor,
                irCall.typeArguments, irCall.origin, newSuperQualifier).apply {
                irCall.descriptor.valueParameters.forEach {
                    val valueArgument = irCall.getValueArgument(it)
                    putValueArgument(it.index, valueArgument)
                }
                extensionReceiver = irCall.extensionReceiver
                dispatchReceiver = irCall.dispatchReceiver
            }
        }

        //---------------------------------------------------------------------//

        override fun visitCallableReference(expression: IrCallableReference): IrExpression {

            val oldReference = super.visitCallableReference(expression) as IrCallableReference
            val oldDescriptor = oldReference.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldReference

            val typeArguments = (oldReference as IrMemberAccessExpressionBase).typeArguments
            val newReference = IrCallableReferenceImpl(expression.startOffset,
                oldReference.endOffset, oldReference.type, newDescriptor as CallableDescriptor,
                typeArguments, oldReference.origin)

            return newReference
        }

        //---------------------------------------------------------------------//

        override fun visitReturn(expression: IrReturn): IrExpression {

            val oldReturn = super.visitReturn(expression) as IrReturn
            val oldDescriptor = oldReturn.returnTarget
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldReturn

            val newReturn = IrReturnImpl(oldReturn.startOffset, oldReturn.endOffset,
                oldReturn.type, newDescriptor as CallableDescriptor, oldReturn.value)

            return newReturn
        }

        //---------------------------------------------------------------------//

        override fun visitGetValue(expression: IrGetValue): IrExpression {

            val oldExpression = super.visitGetValue(expression) as IrGetValue
            val oldDescriptor = oldExpression.descriptor
            val newDescriptor = descriptorSubstituteMap[oldDescriptor]
            if (newDescriptor == null) return oldExpression

            val newExpression = IrGetValueImpl(
                oldExpression.startOffset, oldExpression.endOffset,
                newDescriptor as ValueParameterDescriptor, oldExpression.origin
            )
            return newExpression
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
}