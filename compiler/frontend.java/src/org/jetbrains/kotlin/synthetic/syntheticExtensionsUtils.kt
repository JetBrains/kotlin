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

package org.jetbrains.kotlin.synthetic

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.sam.SamConstructorDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.tower.NewResolvedCallImpl
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

fun FunctionDescriptor.hasJavaOriginInHierarchy(): Boolean {
    return if (original.overriddenDescriptors.isEmpty())
        this is JavaCallableMemberDescriptor || containingDeclaration is JavaClassDescriptor
    else
        original.overriddenDescriptors.any { it.hasJavaOriginInHierarchy() }
}

fun Visibility.isVisibleOutside() = this != Visibilities.PRIVATE && this != Visibilities.PRIVATE_TO_THIS && this != Visibilities.INVISIBLE_FAKE

fun syntheticVisibility(originalDescriptor: DeclarationDescriptorWithVisibility, isUsedForExtension: Boolean): Visibility {
    val originalVisibility = originalDescriptor.visibility
    return when (originalVisibility) {
        Visibilities.PUBLIC -> Visibilities.PUBLIC

        else -> object : Visibility(originalVisibility.name, originalVisibility.isPublicAPI) {
            override fun isVisible(
                    receiver: ReceiverValue?,
                    what: DeclarationDescriptorWithVisibility,
                    from: DeclarationDescriptor
            ) = originalVisibility.isVisible(
                    if (isUsedForExtension) Visibilities.ALWAYS_SUITABLE_RECEIVER else receiver, originalDescriptor, from)

            override fun mustCheckInImports()
                    = throw UnsupportedOperationException("Should never be called for this visibility")

            override fun normalize()
                    = originalVisibility.normalize()

            override val displayName: String
                get() = originalVisibility.displayName + " for synthetic extension"
        }
    }

}

fun <D : CallableDescriptor> ResolvedCall<D>.isResolvedWithSamConversions(): Boolean {
    if (this is NewResolvedCallImpl<D> && resolvedCallAtom.argumentsWithConversion.isNotEmpty()) {
        return true
    }

    // Feature SamConversionPerArgument is disabled
    return this.resultingDescriptor is SamAdapterDescriptor<*> ||
            this.resultingDescriptor is SamConstructorDescriptor ||
            this.resultingDescriptor is SamAdapterExtensionFunctionDescriptor
}