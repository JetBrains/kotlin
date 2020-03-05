/*
 * Copyright 2010-2020 JetBrains s.r.o.
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
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveInDispatchThread
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.ReviewAddedImports.reviewAddedImports
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.util.end
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.core.util.start
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.idea.util.getSourceRoot
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.psi.api.KDocElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.findFunction
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.util.*
import java.util.function.Consumer

class KotlinCopyPasteReferenceProcessor : CopyPastePostProcessor<BasicKotlinReferenceTransferableData>() {

    override fun extractTransferableData(content: Transferable): List<BasicKotlinReferenceTransferableData> {
        if (CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE != CodeInsightSettings.NO) {
            try {
                val flavor = KotlinReferenceData.dataFlavor ?: return listOf()
                val data = content.getTransferData(flavor) as? BasicKotlinReferenceTransferableData ?: return listOf()
                // copy to prevent changing of original by convertLineSeparators
                return listOf(data.clone())
            } catch (ignored: UnsupportedFlavorException) {
            } catch (ignored: IOException) {
            }
        }

        return listOf()
    }

    override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<BasicKotlinReferenceTransferableData> {
        if (file !is KtFile || DumbService.getInstance(file.getProject()).isDumb) return listOf()

        check(startOffsets.size == endOffsets.size) {
            "startOffsets ${startOffsets.contentToString()} has to have same size as endOffsets ${endOffsets.contentToString()}"
        }

        val packageName = file.packageDirective?.fqName?.asString() ?: ""
        val imports = file.importDirectives.map { it.text }
        val endOfImportsOffset = file.importDirectives.map { it.endOffset }.max() ?: file.packageDirective?.endOffset ?: 0
        val offsetDelta = if (startOffsets.any { it <= endOfImportsOffset }) 0 else endOfImportsOffset
        val text = file.text.substring(offsetDelta)
        val ranges = startOffsets.indices.map { TextRange(startOffsets[it], endOffsets[it]) }

        return listOf(
            BasicKotlinReferenceTransferableData(
                sourceFileUrl = file.virtualFile.url,
                packageName = packageName,
                imports = imports,
                sourceTextOffset = offsetDelta,
                sourceText = text,
                textRanges = ranges
            )
        )
    }

    private fun collectReferenceData(
        textRanges: List<TextRange>,
        file: KtFile,
        targetFile: KtFile,
        fakePackageName: String,
        sourcePackageName: String
    ): List<KotlinReferenceData> =
        collectReferenceData(
            file,
            textRanges.map { it.startOffset }.toIntArray(),
            textRanges.map { it.endOffset }.toIntArray(),
            fakePackageName,
            sourcePackageName,
            targetFile.packageDirective?.fqName?.asString() ?: ""
        )

    fun collectReferenceData(
        file: KtFile,
        startOffsets: IntArray,
        endOffsets: IntArray,
        fakePackageName: String? = null,
        sourcePackageName: String? = null,
        targetPackageName: String? = null
    ): List<KotlinReferenceData> {
        val ranges = toTextRanges(startOffsets, endOffsets)
        val elementsByRange = ranges.associateBy({ it }, { textRange ->
            file.elementsInRange(textRange).filter { it is KtElement || it is KDocElement }
        })

        val allElementsToResolve = elementsByRange.values.flatten().flatMap { it.collectDescendantsOfType<KtElement>() }
        // TODO: allowResolveInDispatchThread could be dropped as soon as
        //  ConvertJavaCopyPasteProcessor will perform it on non UI thread
        val bindingContext =
            allowResolveInDispatchThread {
                file.getResolutionFacade().analyze(allElementsToResolve, BodyResolveMode.PARTIAL)
            }

        val result = mutableListOf<KotlinReferenceData>()
        for ((range, elements) in elementsByRange) {
            elements.forEachDescendant { ktElement ->
                result.addReferenceDataInsideElement(
                    ktElement, file, ranges, bindingContext, fakePackageName = fakePackageName,
                    sourcePackageName = sourcePackageName, targetPackageName = targetPackageName
                )
            }
        }
        return result
    }

    private fun List<PsiElement>.forEachDescendant(consumer: (KtElement) -> Unit) {
        this.forEach { it.forEachDescendant(consumer) }
    }

    private fun PsiElement.forEachDescendant(consumer: (KtElement) -> Unit) {
        if (PsiTreeUtil.getNonStrictParentOfType(this, *IGNORE_REFERENCES_INSIDE) != null) return

        this.forEachDescendantOfType<KtElement>(canGoInside = {
            it::class.java as Class<*> !in IGNORE_REFERENCES_INSIDE
        }) { ktElement ->
            consumer(ktElement)
        }
    }

    private fun MutableCollection<KotlinReferenceData>.addReferenceDataInsideElement(
        ktElement: KtElement,
        file: KtFile,
        textRanges: List<TextRange>,
        bindingContext: BindingContext,
        fakePackageName: String? = null,
        sourcePackageName: String? = null,
        targetPackageName: String? = null
    ) {
        val reference = ktElement.mainReference ?: return

        val descriptors = resolveReference(reference, bindingContext)
        //check whether this reference is unambiguous
        if (reference !is KtMultiReference<*> && descriptors.size > 1) return

        for (descriptor in descriptors) {
            val effectiveReferencedDescriptors =
                DescriptorToSourceUtils.getEffectiveReferencedDescriptors(descriptor).asSequence()
            val declaration = effectiveReferencedDescriptors
                .map { DescriptorToSourceUtils.getSourceFromDescriptor(it) }
                .singleOrNull()

            if (declaration?.isInCopiedArea(file, textRanges) == true ||
                !reference.canBeResolvedViaImport(descriptor, bindingContext)
            ) continue

            val importableFqName = descriptor.importableFqName ?: continue
            val importableName = importableFqName.asString()
            val pkgName = descriptor.findPackageFqNameSafe()?.asString() ?: ""
            val importableShortName = importableFqName.shortName().asString()

            val fqName = if (fakePackageName == pkgName) {
                // It is possible to resolve unnecessary references from a target package (as we resolve it from a fake package)
                if (sourcePackageName == targetPackageName && importableName == "$fakePackageName.$importableShortName") {
                    continue
                }
                // roll back to original package name when we faced faked pkg name
                sourcePackageName + importableName.substring(fakePackageName.length)
            } else importableName

            val kind = KotlinReferenceData.Kind.fromDescriptor(descriptor) ?: continue
            val isQualifiable = KotlinReferenceData.isQualifiable(ktElement, descriptor)
            val relativeStart = ktElement.range.start
            val relativeEnd = ktElement.range.end
            add(KotlinReferenceData(relativeStart, relativeEnd, fqName, isQualifiable, kind))
        }
    }

    private data class ReferenceToRestoreData(val reference: KtReference, val refData: KotlinReferenceData)
    private data class PsiElementByTextRange(val originalTextRange: TextRange, val element: PsiElement)

    override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<Boolean>,
        values: List<BasicKotlinReferenceTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb ||
            CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.NO
        ) return

        val document = editor.document
        val file = PsiDocumentManager.getInstance(project).getPsiFile(document)
        if (file !is KtFile) return

        processReferenceData(project, editor, file, bounds.startOffset, values.single())
    }

    private fun processReferenceData(
        project: Project,
        editor: Editor,
        file: KtFile,
        blockStart: Int,
        transferableData: BasicKotlinReferenceTransferableData
    ) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        var startOffsetDelta = blockStart

        // figure out candidate elements in UI thread in paste phase
        // as psi elements could be changed later on - relative offsets are used for mapping purposes
        val elementsByRange = transferableData.textRanges.flatMap { originalTextRange ->
            val textRange = TextRange(
                startOffsetDelta,
                startOffsetDelta + originalTextRange.endOffset - originalTextRange.startOffset
            )
            startOffsetDelta = startOffsetDelta + originalTextRange.endOffset - originalTextRange.startOffset + 1
            file.elementsInRange(textRange)
                .filter { it is KtElement || it is KDocElement }
                .map {
                    val range = TextRange(
                        originalTextRange.startOffset + it.textRange.startOffset - textRange.startOffset,
                        originalTextRange.startOffset + it.textRange.endOffset - textRange.startOffset
                    )
                    PsiElementByTextRange(range, it)
                }
        }

        processReferenceData(project, editor, file) { indicator: ProgressIndicator ->
            findReferenceDataToRestore(
                file,
                indicator,
                elementsByRange,
                transferableData
            )
        }
    }

    fun processReferenceData(project: Project, editor: Editor, file: KtFile, blockStart: Int, referenceData: Array<KotlinReferenceData>) {
        processReferenceData(project, editor, file) { indicator: ProgressIndicator ->
            findReferencesToRestore(file, blockStart, referenceData)
        }
    }

    private fun processReferenceData(
        project: Project,
        editor: Editor,
        file: KtFile,
        findReferenceProvider: (indicator: ProgressIndicator) -> List<ReferenceToRestoreData>
    ) {
        val task: Task.Backgroundable = object : Task.Backgroundable(project, "Resolve pasted references", true) {
            override fun run(indicator: ProgressIndicator) {
                assert(!ApplicationManager.getApplication().isWriteAccessAllowed) {
                    "Resolving references on dispatch thread leads to live lock"
                }
                ProgressIndicatorUtils.awaitWithCheckCanceled(
                    nonBlocking<List<ReferenceToRestoreData>> {
                        return@nonBlocking findReferenceProvider(indicator)
                    }
                        .finishOnUiThread(
                            ModalityState.defaultModalityState(),
                            Consumer<List<ReferenceToRestoreData>> { referencesPossibleToRestore ->
                                applyResolvedImports(project, referencesPossibleToRestore, file, editor)
                            }
                        )
                        .withDocumentsCommitted(project)
                        .submit(AppExecutorUtil.getAppExecutorService())
                )
            }
        }
        ProgressManager.getInstance().run(task)
    }

    private fun applyResolvedImports(
        project: Project,
        referencesPossibleToRestore: List<ReferenceToRestoreData>,
        file: KtFile,
        editor: Editor
    ) {
        val selectedReferencesToRestore =
            showRestoreReferencesDialog(project, referencesPossibleToRestore)
        if (selectedReferencesToRestore.isEmpty()) return

        project.executeWriteCommand("resolve pasted references") {
            val imported = TreeSet<String>()
            restoreReferences(selectedReferencesToRestore, file, imported)

            reviewAddedImports(project, editor, file, imported)
        }
    }

    private fun findReferenceDataToRestore(
        file: PsiFile,
        indicator: ProgressIndicator,
        elementsByRange: List<PsiElementByTextRange>,
        transferableData: BasicKotlinReferenceTransferableData
    ): List<ReferenceToRestoreData> {
        if (file !is KtFile) return emptyList()

        val referencesByRange = elementsByRange.flatMap { elementByTextRange ->
            val element = elementByTextRange.element
            if (!element.isValid) return@flatMap emptyList<Pair<TextRange, KtReference>>()
            val refElementsRanges = mutableListOf<Pair<TextRange, KtReference>>()
            val offsetDelta = elementByTextRange.originalTextRange.startOffset - element.textRange.startOffset
            element.forEachDescendant { ktElement ->
                indicator.checkCanceled()

                ktElement.mainReference?.let { ref ->
                    val textRange = ref.element.textRange
                    findReference(file, textRange)?.let {
                        // remap reference to the original (as it was on paste phase) text range
                        val range = TextRange(textRange.startOffset + offsetDelta, textRange.endOffset + offsetDelta)
                        refElementsRanges.add(range to it)
                    }
                }
            }
            refElementsRanges
        }.toMap()

        val sourcePackageName = transferableData.packageName
        val imports: List<String> = transferableData.imports

        // Step 0. Recreate original source file (i.e. source file as it was on copy action) and resolve references from it
        val ctxFile = sourceFile(file.project, transferableData) ?: file

        // put original source file to some fake package to avoid ambiguous resolution ( a real file VS a virtual file )
        val fakePackageName = "__kotlin.__some.__funky.__package"

        val dummyOrigFileProlog =
            """
            package $fakePackageName
            
            ${buildDummySourceScope(sourcePackageName, imports, fakePackageName, file, transferableData, ctxFile)}
             
            """.trimIndent()

        val dummyOriginalFile = KtPsiFactory(file.project)
            .createAnalyzableFile(
                "dummy-original.kt",
                "$dummyOrigFileProlog${transferableData.sourceText}",
                ctxFile
            )

        val offsetDelta = dummyOrigFileProlog.length - transferableData.sourceTextOffset

        val dummyOriginalFileTextRanges =
            // it is required as it is shifted by dummy prolog
            transferableData.textRanges.map { TextRange(it.startOffset + offsetDelta, it.endOffset + offsetDelta) }

        // Step 1. Find references in copied blocks of (recreated) source file
        val sourceFileBasedReferences =
            collectReferenceData(dummyOriginalFileTextRanges, dummyOriginalFile, file, fakePackageName, sourcePackageName).map {
                it.copy(startOffset = it.startOffset - offsetDelta, endOffset = it.endOffset - offsetDelta)
            }

        indicator.checkCanceled()

        // Step 2. Find references to restore in a target file
        return findReferencesToRestore(file, sourceFileBasedReferences, referencesByRange)
    }

    private fun buildDummySourceScope(
        sourcePkgName: String,
        imports: List<String>,
        fakePkgName: String,
        file: KtFile,
        transferableData: BasicKotlinReferenceTransferableData,
        ctxFile: KtFile
    ): String {
        // it could be that copied block contains inner classes or enums
        // to solve this problem a special file is build:
        // it contains imports from source package replaced with a fake package prefix
        //
        // result scope has to contain
        // - those fake package imports those are successfully resolved (i.e. present in copied block)
        // - those source package imports those are not present in a fake package
        // - all rest imports

        val sourceImportPrefix = "import $sourcePkgName"
        val fakeImportPrefix = "import $fakePkgName"

        val affectedSourcePkgImports = imports.filter { it.startsWith(sourceImportPrefix) }
        val fakePkgImports = affectedSourcePkgImports.map { fakeImportPrefix + it.substring(sourceImportPrefix.length) }

        fun joinLines(items: Collection<String>) = items.joinToString("\n")

        val dummyImportsFile = KtPsiFactory(file.project)
            .createAnalyzableFile(
                "dummy-imports.kt",
                "package $fakePkgName\n" +
                        "${joinLines(fakePkgImports)}\n" +
                        transferableData.sourceText,
                ctxFile
            )

        val dummyFileImports = dummyImportsFile.collectDescendantsOfType<KtImportDirective>().mapNotNull { directive ->
            val importedReference =
                directive.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve() as? KtNamedDeclaration
            importedReference?.let { directive.text }
        }

        val dummyFileImportsSet = dummyFileImports.toSet()
        val filteredImports = imports.filter {
            !it.startsWith(sourceImportPrefix) ||
                    !dummyFileImportsSet.contains(fakeImportPrefix + it.substring(sourceImportPrefix.length))
        }

        return """
            ${joinLines(dummyFileImports)}
            import ${sourcePkgName}.*
            ${joinLines(filteredImports)}
        """
    }

    private fun sourceFile(project: Project, transferableData: BasicKotlinReferenceTransferableData): KtFile? {
        val sourceFile = VirtualFileManager.getInstance().findFileByUrl(transferableData.sourceFileUrl) ?: return null
        if (sourceFile.getSourceRoot(project) == null) return null

        return PsiManager.getInstance(project).findFile(sourceFile) as? KtFile
    }

    private fun filterReferenceData(
        refData: KotlinReferenceData,
        fileResolutionScope: LexicalScope
    ): Boolean {
        if (refData.isQualifiable) return true

        val originalFqName = FqName(refData.fqName)
        val name = originalFqName.shortName()
        return when (refData.kind) {
            // filter if function is already imported
            KotlinReferenceData.Kind.FUNCTION -> fileResolutionScope
                .findFunction(name, NoLookupLocation.FROM_IDE) { it.importableFqName == originalFqName } == null
            // filter if property is already imported
            KotlinReferenceData.Kind.PROPERTY -> fileResolutionScope
                .findVariable(name, NoLookupLocation.FROM_IDE) { it.importableFqName == originalFqName } == null
            else -> true
        }
    }

    private fun findReferencesToRestore(
        file: PsiFile,
        blockStart: Int,
        referenceData: Array<out KotlinReferenceData>
    ): List<ReferenceToRestoreData> = findReferences(file, referenceData.map { it to findReference(it, file as KtFile, blockStart) })

    private fun findReferencesToRestore(
        file: PsiFile,
        referenceData: List<KotlinReferenceData>,
        referencesByPosition: Map<TextRange, KtReference>
    ): List<ReferenceToRestoreData> =
        // use already found reference candidates - so file could be changed
        findReferences(file, referenceData.map {
            val textRange = TextRange(it.startOffset, it.endOffset)
            val reference = referencesByPosition[textRange]
            it to if (reference?.element?.isValid == true) reference else null
        })

    private fun findReferences(
        file: PsiFile,
        references: List<Pair<KotlinReferenceData, KtReference?>>
    ): List<ReferenceToRestoreData> {
        val ktFile = file as? KtFile ?: return listOf()

        val bindingContext = try {
            ktFile.getResolutionFacade().analyze(references.mapNotNull { it.second?.element }, BodyResolveMode.PARTIAL)
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.error("Failed to analyze references after copy paste", e)
            return emptyList()
        }
        val fileResolutionScope = ktFile.getResolutionFacade().getFileResolutionScope(ktFile)
        return references.mapNotNull { pair ->
            val data = pair.first
            val reference = pair.second
            reference?.let { createReferenceToRestoreData(it, data, ktFile, fileResolutionScope, bindingContext) }
        }
    }

    private fun findReference(data: KotlinReferenceData, file: KtFile, blockStart: Int): KtReference? =
        findReference(file, TextRange(data.startOffset + blockStart, data.endOffset + blockStart))

    private fun findReference(file: KtFile, desiredRange: TextRange): KtReference? {
        val element = file.findElementAt(desiredRange.startOffset) ?: return null
        return findReference(element, desiredRange)
    }

    private fun findReference(element: PsiElement, desiredRange: TextRange): KtReference? {
        for (current in element.parentsWithSelf) {
            val range = current.range
            if (current is KtElement && range == desiredRange) {
                current.mainReference?.let { return it }
            }
            if (range !in desiredRange) return null
        }
        return null
    }

    private fun createReferenceToRestoreData(
        reference: KtReference,
        refData: KotlinReferenceData,
        file: KtFile,
        fileResolutionScope: LexicalScope,
        bindingContext: BindingContext
    ): ReferenceToRestoreData? {
        if (!filterReferenceData(refData, fileResolutionScope)) return null

        val originalFqName = FqName(refData.fqName)
        val referencedDescriptors = resolveReference(reference, bindingContext)
        val referencedFqNames = referencedDescriptors
            .filterNot { ErrorUtils.isError(it) }
            .mapNotNull { it.importableFqName }
            .toSet()
        if (referencedFqNames.singleOrNull() == originalFqName) return null

        // check that descriptor to import exists and is accessible from the current module
        if (findImportableDescriptors(originalFqName, file).none { KotlinReferenceData.Kind.fromDescriptor(it) == refData.kind }) {
            return null
        }

        return ReferenceToRestoreData(reference, refData)
    }

    private fun resolveReference(reference: KtReference, bindingContext: BindingContext): List<DeclarationDescriptor> {
        val element = reference.element
        if (element is KtNameReferenceExpression && reference is KtSimpleNameReference) {
            val classifierDescriptor =
                bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element]
            (classifierDescriptor ?: bindingContext[BindingContext.REFERENCE_TARGET, element])?.let { return listOf(it) }
        }

        return reference.resolveToDescriptors(bindingContext).toList()
    }

    private fun restoreReferences(
        referencesToRestore: Collection<ReferenceToRestoreData>,
        file: KtFile,
        imported: MutableSet<String>
    ) {
        val importHelper = ImportInsertHelper.getInstance(file.project)
        val smartPointerManager = SmartPointerManager.getInstance(file.project)

        data class BindingRequest(
            val pointer: SmartPsiElementPointer<KtSimpleNameExpression>,
            val fqName: FqName
        )

        val bindingRequests = ArrayList<BindingRequest>()
        val descriptorsToImport = ArrayList<DeclarationDescriptor>()

        for ((reference, refData) in referencesToRestore) {
            if (!reference.element.isValid) continue
            val fqName = FqName(refData.fqName)

            if (refData.isQualifiable) {
                if (reference is KtSimpleNameReference) {
                    val pointer =
                        smartPointerManager.createSmartPsiElementPointer(reference.element, file)
                    bindingRequests.add(BindingRequest(pointer, fqName))
                } else if (reference is KDocReference) {
                    descriptorsToImport.addAll(findImportableDescriptors(fqName, file))
                }
            } else {
                descriptorsToImport.addIfNotNull(findCallableToImport(fqName, file))
            }
        }

        for (descriptor in descriptorsToImport) {
            importHelper.importDescriptor(file, descriptor)
            descriptor.getImportableDescriptor().importableFqName?.let { imported.add(it.asString()) }
        }

        for ((pointer, fqName) in bindingRequests) {
            pointer.element?.mainReference?.let {
                it.bindToFqName(fqName, KtSimpleNameReference.ShorteningMode.DELAYED_SHORTENING)
                imported.add(fqName.asString())
            }
        }
        performDelayedRefactoringRequests(file.project)
    }

    private fun findImportableDescriptors(fqName: FqName, file: KtFile): Collection<DeclarationDescriptor> {
        return file.resolveImportReference(fqName).filterNot {
            /*TODO: temporary hack until we don't have ability to insert qualified reference into root package*/
            DescriptorUtils.getParentOfType(it, PackageFragmentDescriptor::class.java)?.fqName?.isRoot ?: false
        }
    }

    private fun findCallableToImport(fqName: FqName, file: KtFile): CallableDescriptor? =
        findImportableDescriptors(fqName, file).firstIsInstanceOrNull()

    private tailrec fun DeclarationDescriptor.findPackageFqNameSafe(): FqName? {
        return when {
            this is PackageFragmentDescriptor -> this.fqNameOrNull()
            containingDeclaration == null -> this.fqNameOrNull()
            else -> this.containingDeclaration!!.findPackageFqNameSafe()
        }
    }

    private fun showRestoreReferencesDialog(
        project: Project,
        referencesToRestore: List<ReferenceToRestoreData>
    ): Collection<ReferenceToRestoreData> {
        val fqNames = referencesToRestore.map { it.refData.fqName }.toSortedSet()

        if (ApplicationManager.getApplication().isUnitTestMode) {
            declarationsToImportSuggested = fqNames
        }

        val shouldShowDialog = CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE == CodeInsightSettings.ASK
        if (!shouldShowDialog || referencesToRestore.isEmpty()) {
            return referencesToRestore
        }

        val dialog = RestoreReferencesDialog(project, fqNames.toTypedArray())
        dialog.show()

        val selectedFqNames = dialog.selectedElements!!.toSet()
        return referencesToRestore.filter { selectedFqNames.contains(it.refData.fqName) }
    }

    private fun toTextRanges(startOffsets: IntArray, endOffsets: IntArray): List<TextRange> {
        assert(startOffsets.size == endOffsets.size)
        return startOffsets.indices.map { TextRange(startOffsets[it], endOffsets[it]) }
    }

    private fun PsiElement.isInCopiedArea(fileCopiedFrom: KtFile, textRanges: List<TextRange>): Boolean {
        if (containingFile != fileCopiedFrom) return false
        return textRanges.any { this.range in it }
    }

    companion object {
        @get:TestOnly
        var declarationsToImportSuggested: Collection<String> = emptyList()

        private val LOG = Logger.getInstance(KotlinCopyPasteReferenceProcessor::class.java)

        private val IGNORE_REFERENCES_INSIDE: Array<Class<out KtElement>> = arrayOf(
            KtImportList::class.java,
            KtPackageDirective::class.java
        )
    }

}
