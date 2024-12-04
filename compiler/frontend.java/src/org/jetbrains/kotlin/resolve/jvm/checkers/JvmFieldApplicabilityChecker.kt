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

package org.jetbrains.kotlin.resolve.jvm.checkers

import org.jetbrains.kotlin.JvmFieldApplicabilityProblem.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.isInsideJvmMultifileClassFile
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.isValueClassThatRequiresMangling
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class JvmFieldApplicabilityChecker : DeclarationChecker {
    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor || (declaration !is KtProperty && declaration !is KtParameter)) return

        val annotation = descriptor.findJvmFieldAnnotation()
            ?: descriptor.delegateField?.annotations?.findAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
            ?: return

        val problem = when {
            declaration is KtProperty && declaration.hasDelegate() -> DELEGATE
            !descriptor.hasBackingField(context.trace.bindingContext) -> return
            descriptor.isOverridable -> NOT_FINAL
            DescriptorVisibilities.isPrivate(descriptor.visibility) -> PRIVATE
            declaration is KtProperty && declaration.hasCustomAccessor() -> CUSTOM_ACCESSOR
            descriptor.overriddenDescriptors.isNotEmpty() -> OVERRIDES
            descriptor.isLateInit -> LATEINIT
            descriptor.isConst -> CONST
            descriptor.isInsideCompanionObjectOfInterface() ->
                if (!context.languageVersionSettings.supportsFeature(LanguageFeature.JvmFieldInInterface))
                    INSIDE_COMPANION_OF_INTERFACE
                else {
                    if (!isInterfaceCompanionWithPublicJvmFieldProperties(descriptor.containingDeclaration as ClassDescriptor)) {
                        NOT_PUBLIC_VAL_WITH_JVMFIELD
                    } else return
                }
            DescriptorUtils.isTopLevelDeclaration(descriptor) && declaration.isInsideJvmMultifileClassFile() ->
                TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE
            descriptor.returnType?.isValueClassThatRequiresMangling() == true -> RETURN_TYPE_IS_VALUE_CLASS
            else -> return
        }

        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return

        val factory =
            if (declaration is KtParameter && !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor)) {
                ErrorsJvm.INAPPLICABLE_JVM_FIELD_WARNING
            } else {
                ErrorsJvm.INAPPLICABLE_JVM_FIELD
            }
        context.trace.report(factory.on(annotationEntry, problem.errorMessage))
    }

    private fun isInterfaceCompanionWithPublicJvmFieldProperties(companionObject: ClassDescriptor): Boolean {
        for (next in companionObject.unsubstitutedMemberScope.getContributedDescriptors(
            DescriptorKindFilter.VARIABLES, MemberScope.ALL_NAME_FILTER
        )) {
            if (next !is PropertyDescriptor) continue

            if (next.visibility != DescriptorVisibilities.PUBLIC || next.isVar || next.modality != Modality.FINAL) return false

            if (!DescriptorsJvmAbiUtil.hasJvmFieldAnnotation(next)) return false
        }
        return true
    }

    // This logic has been effectively used since 1.0. It'd be nice to call [PropertyAccessorDescriptor.isDefault] here instead,
    // but that would be a breaking change because in the following case:
    //
    //     @JvmField
    //     @get:Anno
    //     val foo = 42
    //
    // we'd start considering foo as having a custom getter and disallow the JvmField annotation on it
    private fun KtProperty.hasCustomAccessor(): Boolean =
        getter != null || setter != null

    private fun PropertyDescriptor.hasBackingField(bindingContext: BindingContext) =
        bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false
}
