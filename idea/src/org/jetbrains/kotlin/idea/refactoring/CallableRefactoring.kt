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

import com.intellij.CommonBundle
import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.core.getDirectlyOverriddenDeclarations
import org.jetbrains.kotlin.idea.highlighter.markers.actualsForExpected
import org.jetbrains.kotlin.idea.highlighter.markers.isExpectedOrExpectedClassMember
import org.jetbrains.kotlin.idea.highlighter.markers.liftToExpected
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import java.util.*

abstract class CallableRefactoring<out T: CallableDescriptor>(
        val project: Project,
        callableDescriptor: T,
        val commandName: String) {
    private val LOG = Logger.getInstance(CallableRefactoring::class.java)

    @Suppress("UNCHECKED_CAST")
    val callableDescriptor = callableDescriptor.liftToExpected() as? T ?: callableDescriptor

    private val kind = (callableDescriptor as? CallableMemberDescriptor)?.kind ?: CallableMemberDescriptor.Kind.DECLARATION

    protected open fun forcePerformForSelectedFunctionOnly(): Boolean {
        return false
    }

    private fun getClosestModifiableDescriptors(): Collection<CallableDescriptor> {
        return when (kind) {
            DECLARATION -> {
                setOf(callableDescriptor)
            }
            DELEGATION, FAKE_OVERRIDE -> {
                (callableDescriptor as CallableMemberDescriptor).getDirectlyOverriddenDeclarations()
            }
            else -> {
                throw IllegalStateException("Unexpected callable kind: $kind")
            }
        }.map { it.liftToExpected() as? CallableDescriptor ?: it }
    }

    private fun showSuperFunctionWarningDialog(superCallables: Collection<CallableDescriptor>,
                                               callableFromEditor: CallableDescriptor,
                                               options: List<String>): Int {
        val superString = superCallables.joinToString(prefix = "\n    ", separator = ",\n    ", postfix = ".\n\n") {
            it.containingDeclaration.name.asString()
        }
        val message = KotlinBundle.message("x.overrides.y.in.class.list",
                                           DescriptorRenderer.COMPACT.render(callableFromEditor),
                                           callableFromEditor.containingDeclaration.name.asString(), superString,
                                           "refactor")
        val title = IdeBundle.message("title.warning")!!
        val icon = Messages.getQuestionIcon()
        return Messages.showDialog(message, title, options.toTypedArray(), 0, icon)
    }

    protected fun checkModifiable(element: PsiElement): Boolean {
        if (element.canRefactor()) {
            return true
        }

        val unmodifiableFile = element.containingFile?.virtualFile?.presentableUrl
        if (unmodifiableFile != null) {
            val message = RefactoringBundle.message("refactoring.cannot.be.performed") + "\n" +
                          IdeBundle.message("error.message.cannot.modify.file.0", unmodifiableFile)
            Messages.showErrorDialog(project, message, CommonBundle.getErrorTitle()!!)
        }
        else {
            LOG.error("Could not find file for Psi element: " + element.text)
        }

        return false
    }

    protected abstract fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>)

    fun run(): Boolean {
        fun buttonPressed(code: Int, dialogButtons: List<String>, button: String): Boolean {
            return code == dialogButtons.indexOf(button) && button in dialogButtons
        }

        fun performForWholeHierarchy(dialogButtons: List<String>, code: Int): Boolean {
            return buttonPressed(code, dialogButtons, Messages.YES_BUTTON) || buttonPressed(code, dialogButtons, Messages.OK_BUTTON)
        }

        fun performForSelectedFunctionOnly(dialogButtons: List<String>, code: Int): Boolean {
            return buttonPressed(code, dialogButtons, Messages.NO_BUTTON)
        }

        fun buildDialogOptions(isSingleFunctionSelected: Boolean): List<String> {
            return if (isSingleFunctionSelected) {
                arrayListOf(Messages.YES_BUTTON, Messages.NO_BUTTON, Messages.CANCEL_BUTTON)
            }
            else {
                arrayListOf(Messages.OK_BUTTON, Messages.CANCEL_BUTTON)
            }
        }

        if (kind == SYNTHESIZED) {
            LOG.error("Change signature refactoring should not be called for synthesized member " + callableDescriptor)
            return false
        }

        val closestModifiableDescriptors = getClosestModifiableDescriptors()
        if (forcePerformForSelectedFunctionOnly()) {
            performRefactoring(closestModifiableDescriptors)
            return true
        }

        assert(!closestModifiableDescriptors.isEmpty()) { "Should contain original declaration or some of its super declarations" }
        val deepestSuperDeclarations =
                (callableDescriptor as? CallableMemberDescriptor)?.let(CallableMemberDescriptor::getDeepestSuperDeclarations)
                ?: listOf(callableDescriptor)
        if (ApplicationManager.getApplication()!!.isUnitTestMode) {
            performRefactoring(deepestSuperDeclarations)
            return true
        }

        if (closestModifiableDescriptors.size == 1 && deepestSuperDeclarations.subtract(closestModifiableDescriptors).isEmpty()) {
            performRefactoring(closestModifiableDescriptors)
            return true
        }

        val isSingleFunctionSelected = closestModifiableDescriptors.size == 1
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
                return false
            }
        }
        return true
    }
}

fun getAffectedCallables(project: Project, descriptorsForChange: Collection<CallableDescriptor>): List<PsiElement> {
    val baseCallables = descriptorsForChange.mapNotNull { DescriptorToSourceUtilsIde.getAnyDeclaration(project, it) }
    return baseCallables + baseCallables.flatMapTo(HashSet<PsiElement>()) { callable ->
        if (callable is KtDeclaration && callable.isExpectedOrExpectedClassMember()) {
            callable.actualsForExpected()
        }
        else {
            callable.toLightMethods().flatMap { psiMethod ->
                val overrides = OverridingMethodsSearch.search(psiMethod).findAll()
                overrides.map { method -> method.namedUnwrappedElement ?: method}
            }
        }
    }
}

fun DeclarationDescriptor.getContainingScope(): LexicalScope? {
    val declaration = DescriptorToSourceUtils.descriptorToDeclaration(this)
    val block = declaration?.parent as? KtBlockExpression
    return if (block != null) {
        val lastStatement = block.statements.last()
        val bindingContext = lastStatement.analyze()
        lastStatement.getResolutionScope(bindingContext, lastStatement.getResolutionFacade())
    }
    else {
        val containingDescriptor = containingDeclaration ?: return null
        when (containingDescriptor) {
            is ClassDescriptorWithResolutionScopes -> containingDescriptor.scopeForInitializerResolution
            is PackageFragmentDescriptor -> LexicalScope.Base(containingDescriptor.getMemberScope().memberScopeAsImportingScope(), this)
            else -> null
        }
    }
}

fun KtDeclarationWithBody.getBodyScope(bindingContext: BindingContext): LexicalScope? {
    val expression = bodyExpression?.children?.firstOrNull { it is KtExpression } ?: return null
    return expression.getResolutionScope(bindingContext, getResolutionFacade())
}
