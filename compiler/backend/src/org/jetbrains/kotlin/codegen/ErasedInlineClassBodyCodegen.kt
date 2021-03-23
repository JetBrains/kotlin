/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.genTotalOrderEqualsForExpressionOnStack
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.JVM_INLINE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.Synthetic
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type

class ErasedInlineClassBodyCodegen(
    aClass: KtClass,
    context: ClassContext,
    v: ClassBuilder,
    state: GenerationState,
    parentCodegen: MemberCodegen<*>?
) : ClassBodyCodegen(aClass, context, v, state, parentCodegen) {

    private val classAsmType = typeMapper.mapClass(descriptor)

    private val constructorCodegen = ConstructorCodegen(
        descriptor, context, functionCodegen, this, this, state, kind, v, classAsmType, aClass, bindingContext
    )

    override fun generateDeclaration() {}
    override fun generateKotlinMetadataAnnotation() {}
    override fun done() {}

    override fun generateConstructors() {
        val delegationFieldsInfo = DelegationFieldsInfo(classAsmType, descriptor, state, bindingContext)

        constructorCodegen.generatePrimaryConstructor(delegationFieldsInfo, AsmTypes.OBJECT_TYPE)

        for (secondaryConstructor in descriptor.secondaryConstructors) {
            constructorCodegen.generateSecondaryConstructor(secondaryConstructor, AsmTypes.OBJECT_TYPE)
        }
    }

    override fun generateSyntheticPartsAfterBody() {
        super.generateSyntheticPartsAfterBody()

        generateDelegatesToDefaultImpl()
        generateUnboxMethod()
        generateFunctionsFromAny()
        generateSpecializedEqualsStub()
        generateJvmInlineAnnotation()
    }

    private fun generateJvmInlineAnnotation() {
        if (descriptor.isInline) {
            v.newAnnotation(JVM_INLINE_ANNOTATION_FQ_NAME.topLevelClassAsmType().descriptor, true).visitEnd()
        }
    }

    private fun generateFunctionsFromAny() {
        FunctionsFromAnyGeneratorImpl(
            myClass as KtClassOrObject, bindingContext, descriptor, classAsmType, context, v, state
        ).generate()
    }

    private fun generateUnboxMethod() {
        val boxMethodDescriptor = InlineClassDescriptorResolver.createBoxFunctionDescriptor(descriptor)

        functionCodegen.generateMethod(
            Synthetic(null, boxMethodDescriptor), boxMethodDescriptor, object : FunctionGenerationStrategy.CodegenBased(state) {
                override fun mapMethodSignature(
                    functionDescriptor: FunctionDescriptor,
                    typeMapper: KotlinTypeMapper,
                    contextKind: OwnerKind,
                    hasSpecialBridge: Boolean
                ): JvmMethodGenericSignature {
                    // note that in signatures of functions inside erased inline class only underlying types are using,
                    // but return type of `box` function should be a wrapper type, therefore we handle this situation here differently
                    return typeMapper.mapSignatureForBoxMethodOfInlineClass(functionDescriptor)
                }

                override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                    val iv = codegen.v
                    val wrapperType = signature.returnType
                    val baseValueType = signature.valueParameters.single().asmType
                    val constructor = typeMapper.mapToCallableMethod(descriptor.unsubstitutedPrimaryConstructor!!, false)
                    iv.anew(wrapperType)
                    iv.dup()
                    iv.load(0, baseValueType)
                    constructor.genInvokeInstruction(iv)
                    iv.areturn(wrapperType)
                }
            }
        )
    }

    private fun generateSpecializedEqualsStub() {
        val specializedEqualsDescriptor = InlineClassDescriptorResolver.createSpecializedEqualsDescriptor(descriptor)

        functionCodegen.generateMethod(
            Synthetic(null, specializedEqualsDescriptor), specializedEqualsDescriptor, object : FunctionGenerationStrategy.CodegenBased(state) {
                override fun mapMethodSignature(
                    functionDescriptor: FunctionDescriptor,
                    typeMapper: KotlinTypeMapper,
                    contextKind: OwnerKind,
                    hasSpecialBridge: Boolean
                ): JvmMethodGenericSignature {
                    // we shouldn't use default mapping here to avoid adding parameter that relates to carrier type
                    return typeMapper.mapSignatureForSpecializedEqualsOfInlineClass(functionDescriptor)
                }


                override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                    val firstIndex = codegen.frameMap.getIndex(specializedEqualsDescriptor.valueParameters[0])
                    val secondIndex = codegen.frameMap.getIndex(specializedEqualsDescriptor.valueParameters[1])
                    val asmType = signature.valueParameters[0].asmType
                    val left = StackValue.local(firstIndex, asmType)
                    val right = StackValue.local(secondIndex, asmType)
                    genTotalOrderEqualsForExpressionOnStack(left, right, asmType).put(Type.BOOLEAN_TYPE, codegen.v)
                    codegen.v.areturn(Type.BOOLEAN_TYPE)
                }

                override fun skipNotNullAssertionsForParameters(): Boolean = true
            }
        )
    }
}
