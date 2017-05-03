/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridesBackwardCompatibilityHelper
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.serialization.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME

object JvmOverridesBackwardCompatibilityHelper : OverridesBackwardCompatibilityHelper {
    override fun overrideCanBeOmitted(overridingDescriptor: CallableMemberDescriptor): Boolean {
        val visitedDescriptors = hashSetOf<CallableMemberDescriptor>()
        return overridingDescriptor.overriddenDescriptors.all {
            isPlatformSpecificDescriptorThatCanBeImplicitlyOverridden(it, visitedDescriptors)
        }
    }

    private fun isPlatformSpecificDescriptorThatCanBeImplicitlyOverridden(
            overriddenDescriptor: CallableMemberDescriptor,
            visitedDescriptors: MutableSet<CallableMemberDescriptor>
    ): Boolean {
        if (overriddenDescriptor.modality == Modality.FINAL) return false

        if (visitedDescriptors.contains(overriddenDescriptor.original)) return true
        visitedDescriptors.add(overriddenDescriptor.original)

        when (overriddenDescriptor.kind) {
            CallableMemberDescriptor.Kind.DELEGATION,
            CallableMemberDescriptor.Kind.FAKE_OVERRIDE ->
                return isOverridingOnlyDescriptorsThatCanBeImplicitlyOverridden(overriddenDescriptor, visitedDescriptors)

            CallableMemberDescriptor.Kind.DECLARATION -> {
                when {
                    overriddenDescriptor.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME) ->
                        return true
                    overriddenDescriptor is JavaMethodDescriptor -> {
                        val containingClass = DescriptorUtils.getContainingClass(overriddenDescriptor)
                                              ?: return false

                        if (JavaToKotlinClassMap.mapKotlinToJava(containingClass.fqNameUnsafe) != null) return true
                        if (overriddenDescriptor.overriddenDescriptors.isEmpty()) return false

                        return isOverridingOnlyDescriptorsThatCanBeImplicitlyOverridden(overriddenDescriptor, visitedDescriptors)
                    }
                    else ->
                        return false
                }

            }

            else ->
                return false
        }
    }

    private fun isOverridingOnlyDescriptorsThatCanBeImplicitlyOverridden(
            overriddenDescriptor: CallableMemberDescriptor,
            visitedDescriptors: MutableSet<CallableMemberDescriptor>
    ): Boolean =
            overriddenDescriptor.overriddenDescriptors.all {
                isPlatformSpecificDescriptorThatCanBeImplicitlyOverridden(it, visitedDescriptors)
            }

}