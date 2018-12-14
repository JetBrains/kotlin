/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.bridges.*
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.hasPlatformDependentAnnotation

class DescriptorBasedFunctionHandleForJvm(
    descriptor: FunctionDescriptor,
    private val jvmTarget: JvmTarget
) : DescriptorBasedFunctionHandle(descriptor) {
    override fun createHandleForOverridden(overridden: FunctionDescriptor) =
        DescriptorBasedFunctionHandleForJvm(overridden, jvmTarget)

    /*
        For @JvmDefault JVM members they are placed in interface classes and
        we need generate bridge for such function ('isAbstract' will return false).
        For non-@JvmDefault interfaces function, its body is generated in a separate place (DefaultImpls) and
        the method in the interface is abstract so we must not generate bridges for such cases.
    */
    override val isAbstract: Boolean = super.isAbstract || isAbstractOnJvmIgnoringActualModality(jvmTarget, descriptor)

    override val mayBeUsedAsSuperImplementation: Boolean =
        super.mayBeUsedAsSuperImplementation || descriptor.isJvmDefaultOrPlatformDependent()

    override val isDeclaration: Boolean =
        descriptor.kind.isReal || needToGenerateDelegationToDefaultImpls(descriptor)
}

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
fun isAbstractOnJvmIgnoringActualModality(jvmTarget: JvmTarget, descriptor: FunctionDescriptor): Boolean {
    if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return false
    if (jvmTarget == JvmTarget.JVM_1_6) return true

    return !descriptor.isJvmDefaultOrPlatformDependent()
}

fun <Signature> generateBridgesForFunctionDescriptorForJvm(
    descriptor: FunctionDescriptor,
    signature: (FunctionDescriptor) -> Signature,
    jvmTarget: JvmTarget
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandleForJvm(descriptor, jvmTarget)) { signature(it.descriptor) }
}
