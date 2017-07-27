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

package org.jetbrains.kotlin.idea.actions.internal


import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.ProgressManager.progress
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.findModuleDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe


class SearchNotPropertyCandidatesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent?) {
        val project = e?.project!!
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return

        val desc = psiFile.findModuleDescriptor()
        val result = Messages.showInputDialog("Enter package FqName", "Search for Not Property candidates", Messages.getQuestionIcon())
        val packageDesc = try {
            val fqName = FqName.fromSegments(result!!.split('.'))
            desc.getPackage(fqName)
        }
        catch (e: Exception) {
            return
        }

        ProgressManager.getInstance().runProcessWithProgressSynchronously(
                {
                    runReadAction {
                        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
                        processAllDescriptors(packageDesc, project)
                    }
                },
                "Searching for Not Property candidates",
                true,
                project)
    }


    private fun processAllDescriptors(desc: DeclarationDescriptor, project: Project) {
        val processed = mutableSetOf<DeclarationDescriptor>()
        var pFunctions = 0
        val matchedDescriptors = mutableSetOf<FunctionDescriptor>()

        fun recursive(desc: DeclarationDescriptor) {
            if (desc in processed) return
            progress("Step 1: Collecting ${processed.size}:$pFunctions:${matchedDescriptors.size}", "$desc")
            when (desc) {

                is ModuleDescriptor -> recursive(desc.getPackage(FqName("java")))
                is ClassDescriptor -> desc.unsubstitutedMemberScope.getContributedDescriptors { true }.forEach(::recursive)
                is PackageViewDescriptor -> desc.memberScope.getContributedDescriptors { true }.forEach(::recursive)

                is FunctionDescriptor -> {
                    if (desc.isFromJava) {
                        val name = desc.fqNameUnsafe.shortName().asString()
                        if (name.length > 3 &&
                            ((name.startsWith("get") && desc.valueParameters.isEmpty() && desc.returnType != null) ||
                             (name.startsWith("set") && desc.valueParameters.size == 1))) {
                            if (desc in matchedDescriptors) return
                            matchedDescriptors += desc
                        }
                    }
                    pFunctions++
                    return
                }

            }
            processed += desc
        }
        recursive(desc)
        val resultDescriptors = mutableSetOf<FunctionDescriptor>()
        matchedDescriptors.flatMapTo(resultDescriptors) {
            sequenceOf(it, *(it.overriddenDescriptors.toTypedArray())).asIterable()
        }
        println("Found ${resultDescriptors.size} accessors")


        fun PsiMethod.isTrivial(): Boolean {
            val t = this.text
            val s = t.indexOf('{')
            val e = t.lastIndexOf('}')
            return if (s != e && s != -1) t.substring(s, e).lines().size <= 3 else true
        }


        val descriptorToPsiBinding = mutableMapOf<FunctionDescriptor, PsiMethod>()


        var i = 0
        resultDescriptors.forEach { desc ->
            progress("Step 2: ${i++} of ${resultDescriptors.size}", "$desc")
            val source = DescriptorToSourceUtilsIde.getAllDeclarations(project, desc)
                                 .filterIsInstance<PsiMethod>()
                                 .firstOrNull() ?: return@forEach
            val abstract = source.modifierList.hasModifierProperty(PsiModifier.ABSTRACT)
            if (!abstract) {
                if (!source.isTrivial()) {
                    descriptorToPsiBinding[desc] = source
                }
            }
        }

        val nonTrivial = mutableSetOf<FunctionDescriptor>()
        i = 0
        descriptorToPsiBinding.forEach { t, u ->
            progress("Step 3: ${i++} of ${descriptorToPsiBinding.size}", "$t")
            val descriptors = t.overriddenDescriptors
            var impl = false
            descriptors.forEach {
                val source = DescriptorToSourceUtilsIde.getAllDeclarations(project, it).filterIsInstance<PsiMethod>().firstOrNull()
                if (source != null) {
                    if (source.body != null || source.hasModifierProperty(PsiModifier.ABSTRACT))
                        nonTrivial += it
                    impl = true
                }
            }
            if (u.body != null)
                if (!impl)
                    nonTrivial += t
        }
        nonTrivial.forEach(::println)
        println("Non trivial count: ${nonTrivial.size}")
    }

    override fun update(e: AnActionEvent) {
        if (!KotlinInternalMode.enabled) {
            e.presentation.isVisible = false
            e.presentation.isEnabled = false
        }
        else {
            e.presentation.isVisible = true
            e.presentation.isEnabled = e.getData(CommonDataKeys.PSI_FILE) is KtFile
        }
    }

}