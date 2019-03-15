/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.bridges.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasPlatformDependentAnnotation

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
    override val isAbstract: Boolean = super.isAbstract || isAbstractOnJvmIgnoringActualModality(descriptor)

    override val mayBeUsedAsSuperImplementation: Boolean =
        super.mayBeUsedAsSuperImplementation || descriptor.isJvmDefaultOrPlatformDependent()

    private val asmMethod by lazy(LazyThreadSafetyMode.NONE) {
        state.typeMapper.mapAsmMethod(descriptor)
    }

    override val isDeclaration: Boolean =
        descriptor.kind.isReal || needToGenerateDelegationToDefaultImpls(descriptor)

    override val mightBeIncorrectCode: Boolean
        get() = state.classBuilderMode.mightBeIncorrectCode

    override fun hashCode(): Int = descriptor.containerEntityForEqualityAndHashCode().hashCode() + 31 * asmMethod.hashCode()
    override fun equals(other: Any?): Boolean {
        if (other !is DescriptorBasedFunctionHandleForJvm) return false

        return asmMethod == other.asmMethod &&
                descriptor.containerEntityForEqualityAndHashCode() == other.descriptor.containerEntityForEqualityAndHashCode()
    }
}

private fun FunctionDescriptor.containerEntityForEqualityAndHashCode(): Any =
    (containingDeclaration as? ClassDescriptor)?.typeConstructor ?: containingDeclaration

private fun CallableMemberDescriptor.isJvmDefaultOrPlatformDependent() =
    hasJvmDefaultAnnotation() || hasPlatformDependentAnnotation()

private fun needToGenerateDelegationToDefaultImpls(descriptor: FunctionDescriptor): Boolean {
    if (findInterfaceImplementation(descriptor) == null) return false
    val overriddenFromInterface = findImplementationFromInterface(descriptor) ?: return false

    return !overriddenFromInterface.isJvmDefaultOrPlatformDependent()
}

/**
 * @return return true for interface method not annotated with @JvmDefault or @PlatformDependent
 */
fun isAbstractOnJvmIgnoringActualModality(descriptor: FunctionDescriptor): Boolean {
    if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return false

    return !descriptor.isJvmDefaultOrPlatformDependent()
}

fun <Signature> generateBridgesForFunctionDescriptorForJvm(
    descriptor: FunctionDescriptor,
    signature: (FunctionDescriptor) -> Signature,
    state: GenerationState
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandleForJvm(descriptor, state)) { signature(it.descriptor) }
}
