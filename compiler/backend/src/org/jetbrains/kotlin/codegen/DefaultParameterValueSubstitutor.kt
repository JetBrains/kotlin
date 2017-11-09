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

import org.jetbrains.kotlin.codegen.JvmCodegenUtil.getDispatchReceiverParameterForConstructorCall
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.KtPureElement
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmOverloadsAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOriginFromPure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * Generates Java overloads for functions and constructors that have the default
 * parameter values substituted.
 */
class DefaultParameterValueSubstitutor(val state: GenerationState) {
    private companion object {
        // rename -> JvmOverloads
        private val ANNOTATION_TYPE_DESCRIPTOR_FOR_JVMOVERLOADS_GENERATED_METHODS: String =
                Type.getObjectType("synthetic/kotlin/jvm/GeneratedByJvmOverloads").descriptor
    }

    /**
     * If all of the parameters of the specified constructor declare default values,
     * generates a no-argument constructor that passes default values for all arguments.
     */
    fun generatePrimaryConstructorOverloadsIfNeeded(
            constructorDescriptor: ConstructorDescriptor,
            classBuilder: ClassBuilder,
            memberCodegen: MemberCodegen<*>,
            contextKind: OwnerKind,
            classOrObject: KtPureClassOrObject
    ) {
        val methodElement = classOrObject.primaryConstructor ?: classOrObject

        if (generateOverloadsIfNeeded(methodElement, constructorDescriptor, constructorDescriptor, contextKind, classBuilder, memberCodegen)) {
            return
        }

        if (!isEmptyConstructorNeeded(constructorDescriptor, classOrObject)) {
            return
        }

        generateOverloadWithSubstitutedParameters(
                constructorDescriptor, constructorDescriptor, classBuilder, memberCodegen, methodElement, contextKind,
                constructorDescriptor.countDefaultParameters())
    }

    /**
     * If the function is annotated with [kotlin.jvm.jvmOverloads], generates Java methods that
     * have the default parameter values substituted. If a method has N parameters and M of which
     * have default values, M overloads are generated: the first one takes N-1 parameters (all but
     * the last one that takes a default value), the second takes N-2 parameters, and so on.
     *
     * @param functionDescriptor the method for which the overloads are generated
     * @param delegateFunctionDescriptor the method descriptor for the implementation that we need to call
     *     (same as [functionDescriptor] in all cases except for companion object methods annotated with @JvmStatic,
     *     where [functionDescriptor] is the static method in the main class and [delegateFunctionDescriptor] is the
     *     implementation in the companion object class)
     * @return true if the overloads annotation was found on the element, false otherwise
     */
    fun generateOverloadsIfNeeded(
            methodElement: KtPureElement?,
            functionDescriptor: FunctionDescriptor,
            delegateFunctionDescriptor: FunctionDescriptor,
            contextKind: OwnerKind,
            classBuilder: ClassBuilder,
            memberCodegen: MemberCodegen<*>
    ): Boolean {
        functionDescriptor.findJvmOverloadsAnnotation() ?: return false

        val count = functionDescriptor.countDefaultParameters()

        for (i in 1..count) {
            generateOverloadWithSubstitutedParameters(
                    functionDescriptor, delegateFunctionDescriptor, classBuilder, memberCodegen, methodElement, contextKind, i
            )
        }

        return true
    }

    private fun FunctionDescriptor.countDefaultParameters() =
        valueParameters.count { it.hasDefaultValue() }

    /**
     * Generates an overload for [functionDescriptor] that substitutes default values for the last
     * [substituteCount] parameters that have default values.
     *
     * @param functionDescriptor the method for which the overloads are generated
     * @param delegateFunctionDescriptor the method descriptor for the implementation that we need to call
     *     (same as [functionDescriptor] in all cases except for companion object methods annotated with @JvmStatic,
     *     where [functionDescriptor] is the static method in the main class and [delegateFunctionDescriptor] is the
     *     implementation in the companion object class)
     * @param methodElement the PSI element for the method implementation (used in diagnostic messages only)
     */
    private fun generateOverloadWithSubstitutedParameters(
            functionDescriptor: FunctionDescriptor,
            delegateFunctionDescriptor: FunctionDescriptor,
            classBuilder: ClassBuilder,
            memberCodegen: MemberCodegen<*>,
            methodElement: KtPureElement?,
            contextKind: OwnerKind,
            substituteCount: Int
    ) {
        val typeMapper = state.typeMapper
        val isStatic = AsmUtil.isStaticMethod(contextKind, functionDescriptor)
        val baseMethodFlags = AsmUtil.getCommonCallableFlags(functionDescriptor, state) and Opcodes.ACC_VARARGS.inv()
        val remainingParameters = getRemainingParameters(functionDescriptor.original, substituteCount)
        val flags =
                baseMethodFlags or
                (if (isStatic) Opcodes.ACC_STATIC else 0) or
                (if (functionDescriptor.modality == Modality.FINAL && functionDescriptor !is ConstructorDescriptor) Opcodes.ACC_FINAL else 0) or
                (if (remainingParameters.lastOrNull()?.varargElementType != null) Opcodes.ACC_VARARGS else 0)
        val signature = typeMapper.mapSignatureWithCustomParameters(functionDescriptor, contextKind, remainingParameters, false)
        val mv = classBuilder.newMethod(OtherOriginFromPure(methodElement, functionDescriptor), flags,
                                        signature.asmMethod.name,
                                        signature.asmMethod.descriptor,
                                        signature.genericsSignature,
                                        FunctionCodegen.getThrownExceptions(functionDescriptor, typeMapper))

        AnnotationCodegen.forMethod(mv, memberCodegen, typeMapper).genAnnotations(functionDescriptor, signature.returnType)

        if (state.classBuilderMode == ClassBuilderMode.KAPT3) {
            mv.visitAnnotation(ANNOTATION_TYPE_DESCRIPTOR_FOR_JVMOVERLOADS_GENERATED_METHODS, /* visible = */ false)
        }

        FunctionCodegen.generateParameterAnnotations(functionDescriptor, mv, signature, remainingParameters, memberCodegen, state)

        if (!state.classBuilderMode.generateBodies) {
            FunctionCodegen.generateLocalVariablesForParameters(mv, signature, null, Label(), Label(), remainingParameters, isStatic, typeMapper)
            mv.visitEnd()
            return
        }

        val frameMap = FrameMap()
        val v = InstructionAdapter(mv)
        mv.visitCode()

        val methodBegin = Label()
        mv.visitLabel(methodBegin)

        val methodOwner = typeMapper.mapToCallableMethod(delegateFunctionDescriptor, false).owner
        if (!isStatic) {
            val thisIndex = frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            v.load(thisIndex, methodOwner) // Load this on stack

            if (functionDescriptor is ConstructorDescriptor) {
                val closure = state.bindingContext.get(CodegenBinding.CLOSURE, functionDescriptor.constructedClass)
                val captureThis = getDispatchReceiverParameterForConstructorCall(functionDescriptor, closure)
                if (captureThis != null) {
                    val outerIndex = frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
                    v.load(outerIndex, typeMapper.mapType(captureThis))
                }
            }
        }
        else {
            val delegateOwner = delegateFunctionDescriptor.containingDeclaration
            if (delegateOwner is ClassDescriptor && delegateOwner.isCompanionObject) {
                val singletonValue = StackValue.singleton(delegateOwner, typeMapper)
                singletonValue.put(singletonValue.type, v)
            }
        }

        val receiver = functionDescriptor.extensionReceiverParameter
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
        for (parameterDescriptor in functionDescriptor.valueParameters) {
            val paramType = typeMapper.mapType(parameterDescriptor.type)
            if (parameterDescriptor in remainingParameters) {
                val index = frameMap.getIndex(parameterDescriptor)
                StackValue.local(index, paramType).put(paramType, v)
            }
            else {
                AsmUtil.pushDefaultValueOnStack(paramType, v)
                val i = parameterDescriptor.index
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

        // for default constructors: just marks default constructor (see DEFAULT_CONSTRUCTOR_MARKER)
        // for default methods: contains MethodHandle for super calls ('null' cause not super call)
        v.aconst(null)

        val defaultMethod = typeMapper.mapDefaultMethod(delegateFunctionDescriptor, contextKind)
        if (functionDescriptor is ConstructorDescriptor) {
            v.invokespecial(methodOwner.internalName, defaultMethod.name, defaultMethod.descriptor, false)
        }
        else {
            v.invokestatic(methodOwner.internalName, defaultMethod.name, defaultMethod.descriptor, false)
        }
        v.areturn(signature.returnType)

        val methodEnd = Label()
        mv.visitLabel(methodEnd)

        FunctionCodegen.generateLocalVariablesForParameters(mv, signature, null, methodBegin, methodEnd,
                                                            remainingParameters, isStatic, typeMapper)

        FunctionCodegen.endVisit(mv, null, methodElement)
    }

    private fun getRemainingParameters(functionDescriptor: FunctionDescriptor,
                                       substituteCount: Int): List<ValueParameterDescriptor> {
        var remainingCount = functionDescriptor.countDefaultParameters() - substituteCount
        return functionDescriptor.valueParameters.filter { !it.declaresDefaultValue() || --remainingCount >= 0 }
    }

    private fun isEmptyConstructorNeeded(constructorDescriptor: ConstructorDescriptor, classOrObject: KtPureClassOrObject): Boolean {
        val classDescriptor = constructorDescriptor.constructedClass
        if (classDescriptor.kind != ClassKind.CLASS) return false

        if (classOrObject.isLocal) return false

        if (CodegenBinding.canHaveOuter(state.bindingContext, classDescriptor)) return false

        if (Visibilities.isPrivate(constructorDescriptor.visibility))
            return false

        if (constructorDescriptor.valueParameters.isEmpty()) return false
        if (classOrObject is KtClass && hasSecondaryConstructorsWithNoParameters(classOrObject)) return false

        return constructorDescriptor.valueParameters.all { it.declaresDefaultValue() }
    }

    private fun hasSecondaryConstructorsWithNoParameters(klass: KtClass) =
        klass.secondaryConstructors.any { it.valueParameters.isEmpty() }

}
