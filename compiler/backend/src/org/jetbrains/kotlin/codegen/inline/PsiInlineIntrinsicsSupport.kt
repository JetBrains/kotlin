/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.INT_TYPE
import org.jetbrains.org.objectweb.asm.Type.VOID_TYPE
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class PsiInlineIntrinsicsSupport(private val state: GenerationState) : ReifiedTypeInliner.IntrinsicsSupport<KotlinType> {
    override fun putClassInstance(v: InstructionAdapter, type: KotlinType) {
        DescriptorAsmUtil.putJavaLangClassInstance(v, state.typeMapper.mapType(type), type, state.typeMapper)
    }

    override fun generateTypeParameterContainer(v: InstructionAdapter, typeParameter: TypeParameterMarker) {
        require(typeParameter is TypeParameterDescriptor)

        when (val container = typeParameter.containingDeclaration) {
            is ClassDescriptor -> putClassInstance(v, container.defaultType).also { AsmUtil.wrapJavaClassIntoKClass(v) }
            is FunctionDescriptor -> generateFunctionReference(v, container)
            is PropertyDescriptor -> MemberCodegen.generatePropertyReference(v, container, state)
            else -> error("Unknown container of type parameter: $container (${typeParameter.name})")
        }
    }

    private fun generateFunctionReference(v: InstructionAdapter, descriptor: FunctionDescriptor) {
        check(state.generateOptimizedCallableReferenceSuperClasses) {
            "typeOf() of a non-reified type parameter is only allowed if optimized callable references are enabled.\n" +
                    "Please make sure API version is set to 1.4, and -Xno-optimized-callable-references is NOT used.\n" +
                    "Container: $descriptor"
        }

        v.anew(FUNCTION_REFERENCE_IMPL)
        v.dup()
        v.aconst(descriptor.arity)
        generateCallableReferenceDeclarationContainerClass(v, descriptor, state)
        v.aconst(descriptor.name.asString())
        generateFunctionReferenceSignature(v, descriptor, state)
        v.aconst(getCallableReferenceTopLevelFlag(descriptor))
        v.invokespecial(
            FUNCTION_REFERENCE_IMPL.internalName, "<init>",
            Type.getMethodDescriptor(VOID_TYPE, INT_TYPE, JAVA_CLASS_TYPE, JAVA_STRING_TYPE, JAVA_STRING_TYPE, INT_TYPE),
            false
        )
    }

    override fun toKotlinType(type: KotlinType): KotlinType = type
}
