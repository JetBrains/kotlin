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

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.isInsideJvmMultifileClassFile
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.checkers.JvmFieldApplicabilityChecker.Problem.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.isInlineClassThatRequiresMangling
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class JvmFieldApplicabilityChecker : DeclarationChecker {

    internal enum class Problem(val errorMessage: String) {
        NOT_FINAL("JvmField can only be applied to final property"),
        PRIVATE("JvmField has no effect on a private property"),
        CUSTOM_ACCESSOR("JvmField cannot be applied to a property with a custom accessor"),
        OVERRIDES("JvmField cannot be applied to a property that overrides some other property"),
        LATEINIT("JvmField cannot be applied to lateinit property"),
        CONST("JvmField cannot be applied to const property"),
        INSIDE_COMPANION_OF_INTERFACE("JvmField cannot be applied to a property defined in companion object of interface"),
        NOT_PUBLIC_VAL_WITH_JVMFIELD("JvmField could be applied only if all interface companion properties are 'public final val' with '@JvmField' annotation"),
        TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE("JvmField cannot be applied to top level property of a file annotated with ${JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT}"),
        DELEGATE("JvmField cannot be applied to delegated property"),
        RETURN_TYPE_IS_INLINE_CLASS("JvmField cannot be applied to a property of an inline class type")
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (descriptor !is PropertyDescriptor || declaration !is KtProperty) return

        val annotation = descriptor.findJvmFieldAnnotation()
            ?: descriptor.delegateField?.annotations?.findAnnotation(JvmAbi.JVM_FIELD_ANNOTATION_FQ_NAME)
            ?: return

        val problem = when {
            declaration.hasDelegate() -> DELEGATE
            !descriptor.hasBackingField(context.trace.bindingContext) -> return
            descriptor.isOverridable -> NOT_FINAL
            Visibilities.isPrivate(descriptor.visibility) -> PRIVATE
            declaration.hasCustomAccessor() -> CUSTOM_ACCESSOR
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
            descriptor.returnType?.isInlineClassThatRequiresMangling() == true -> RETURN_TYPE_IS_INLINE_CLASS
            else -> return
        }

        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
        context.trace.report(ErrorsJvm.INAPPLICABLE_JVM_FIELD.on(annotationEntry, problem.errorMessage))
    }

    private fun isInterfaceCompanionWithPublicJvmFieldProperties(companionObject: ClassDescriptor): Boolean {
        for (next in companionObject.unsubstitutedMemberScope.getContributedDescriptors(
            DescriptorKindFilter.VARIABLES, MemberScope.ALL_NAME_FILTER
        )) {
            if (next !is PropertyDescriptor) continue

            if (next.visibility != Visibilities.PUBLIC || next.isVar || next.modality != Modality.FINAL) return false

            if (!JvmAbi.hasJvmFieldAnnotation(next)) return false
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

    private fun PropertyDescriptor.isInsideCompanionObjectOfInterface(): Boolean {
        val containingClass = containingDeclaration as? ClassDescriptor ?: return false
        if (!DescriptorUtils.isCompanionObject(containingClass)) return false

        val outerClassKind = (containingClass.containingDeclaration as? ClassDescriptor)?.kind
        return outerClassKind == ClassKind.INTERFACE || outerClassKind == ClassKind.ANNOTATION_CLASS
    }
}
