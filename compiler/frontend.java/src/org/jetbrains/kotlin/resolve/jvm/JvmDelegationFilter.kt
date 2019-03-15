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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmDefaultAnnotation
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter

object JvmDelegationFilter : DelegationFilter {

    override fun filter(interfaceMember: CallableMemberDescriptor, languageVersionSettings: LanguageVersionSettings): Boolean {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NoDelegationToJavaDefaultInterfaceMembers)) return true

        //We always have only one implementation otherwise it's an error in kotlin and java
        val realMember = DescriptorUtils.unwrapFakeOverride(interfaceMember)
        return !isJavaDefaultMethod(realMember) &&
                !realMember.hasJvmDefaultAnnotation() &&
                !isBuiltInMemberMappedToJavaDefault(realMember)
    }

    private fun isJavaDefaultMethod(interfaceMember: CallableMemberDescriptor): Boolean {
        return interfaceMember is JavaMethodDescriptor && interfaceMember.modality != Modality.ABSTRACT
    }

    private fun isBuiltInMemberMappedToJavaDefault(interfaceMember: CallableMemberDescriptor): Boolean {
        return interfaceMember.modality != Modality.ABSTRACT &&
               KotlinBuiltIns.isBuiltIn(interfaceMember) &&
               interfaceMember.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)
    }
}
