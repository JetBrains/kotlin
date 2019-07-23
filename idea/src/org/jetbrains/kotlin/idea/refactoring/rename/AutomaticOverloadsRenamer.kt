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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.expectedDescriptor
import org.jetbrains.kotlin.idea.util.getAllAccessibleFunctions
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.UserDataProperty
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi

class AutomaticOverloadsRenamer(function: KtNamedFunction, newName: String) : AutomaticRenamer() {
    companion object {
        @get:TestOnly
        @set:TestOnly
        internal var PsiElement.elementFilter: ((PsiElement) -> Boolean)? by UserDataProperty(Key.create("ELEMENT_FILTER"))
    }

    init {
        val filter = function.elementFilter
        function.getOverloads().mapNotNullTo(myElements) {
            val candidate = it.source.getPsi() as? KtNamedFunction ?: return@mapNotNullTo null
            if (filter != null && !filter(candidate)) return@mapNotNullTo null
            if (candidate != function) candidate else null
        }
        suggestAllNames(function.name, newName)
    }

    override fun getDialogTitle() = "Rename Overloads"
    override fun getDialogDescription() = "Rename overloads to:"
    override fun entityName() = "Overload"
    override fun isSelectedByDefault(): Boolean = true
}

private fun KtNamedFunction.getOverloads(): Collection<FunctionDescriptor> {
    val name = nameAsName ?: return emptyList()
    val resolutionFacade = getResolutionFacade()
    val descriptor = this.unsafeResolveToDescriptor() as FunctionDescriptor
    val context = resolutionFacade.analyze(this, BodyResolveMode.FULL)
    val scope = getResolutionScope(context, resolutionFacade)
    val extensionReceiverClass = descriptor.extensionReceiverParameter?.type?.constructor?.declarationDescriptor as? ClassDescriptor

    if (descriptor.isActual && descriptor.expectedDescriptor() != null) return emptyList()

    val result = LinkedHashSet<FunctionDescriptor>()
    result += scope.getAllAccessibleFunctions(name)
    if (extensionReceiverClass != null) {
        result += extensionReceiverClass.unsubstitutedMemberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
    }

    return result
}

class AutomaticOverloadsRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        if (element !is KtNamedFunction) return false
        if (element.isLocal) return false
        return element.getOverloads().size > 1
    }

    override fun getOptionName() = RefactoringBundle.message("rename.overloads")

    override fun isEnabled() = KotlinRefactoringSettings.instance.renameOverloads

    override fun setEnabled(enabled: Boolean) {
        KotlinRefactoringSettings.instance.renameOverloads = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>)
            = AutomaticOverloadsRenamer(element as KtNamedFunction, newName)
}