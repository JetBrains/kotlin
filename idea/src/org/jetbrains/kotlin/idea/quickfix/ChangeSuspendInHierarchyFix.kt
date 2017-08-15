/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.ArrayList
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.List
import kotlin.collections.Set
import kotlin.collections.emptyList
import kotlin.collections.emptySet
import kotlin.collections.filter
import kotlin.collections.flatMap
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.listOf
import kotlin.collections.mapNotNullTo
import kotlin.collections.plus

class ChangeSuspendInHierarchyFix(
        element: KtNamedFunction,
        private val addModifier: Boolean
) : KotlinQuickFixAction<KtNamedFunction>(element) {
    override fun getFamilyName(): String {
        return if (addModifier) {
            "Add 'suspend' modifier to all functions in hierarchy"
        } else {
            "Remove 'suspend' modifier from all functions in hierarchy"
        }
    }

    override fun getText() = familyName

    override fun startInWriteAction() = false

    private fun findAllFunctionToProcess(project: Project): Set<KtNamedFunction> {
        val result = LinkedHashSet<KtNamedFunction>()

        val progressIndicator = ProgressManager.getInstance().progressIndicator

        val function = element ?: return emptySet()
        val functionDescriptor = function.resolveToDescriptor() as FunctionDescriptor

        val baseFunctionDescriptors = functionDescriptor.findTopMostOverriddables()
        baseFunctionDescriptors.forEach { baseFunctionDescriptor ->
            val baseClassDescriptor = baseFunctionDescriptor.containingDeclaration as? ClassDescriptor ?: return@forEach
            val baseClass = DescriptorToSourceUtilsIde.getAnyDeclaration(project, baseClassDescriptor) ?: return@forEach

            val name = (baseClass as? PsiNamedElement)?.name ?: return@forEach
            progressIndicator.text = "Looking for class $name inheritors..."
            val classes = listOf(baseClass) + HierarchySearchRequest(baseClass, baseClass.useScope).searchInheritors()
            classes.mapNotNullTo(result) {
                val subClass = it.unwrapped as? KtClassOrObject ?: return@mapNotNullTo null
                val classDescriptor = subClass.resolveToDescriptor() as ClassDescriptor
                val substitutor = getTypeSubstitutor(baseClassDescriptor.defaultType, classDescriptor.defaultType)
                                  ?: return@mapNotNullTo null
                val signatureInSubClass = baseFunctionDescriptor.substitute(substitutor) as FunctionDescriptor
                val subFunctionDescriptor = classDescriptor.findCallableMemberBySignature(signatureInSubClass, true)
                                            ?: return@mapNotNullTo null
                subFunctionDescriptor.source.getPsi() as? KtNamedFunction
            }
        }

        return result
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val functions = project.runSynchronouslyWithProgress("Analyzing class hierarchy...", true) {
            runReadAction { findAllFunctionToProcess(project) }
        } ?: return

        runWriteAction {
            functions.forEach {
                if (addModifier) {
                    it.addModifier(KtTokens.SUSPEND_KEYWORD)
                }
                else {
                    it.removeModifier(KtTokens.SUSPEND_KEYWORD)
                }
            }
        }
    }

    companion object : KotlinIntentionActionsFactory() {
        fun FunctionDescriptor.findTopMostOverriddables(): List<FunctionDescriptor> {
            val overridablesCache = HashMap<FunctionDescriptor, List<FunctionDescriptor>>()

            fun FunctionDescriptor.getOverridables(): List<FunctionDescriptor> {
                return overridablesCache.getOrPut(this) {
                    val classDescriptor = containingDeclaration as? ClassDescriptorWithResolutionScopes ?: return emptyList()
                    DescriptorUtils.getSuperclassDescriptors(classDescriptor).flatMap { superClassDescriptor ->
                        if (superClassDescriptor !is ClassDescriptorWithResolutionScopes) return@flatMap emptyList<FunctionDescriptor>()
                        val candidates = superClassDescriptor.unsubstitutedMemberScope.getContributedFunctions(name, NoLookupLocation.FROM_IDE)
                        val substitutor = getTypeSubstitutor(superClassDescriptor.defaultType, classDescriptor.defaultType)
                                          ?: return@flatMap emptyList<FunctionDescriptor>()
                        candidates.filter {
                            val signature = it.substitute(substitutor) as FunctionDescriptor
                            classDescriptor.findCallableMemberBySignature(signature, true) == this
                        }
                    }
                }
            }

            return DFS.dfs(
                    listOf(this),
                    { it?.getOverridables() ?: emptyList() },
                    object : DFS.CollectingNodeHandler<FunctionDescriptor, FunctionDescriptor, ArrayList<FunctionDescriptor>>(ArrayList()) {
                        override fun afterChildren(current: FunctionDescriptor) {
                            if (current.getOverridables().isEmpty()) {
                                result.add(current)
                            }
                        }
                    })
        }

        private fun Collection<DeclarationDescriptor>.getOverridables(
                currentDescriptor: FunctionDescriptor
        ): List<DeclarationDescriptor> {
            val currentClassDescriptor = currentDescriptor.containingDeclaration as? ClassDescriptor ?: return emptyList()
            return filter {
                if (it !is FunctionDescriptor || it == currentDescriptor) return@filter false
                if (it.isSuspend == currentDescriptor.isSuspend) return@filter false
                val containingClassDescriptor = it.containingDeclaration as? ClassDescriptor ?: return@filter false
                if (!currentClassDescriptor.isSubclassOf(containingClassDescriptor)) return@filter false
                val substitutor = getTypeSubstitutor(
                        containingClassDescriptor.defaultType,
                        currentClassDescriptor.defaultType
                ) ?: return@filter false
                val signatureInCurrentClass = it.substitute(substitutor) ?: return@filter false
                OverridingUtil.DEFAULT.isOverridableBy(signatureInCurrentClass, currentDescriptor, null).result ==
                        OverridingUtil.OverrideCompatibilityInfo.Result.CONFLICT
            }
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val currentFunction = diagnostic.psiElement as? KtNamedFunction ?: return emptyList()
            val currentDescriptor = currentFunction.resolveToDescriptor() as FunctionDescriptor
            Errors.CONFLICTING_OVERLOADS.cast(diagnostic).a.getOverridables(currentDescriptor).ifEmpty { return emptyList() }

            return listOf(
                    ChangeSuspendInHierarchyFix(currentFunction, true),
                    ChangeSuspendInHierarchyFix(currentFunction, false)
            )
        }
    }
}