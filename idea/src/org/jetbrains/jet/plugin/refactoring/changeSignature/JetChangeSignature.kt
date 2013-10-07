/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.changeSignature

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.OverridingUtil
import org.jetbrains.jet.plugin.JetBundle
import org.jetbrains.jet.renderer.DescriptorRenderer
import java.util.*
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.jet.lang.resolve.BindingContextUtils.*
import org.jetbrains.annotations.TestOnly

public trait JetChangeSignatureConfiguration {
    fun configure(changeSignatureData: JetChangeSignatureData, bindingContext: BindingContext)
}

public fun runChangeSignature(project: Project,
                              functionDescriptor: FunctionDescriptor,
                              configuration: JetChangeSignatureConfiguration,
                              bindingContext: BindingContext,
                              defaultValueContext: PsiElement,
                              commandName: String? = null,
                              performSilently: Boolean = false): Unit {
    JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, commandName, performSilently).run()
}

public class JetChangeSignature(val project: Project,
                                val functionDescriptor: FunctionDescriptor,
                                val configuration: JetChangeSignatureConfiguration,
                                val bindingContext: BindingContext,
                                val defaultValueContext: PsiElement,
                                val commandName: String?,
                                val performSilently: Boolean) {

    private val LOG = Logger.getInstance(javaClass<JetChangeSignature>())

    public fun run() {
        if (functionDescriptor.getKind() == SYNTHESIZED) {
            LOG.error("Change signature refactoring should not be called for synthesized member " + functionDescriptor)
            return
        }

        val closestModifiableDescriptors = getClosestModifiableDescriptors()
        assert(!closestModifiableDescriptors.isEmpty()) { "Should contain functionDescriptor itself or some of its super declarations" }
        val deepestSuperDeclarations = getDeepestSuperDeclarations()
        if (ApplicationManager.getApplication()!!.isUnitTestMode()) {
            showChangeSignatureDialog(deepestSuperDeclarations)
            return
        }

        if ((closestModifiableDescriptors.size()) == 1 && deepestSuperDeclarations.equals(closestModifiableDescriptors)) {
            showChangeSignatureDialog(closestModifiableDescriptors)
            return
        }

        val isSingleFunctionSelected = closestModifiableDescriptors.size() == 1
        val selectedFunction = if (isSingleFunctionSelected) closestModifiableDescriptors.first() else functionDescriptor
        val optionsForDialog = buildDialogOptions(isSingleFunctionSelected)
        val code = showSuperFunctionWarningDialog(deepestSuperDeclarations, selectedFunction, optionsForDialog.copyToArray())
        when {
            performForWholeHierarchy(optionsForDialog, code) -> {
                showChangeSignatureDialog(deepestSuperDeclarations)
            }
            performForSelectedFunctionOnly(optionsForDialog, code) -> {
                showChangeSignatureDialog(closestModifiableDescriptors)
            }
            else -> {
                //do nothing
            }
        }
    }

    private fun getClosestModifiableDescriptors(): Set<FunctionDescriptor> {
        val kind = functionDescriptor.getKind()
        if (kind == DELEGATION || kind == FAKE_OVERRIDE) {
            return getDirectlyOverriddenDeclarations(functionDescriptor)
        }

        assert(kind == DECLARATION) { "Unexpected callable kind: " + kind }
        return Collections.singleton(functionDescriptor)
    }

    fun getDeepestSuperDeclarations(): Set<FunctionDescriptor> {
        val overriddenDeclarations = getAllOverriddenDeclarations(functionDescriptor)
        if (overriddenDeclarations.isEmpty()) {
            return Collections.singleton(functionDescriptor)
        }

        return OverridingUtil.filterOutOverriding(overriddenDeclarations)
    }

    private fun showChangeSignatureDialog(descriptorsForSignatureChange: Collection<FunctionDescriptor>) {
        val dialog = createChangeSignatureDialog(descriptorsForSignatureChange)
        if (dialog == null) {
            return
        }

        if (performSilently || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            performRefactoringSilently(dialog)
        }
        else {
            dialog.show()
        }
    }

    fun createChangeSignatureDialog(descriptorsForSignatureChange: Collection<FunctionDescriptor>): JetChangeSignatureDialog? {
        val baseDescriptor = preferContainedInClass(descriptorsForSignatureChange)
        val functionDeclaration = callableDescriptorToDeclaration(bindingContext, baseDescriptor)
        if (functionDeclaration == null) {
            LOG.error("Could not find declaration for " + baseDescriptor)
            return null
        }

        val changeSignatureData = JetChangeSignatureData(baseDescriptor, functionDeclaration, bindingContext, descriptorsForSignatureChange)
        configuration.configure(changeSignatureData, bindingContext)
        return JetChangeSignatureDialog(project, changeSignatureData, defaultValueContext, commandName)
    }

    private fun performRefactoringSilently(dialog: JetChangeSignatureDialog) {
        ApplicationManager.getApplication()!!.runWriteAction {
            dialog.createRefactoringProcessor().run()
            Disposer.dispose(dialog.getDisposable()!!)
        }
    }

    private fun preferContainedInClass(descriptorsForSignatureChange: Collection<FunctionDescriptor>): FunctionDescriptor {
        for (descriptor in descriptorsForSignatureChange) {
            val containingDeclaration = descriptor.getContainingDeclaration()
            if (containingDeclaration is ClassDescriptor && containingDeclaration.getKind() != ClassKind.TRAIT) {
                return descriptor
            }

        }
        //choose at random
        return descriptorsForSignatureChange.first()
    }

    private fun buildDialogOptions(isSingleFunctionSelected: Boolean): List<String> {
        val optionsForDialog = ArrayList<String>()
        optionsForDialog.add(if (isSingleFunctionSelected) Messages.YES_BUTTON else Messages.OK_BUTTON)
        if (isSingleFunctionSelected) {
            optionsForDialog.add(Messages.NO_BUTTON)
        }
        optionsForDialog.add(Messages.CANCEL_BUTTON)
        return optionsForDialog
    }

    private fun performForWholeHierarchy(optionsForDialog: List<String>, code: Int): Boolean {
        return buttonPressed(code, optionsForDialog, Messages.YES_BUTTON) || buttonPressed(code, optionsForDialog, Messages.OK_BUTTON)
    }

    private fun performForSelectedFunctionOnly(dialogButtons: List<String>, code: Int): Boolean {
        return buttonPressed(code, dialogButtons, Messages.NO_BUTTON)
    }

    private fun buttonPressed(code: Int, dialogButtons: List<String>, button: String): Boolean {
        return code == dialogButtons.indexOf(button) && dialogButtons.contains(button)
    }

    private fun showSuperFunctionWarningDialog(superFunctions: Collection<FunctionDescriptor>,
                                               functionFromEditor: FunctionDescriptor,
                                               options: Array<String>): Int {
        val superString = superFunctions.map {
            it.getContainingDeclaration().getName().asString()
        }.makeString(prefix = "\n    ", separator = ",\n    ", postfix = ".\n\n")
        val message = JetBundle.message("x.overrides.y.in.class.list",
                                        DescriptorRenderer.COMPACT.render(functionFromEditor),
                                        functionFromEditor.getContainingDeclaration().getName().asString(), superString,
                                        "refactor")
        val title = IdeBundle.message("title.warning")
        val icon = Messages.getQuestionIcon()
        return Messages.showDialog(message, title!!, options, 0, icon!!)
    }
}

TestOnly public fun getChangeSignatureDialog(project: Project,
                                             functionDescriptor: FunctionDescriptor,
                                             configuration: JetChangeSignatureConfiguration,
                                             bindingContext: BindingContext,
                                             defaultValueContext: PsiElement): JetChangeSignatureDialog? {
    val jetChangeSignature = JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, null, true)
    return jetChangeSignature.createChangeSignatureDialog(jetChangeSignature.getDeepestSuperDeclarations())
}