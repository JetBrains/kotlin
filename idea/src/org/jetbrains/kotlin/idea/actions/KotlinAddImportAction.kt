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
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.statistics.StatisticsManager
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.PsiProximityComparator
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.JetDescriptorIconProvider
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ImportableFqNameClassifier
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 */
public class KotlinAddImportAction(
        private val project: Project,
        private val editor: Editor,
        private val element: KtSimpleNameExpression,
        candidates: Collection<DeclarationDescriptor>
) : QuestionAction {

    private val file = element.getContainingJetFile()
    private val prioritizer = Prioritizer(file)

    private inner class Variant(
            val fqName: FqName,
            val descriptors: Collection<DeclarationDescriptor>
    ) {
        val priority = descriptors
                .map { prioritizer.priority(fqName, it) }
                .min()!!

        val descriptorToImport: DeclarationDescriptor
            get() {
                return descriptors.singleOrNull()
                       ?: descriptors.sortedBy { if (it is ClassDescriptor) 0 else 1 }.first()
            }

        val declarationToImport = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptorToImport)
    }

    private val variants = candidates
            .groupBy { it.importableFqName!! }
            .map { Variant(it.key, it.value) }
            .sortedBy { it.priority }

    public val highestPriorityFqName: FqName
        get() = variants.first().fqName

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        if (!element.isValid()) return false

        // TODO: Validate resolution variants. See AddImportAction.execute()

        if (variants.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
            addImport(variants.first())
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
                if (selectedValue == null || project.isDisposed) return null

                if (finalChoice) {
                    addImport(selectedValue)
                    return null
                }

                val toExclude = AddImportAction.getAllExcludableStrings(selectedValue.fqName.asString())

                return object : BaseListPopupStep<String>(null, toExclude) {
                    override fun getTextFor(value: String): String {
                        return "Exclude '$value' from auto-import"
                    }

                    override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<Any>? {
                        if (finalChoice && !project.isDisposed) {
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

    private fun addImport(selectedVariant: Variant) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        project.executeWriteCommand(QuickFixBundle.message("add.import")) {
            if (!element.isValid()) return@executeWriteCommand

            selectedVariant.declarationToImport?.let {
                val location = ProximityLocation(file, ModuleUtilCore.findModuleForPsiElement(file))
                StatisticsManager.getInstance().incUseCount(PsiProximityComparator.STATISTICS_KEY, it, location)
            }

            val descriptor = selectedVariant.descriptorToImport
            // for class or package we use ShortenReferences because we not necessary insert an import but may want to insert partly qualified name
            if (descriptor is ClassDescriptor || descriptor is PackageViewDescriptor) {
                element.mainReference.bindToFqName(descriptor.importableFqName!!, KtSimpleNameReference.ShorteningMode.FORCED_SHORTENING)
            }
            else {
                ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
            }
        }
    }

    private class Prioritizer(private val file: KtFile) {
        private val classifier = ImportableFqNameClassifier(file)
        private val proximityComparator = PsiProximityComparator(file)

        private inner class Priority(private val fqName: FqName, private val descriptor: DeclarationDescriptor) : Comparable<Priority> {
            private val isDeprecated = KotlinBuiltIns.isDeprecated(descriptor)
            private val classification = classifier.classify(fqName, false)
            private val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(file.project, descriptor)

            override fun compareTo(other: Priority): Int {
                if (isDeprecated != other.isDeprecated) {
                    return if (isDeprecated) +1 else -1
                }

                val c1 = classification.compareTo(other.classification)
                if (c1 != 0) return c1

                val c2 = proximityComparator.compare(declaration, other.declaration)
                if (c2 != 0) return c2

                return fqName.asString().compareTo(other.fqName.asString())
            }
        }

        fun priority(fqName: FqName, descriptor: DeclarationDescriptor) = Priority(fqName, descriptor)
    }
}
