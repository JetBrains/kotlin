/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.bridges.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaForKotlinOverridePropertyDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.isCompiledToJvmDefault
import org.jetbrains.kotlin.resolve.jvm.annotations.hasPlatformDependentAnnotation
import org.jetbrains.kotlin.util.findImplementationFromInterface
import org.jetbrains.kotlin.util.findInterfaceImplementation

class DescriptorBasedFunctionHandleForJvm(
    descriptor: FunctionDescriptor,
    private val state: GenerationState
) : DescriptorBasedFunctionHandle(descriptor) {
    override fun createHandleForOverridden(overridden: FunctionDescriptor) =
        DescriptorBasedFunctionHandleForJvm(overridden, state)

    /*
        For @JvmDefault JVM members they are placed in interface classes and
        we need generate bridge for such function ('isAbstract' will return false).
        For non-@JvmDefault interfaces function, its body is generated in a separate place (DefaultImpls) and
        the method in the interface is abstract so we must not generate bridges for such cases.
    */
    override val isAbstract: Boolean = super.isAbstract || isAbstractOnJvmIgnoringActualModality(descriptor, state.jvmDefaultMode)

    override val mayBeUsedAsSuperImplementation: Boolean =
        super.mayBeUsedAsSuperImplementation || descriptor.isJvmDefaultOrPlatformDependent(state.jvmDefaultMode)

    private val asmMethod by lazy(LazyThreadSafetyMode.NONE) {
        state.typeMapper.mapAsmMethod(descriptor)
    }

    override val isDeclaration: Boolean =
        descriptor.kind.isReal || needToGenerateDelegationToDefaultImpls(descriptor, state.jvmDefaultMode)

    override val mightBeIncorrectCode: Boolean
        get() = state.classBuilderMode.mightBeIncorrectCode

    override fun hashCode(): Int =
        (descriptor.containerEntityForEqualityAndHashCode().hashCode() * 31 +
                descriptor.isJavaForKotlinOverrideProperty.hashCode()) * 31 +
                asmMethod.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is DescriptorBasedFunctionHandleForJvm &&
                asmMethod == other.asmMethod &&
                descriptor.containerEntityForEqualityAndHashCode() == other.descriptor.containerEntityForEqualityAndHashCode() &&
                descriptor.isJavaForKotlinOverrideProperty == other.descriptor.isJavaForKotlinOverrideProperty
    }
}

private fun FunctionDescriptor.containerEntityForEqualityAndHashCode(): Any =
    (containingDeclaration as? ClassDescriptor)?.typeConstructor ?: containingDeclaration

private val FunctionDescriptor.isJavaForKotlinOverrideProperty: Boolean
    get() = this is PropertyAccessorDescriptor && correspondingProperty is JavaForKotlinOverridePropertyDescriptor

private fun CallableMemberDescriptor.isJvmDefaultOrPlatformDependent(jvmDefaultMode: JvmDefaultMode) =
    isCompiledToJvmDefault(jvmDefaultMode) || hasPlatformDependentAnnotation()

private fun needToGenerateDelegationToDefaultImpls(descriptor: FunctionDescriptor, jvmDefaultMode: JvmDefaultMode): Boolean {
    if (findInterfaceImplementation(descriptor) == null) return false
    val overriddenFromInterface = findImplementationFromInterface(descriptor) ?: return false

    return !overriddenFromInterface.isJvmDefaultOrPlatformDependent(jvmDefaultMode)
}

/**
 * @return return true for interface method not annotated with @JvmDefault or @PlatformDependent
 */
fun isAbstractOnJvmIgnoringActualModality(descriptor: FunctionDescriptor, jvmDefaultMode: JvmDefaultMode): Boolean {
    if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return false

    return !descriptor.isJvmDefaultOrPlatformDependent(jvmDefaultMode)
}

fun <Signature> generateBridgesForFunctionDescriptorForJvm(
    descriptor: FunctionDescriptor,
    signature: (FunctionDescriptor) -> Signature,
    state: GenerationState
): Set<Bridge<Signature, DescriptorBasedFunctionHandleForJvm>> {
    return generateBridges(DescriptorBasedFunctionHandleForJvm(descriptor, state)) { signature(it.descriptor) }
}
