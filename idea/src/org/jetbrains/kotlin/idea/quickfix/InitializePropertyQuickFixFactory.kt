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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.impl.FinishMarkAction
import com.intellij.openapi.command.impl.StartMarkAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.MethodReferencesSearch
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.codeInsight.shorten.runRefactoringAndKeepDelayedRequests
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.appendElement
import org.jetbrains.kotlin.idea.core.getOrCreateBody
import org.jetbrains.kotlin.idea.refactoring.CompositeRefactoringRunner
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.secondaryConstructors
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.source.getPsi
import java.util.*

object InitializePropertyQuickFixFactory : KotlinIntentionActionsFactory() {
    class AddInitializerFix(property: KtProperty) : KotlinQuickFixAction<KtProperty>(property) {
        override fun getText() = "Add initializer"
        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val descriptor = element.resolveToDescriptorIfAny() as? PropertyDescriptor ?: return
            val initializerText = CodeInsightUtils.defaultInitializer(descriptor.type) ?: "null"
            val initializer = element.setInitializer(KtPsiFactory(project).createExpression(initializerText))!!
            if (editor != null) {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                editor.selectionModel.setSelection(initializer.startOffset, initializer.endOffset)
                editor.caretModel.moveToOffset(initializer.endOffset)
            }
        }
    }

    class MoveToConstructorParameters(property: KtProperty) : KotlinQuickFixAction<KtProperty>(property) {
        override fun getText() = "Move to constructor parameters"
        override fun getFamilyName() = text

        override fun startInWriteAction(): Boolean = false

        private fun configureChangeSignature(property: KtProperty, propertyDescriptor: PropertyDescriptor): KotlinChangeSignatureConfiguration {
            return object : KotlinChangeSignatureConfiguration {
                override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                    return originalDescriptor.modify {
                        val initializerText = CodeInsightUtils.defaultInitializer(propertyDescriptor.type) ?: "null"
                        val newParam = KotlinParameterInfo(
                                callableDescriptor = originalDescriptor.baseDescriptor,
                                name = propertyDescriptor.name.asString(),
                                originalTypeInfo = KotlinTypeInfo(false, propertyDescriptor.type),
                                valOrVar = property.valOrVarKeyword.toValVar(),
                                modifierList = property.modifierList,
                                defaultValueForCall = KtPsiFactory(property.project).createExpression(initializerText)
                        )
                        it.addParameter(newParam)
                    }
                }

                override fun performSilently(affectedFunctions: Collection<PsiElement>) = noUsagesExist(affectedFunctions)
            }
        }

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val klass = element.containingClassOrObject ?: return
            val propertyDescriptor = element.resolveToDescriptorIfAny() as? PropertyDescriptor ?: return

            StartMarkAction.canStart(project)?.let { return }
            val startMarkAction = StartMarkAction.start(editor, project, text)

            try {
                val parameterToInsert = KtPsiFactory(project).createParameter(element.text)
                runWriteAction { element.delete() }

                val classDescriptor = klass.resolveToDescriptorIfAny() as? ClassDescriptorWithResolutionScopes ?: return
                val constructorDescriptor = classDescriptor.unsubstitutedPrimaryConstructor ?: return
                val contextElement = constructorDescriptor.source.getPsi() ?: return
                val constructorPointer = contextElement.createSmartPointer()
                val config = configureChangeSignature(element, propertyDescriptor)
                object : CompositeRefactoringRunner(project, "refactoring.changeSignature") {
                    override fun runRefactoring() {
                        runChangeSignature(project, constructorDescriptor, config, contextElement, text)
                    }

                    override fun onRefactoringDone() {
                        val constructorOrClass = constructorPointer.element
                        val constructor = constructorOrClass as? KtConstructor<*> ?: (constructorOrClass as? KtClass)?.primaryConstructor
                        constructor?.getValueParameters()?.lastOrNull()?.replace(parameterToInsert)
                    }
                }.run()
            }
            finally {
                FinishMarkAction.finish(project, editor, startMarkAction)
            }
        }
    }

    class InitializeWithConstructorParameter(property: KtProperty) : KotlinQuickFixAction<KtProperty>(property) {
        override fun getText() = "Initialize with constructor parameter"
        override fun getFamilyName() = text

        override fun startInWriteAction(): Boolean = false

        private fun configureChangeSignature(propertyDescriptor: PropertyDescriptor): KotlinChangeSignatureConfiguration {
            return object : KotlinChangeSignatureConfiguration {
                override fun configure(originalDescriptor: KotlinMethodDescriptor): KotlinMethodDescriptor {
                    return originalDescriptor.modify {
                        val classDescriptor = propertyDescriptor.containingDeclaration as ClassDescriptorWithResolutionScopes
                        val constructorScope = classDescriptor.scopeForClassHeaderResolution
                        val validator = CollectingNameValidator(originalDescriptor.parameters.map { it.name }) { name ->
                            constructorScope.getContributedDescriptors(DescriptorKindFilter.VARIABLES).all {
                                it !is VariableDescriptor || it.name.asString() != name
                            }
                        }
                        val initializerText = CodeInsightUtils.defaultInitializer(propertyDescriptor.type) ?: "null"
                        val newParam = KotlinParameterInfo(
                                callableDescriptor = originalDescriptor.baseDescriptor,
                                name = KotlinNameSuggester.suggestNameByName(propertyDescriptor.name.asString(), validator),
                                originalTypeInfo = KotlinTypeInfo(false, propertyDescriptor.type),
                                defaultValueForCall = KtPsiFactory(element!!.project).createExpression(initializerText)
                        )
                        it.addParameter(newParam)
                    }
                }

                override fun performSilently(affectedFunctions: Collection<PsiElement>): Boolean = noUsagesExist(affectedFunctions)
            }
        }

        // TODO: Allow processing of multiple functions in Change Signature so that Start/Finish Mark can be used here
        private fun processConstructors(
                project: Project,
                propertyDescriptor: PropertyDescriptor,
                descriptorsToProcess: Iterator<ConstructorDescriptor>,
                visitedElements: MutableSet<PsiElement> = HashSet()
        ) {
            val element = element!!

            if (!descriptorsToProcess.hasNext()) return
            val descriptor = descriptorsToProcess.next()
            val constructorPointer = descriptor.source.getPsi()?.createSmartPointer()
            val config = configureChangeSignature(propertyDescriptor)
            val changeSignature = {  }

            object : CompositeRefactoringRunner(project, "refactoring.changeSignature") {
                override fun runRefactoring() {
                    runChangeSignature(project, descriptor, config, element.containingClassOrObject!!, text)
                }

                override fun onRefactoringDone() {
                    val constructorOrClass = constructorPointer?.element
                    val constructor = constructorOrClass as? KtConstructor<*> ?: (constructorOrClass as? KtClass)?.primaryConstructor
                    if (constructor == null || !visitedElements.add(constructor)) return
                    constructor.getValueParameters().lastOrNull()?.let { newParam ->
                        val psiFactory = KtPsiFactory(project)
                        (constructor as? KtSecondaryConstructor)?.getOrCreateBody()?.appendElement(
                                psiFactory.createExpression("this.${element.name} = ${newParam.name!!}")
                        ) ?: element.setInitializer(psiFactory.createExpression(newParam.name!!))
                    }
                    processConstructors(project, propertyDescriptor, descriptorsToProcess)
                }
            }.run()
        }

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val propertyDescriptor = element.resolveToDescriptorIfAny() as? PropertyDescriptor ?: return
            val classDescriptor = propertyDescriptor.containingDeclaration as? ClassDescriptorWithResolutionScopes ?: return
            val klass = element.containingClassOrObject ?: return
            val constructorDescriptors = if (klass.hasExplicitPrimaryConstructor() || klass.secondaryConstructors.isEmpty()) {
                listOf(classDescriptor.unsubstitutedPrimaryConstructor!!)
            }
            else {
                classDescriptor.secondaryConstructors.filter {
                    val constructor = it.source.getPsi() as? KtSecondaryConstructor
                    constructor != null && !constructor.getDelegationCall().isCallToThis
                }
            }

            project.runRefactoringAndKeepDelayedRequests {
                processConstructors(project, propertyDescriptor, constructorDescriptors.iterator())
            }
        }
    }

    private fun noUsagesExist(affectedFunctions: Collection<PsiElement>): Boolean {
        return affectedFunctions.flatMap { it.toLightMethods() }.all { MethodReferencesSearch.search(it).findFirst() == null }
    }

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val property = diagnostic.psiElement as? KtProperty ?: return emptyList()
        if (property.receiverTypeReference != null) return emptyList()

        val actions = ArrayList<IntentionAction>(2)

        actions.add(AddInitializerFix(property))

        (property.containingClassOrObject as? KtClass)?.let { klass ->
            if (klass.isAnnotation() || klass.isInterface()) return@let

            if (property.accessors.isNotEmpty() || klass.secondaryConstructors.any { !it.getDelegationCall().isCallToThis }) {
                actions.add(InitializeWithConstructorParameter(property))
            }
            else {
                actions.add(MoveToConstructorParameters(property))
            }
        }

        return actions
    }
}
