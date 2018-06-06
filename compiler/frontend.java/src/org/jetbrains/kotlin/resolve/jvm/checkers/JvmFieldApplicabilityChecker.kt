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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.isInsideJvmMultifileClassFile
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.checkers.DeclarationChecker
import org.jetbrains.kotlin.resolve.checkers.DeclarationCheckerContext
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.checkers.JvmFieldApplicabilityChecker.Problem.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm

class JvmFieldApplicabilityChecker : DeclarationChecker {

    internal enum class Problem(val errorMessage: String) {
        NOT_FINAL("JvmField can only be applied to final property"),
        PRIVATE("JvmField has no effect on a private property"),
        CUSTOM_ACCESSOR("JvmField cannot be applied to a property with a custom accessor"),
        OVERRIDES("JvmField cannot be applied to a property that overrides some other property"),
        LATEINIT("JvmField cannot be applied to lateinit property"),
        CONST("JvmField cannot be applied to const property"),
        INSIDE_COMPANION_OF_INTERFACE("JvmField cannot be applied to a property defined in companion object of interface"),
        TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE("JvmField cannot be applied to top level property of a file annotated with ${JvmFileClassUtil.JVM_MULTIFILE_CLASS_SHORT}"),
        DELEGATE("JvmField cannot be applied to delegated property")
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        val annotation = descriptor.findJvmFieldAnnotation() ?: return

        val problem = when {
            descriptor !is PropertyDescriptor -> return
            declaration is KtProperty && declaration.hasDelegate() -> DELEGATE
            !descriptor.hasBackingField(context.trace.bindingContext) -> return
            descriptor.isOverridable -> NOT_FINAL
            Visibilities.isPrivate(descriptor.visibility) -> PRIVATE
            descriptor.hasCustomAccessor() -> CUSTOM_ACCESSOR
            descriptor.overriddenDescriptors.isNotEmpty() -> OVERRIDES
            descriptor.isLateInit -> LATEINIT
            descriptor.isConst -> CONST
            descriptor.isInsideCompanionObjectOfInterface() -> INSIDE_COMPANION_OF_INTERFACE
            DescriptorUtils.isTopLevelDeclaration(descriptor) && declaration.isInsideJvmMultifileClassFile() ->
                TOP_LEVEL_PROPERTY_OF_MULTIFILE_FACADE
            else -> return
        }

        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
        context.trace.report(ErrorsJvm.INAPPLICABLE_JVM_FIELD.on(annotationEntry, problem.errorMessage))
    }

    private fun PropertyDescriptor.hasCustomAccessor()
            = !(getter?.isDefault ?: true) || !(setter?.isDefault ?: true)

    private fun PropertyDescriptor.hasBackingField(bindingContext: BindingContext)
            = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, this) ?: false

    private fun PropertyDescriptor.isInsideCompanionObjectOfInterface(): Boolean {
        val containingClass = containingDeclaration as? ClassDescriptor ?: return false
        if (!DescriptorUtils.isCompanionObject(containingClass)) return false

        val outerClassKind = (containingClass.containingDeclaration as? ClassDescriptor)?.kind
        return outerClassKind == ClassKind.INTERFACE || outerClassKind == ClassKind.ANNOTATION_CLASS
    }
}
