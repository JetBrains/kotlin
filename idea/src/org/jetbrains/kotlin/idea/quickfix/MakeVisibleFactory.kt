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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object MakeVisibleFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val element = diagnostic.psiElement as? KtElement ?: return emptyList()
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val usageModule = context.get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, element.containingKtFile)?.module

        @Suppress("UNCHECKED_CAST")
        val factory = diagnostic.factory as DiagnosticFactory3<*, DeclarationDescriptor, *, DeclarationDescriptor>
        val descriptor = factory.cast(diagnostic).c as? DeclarationDescriptorWithVisibility ?: return emptyList()
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? KtModifierListOwner ?: return emptyList()

        val module = DescriptorUtils.getContainingModule(descriptor)
        val targetVisibilities = when (descriptor.visibility) {
            PRIVATE, INVISIBLE_FAKE -> mutableListOf(PUBLIC).apply {
                if (module == usageModule) add(INTERNAL)
                val superClasses = (element.containingClass()?.descriptor as? ClassDescriptor)?.getAllSuperclassesWithoutAny()
                if (superClasses?.contains(declaration.containingClass()?.descriptor) == true) add(PROTECTED)
            }
            else -> listOf(PUBLIC)
        }

        return targetVisibilities.mapNotNull { ChangeVisibilityFix.create(declaration, descriptor, it) }
    }
}