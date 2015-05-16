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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedShortening
import org.jetbrains.kotlin.idea.conversion.copy.end
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.conversion.copy.start
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.JetMultiReference
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.util.ArrayList

//NOTE: this class is based on CopyPasteReferenceProcessor and JavaCopyPasteReferenceProcessor
public class KotlinCopyPasteReferenceProcessor() : CopyPastePostProcessor<KotlinReferenceTransferableData>() {
    private val LOG = Logger.getInstance(javaClass<KotlinCopyPasteReferenceProcessor>())

    private val IGNORE_REFERENCES_INSIDE: Array<Class<out JetElement>> = array(
            javaClass<JetImportList>(),
            javaClass<JetPackageDirective>()
    )

    override fun extractTransferableData(content: Transferable): List<KotlinReferenceTransferableData> {
        if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE != CodeInsightSettings.NO) {
            try {
                val flavor = KotlinReferenceData.dataFlavor ?: return listOf()
                val data = content.getTransferData(flavor) as? KotlinReferenceTransferableData ?: return listOf()
                // copy to prevent changing of original by convertLineSeparators
                return listOf(data.clone())
            }
            catch (ignored: UnsupportedFlavorException) {
            }
            catch (ignored: IOException) {
            }
        }

        return listOf()
    }

    override fun collectTransferableData(
            file: PsiFile,
            editor: Editor,
            startOffsets: IntArray,
            endOffsets: IntArray
    ): List<KotlinReferenceTransferableData> {
        if (file !is JetFile || DumbService.getInstance(file.getProject()).isDumb()) return listOf()

        val collectedData = try {
            collectReferenceData(file, startOffsets, endOffsets)
        }
        catch (e: ProcessCanceledException) {
            // supposedly analysis can only be canceled from another thread
            // do not log ProcessCanceledException as it is rethrown by IdeaLogger and code won't be copied
            LOG.error("ProcessCanceledException while analyzing references in ${file.getName()}. References can't be processed.")
            return listOf()
        }
        catch (e: Throwable) {
            LOG.error("Exception in processing references for copy paste in file ${file.getName()}}", e)
            return listOf()
        }

        if (collectedData.isEmpty()) return listOf()

        return listOf(KotlinReferenceTransferableData(collectedData.copyToArray()))
    }

    public fun collectReferenceData(
            file: JetFile,
            startOffsets: IntArray,
            endOffsets: IntArray
    ): List<KotlinReferenceData> {
        val result = ArrayList<KotlinReferenceData>()
        for (range in toTextRanges(startOffsets, endOffsets)) {
            for (element in file.elementsInRange(range)) {
                result.addReferenceDataInsideElement(element, file, range.start, startOffsets, endOffsets)
            }
        }
        return result
    }

    private fun MutableCollection<KotlinReferenceData>.addReferenceDataInsideElement(
            element: PsiElement,
            file: JetFile,
            startOffset: Int,
            startOffsets: IntArray,
            endOffsets: IntArray
    ) {
        if (PsiTreeUtil.getNonStrictParentOfType(element, *IGNORE_REFERENCES_INSIDE) != null) return

        element.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.javaClass in IGNORE_REFERENCES_INSIDE) return

                element.acceptChildren(this)

                val reference = element.getReference() as? JetReference ?: return

                val descriptors = reference.resolveToDescriptors((element as JetElement).analyze()) //TODO: we could use partial body resolve for all references together
                //check whether this reference is unambiguous
                if (reference !is JetMultiReference<*> && descriptors.size() > 1) return

                for (descriptor in descriptors) {
                    val declarations = DescriptorToSourceUtilsIde.getAllDeclarations(file.getProject(), descriptor)
                    val declaration = declarations.singleOrNull()
                    if (declaration != null && declaration.isInCopiedArea(file, startOffsets, endOffsets)) continue

                    if (!descriptor.isExtension) {
                        if (element !is JetNameReferenceExpression) continue
                        if (element.getIdentifier() == null) continue // skip 'this' etc
                        if (element.getReceiverExpression() != null) continue
                    }

                    val fqName = descriptor.importableFqName ?: continue
                    if (!descriptor.canBeReferencedViaImport()) continue

                    val kind = KotlinReferenceData.Kind.fromDescriptor(descriptor) ?: continue
                    add(KotlinReferenceData(element.range.start - startOffset, element.range.end - startOffset, fqName.asString(), kind))
                }
            }
        })
    }

    private data class ReferenceToRestoreData(
            val reference: JetReference,
            val refData: KotlinReferenceData
    )

    override fun processTransferableData(
            project: Project,
            editor: Editor,
            bounds: RangeMarker,
            caretOffset: Int,
            indented: Ref<Boolean>,
            values: List<KotlinReferenceTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb()) return

        val document = editor.getDocument()
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (file !is JetFile) return

        val referenceData = values.single().data

        processReferenceData(project, file, bounds.getStartOffset(), referenceData)
    }

    public fun processReferenceData(project: Project, file: JetFile, blockStart: Int, referenceData: Array<KotlinReferenceData>) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val referencesPossibleToRestore = findReferencesToRestore(file, blockStart, referenceData)

        val selectedReferencesToRestore = showRestoreReferencesDialog(project, referencesPossibleToRestore)
        if (selectedReferencesToRestore.isEmpty()) return

        runWriteAction {
            restoreReferences(selectedReferencesToRestore, file)
        }
    }

    private fun findReferencesToRestore(file: PsiFile, blockStart: Int, referenceData: Array<out KotlinReferenceData>): List<ReferenceToRestoreData> {
        if (file !is JetFile) return listOf()

        val fileResolutionScope = file.getResolutionFacade().getFileTopLevelScope(file)
        return referenceData.map {
            val referenceElement = findReference(it, file, blockStart)
            if (referenceElement != null)
                createReferenceToRestoreData(referenceElement, it, file, fileResolutionScope)
            else
                null
        }.filterNotNull()
    }

    private fun findReference(data: KotlinReferenceData, file: JetFile, blockStart: Int): JetElement? {
        val startOffset = data.startOffset + blockStart
        val endOffset = data.endOffset + blockStart
        val element = file.findElementAt(startOffset)
        val desiredRange = TextRange(startOffset, endOffset)
        var expression = element
        while (expression != null) {
            val range = expression!!.range
            if (range == desiredRange && expression!!.getReference() != null) {
                return expression as? JetElement
            }
            if (range in desiredRange) {
                expression = expression!!.getParent()
            }
            else {
                return null
            }
        }
        return null
    }

    private fun createReferenceToRestoreData(element: JetElement, refData: KotlinReferenceData, file: JetFile, fileResolutionScope: JetScope): ReferenceToRestoreData? {
        val originalFqName = FqName(refData.fqName)

        if (refData.kind == KotlinReferenceData.Kind.EXTENSION_FUNCTION) {
            if (fileResolutionScope.getFunctions(originalFqName.shortName()).any { it.importableFqName == originalFqName }) return null // already imported
        }
        else if (refData.kind == KotlinReferenceData.Kind.EXTENSION_PROPERTY) {
            if (fileResolutionScope.getProperties(originalFqName.shortName()).any { it.importableFqName == originalFqName }) return null // already imported
        }

        val reference = element.getReference() as? JetReference ?: return null
        val referencedDescriptors = try {
            reference.resolveToDescriptors(element.analyze()) //TODO: we could use partial body resolve for all references together
        }
        catch (e: Throwable) {
            LOG.error("Failed to analyze reference (${element.getText()}) after copy paste", e)
            return null
        }
        val referencedFqNames = referencedDescriptors
                .filterNot { ErrorUtils.isError(it) }
                .map { it.importableFqName }
                .filterNotNull()
                .toSet()
        if (referencedFqNames.singleOrNull() == originalFqName) return null

        // check that descriptor to import exists and is accessible from the current module
        if (findImportableDescriptors(originalFqName, file).none { KotlinReferenceData.Kind.fromDescriptor(it) == refData.kind }) {
            return null
        }

        return ReferenceToRestoreData(reference, refData)
    }

    private fun restoreReferences(referencesToRestore: Collection<ReferenceToRestoreData>, file: JetFile) {
        val importHelper = ImportInsertHelper.getInstance(file.getProject())
        val smartPointerManager = SmartPointerManager.getInstance(file.getProject())

        [data] class BindingRequest(
                val pointer: SmartPsiElementPointer<JetSimpleNameExpression>,
                val fqName: FqName
        )

        val bindingRequests = ArrayList<BindingRequest>()
        val extensionsToImport = ArrayList<CallableDescriptor>()
        for ((reference, refData) in referencesToRestore) {
            val fqName = FqName(refData.fqName)

            if (!refData.kind.isExtension() && reference is JetSimpleNameReference) {
                val pointer = smartPointerManager.createSmartPsiElementPointer(reference.getElement(), file)
                bindingRequests.add(BindingRequest(pointer, fqName))
            }

            if (refData.kind.isExtension()) {
                extensionsToImport.addIfNotNull(findCallableToImport(fqName, file))
            }
        }

        for (descriptor in extensionsToImport) {
            importHelper.importDescriptor(file, descriptor)
        }
        for ((pointer, fqName) in bindingRequests) {
            val reference = pointer.getElement().getReference() as JetSimpleNameReference
            reference.bindToFqName(fqName, JetSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)
        }
        performDelayedShortening(file.getProject())
    }

    private fun KotlinReferenceData.Kind.isExtension()
            = this == KotlinReferenceData.Kind.EXTENSION_FUNCTION || this == KotlinReferenceData.Kind.EXTENSION_PROPERTY

    private fun findImportableDescriptors(fqName: FqName, file: JetFile): Collection<DeclarationDescriptor> {
        return file.getResolutionFacade().resolveImportReference(file, fqName)
    }

    private fun findCallableToImport(fqName: FqName, file: JetFile): CallableDescriptor?
            = findImportableDescriptors(fqName, file).firstIsInstanceOrNull<CallableDescriptor>()

    private fun showRestoreReferencesDialog(project: Project, referencesToRestore: List<ReferenceToRestoreData>): Collection<ReferenceToRestoreData> {
        val fqNames = referencesToRestore.map { it.refData.fqName }.toSortedSet()

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            declarationsToImportSuggested = fqNames
        }

        val shouldShowDialog = CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.ASK
        if (!shouldShowDialog || referencesToRestore.isEmpty()) {
            return referencesToRestore
        }

        val dialog = RestoreReferencesDialog(project, fqNames.copyToArray())
        dialog.show()

        val selectedFqNames = dialog.getSelectedElements()!!.toSet()
        return referencesToRestore.filter { selectedFqNames.contains(it.refData.fqName) }
    }

    private fun toTextRanges(startOffsets: IntArray, endOffsets: IntArray): List<TextRange> {
        assert(startOffsets.size() == endOffsets.size())
        return startOffsets.indices.map { TextRange(startOffsets[it], endOffsets[it]) }
    }

    private fun PsiElement.isInCopiedArea(fileCopiedFrom: JetFile, startOffsets: IntArray, endOffsets: IntArray): Boolean {
        if (getContainingFile() != fileCopiedFrom) return false
        return toTextRanges(startOffsets, endOffsets).any { this.range in it }
    }

    companion object {
        [TestOnly]
        public var declarationsToImportSuggested: Collection<String> = emptyList()
    }
}
