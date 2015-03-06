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

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.resolve.BindingContext
import com.intellij.psi.PsiElement
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED
import org.jetbrains.kotlin.resolve.OverrideResolver
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import java.util.Collections
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.ide.IdeBundle
import com.intellij.openapi.ui.Messages
import com.intellij.CommonBundle
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import java.util.HashSet
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.psi.JetDeclarationWithBody
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetDeclaration

public abstract class CallableRefactoring<T: CallableDescriptor>(
        val project: Project,
        val callableDescriptor: T,
        val bindingContext: BindingContext,
        val commandName: String?) {
    private val LOG = Logger.getInstance(javaClass<CallableRefactoring<*>>())

    private val kind = (callableDescriptor as? CallableMemberDescriptor)?.getKind() ?: CallableMemberDescriptor.Kind.DECLARATION

    private fun getClosestModifiableDescriptors(): Set<CallableDescriptor> {
        return when (kind) {
            DECLARATION -> {
                Collections.singleton(callableDescriptor)
            }
            DELEGATION, FAKE_OVERRIDE -> {
                OverrideResolver.getDirectlyOverriddenDeclarations(callableDescriptor as CallableMemberDescriptor)
            }
            else -> {
                throw IllegalStateException("Unexpected callable kind: ${kind}")
            }
        }
    }

    private fun showSuperFunctionWarningDialog(superCallables: Collection<CallableDescriptor>,
                                               callableFromEditor: CallableDescriptor,
                                               options: List<String>): Int {
        val superString = superCallables.map {
            it.getContainingDeclaration().getName().asString()
        }.joinToString(prefix = "\n    ", separator = ",\n    ", postfix = ".\n\n")
        val message = JetBundle.message("x.overrides.y.in.class.list",
                                        DescriptorRenderer.COMPACT.render(callableFromEditor),
                                        callableFromEditor.getContainingDeclaration().getName().asString(), superString,
                                        "refactor")
        val title = IdeBundle.message("title.warning")!!
        val icon = Messages.getQuestionIcon()!!
        return Messages.showDialog(message, title, options.copyToArray(), 0, icon)
    }

    protected fun checkModifiable(element: PsiElement): Boolean {
        if (QuickFixUtil.canModifyElement(element)) {
            return true
        }

        val unmodifiableFile = element.getContainingFile()?.getVirtualFile()?.getPresentableUrl()
        if (unmodifiableFile != null) {
            val message = RefactoringBundle.message("refactoring.cannot.be.performed") + "\n" +
                          IdeBundle.message("error.message.cannot.modify.file.0", unmodifiableFile)
            Messages.showErrorDialog(project, message, CommonBundle.getErrorTitle()!!)
        }
        else {
            LOG.error("Could not find file for Psi element: " + element.getText())
        }

        return false
    }

    protected abstract fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>)

    public fun run() {
        fun buttonPressed(code: Int, dialogButtons: List<String>, button: String): Boolean {
            return code == dialogButtons indexOf button && button in dialogButtons
        }

        fun performForWholeHierarchy(dialogButtons: List<String>, code: Int): Boolean {
            return buttonPressed(code, dialogButtons, Messages.YES_BUTTON) || buttonPressed(code, dialogButtons, Messages.OK_BUTTON)
        }

        fun performForSelectedFunctionOnly(dialogButtons: List<String>, code: Int): Boolean {
            return buttonPressed(code, dialogButtons, Messages.NO_BUTTON)
        }

        fun buildDialogOptions(isSingleFunctionSelected: Boolean): List<String> {
            if (isSingleFunctionSelected) {
                return arrayListOf(Messages.YES_BUTTON, Messages.NO_BUTTON, Messages.CANCEL_BUTTON)
            }
            else {
                return arrayListOf(Messages.OK_BUTTON, Messages.CANCEL_BUTTON)
            }
        }

        if (kind == SYNTHESIZED) {
            LOG.error("Change signature refactoring should not be called for synthesized member " + callableDescriptor)
            return
        }

        val closestModifiableDescriptors = getClosestModifiableDescriptors()
        assert(!closestModifiableDescriptors.isEmpty(), "Should contain original declaration or some of its super declarations")
        val deepestSuperDeclarations =
                (callableDescriptor as? CallableMemberDescriptor)?.let { OverrideResolver.getDeepestSuperDeclarations(it) }
                ?: Collections.singletonList(callableDescriptor)
        if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
            performRefactoring(deepestSuperDeclarations)
            return
        }

        if (closestModifiableDescriptors.size() == 1 && deepestSuperDeclarations == closestModifiableDescriptors) {
            performRefactoring(closestModifiableDescriptors)
            return
        }

        val isSingleFunctionSelected = closestModifiableDescriptors.size() == 1
        val selectedFunction = if (isSingleFunctionSelected) closestModifiableDescriptors.first() else callableDescriptor
        val optionsForDialog = buildDialogOptions(isSingleFunctionSelected)
        val code = showSuperFunctionWarningDialog(deepestSuperDeclarations, selectedFunction, optionsForDialog)
        when {
            performForWholeHierarchy(optionsForDialog, code) -> {
                performRefactoring(deepestSuperDeclarations)
            }
            performForSelectedFunctionOnly(optionsForDialog, code) -> {
                performRefactoring(closestModifiableDescriptors)
            }
            else -> {
                //do nothing
            }
        }
    }
}

fun getAffectedCallables(project: Project, descriptorsForChange: Collection<CallableDescriptor>): List<PsiElement> {
    val baseCallables = descriptorsForChange.map { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }.filterNotNull()
    return baseCallables + baseCallables.flatMap { it.toLightMethods() }.flatMapTo(HashSet<PsiElement>()) { psiMethod ->
        val overrides = OverridingMethodsSearch.search(psiMethod).findAll()
        overrides.map { method -> method.namedUnwrappedElement ?: method}
    }
}

fun DeclarationDescriptor.getContainingScope(bindingContext: BindingContext): JetScope? {
    val containingDescriptor = getContainingDeclaration() ?: return null
    return when (containingDescriptor) {
        is ClassDescriptorWithResolutionScopes -> containingDescriptor.getScopeForInitializerResolution()
        is FunctionDescriptor -> {
            (DescriptorToSourceUtils.descriptorToDeclaration(containingDescriptor) as? JetDeclarationWithBody)?.let {
                it.getBodyScope(bindingContext)
            }
        }
        is PackageFragmentDescriptor -> containingDescriptor.getMemberScope()
        else -> null
    }
}

fun JetDeclarationWithBody.getBodyScope(bindingContext: BindingContext): JetScope? {
    val expression = getBodyExpression()?.getChildren()?.firstOrNull { it is JetExpression } as JetExpression?
    return expression?.let { bindingContext[BindingContext.RESOLUTION_SCOPE, it] }
}