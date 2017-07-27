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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.changeSignature.*
import com.intellij.refactoring.util.CanonicalTypes
import com.intellij.util.VisibilityUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.refactoring.CallableRefactoring
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinChangePropertySignatureDialog
import org.jetbrains.kotlin.idea.refactoring.changeSignature.ui.KotlinChangeSignatureDialog
import org.jetbrains.kotlin.idea.refactoring.createJavaMethod
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

interface KotlinChangeSignatureConfiguration {
    fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor = originalDescriptor
    fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = false
    fun forcePerformForSelectedFunctionOnly(): Boolean = false

    object Empty: KotlinChangeSignatureConfiguration
}

fun KotlinMethodDescriptor.modify(action: (KotlinMutableMethodDescriptor) -> Unit): KotlinMethodDescriptor {
    val newDescriptor = KotlinMutableMethodDescriptor(this)
    action(newDescriptor)
    return newDescriptor
}

fun runChangeSignature(project: Project,
                              callableDescriptor: CallableDescriptor,
                              configuration: KotlinChangeSignatureConfiguration,
                              defaultValueContext: PsiElement,
                              commandName: String? = null): Boolean {
    return KotlinChangeSignature(project, callableDescriptor, configuration, defaultValueContext, commandName).run()
}

class KotlinChangeSignature(
        project: Project,
        callableDescriptor: CallableDescriptor,
        val configuration: KotlinChangeSignatureConfiguration,
        val defaultValueContext: PsiElement,
        commandName: String?
): CallableRefactoring<CallableDescriptor>(project, callableDescriptor, commandName ?: ChangeSignatureHandler.REFACTORING_NAME) {

    private val LOG = Logger.getInstance(KotlinChangeSignature::class.java)

    override fun forcePerformForSelectedFunctionOnly() = configuration.forcePerformForSelectedFunctionOnly()

    private fun runSilentRefactoring(descriptor: KotlinMethodDescriptor) {
        val baseDeclaration = descriptor.baseDeclaration
        val processor = when (baseDeclaration) {
            is KtFunction, is KtClass -> {
                KotlinChangeSignatureDialog.createRefactoringProcessorForSilentChangeSignature(project, commandName, descriptor, defaultValueContext)
            }
            is KtProperty, is KtParameter -> {
                KotlinChangePropertySignatureDialog.createProcessorForSilentRefactoring(project, commandName, descriptor)
            }
            is PsiMethod -> {
                if (baseDeclaration.language != JavaLanguage.INSTANCE) {
                    Messages.showErrorDialog("Can't change signature of ${baseDeclaration.language.displayName} method", commandName)
                    return
                }

                ChangeSignatureProcessor(project, getPreviewInfoForJavaMethod(descriptor).second)
            }
            else -> throw AssertionError("Unexpected declaration: ${baseDeclaration.getElementTextWithContext()}")
        }
        processor.run()
    }

    private fun runInteractiveRefactoring(descriptor: KotlinMethodDescriptor) {
        val baseDeclaration = descriptor.baseDeclaration
        val dialog = when (baseDeclaration) {
            is KtFunction, is KtClass -> KotlinChangeSignatureDialog(project, descriptor, defaultValueContext, commandName)
            is KtProperty, is KtParameter -> KotlinChangePropertySignatureDialog(project, descriptor, commandName)
            is PsiMethod -> {
                // No changes are made from Kotlin side: just run foreign refactoring
                if (descriptor is KotlinChangeSignatureData) {
                    ChangeSignatureUtil.invokeChangeSignatureOn(baseDeclaration, project)
                    return
                }

                if (baseDeclaration.language != JavaLanguage.INSTANCE) {
                    Messages.showErrorDialog("Can't change signature of ${baseDeclaration.language.displayName} method", commandName)
                    return
                }

                val (preview, javaChangeInfo) = getPreviewInfoForJavaMethod(descriptor)
                object: JavaChangeSignatureDialog(project, JavaMethodDescriptor(preview), false, null) {
                    override fun createRefactoringProcessor(): BaseRefactoringProcessor? {
                        val processor = super.createRefactoringProcessor()
                        (processor as? ChangeSignatureProcessor)?.changeInfo?.updateMethod(javaChangeInfo.method)
                        return processor
                    }
                }
            }
            else -> throw AssertionError("Unexpected declaration: ${baseDeclaration.getElementTextWithContext()}")
        }

        dialog.show()
    }

    private fun getPreviewInfoForJavaMethod(descriptor: KotlinMethodDescriptor): Pair<PsiMethod, JavaChangeInfo> {
        val originalMethod = descriptor.baseDeclaration as PsiMethod
        val contextFile = defaultValueContext.containingFile as KtFile

        // Generate new Java method signature from the Kotlin point of view
        val ktChangeInfo = KotlinChangeInfo(methodDescriptor = descriptor, context = defaultValueContext)
        val ktSignature = ktChangeInfo.getNewSignature(descriptor.originalPrimaryCallable)
        val dummyFileText = with(StringBuilder()) {
            contextFile.packageDirective?.let { append(it.text).append("\n") }
            append("class Dummy {\n").append(ktSignature).append("{}\n}")
            toString()
        }
        val dummyFile = KtPsiFactory(project).createFileWithLightClassSupport("dummy.kt", dummyFileText, originalMethod)
        val dummyDeclaration = (dummyFile.declarations.first() as KtClass).getBody()!!.declarations.first()

        // Convert to PsiMethod which can be used in Change Signature dialog
        val containingClass = PsiElementFactory.SERVICE.getInstance(project).createClass("Dummy")
        val preview = createJavaMethod(dummyDeclaration.getRepresentativeLightMethod()!!, containingClass)

        // Create JavaChangeInfo based on new signature
        // TODO: Support visibility change
        val visibility = VisibilityUtil.getVisibilityModifier(originalMethod.modifierList)
        val returnType = CanonicalTypes.createTypeWrapper(preview.returnType ?: PsiType.VOID)
        val params = (preview.parameterList.parameters.zip(ktChangeInfo.newParameters)).map {
            val (param, paramInfo) = it
            // Keep original default value for proper update of Kotlin usages
            KotlinAwareJavaParameterInfoImpl(paramInfo.oldIndex, param.name!!, param.type, paramInfo.defaultValueForCall)
        }.toTypedArray()

        return preview to JavaChangeInfoImpl(visibility,
                                             originalMethod,
                                             preview.name,
                                             returnType,
                                             params,
                                             arrayOf<ThrownExceptionInfo>(),
                                             false,
                                             emptySet<PsiMethod>(),
                                             emptySet<PsiMethod>())
    }

    override fun performRefactoring(descriptorsForChange: Collection<CallableDescriptor>) {
        val adjustedDescriptor = adjustDescriptor(descriptorsForChange) ?: return

        val affectedFunctions = adjustedDescriptor.affectedCallables.mapNotNull { it.element }
        if (affectedFunctions.any { !checkModifiable(it) }) return

        if (configuration.performSilently(affectedFunctions) || ApplicationManager.getApplication()!!.isUnitTestMode) {
            runSilentRefactoring(adjustedDescriptor)
        }
        else {
            runInteractiveRefactoring(adjustedDescriptor)
        }
    }

    fun adjustDescriptor(descriptorsForSignatureChange: Collection<CallableDescriptor>): KotlinMethodDescriptor? {
        val baseDescriptor = preferContainedInClass(descriptorsForSignatureChange)
        val functionDeclaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, baseDescriptor)
        if (functionDeclaration == null) {
            LOG.error("Could not find declaration for $baseDescriptor")
            return null
        }

        if (!checkModifiable(functionDeclaration)) {
            return null
        }

        val originalDescriptor = KotlinChangeSignatureData(baseDescriptor, functionDeclaration, descriptorsForSignatureChange)
        return configuration.configure(originalDescriptor)
    }

    private fun preferContainedInClass(descriptorsForSignatureChange: Collection<CallableDescriptor>): CallableDescriptor {
        for (descriptor in descriptorsForSignatureChange) {
            val containingDeclaration = descriptor.containingDeclaration
            if (containingDeclaration is ClassDescriptor && containingDeclaration.kind != ClassKind.INTERFACE) {
                return descriptor
            }
        }
        //choose at random
        return descriptorsForSignatureChange.first()
    }
}

@TestOnly fun createChangeInfo(
        project: Project,
        callableDescriptor: CallableDescriptor,
        configuration: KotlinChangeSignatureConfiguration,
        defaultValueContext: PsiElement
): KotlinChangeInfo? {
    val jetChangeSignature = KotlinChangeSignature(project, callableDescriptor, configuration, defaultValueContext, null)
    val declarations =
            (callableDescriptor as? CallableMemberDescriptor)?.getDeepestSuperDeclarations() ?: listOf(callableDescriptor)

    val adjustedDescriptor = jetChangeSignature.adjustDescriptor(declarations) ?: return null

    val processor = KotlinChangeSignatureDialog.createRefactoringProcessorForSilentChangeSignature(
            project,
            ChangeSignatureHandler.REFACTORING_NAME,
            adjustedDescriptor,
            defaultValueContext
    ) as KotlinChangeSignatureProcessor
    return processor.ktChangeInfo
}
