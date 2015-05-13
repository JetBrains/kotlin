/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * Generates Java overloads for functions and constructors that have the default
 * parameter values substituted.
 */
public class DefaultParameterValueSubstitutor(val state: GenerationState) {
    /**
     * If all of the parameters of the specified constructor declare default values,
     * generates a no-argument constructor that passes default values for all arguments.
     */
    fun generateConstructorOverloadsIfNeeded(constructorDescriptor: ConstructorDescriptor,
                                             classBuilder: ClassBuilder,
                                             context: CodegenContext<*>,
                                             classOrObject: JetClassOrObject) {
        if (generateOverloadsIfNeeded(classOrObject, constructorDescriptor, constructorDescriptor,
                                      context, classBuilder)) {
            return
        }

        if (!isEmptyConstructorNeeded(constructorDescriptor, classOrObject)) {
            return
        }

        generateOverloadWithSubstitutedParameters(constructorDescriptor, constructorDescriptor, classBuilder, classOrObject,
                                                  context,
                                                  constructorDescriptor.countDefaultParameters())
    }

    /**
     * If the function is annotated with [kotlin.jvm.overloads], generates Java methods that
     * have the default parameter values substituted. If a method has N parameters and M of which
     * have default values, M overloads are generated: the first one takes N-1 parameters (all but
     * the last one that takes a default value), the second takes N-2 parameters, and so on.
     *
     * @param functionDescriptor the method for which the overloads are generated
     * @param delegateFunctionDescriptor the method descriptor for the implementation that we need to call
     *     (same as [functionDescriptor] in all cases except for companion object methods annotated with [platformStatic],
     *     where [functionDescriptor] is the static method in the main class and [delegateFunctionDescriptor] is the
     *     implementation in the companion object class)
     * @return true if the overloads annotation was found on the element, false otherwise
     */
    fun generateOverloadsIfNeeded(methodElement: JetElement?,
                                  functionDescriptor: FunctionDescriptor,
                                  delegateFunctionDescriptor: FunctionDescriptor,
                                  owner: CodegenContext<*>,
                                  classBuilder: ClassBuilder): Boolean {
        if (functionDescriptor.getAnnotations().findAnnotation(FqName("kotlin.jvm.overloads")) == null) {
            return false
        }

        val count = functionDescriptor.countDefaultParameters()
        val context = owner.intoFunction(functionDescriptor)

        for (i in 1..count) {
            generateOverloadWithSubstitutedParameters(functionDescriptor, delegateFunctionDescriptor, classBuilder, methodElement, context, i)
        }
        return true
    }

    private fun FunctionDescriptor.countDefaultParameters() =
        getValueParameters().count { it.hasDefaultValue() }

    /**
     * Generates an overload for [functionDescriptor] that substitutes default values for the last
     * [substituteCount] parameters that have default values.
     *
     * @param functionDescriptor the method for which the overloads are generated
     * @param delegateFunctionDescriptor the method descriptor for the implementation that we need to call
     *     (same as [functionDescriptor] in all cases except for companion object methods annotated with [platformStatic],
     *     where [functionDescriptor] is the static method in the main class and [delegateFunctionDescriptor] is the
     *     implementation in the companion object class)
     * @param methodElement the PSI element for the method implementation (used in diagnostic messages only)
     */
    fun generateOverloadWithSubstitutedParameters(functionDescriptor: FunctionDescriptor,
                                                  delegateFunctionDescriptor: FunctionDescriptor,
                                                  classBuilder: ClassBuilder,
                                                  methodElement: JetElement?,
                                                  context: CodegenContext<*>,
                                                  substituteCount: Int) {
        val typeMapper = state.getTypeMapper()

        val isStatic = AsmUtil.isStaticMethod(context.getContextKind(), functionDescriptor)
        val flags = AsmUtil.getVisibilityAccessFlag(functionDescriptor) or (if (isStatic) Opcodes.ACC_STATIC else 0)
        val remainingParameters = getRemainingParameters(functionDescriptor.getOriginal(), substituteCount)
        val signature = typeMapper.mapSignature(functionDescriptor, context.getContextKind(), remainingParameters)
        val mv = classBuilder.newMethod(OtherOrigin(methodElement, functionDescriptor), flags,
                                        signature.getAsmMethod().getName(),
                                        signature.getAsmMethod().getDescriptor(),
                                        signature.getGenericsSignature(),
                                        FunctionCodegen.getThrownExceptions(functionDescriptor, typeMapper))

        AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(functionDescriptor, signature.getReturnType())

        remainingParameters.withIndex().forEach {
            val annotationCodegen = AnnotationCodegen.forParameter(it.index, mv, typeMapper)
            annotationCodegen.genAnnotations(it.value, signature.getValueParameters()[it.index].getAsmType())
        }

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) {
            mv.visitEnd()
            return
        }

        val frameMap = FrameMap()
        val v = InstructionAdapter(mv)
        mv.visitCode()

        val methodOwner = typeMapper.mapToCallableMethod(delegateFunctionDescriptor, false, context).owner
        if (!isStatic) {
            val thisIndex = frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            v.load(thisIndex, methodOwner) // Load this on stack
        }
        else {
            val delegateOwner = delegateFunctionDescriptor.getContainingDeclaration()
            if (delegateOwner is ClassDescriptor && delegateOwner.isCompanionObject()) {
                val singletonValue = StackValue.singleton(delegateOwner, typeMapper)
                singletonValue.put(singletonValue.type, v);
            }
        }

        val receiver = functionDescriptor.getExtensionReceiverParameter()
        if (receiver != null) {
            val receiverType = typeMapper.mapType(receiver)
            val receiverIndex = frameMap.enter(receiver, receiverType)
            StackValue.local(receiverIndex, receiverType).put(receiverType, v)
        }
        remainingParameters.forEach {
            frameMap.enter(it, typeMapper.mapType(it))
        }

        var mask = 0
        val masks = arrayListOf<Int>()
        for (parameterDescriptor in functionDescriptor.getValueParameters()) {
            val paramType = typeMapper.mapType(parameterDescriptor.getType())
            if (parameterDescriptor in remainingParameters) {
                val index = frameMap.getIndex(parameterDescriptor)
                StackValue.local(index, paramType).put(paramType, v)
            }
            else {
                AsmUtil.pushDefaultValueOnStack(paramType, v)
                val i = parameterDescriptor.getIndex()
                if (i != 0 && i % Integer.SIZE == 0) {
                    masks.add(mask)
                    mask = 0
                }
                mask = mask or (1 shl (i % Integer.SIZE))
            }
        }
        masks.add(mask)
        for (m in masks) {
            v.iconst(m)
        }

        // constructors with default arguments has last synthetic argument of specific type
        if (functionDescriptor is ConstructorDescriptor) {
            v.aconst(null)
        }

        val defaultMethod = typeMapper.mapDefaultMethod(delegateFunctionDescriptor, context.getContextKind(), context)
        if (functionDescriptor is ConstructorDescriptor) {
            v.invokespecial(methodOwner.getInternalName(), defaultMethod.getName(), defaultMethod.getDescriptor(), false)
        }
        else {
            v.invokestatic(methodOwner.getInternalName(), defaultMethod.getName(), defaultMethod.getDescriptor(), false)
        }
        v.areturn(signature.getReturnType())
        FunctionCodegen.endVisit(mv, null, methodElement)
    }

    private fun getRemainingParameters(functionDescriptor: FunctionDescriptor,
                                       substituteCount: Int): List<ValueParameterDescriptor> {
        var remainingCount = functionDescriptor.countDefaultParameters() - substituteCount
        return functionDescriptor.getValueParameters().filter { !it.declaresDefaultValue() || --remainingCount >= 0 }
    }

    private fun isEmptyConstructorNeeded(constructorDescriptor: ConstructorDescriptor, classOrObject: JetClassOrObject): Boolean {
        val classDescriptor = constructorDescriptor.getContainingDeclaration()

        if (classOrObject.isLocal()) return false

        if (CodegenBinding.canHaveOuter(state.getBindingContext(), classDescriptor)) return false

        if (Visibilities.isPrivate(classDescriptor.getVisibility()) || Visibilities.isPrivate(constructorDescriptor.getVisibility()))
            return false

        if (constructorDescriptor.getValueParameters().isEmpty()) return false
        if (classOrObject is JetClass && hasSecondaryConstructorsWithNoParameters(classOrObject)) return false

        return constructorDescriptor.getValueParameters().all { it.declaresDefaultValue() }
    }

    private fun hasSecondaryConstructorsWithNoParameters(klass: JetClass) =
        klass.getSecondaryConstructors().any { it.getValueParameters().isEmpty() }

}
