/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.DescriptorBasedFunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.generateBridges
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation

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
}

/**
 * @return return true for interface method not annotated with @JvmDefault
 */
fun isAbstractOnJvmIgnoringActualModality(jvmTarget: JvmTarget, descriptor: FunctionDescriptor): Boolean {
    if (!DescriptorUtils.isInterface(descriptor.containingDeclaration)) return false
    return jvmTarget == JvmTarget.JVM_1_6 || !descriptor.hasJvmDefaultAnnotation()
}

fun <Signature> generateBridgesForFunctionDescriptorForJvm(
    descriptor: FunctionDescriptor,
    signature: (FunctionDescriptor) -> Signature,
    jvmTarget: JvmTarget
): Set<Bridge<Signature>> {
    return generateBridges(DescriptorBasedFunctionHandleForJvm(descriptor, jvmTarget)) { signature(it.descriptor) }
}
