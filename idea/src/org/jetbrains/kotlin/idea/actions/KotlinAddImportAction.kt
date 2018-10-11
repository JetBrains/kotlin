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
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.statistics.StatisticsManager
import com.intellij.psi.util.ProximityLocation
import com.intellij.psi.util.proximity.PsiProximityComparator
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinDescriptorIconProvider
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.ImportableFqNameClassifier
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal fun createSingleImportAction(
        project: Project,
        editor: Editor,
        element: KtElement,
        fqNames: Collection<FqName>
): KotlinAddImportAction {
    val file = element.containingKtFile
    val prioritizer = Prioritizer(element.containingKtFile)
    val variants = fqNames.mapNotNull { fqName ->
        val sameFqNameDescriptors = file.resolveImportReference(fqName)
        val priority = sameFqNameDescriptors.map { prioritizer.priority(it) }.min() ?: return@mapNotNull null
        Prioritizer.VariantWithPriority(SingleImportVariant(fqName, sameFqNameDescriptors), priority)
    }.sortedBy { it.priority }.map { it.variant }

    return KotlinAddImportAction(project, editor, element, variants)
}

internal fun createSingleImportActionForConstructor(
        project: Project,
        editor: Editor,
        element: KtElement,
        fqNames: Collection<FqName>
): KotlinAddImportAction {
    val file = element.containingKtFile
    val prioritizer = Prioritizer(element.containingKtFile)
    val variants = fqNames.mapNotNull { fqName ->
        val sameFqNameDescriptors = file.resolveImportReference(fqName.parent())
                .filterIsInstance<ClassDescriptor>()
                .flatMap { it.constructors }

        val priority = sameFqNameDescriptors.asSequence().map { prioritizer.priority(it) }.min() ?: return@mapNotNull null
        Prioritizer.VariantWithPriority(SingleImportVariant(fqName, sameFqNameDescriptors), priority)
    }.sortedBy { it.priority }.map { it.variant }
    return KotlinAddImportAction(project, editor, element, variants)
}

internal fun createGroupedImportsAction(
        project: Project,
        editor: Editor,
        element: KtElement,
        autoImportDescription: String,
        fqNames: Collection<FqName>
): KotlinAddImportAction {
    val prioritizer = DescriptorGroupPrioritizer(element.containingKtFile)

    val file = element.containingKtFile
    val variants = fqNames
            .groupBy { it.parentOrNull() ?: FqName.ROOT }
            .map {
                val samePackageFqNames = it.value
                val descriptors = samePackageFqNames.flatMap { file.resolveImportReference(it) }
                val variant = if (samePackageFqNames.size > 1) {
                    GroupedImportVariant(autoImportDescription, descriptors)
                }
                else {
                    SingleImportVariant(samePackageFqNames.first(), descriptors)
                }

                val priority = prioritizer.priority(descriptors)
                DescriptorGroupPrioritizer.VariantWithPriority(variant, priority)
            }
            .sortedBy {
                it.priority
            }
            .map { it.variant }

    return KotlinAddImportAction(project, editor, element, variants)
}

/**
 * Automatically adds import directive to the file for resolving reference.
 * Based on {@link AddImportAction}
 */
class KotlinAddImportAction internal constructor(
        private val project: Project,
        private val editor: Editor,
        private val element: KtElement,
        private val variants: List<AutoImportVariant>) : QuestionAction {
    fun showHint(): Boolean {
        if (variants.isEmpty()) return false

        val hintText = ShowAutoImportPass.getMessage(variants.size > 1, variants.first().hint)
        HintManager.getInstance().showQuestionHint(editor, hintText, element.textOffset, element.textRange!!.endOffset, this)

        return true
    }

    fun isUnambiguous(): Boolean {
        return variants.size == 1
    }

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        if (!element.isValid) return false
        if (variants.isEmpty()) return false

        if (variants.size == 1 || ApplicationManager.getApplication().isUnitTestMode) {
            addImport(variants.first())
            return true
        }

        JBPopupFactory.getInstance().createListPopup(getVariantSelectionPopup()).showInBestPositionFor(editor)
        return true
    }

    private fun getVariantSelectionPopup(): BaseListPopupStep<AutoImportVariant> {
        return object : BaseListPopupStep<AutoImportVariant>(KotlinBundle.message("imports.chooser.title"), variants) {
            override fun isAutoSelectionEnabled() = false

            override fun isSpeedSearchEnabled() = true

            override fun onChosen(selectedValue: AutoImportVariant?, finalChoice: Boolean): PopupStep<String>? {
                if (selectedValue == null || project.isDisposed) return null

                if (finalChoice) {
                    addImport(selectedValue)
                    return null
                }

                val toExclude = AddImportAction.getAllExcludableStrings(selectedValue.excludeFqNameCheck.asString())

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

            override fun hasSubstep(selectedValue: AutoImportVariant?) = true
            override fun getTextFor(value: AutoImportVariant) = value.hint
            override fun getIconFor(value: AutoImportVariant) = value.icon(project)
        }
    }

    private fun addImport(variant: AutoImportVariant) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        project.executeWriteCommand(QuickFixBundle.message("add.import")) {
            if (!element.isValid) return@executeWriteCommand

            val file = element.containingKtFile

            variant.declarationToImport(project)?.let {
                val location = ProximityLocation(element, ModuleUtilCore.findModuleForPsiElement(element))
                StatisticsManager.getInstance().incUseCount(PsiProximityComparator.STATISTICS_KEY, it, location)
            }

            for (descriptor in variant.descriptorsToImport) {
                // for class or package we use ShortenReferences because we not necessary insert an import but may want to
                // insert partly qualified name
                if (descriptor is ClassDescriptor || descriptor is PackageViewDescriptor) {
                    if (element is KtSimpleNameExpression) {
                        element.mainReference.bindToFqName(descriptor.importableFqName!!, KtSimpleNameReference.ShorteningMode.FORCED_SHORTENING)
                    }
                } else {
                    ImportInsertHelper.getInstance(project).importDescriptor(file, descriptor)
                }
            }
        }
    }
}

private class Prioritizer(private val file: KtFile, private val compareNames: Boolean = true) {
    private val classifier = ImportableFqNameClassifier(file)
    private val proximityComparator = PsiProximityComparator(file)

    inner class Priority(descriptor: DeclarationDescriptor) : Comparable<Priority> {
        private val isDeprecated = KotlinBuiltIns.isDeprecated(descriptor)
        private val fqName = descriptor.importableFqName!!
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

            if (compareNames) {
                return fqName.asString().compareTo(other.fqName.asString())
            }

            return 0
        }
    }

    fun priority(descriptor: DeclarationDescriptor) = Priority(descriptor)

    data class VariantWithPriority(val variant: AutoImportVariant, val priority: Priority)
}

private class DescriptorGroupPrioritizer(file: KtFile) {
    private val prioritizer = Prioritizer(file, false)

    inner class Priority(val descriptors: List<DeclarationDescriptor>) : Comparable<Priority> {
        val ownDescriptorsPriority = descriptors.asSequence().map { prioritizer.priority(it) }.max()!!

        override fun compareTo(other: Priority): Int {
            val c1 = ownDescriptorsPriority.compareTo(other.ownDescriptorsPriority)
            if (c1 != 0) return c1

            return other.descriptors.size - descriptors.size
        }
    }

    fun priority(descriptors: List<DeclarationDescriptor>) = Priority(descriptors)

    data class VariantWithPriority(val variant: AutoImportVariant, val priority: Priority)
}

internal interface AutoImportVariant {
    val descriptorsToImport: Collection<DeclarationDescriptor>
    val hint: String
    val excludeFqNameCheck: FqName

    fun icon(project: Project) = KotlinDescriptorIconProvider.getIcon(descriptorsToImport.first(), declarationToImport(project), 0)

    fun declarationToImport(project: Project): PsiElement? =
            DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptorsToImport.first())
}

private class GroupedImportVariant(val autoImportDescription: String, val descriptors: Collection<DeclarationDescriptor>) : AutoImportVariant {
    override val excludeFqNameCheck: FqName = descriptors.first().importableFqName!!.parent()
    override val descriptorsToImport: Collection<DeclarationDescriptor> get() = descriptors
    override val hint: String get() = "$autoImportDescription from $excludeFqNameCheck"
}

private class SingleImportVariant(
        override val excludeFqNameCheck: FqName,
        val descriptors: Collection<DeclarationDescriptor>
) : AutoImportVariant {
    override val descriptorsToImport: Collection<DeclarationDescriptor> get() =
            listOf(descriptors.singleOrNull() ?: descriptors.sortedBy { if (it is ClassDescriptor) 0 else 1 }.first())

    override val hint: String get() = excludeFqNameCheck.asString()
}
