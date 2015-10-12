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

import org.jetbrains.kotlin.backend.common.bridges.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.getBuiltinSpecialOverridden
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class BridgeForBuiltinSpecial<Signature>(
        val from: Signature, val to: Signature,
        val isSpecial: Boolean = false,
        val isDelegateToSuper: Boolean = false
)

object BuiltinSpecialBridgesUtil {
    @JvmStatic
    public fun <Signature> generateBridgesForBuiltinSpecial(
            function: FunctionDescriptor,
            signatureByDescriptor: (FunctionDescriptor) -> Signature
    ): Set<BridgeForBuiltinSpecial<Signature>> {

        val functionHandle = DescriptorBasedFunctionHandle(function)
        val fake = !functionHandle.isDeclaration
        val overriddenBuiltin = function.getBuiltinSpecialOverridden()!!

        val reachableDeclarations = findAllReachableDeclarations(function)
        val needGenerateSpecialBridge = needGenerateSpecialBridge(function, reachableDeclarations, overriddenBuiltin)

        // e.g. `getSize()I`
        val methodItself = signatureByDescriptor(function)
        // e.g. `size()I`
        val overriddenBuiltinSignature = signatureByDescriptor(overriddenBuiltin)

        val specialBridge = if (needGenerateSpecialBridge)
            BridgeForBuiltinSpecial(overriddenBuiltinSignature, methodItself, isSpecial = true)
        else null

        val bridgesToGenerate = reachableDeclarations.mapTo(LinkedHashSet<Signature>(), signatureByDescriptor)
        bridgesToGenerate.remove(overriddenBuiltinSignature)
        bridgesToGenerate.remove(methodItself)

        if (fake) {
            for (overridden in function.overriddenDescriptors.map { it.original }) {
                if (!DescriptorBasedFunctionHandle(overridden).isAbstract) {
                    bridgesToGenerate.removeAll(findAllReachableDeclarations(overridden).map(signatureByDescriptor))
                }
            }
        }

        val bridges: MutableSet<BridgeForBuiltinSpecial<Signature>> =
                (bridgesToGenerate.map { BridgeForBuiltinSpecial(it, methodItself) } + specialBridge.singletonOrEmptyList()).toMutableSet()

        if (function.modality == Modality.OPEN && fake) {
            val implementation = findConcreteSuperDeclaration(DescriptorBasedFunctionHandle(function)).descriptor
            if (!DescriptorUtils.isInterface(implementation.containingDeclaration)) {
                bridges.add(BridgeForBuiltinSpecial(methodItself, signatureByDescriptor(implementation), isDelegateToSuper = true))
            }
        }

        return bridges
    }
}

private fun findAllReachableDeclarations(functionDescriptor: FunctionDescriptor): MutableSet<FunctionDescriptor> =
        findAllReachableDeclarations(DescriptorBasedFunctionHandle(functionDescriptor)).map { it.descriptor }.toMutableSet()

private fun needGenerateSpecialBridge(
        functionDescriptor: FunctionDescriptor,
        reachableDeclarations: Collection<FunctionDescriptor>,
        specialCallableDescriptor: CallableDescriptor
): Boolean {
    val classDescriptor = functionDescriptor.containingDeclaration as ClassDescriptor
    val builtinContainerDefaultType = (specialCallableDescriptor.containingDeclaration as ClassDescriptor).defaultType

    var superClassDescriptor = DescriptorUtils.getSuperClassDescriptor(classDescriptor)

    while (superClassDescriptor != null) {
        val implementsBuiltinDeclaration =
                TypeCheckingProcedure.findCorrespondingSupertype(superClassDescriptor.defaultType, builtinContainerDefaultType) != null

        if (superClassDescriptor !is JavaClassDescriptor) {
            // Kotlin class
            // ?
            if (implementsBuiltinDeclaration) return false
        }
        else {
            // java super class inherits builtin class and it's declaration is final
            if (implementsBuiltinDeclaration
                && reachableDeclarations.any { it.containingDeclaration == superClassDescriptor && it.modality == Modality.FINAL }) {
                return false
            }
        }

        superClassDescriptor = DescriptorUtils.getSuperClassDescriptor(superClassDescriptor as ClassDescriptor)
    }

    return true
}
