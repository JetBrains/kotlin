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

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.imports.importableFqNameSafe
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 */
public class JetAddImportAction(
        private val project: Project,
        private val editor: Editor,
        private val element: JetSimpleNameExpression,
        candidates: Collection<DeclarationDescriptor>
) : QuestionAction {

    private val module = ModuleUtilCore.findModuleForPsiElement(element)

    private enum class Priority {
        MODULE
        PROJECT
        OTHER
    }

    private fun detectPriority(descriptor: DeclarationDescriptor): Priority {
        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor)
        return when {
            declaration == null -> Priority.OTHER
            ModuleUtilCore.findModuleForPsiElement(declaration) == module -> Priority.MODULE
            ProjectRootsUtil.isInProjectSource(declaration) -> Priority.PROJECT
            else -> Priority.OTHER
        }
    }

    private inner class Variant(
            val fqName: FqName,
            val descriptors: Collection<DeclarationDescriptor>
    ) {
        val priority = descriptors.map { detectPriority(it) }.min()!!

        val descriptorToImport: DeclarationDescriptor
            get() {
                if (descriptors.size() == 1) return descriptors.single()
                return descriptors.sortBy {
                    when (it) {
                        is ClassDescriptor -> 0
                        is PackageViewDescriptor -> 1
                        else -> 2
                    }
                }.first()
            }
        val declarationToImport = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptorToImport)
    }

    private val variants = candidates
            .groupBy { DescriptorUtils.getFqNameSafe(it) }
            .map { Variant(it.key, it.value) }
            .sortBy { it.priority }

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        if (!element.isValid()) return false

        // TODO: Validate resolution variants. See AddImportAction.execute()

        if (variants.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
            addImport(element, project, variants.first())
        }
        else {
            chooseCandidateAndImport()
        }

        return true
    }

    protected fun getImportSelectionPopup(): BaseListPopupStep<Variant> {
        return object : BaseListPopupStep<Variant>(JetBundle.message("imports.chooser.title"), variants) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: Variant?, finalChoice: Boolean): PopupStep<String>? {
                if (selectedValue == null) return null

                if (finalChoice) {
                    addImport(element, project, selectedValue)
                    return null
                }

                val toExclude = AddImportAction.getAllExcludableStrings(selectedValue.fqName.asString())

                return object : BaseListPopupStep<String>(null, toExclude) {
                    override fun getTextFor(value: String): String {
                        return "Exclude '$value' from auto-import"
                    }

                    override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<Any>? {
                        if (finalChoice) {
                            AddImportAction.excludeFromImport(project, selectedValue)
                        }
                        return null
                    }
                }
            }

            override fun hasSubstep(selectedValue: Variant?) = true

            override fun getTextFor(value: Variant) = value.fqName.asString()

            override fun getIconFor(value: Variant) = JetDescriptorIconProvider.getIcon(value.descriptorToImport, value.declarationToImport, 0)
        }
    }

    private fun chooseCandidateAndImport() {
        JBPopupFactory.getInstance().createListPopup(getImportSelectionPopup()).showInBestPositionFor(editor)
    }

    default object {

        protected fun addImport(element: PsiElement, project: Project, selectedVariant: Variant) {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            CommandProcessor.getInstance().executeCommand(project, object : Runnable {
                override fun run() {
                    ApplicationManager.getApplication().runWriteAction {
                        val file = element.getContainingFile() as JetFile
                        val descriptor = selectedVariant.descriptorToImport
                        // for class or package we use ShortenReferences because we not necessary insert an import but may want to insert partly qualified name
                        if (descriptor is ClassDescriptor || descriptor is PackageViewDescriptor) {
                            val fqName = descriptor.importableFqNameSafe
                            val reference = element.getReference() as JetSimpleNameReference
                            reference.bindToFqName(fqName, JetSimpleNameReference.ShorteningMode.FORCED_SHORTENING)
                        }
                        else {
                            ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
                        }
                    }
                }
            }, QuickFixBundle.message("add.import"), null)
        }
    }
}
