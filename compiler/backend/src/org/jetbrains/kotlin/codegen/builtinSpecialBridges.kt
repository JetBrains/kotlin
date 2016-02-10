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

import org.jetbrains.kotlin.backend.common.bridges.DescriptorBasedFunctionHandle
import org.jetbrains.kotlin.backend.common.bridges.findAllReachableDeclarations
import org.jetbrains.kotlin.backend.common.bridges.findConcreteSuperDeclaration
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature.getSpecialSignatureInfo
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.firstOverridden
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class BridgeForBuiltinSpecial<Signature>(
        val from: Signature, val to: Signature,
        val isSpecial: Boolean = false,
        val isDelegateToSuper: Boolean = false
)

object BuiltinSpecialBridgesUtil {
    @JvmStatic fun <Signature> generateBridgesForBuiltinSpecial(
            function: FunctionDescriptor,
            signatureByDescriptor: (FunctionDescriptor) -> Signature
    ): Set<BridgeForBuiltinSpecial<Signature>> {

        val functionHandle = DescriptorBasedFunctionHandle(function)
        val fake = !functionHandle.isDeclaration
        val overriddenBuiltin = function.getOverriddenBuiltinReflectingJvmDescriptor()!!

        val reachableDeclarations = findAllReachableDeclarations(function)

        // e.g. `getSize()I`
        val methodItself = signatureByDescriptor(function)
        // e.g. `size()I`
        val specialBridgeSignature = signatureByDescriptor(overriddenBuiltin)

        val needGenerateSpecialBridge = needGenerateSpecialBridge(
                function, reachableDeclarations, signatureByDescriptor, specialBridgeSignature)

        val specialBridge = if (needGenerateSpecialBridge)
            BridgeForBuiltinSpecial(specialBridgeSignature, methodItself, isSpecial = true)
        else null

        val commonBridges = reachableDeclarations.mapTo(LinkedHashSet<Signature>(), signatureByDescriptor)
        commonBridges.remove(specialBridgeSignature)

        val superImplementationDescriptor = findSuperImplementationForStubDelegation(function, fake)
        if (superImplementationDescriptor != null || !fake) {
            commonBridges.remove(methodItself)
        }

        if (fake) {
            for (overridden in function.overriddenDescriptors.map { it.original }) {
                if (!DescriptorBasedFunctionHandle(overridden).isAbstract) {
                    commonBridges.removeAll(findAllReachableDeclarations(overridden).map(signatureByDescriptor))
                }
            }
        }

        val bridges: MutableSet<BridgeForBuiltinSpecial<Signature>> =
                (commonBridges.map { BridgeForBuiltinSpecial(it, specialBridgeSignature) } + specialBridge.singletonOrEmptyList()).toMutableSet()

        if (superImplementationDescriptor != null) {
            bridges.add(BridgeForBuiltinSpecial(methodItself, signatureByDescriptor(superImplementationDescriptor), isDelegateToSuper = true))
        }

        return bridges
    }

    @JvmStatic fun <Signature> FunctionDescriptor.shouldHaveTypeSafeBarrier(
            signatureByDescriptor: (FunctionDescriptor) -> Signature
    ): Boolean {
        if (BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(this) == null) return false

        val builtin = getOverriddenBuiltinReflectingJvmDescriptor()!!
        return signatureByDescriptor(this) == signatureByDescriptor(builtin)
    }
}


private fun findSuperImplementationForStubDelegation(function: FunctionDescriptor, fake: Boolean): FunctionDescriptor? {
    if (function.modality != Modality.OPEN || !fake) return null
    val implementation = findConcreteSuperDeclaration(DescriptorBasedFunctionHandle(function)).descriptor
    if (DescriptorUtils.isInterface(implementation.containingDeclaration)) return null

    return implementation
}

private fun findAllReachableDeclarations(functionDescriptor: FunctionDescriptor): MutableSet<FunctionDescriptor> =
        findAllReachableDeclarations(DescriptorBasedFunctionHandle(functionDescriptor)).map { it.descriptor }.toMutableSet()

private fun <Signature> needGenerateSpecialBridge(
        functionDescriptor: FunctionDescriptor,
        reachableDeclarations: Collection<FunctionDescriptor>,
        signatureByDescriptor: (FunctionDescriptor) -> Signature,
        overriddenBuiltinSignature: Signature
): Boolean {
    // We do not generate special bridge unless it has different JVM descriptor
    // e.g. `containsAll(Collection<E> c)` in ListImpl<E> has the same signature as `containsAll(Collection<?> c)`
    // or `contains(E e)` has the same signature as `contains(Object e)`.
    // While `contains(String e)` in StringList : List<String> has different JVM descriptor from `contains(Object e)`
    // and there should be special bridge in latter case.
    if (signatureByDescriptor(functionDescriptor) == overriddenBuiltinSignature) return false

    // Is there Kotlin superclass that already has generated special bridge
    if (functionDescriptor.firstOverridden {
        overridden ->
        overridden !== functionDescriptor && overridden.getSpecialBridgeSignatureIfExists(signatureByDescriptor) != null
    } != null) return false

    return reachableDeclarations.none { it.modality == Modality.FINAL
                                        && signatureByDescriptor(it) == overriddenBuiltinSignature }
}

private fun <Signature> CallableMemberDescriptor.getSpecialBridgeSignatureIfExists(
        signatureByDescriptor: (FunctionDescriptor) -> Signature
): Signature? {
    // Ignore itself and non-functions (may be assertion)
    if (this !is FunctionDescriptor) return null

    // Only Kotlin classes can have special bridges
    if (containingDeclaration is JavaClassDescriptor || DescriptorUtils.isInterface(containingDeclaration)) return null

    // Getting original is necessary here, because we want to determine JVM signature of descriptor as it was declared in containing class
    val originalOverridden = original
    val overriddenSpecial = originalOverridden.getOverriddenBuiltinReflectingJvmDescriptor()?.original ?: return null
    val specialBridgeSignature = signatureByDescriptor(overriddenSpecial)

    // Does special bridge has different signature
    if (signatureByDescriptor(originalOverridden) == specialBridgeSignature) return null

    return specialBridgeSignature
}

fun isValueArgumentForCallToMethodWithTypeCheckBarrier(
        element: KtElement,
        bindingContext: BindingContext
): Boolean {

    val parentCall = element.getParentCall(bindingContext, strict = true) ?: return false
    val argumentExpression = parentCall.valueArguments.singleOrNull()?.getArgumentExpression() ?: return false
    if (KtPsiUtil.deparenthesize(argumentExpression) !== element) return false

    val candidateDescriptor = parentCall.getResolvedCall(bindingContext)?.candidateDescriptor as CallableMemberDescriptor?
                              ?: return false

    return candidateDescriptor.getSpecialSignatureInfo()?.isObjectReplacedWithTypeParameter ?: false
}
