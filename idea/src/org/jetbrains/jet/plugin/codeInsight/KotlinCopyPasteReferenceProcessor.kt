/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight

import com.intellij.psi.PsiFile
import com.intellij.psi.PsiElement
import com.intellij.codeInsight.editorActions.ReferenceData
import java.util.ArrayList
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.Editor
import com.intellij.codeInsight.editorActions.ReferenceTransferableData
import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiDocumentManager
import com.intellij.openapi.application.ApplicationManager
import java.awt.datatransfer.Transferable
import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import com.intellij.openapi.util.TextRange
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.plugin.references.JetMultiReference
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.lang.psi.JetSuperExpression
import org.jetbrains.jet.plugin.conversion.copy.*
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils
import org.jetbrains.jet.plugin.imports.*
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.utils.*

//NOTE: this class is based on CopyPasteReferenceProcessor and JavaCopyPasteReferenceProcessor
public class KotlinCopyPasteReferenceProcessor() : CopyPastePostProcessor<ReferenceTransferableData>() {

    override fun extractTransferableData(content: Transferable): List<ReferenceTransferableData> {
        if (CodeInsightSettings.getInstance()!!.ADD_IMPORTS_ON_PASTE != CodeInsightSettings.NO) {
            try {
                val flavor = ReferenceData.getDataFlavor()
                if (flavor != null) {
                    val referenceData = content.getTransferData(flavor) as? ReferenceTransferableData
                    if (referenceData != null) {
                        // copy to prevent changing of original by convertLineSeparators
                        return listOf(referenceData.clone())
                    }
                }
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
    ): List<ReferenceTransferableData> {
        if (file !is JetFile) {
            return listOf()
        }

        val collectedData = try {
            zip(startOffsets, endOffsets).toList().flatMap {
                val (startOffset, endOffset) = it
                CollectHighlightsUtil.getElementsInRange(file, startOffset, endOffset).flatMap { element ->
                    collectReferenceDataFromElement(element, file, startOffset, startOffsets, endOffsets)
                }
            }
        }
        catch (e: Throwable) {
            LOG.error("Exception in processing references for copy paste in file ${file.getName()}}", e)
            return listOf()
        }

        if (collectedData.isEmpty()) {
            return listOf()
        }

        return listOf(ReferenceTransferableData(collectedData.copyToArray()))
    }

    private fun collectReferenceDataFromElement(
            element: PsiElement,
            file: JetFile,
            startOffset: Int,
            startOffsets: IntArray,
            endOffsets: IntArray
    ): Collection<ReferenceData> {

        fun collectReferenceData(referencedDeclaration: PsiElement, referencedDescriptor: DeclarationDescriptor): ReferenceData? {
            if (referencedDeclaration.isInCopiedArea(file, startOffsets, endOffsets)) {
                return null
            }
            if (isInReceiverScope(element, referencedDescriptor)) {
                return null
            }
            val fqName = referencedDescriptor.importableFqName
            if (fqName != null && referencedDescriptor.canBeReferencedViaImport()) {
                return createReferenceData(element, startOffset, fqName)
            }
            return null
        }


        val isInsideIgnoredElement = PsiTreeUtil.getParentOfType(element, *IGNORE_REFERENCES_INSIDE) != null
        if (isInsideIgnoredElement) {
            return Collections.emptyList()
        }
        val reference = element.getReference()
        if (reference !is JetReference) {
            return Collections.emptyList()
        }

        val resolveMap = reference.resolveMap()
        //check whether this reference is unambiguous
        if (reference !is JetMultiReference<*> && resolveMap.size > 1) {
            return Collections.emptyList()
        }
        val collectedData = ArrayList<ReferenceData>()
        for ((referencedDescriptor, declarations) in resolveMap) {
            if (declarations.size != 1) {
                continue
            }
            collectedData.addIfNotNull(collectReferenceData(declarations.first(), referencedDescriptor))
        }
        return collectedData
    }

    private fun createReferenceData(element: PsiElement, startOffset: Int, fqName: FqName): ReferenceData {
        val range = element.range
        return ReferenceData(range.start - startOffset, range.end - startOffset, fqName.asString(), null)
    }

    private data class ReferenceToRestoreData(
            val expression: JetElement,
            val fqName: FqName,
            val lengthenReference: Boolean = false
    )

    override fun processTransferableData (
            project: Project,
            editor: Editor,
            bounds: RangeMarker,
            caretOffset: Int,
            indented: Ref<Boolean>,
            values: List<ReferenceTransferableData>
    ) {
        if (DumbService.getInstance(project)!!.isDumb()) {
            return
        }
        val document = editor.getDocument()
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (file !is JetFile) {
            return
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assert(values.size() == 1)

        val referenceData = values.first().getData()!!
        val referencesPossibleToRestore = findReferencesToRestore(file, bounds, referenceData)

        val selectedReferencesToRestore = showRestoreReferencesDialog(project, referencesPossibleToRestore)
        if (selectedReferencesToRestore.isEmpty()) {
            return
        }
        ApplicationManager.getApplication()!!.runWriteAction(Runnable {
            restoreReferences(selectedReferencesToRestore, file)
        })
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun findReferencesToRestore(file: PsiFile, bounds: RangeMarker, referenceData: Array<out ReferenceData>): List<ReferenceToRestoreData> {
        if (file !is JetFile) {
            return Collections.emptyList()
        }
        return referenceData.map {
            if (ImportInsertHelper.needImport(it.fqName, file)) {
                val referenceExpression = findReference(it, file, bounds)
                if (referenceExpression != null) createReferenceToRestoreData(referenceExpression, it.fqName) else null
            }
            else null
        }.filterNotNull()
    }

    private fun findReference(data: ReferenceData, file: JetFile, bounds: RangeMarker): JetElement? {
        val startOffset = data.startOffset + bounds.getStartOffset()
        val endOffset = data.endOffset + bounds.getStartOffset()
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

    private fun createReferenceToRestoreData(expression: JetElement, originalReferencedFqName: FqName): ReferenceToRestoreData? {
        val reference = expression.getReference() as? JetReference
        if (reference == null) {
            return null
        }
        val referencedDescriptors = try {
            reference.resolveToDescriptors()
        }
        catch (e: Throwable) {
            LOG.error("Failed to analyze reference (${expression.getText()}) after copy paste", e)
            return null
        }
        val referencedFqNames = referencedDescriptors.filterNot { ErrorUtils.isError(it) } .map { it.importableFqName }
        val referencesSame = referencedFqNames any { it == originalReferencedFqName }
        val conflict = referencedFqNames any { it != originalReferencedFqName && (it?.shortName() == originalReferencedFqName.shortName()) }
        when {
            referencesSame && !conflict -> {
                return null
            }
            !referencesSame && !conflict -> {
                return ReferenceToRestoreData(expression, originalReferencedFqName)
            }
            conflict -> {
                val mustBeReferencedWithReceiver = referencedDescriptors.any { it.isExtension }
                if (!mustBeReferencedWithReceiver && LengthenReferences.canLengthenReferenceExpression(expression, originalReferencedFqName)) {
                    return ReferenceToRestoreData(expression, originalReferencedFqName, lengthenReference = true)
                }
            }
        }
        return null
    }

    private fun restoreReferences(referencesToRestore: Collection<ReferenceToRestoreData>, file: JetFile) {
        for ((referenceExpression, fqName, shouldLengthen) in referencesToRestore) {
            if (!shouldLengthen) {
                ImportInsertHelper.addImportDirectiveIfNeeded(fqName, file)
            }
            else {
                //TODO: try to shorten reference after (sometimes is possible), need shorten reference to support all relevant cases
                LengthenReferences.lengthenReference(referenceExpression, fqName)
            }
        }
    }

    private fun showRestoreReferencesDialog(project: Project, referencesToRestore: List<ReferenceToRestoreData>): Collection<ReferenceToRestoreData> {
        val shouldShowDialog = CodeInsightSettings.getInstance()!!.ADD_IMPORTS_ON_PASTE == CodeInsightSettings.ASK
        if (!shouldShowDialog || referencesToRestore.isEmpty()) {
            return referencesToRestore
        }
        val fqNames = referencesToRestore. map { it.fqName.asString() }.toSortedSet()
        val dialog = RestoreReferencesDialog(project, fqNames.copyToArray())
        dialog.show()
        val selectedFqNames = dialog.getSelectedElements()!!.toSet()
        return referencesToRestore.filter { ref -> selectedFqNames.contains(ref.fqName.asString()) }
    }

    class object {
        private val LOG = Logger.getInstance(javaClass<KotlinCopyPasteReferenceProcessor>())

        private val IGNORE_REFERENCES_INSIDE: Array<Class<out JetElement>?> = array(
                javaClass<JetImportDirective>(),
                javaClass<JetPackageDirective>(),
                javaClass<JetSuperExpression>(),
                javaClass<JetThisExpression>()
        )
    }

    private object LengthenReferences {

        private fun createQualifiedExpression(psiFactory: JetPsiFactory, text: String): JetDotQualifiedExpression {
            val newExpression = psiFactory.createExpression(text)
            LOG.assertTrue(newExpression is JetDotQualifiedExpression,
                           "\"${newExpression.getText()}\" is ${newExpression.javaClass}," +
                           "not ${javaClass<JetDotQualifiedExpression>().getSimpleName()}."
            )
            return newExpression as JetDotQualifiedExpression
        }

        fun lengthenReference(expression: JetElement, fqName: FqName) {
            assert(canLengthenReferenceExpression(expression, fqName))
            val project = expression.getProject()
            val parent = expression.getParent()
            val prefixToInsert = fqName.parent().asString()
            val psiFactory = JetPsiFactory(expression)
            if (parent is JetCallExpression) {
                val text = "$prefixToInsert.${parent.getText()}"
                parent.replace(createQualifiedExpression(psiFactory, text))
            }
            else if (parent is JetUserType) {
                val typeReference = PsiTreeUtil.getParentOfType(expression, javaClass<JetTypeReference>())
                LOG.assertTrue(typeReference != null, "JetUserType is expected to have parent of type JetTypeReference:\n" +
                    "At: ${DiagnosticUtils.atLocation(expression)}\nFILE:\n${expression.getContainingFile()!!.getText()}")
                typeReference!!.replace(psiFactory.createType("$prefixToInsert.${typeReference.getText()}"))
            }
            else {
                expression.replace(createQualifiedExpression(psiFactory, fqName.asString()))
            }
        }

        fun canLengthenReferenceExpression(expression: JetElement, fqName: FqName): Boolean {
            if (fqName.pathSegments().size() < 2) {
                return false
            }
            if (expression is JetSimpleNameExpression && expression.getReceiverExpression() == null) {
                return true
            }
            return false
        }
    }
}

private val ReferenceData.fqName: FqName
    get() = FqName(qClassName!!)

private fun zip(first: IntArray, second: IntArray): Iterable<Pair<Int, Int>> {
    assert(first.size == second.size)
    return first.zip(second.toList())
}

private fun PsiElement.isInCopiedArea(fileCopiedFrom: JetFile, startOffsets: IntArray, endOffsets: IntArray): Boolean {
    if (getContainingFile() != fileCopiedFrom) {
        return false
    }
    return zip(startOffsets, endOffsets).any {
        val (start, end) = it
        range in TextRange(start, end)
    }
}