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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.OverrideResolver

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
                              commandName: String? = null): Boolean {
    return JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, commandName).run()
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

        @suppress("UNCHECKED_CAST")
        val adjustedDescriptor = adjustDescriptor(descriptorsForChange as Collection<FunctionDescriptor>)
        if (adjustedDescriptor == null) return

        val affectedFunctions = adjustedDescriptor.affectedFunctions.map { it.getElement() }.filterNotNull()

        if (affectedFunctions.any { !checkModifiable(it) }) return

        if (configuration.performSilently(affectedFunctions)
            || ApplicationManager.getApplication()!!.isUnitTestMode()) {
            JetChangeSignatureDialog.createRefactoringProcessorForSilentChangeSignature(
                    project,
                    commandName ?: ChangeSignatureHandler.REFACTORING_NAME,
                    adjustedDescriptor,
                    defaultValueContext
            ).run()
        }
        else {
            val dialog = JetChangeSignatureDialog(project, adjustedDescriptor, defaultValueContext, commandName)

            dialog.show()
        }
    }

    fun adjustDescriptor(descriptorsForSignatureChange: Collection<FunctionDescriptor>): JetMethodDescriptor? {
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
        return configuration.configure(originalDescriptor, bindingContext)
    }

    private fun preferContainedInClass(descriptorsForSignatureChange: Collection<FunctionDescriptor>): FunctionDescriptor {
        for (descriptor in descriptorsForSignatureChange) {
            val containingDeclaration = descriptor.getContainingDeclaration()
            if (containingDeclaration is ClassDescriptor && containingDeclaration.getKind() != ClassKind.INTERFACE) {
                return descriptor
            }
        }
        //choose at random
        return descriptorsForSignatureChange.first()
    }
}

TestOnly public fun createChangeInfo(project: Project,
                                             functionDescriptor: FunctionDescriptor,
                                             configuration: JetChangeSignatureConfiguration,
                                             bindingContext: BindingContext,
                                             defaultValueContext: PsiElement): JetChangeInfo? {
    val jetChangeSignature = JetChangeSignature(project, functionDescriptor, configuration, bindingContext, defaultValueContext, null)
    val declarations = OverrideResolver.getDeepestSuperDeclarations(functionDescriptor)

    val adjustedDescriptor = jetChangeSignature.adjustDescriptor(declarations) ?: return null

    val processor = JetChangeSignatureDialog.createRefactoringProcessorForSilentChangeSignature(project,  ChangeSignatureHandler.REFACTORING_NAME, adjustedDescriptor, defaultValueContext) as JetChangeSignatureProcessor
    return processor.getChangeInfo()
}
