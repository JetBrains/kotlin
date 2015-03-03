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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import java.util.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.quickfix.QuickFixUtil
import com.intellij.CommonBundle
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.resolve.OverrideResolver
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring

public trait JetChangeSignatureConfiguration {
    fun configure(originalDescriptor: JetMethodDescriptor, bindingContext: BindingContext): JetMethodDescriptor

    fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean {
        return false
    }
}

fun JetMethodDescriptor.modify(action: JetMutableMethodDescriptor.() -> Unit): JetMethodDescriptor {
    val newDescriptor = JetMutableMethodDescriptor(this)
    newDescriptor.action()
    return newDescriptor
}

public fun runChangeSignature(project: Project,
                              functionDescriptor: FunctionDescriptor,
                              configuration: JetChangeSignatureConfiguration,
                              bindingContext: BindingContext,
                              defaultValueContext: PsiElement,
                              commandName: String? = null) {
    JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, commandName).run()
}

public class JetChangeSignature(project: Project,
                                functionDescriptor: FunctionDescriptor,
                                val configuration: JetChangeSignatureConfiguration,
                                bindingContext: BindingContext,
                                val defaultValueContext: PsiElement,
                                commandName: String?): CallableRefactoring<FunctionDescriptor>(project, functionDescriptor, bindingContext, commandName) {

    private val LOG = Logger.getInstance(javaClass<JetChangeSignature>())

    override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
        assert (descriptorsForChange.all { it is FunctionDescriptor }) {
            "Function descriptors expected: " + descriptorsForChange.joinToString(separator = "\n")
        }

        [suppress("UNCHECKED_CAST")]
        val dialog = createChangeSignatureDialog(descriptorsForChange as Collection<FunctionDescriptor>)
        if (dialog == null) return

        val affectedFunctions = dialog.getMethodDescriptor().affectedFunctions.map { it.getElement() }.filterNotNull()

        if (affectedFunctions.any { !checkModifiable(it) }) return

        if (configuration.performSilently(affectedFunctions)
            || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            performRefactoringSilently(dialog)
        }
        else {
            dialog.show()
        }
    }

    fun createChangeSignatureDialog(descriptorsForSignatureChange: Collection<FunctionDescriptor>): JetChangeSignatureDialog? {
        val baseDescriptor = preferContainedInClass(descriptorsForSignatureChange)
        val functionDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, baseDescriptor)
        if (functionDeclaration == null) {
            LOG.error("Could not find declaration for $baseDescriptor")
            return null
        }

        if (!checkModifiable(functionDeclaration)) {
            return null
        }

        val originalDescriptor = JetChangeSignatureData(baseDescriptor, functionDeclaration, descriptorsForSignatureChange)
        val adjustedDescriptor = configuration.configure(originalDescriptor, bindingContext)
        return JetChangeSignatureDialog(project, adjustedDescriptor, defaultValueContext, commandName)
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
}

TestOnly public fun getChangeSignatureDialog(project: Project,
                                             functionDescriptor: FunctionDescriptor,
                                             configuration: JetChangeSignatureConfiguration,
                                             bindingContext: BindingContext,
                                             defaultValueContext: PsiElement): JetChangeSignatureDialog? {
    val jetChangeSignature = JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, null)
    return jetChangeSignature.createChangeSignatureDialog(OverrideResolver.getDeepestSuperDeclarations(functionDescriptor))
}
