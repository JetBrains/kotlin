/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.openapi.application.ApplicationManager
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
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.allowResolveInDispatchThread
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.ReviewAddedImports.reviewAddedImports
import org.jetbrains.kotlin.idea.codeInsight.shorten.performDelayedRefactoringRequests
import org.jetbrains.kotlin.idea.core.util.end
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.core.util.start
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.*
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ProgressIndicatorUtils
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.invokeLater
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode
import org.jetbrains.kotlin.idea.util.application.runReadAction
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

        val locationFqName = if (startOffsets.size == 1) {
            val fqName = file.namedDeclarationFqName(startOffsets[0] - 1)
            fqName?.takeIf { it == file.namedDeclarationFqName(endOffsets[0] + 1) }
        } else null

        return listOf(
            BasicKotlinReferenceTransferableData(
                sourceFileUrl = file.virtualFile.url,
                packageName = packageName,
                imports = imports,
                sourceTextOffset = offsetDelta,
                sourceText = text,
                textRanges = ranges,
                locationFqName = locationFqName
            )
        )
    }

    private fun KtFile.namedDeclarationFqName(offset: Int): String? =
        if (offset in 0 until textLength)
            PsiTreeUtil.getNonStrictParentOfType(this.findElementAt(offset), KtNamedDeclaration::class.java)?.fqName?.asString()
        else null

    private fun collectReferenceData(
        textRanges: List<TextRange>,
        file: KtFile,
        targetFile: KtFile,
        fakePackageName: String,
        sourcePackageName: String,
        indicator: ProgressIndicator
    ): List<KotlinReferenceData> =
        collectReferenceData(
            file = file,
            startOffsets = textRanges.map { it.startOffset }.toIntArray(),
            endOffsets = textRanges.map { it.endOffset }.toIntArray(),
            fakePackageName = fakePackageName,
            sourcePackageName = sourcePackageName,
            targetPackageName = runReadAction { targetFile.packageDirective?.fqName?.asString() ?: "" },
            indicator = indicator
        )

    fun collectReferenceData(
        file: KtFile,
        startOffsets: IntArray,
        endOffsets: IntArray,
        fakePackageName: String? = null,
        sourcePackageName: String? = null,
        targetPackageName: String? = null,
        indicator: ProgressIndicator? = null
    ): List<KotlinReferenceData> {
        val ranges = toTextRanges(startOffsets, endOffsets)
        val elements = ranges.flatMap { textRange ->
            runReadAction {
                indicator?.checkCanceled()
                file.elementsInRange(textRange).filter { it is KtElement || it is KDocElement }
            }
        }

        val allElementsToResolve = runReadAction {
            elements.flatMap { it.collectDescendantsOfType<KtElement>() }
        }

        // TODO: allowResolveInDispatchThread could be dropped as soon as
        //  ConvertJavaCopyPasteProcessor will perform it on non UI thread
        val bindingContext = runReadAction {
            allowResolveInDispatchThread {
                file.getResolutionFacade().analyze(allElementsToResolve, BodyResolveMode.PARTIAL)
            }
        }

        val result = mutableListOf<KotlinReferenceData>()
        for (ktElement in allElementsToResolve) {
            runReadAction {
                indicator?.checkCanceled()
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
        val reference = runReadAction { ktElement.mainReference } ?: return

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
    private data class PsiElementByTextRange(val originalTextRange: TextRange, val element: SmartPsiElementPointer<KtElement>)

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

        if (isPastedToTheSameOrigin(file, caretOffset, values)) return

        processReferenceData(project, editor, file, bounds.startOffset, values.single())
    }

    private fun isPastedToTheSameOrigin(
        file: KtFile,
        caretOffset: Int,
        values: List<BasicKotlinReferenceTransferableData>
    ): Boolean {
        if (values.size == 1 && values[0].sourceFileUrl == file.virtualFile.url &&
            values[0].locationFqName != null &&
            // check locationFqName on position before pasted snippet
            values[0].locationFqName == file.namedDeclarationFqName(caretOffset - 1)
        ) {
            val currentImports = file.importDirectives.map { it.text }.toSet()
            val originalImports = values.flatMap { it.imports }.toSet()
            if (currentImports == originalImports) {
                return true
            }
        }
        return false
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
        val elementsByRange = runReadAction {
            transferableData.textRanges.flatMap { originalTextRange ->
                val elementsList = mutableListOf<PsiElementByTextRange>()
                val textRange = TextRange(
                    startOffsetDelta,
                    startOffsetDelta + originalTextRange.endOffset - originalTextRange.startOffset
                )
                startOffsetDelta = startOffsetDelta + originalTextRange.endOffset - originalTextRange.startOffset + 1
                file.elementsInRange(textRange)
                    .filter { it is KtElement || it is KDocElement }
                    .forEachDescendant {
                        val range = TextRange(
                            originalTextRange.startOffset + it.textRange.startOffset - textRange.startOffset,
                            originalTextRange.startOffset + it.textRange.endOffset - textRange.startOffset
                        )
                        elementsList.add(PsiElementByTextRange(range, it.createSmartPointer()))
                    }
                elementsList
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
        val task = object : Task.Backgroundable(project, KotlinBundle.message("copy.paste.resolve.references"), true) {
            override fun run(indicator: ProgressIndicator) {
                assert(!ApplicationManager.getApplication().isWriteAccessAllowed) {
                    "Resolving references on dispatch thread leads to live lock"
                }

                val referencesPossibleToRestore = findReferenceProvider(indicator)

                applyResolvedImports(project, referencesPossibleToRestore, file, editor)
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
        invokeLater {
            val selectedReferencesToRestore =
                showRestoreReferencesDialog(project, referencesPossibleToRestore)
            if (selectedReferencesToRestore.isEmpty()) return@invokeLater

            project.executeWriteCommand(KotlinBundle.message("resolve.pasted.references")) {
                val imported = TreeSet<String>()
                restoreReferences(selectedReferencesToRestore, file, imported)

                reviewAddedImports(project, editor, file, imported)
            }
        }
    }

    private fun findReferenceDataToRestore(
        file: PsiFile,
        indicator: ProgressIndicator,
        elementsByRange: List<PsiElementByTextRange>,
        transferableData: BasicKotlinReferenceTransferableData
    ): List<ReferenceToRestoreData> {
        if (file !is KtFile) return emptyList()

        val referencesByRange: Map<TextRange, KtReference> = elementsByRange.mapNotNull { elementByTextRange ->
            runReadAction {
                val element = elementByTextRange.element.element ?: return@runReadAction null

                val mainReference = element.mainReference
                mainReference?.let { ref ->
                    val textRange = ref.element.textRange
                    val findReference = findReference(file, textRange)
                    findReference?.let {
                        // remap reference to the original (as it was on paste phase) text range
                        val itTextRange = it.element.textRange
                        val refTextRange = ref.element.textRange
                        val originalTextRange = elementByTextRange.originalTextRange
                        val offset = originalTextRange.start - refTextRange.start
                        val range = TextRange(itTextRange.start + offset, itTextRange.end + offset)
                        range to it
                    }
                }
            }
        }.toMap()

        val sourcePackageName = transferableData.packageName
        val imports: List<String> = transferableData.imports

        val project = runReadAction { file.project }
        // Step 0. Recreate original source file (i.e. source file as it was on copy action) and resolve references from it
        val ctxFile = sourceFile(project, transferableData) ?: file

        // put original source file to some fake package to avoid ambiguous resolution ( a real file VS a virtual file )
        val fakePackageName = "__kotlin.__some.__funky.__package"

        val dummyOrigFileProlog =
            """
            package $fakePackageName
            
            ${buildDummySourceScope(sourcePackageName, imports, fakePackageName, file, transferableData, ctxFile)}
             
            """.trimIndent()

        val dummyOriginalFile = runReadAction {
            KtPsiFactory(project)
                .createAnalyzableFile(
                    "dummy-original.kt",
                    "$dummyOrigFileProlog${transferableData.sourceText}",
                    ctxFile
                )
        }

        val offsetDelta = dummyOrigFileProlog.length - transferableData.sourceTextOffset

        val dummyOriginalFileTextRanges =
            // it is required as it is shifted by dummy prolog
            transferableData.textRanges.map { TextRange(it.startOffset + offsetDelta, it.endOffset + offsetDelta) }

        // Step 1. Find references in copied blocks of (recreated) source file
        val sourceFileBasedReferences =
            collectReferenceData(dummyOriginalFileTextRanges, dummyOriginalFile, file, fakePackageName, sourcePackageName, indicator).map {
                it.copy(startOffset = it.startOffset - offsetDelta, endOffset = it.endOffset - offsetDelta)
            }

        indicator.checkCanceled()

        // Step 2. Find references to restore in a target file
        return ProgressIndicatorUtils.awaitWithCheckCanceled(
            submitNonBlocking(project, indicator) {
                return@submitNonBlocking findReferencesToRestore(file, indicator, sourceFileBasedReferences, referencesByRange)
            }
        )
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

        val dummyImportsFile = runReadAction {
            KtPsiFactory(file.project)
                .createAnalyzableFile(
                    "dummy-imports.kt",
                    "package $fakePkgName\n" +
                            "${joinLines(fakePkgImports)}\n" +
                            transferableData.sourceText,
                    ctxFile
                )
        }

        val dummyFileImports = runReadAction {
            dummyImportsFile.collectDescendantsOfType<KtImportDirective>().mapNotNull { directive ->
                val importedReference =
                    directive.importedReference?.getQualifiedElementSelector()?.mainReference?.resolve() as? KtNamedDeclaration
                importedReference?.let { directive.text }
            }
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

    private fun sourceFile(project: Project, transferableData: BasicKotlinReferenceTransferableData): KtFile? =
        runReadAction {
            val sourceFile = VirtualFileManager.getInstance().findFileByUrl(transferableData.sourceFileUrl) ?: return@runReadAction null
            if (sourceFile.getSourceRoot(project) == null) return@runReadAction null

            PsiManager.getInstance(project).findFile(sourceFile) as? KtFile
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
        indicator: ProgressIndicator,
        referenceData: List<KotlinReferenceData>,
        referencesByPosition: Map<TextRange, KtReference>
    ): List<ReferenceToRestoreData> {
        // use already found reference candidates - so file could be changed
        return findReferences(file, referenceData.map {
            indicator.checkCanceled()
            val textRange = TextRange(it.startOffset, it.endOffset)
            val reference = referencesByPosition[textRange]
            it to reference
        })
    }

    private fun findReferences(
        file: PsiFile,
        references: List<Pair<KotlinReferenceData, KtReference?>>
    ): List<ReferenceToRestoreData> {
        val ktFile = file as? KtFile ?: return listOf()

        val resolutionFacade = runReadAction { ktFile.getResolutionFacade() }
        val referencesList = mutableListOf<Pair<KotlinReferenceData, KtReference?>>()
        val bindingContext =
            try {
                runReadAction {
                    referencesList.addAll(references.mapNotNull { if (it.second?.element?.isValid == true) it.first to it.second else null })
                    resolutionFacade.analyze(
                        referencesList.mapNotNull { it.second?.element },
                        BodyResolveMode.PARTIAL
                    )
                }
            } catch (e: Throwable) {
                if (e is ControlFlowException) throw e
                LOG.error("Failed to analyze references after copy paste", e)
                return emptyList()
            }

        val fileResolutionScope = runReadAction { resolutionFacade.getFileResolutionScope(ktFile) }
        return referencesList.mapNotNull { pair ->
            val data = pair.first
            val reference = pair.second
            reference?.let { createReferenceToRestoreData(it, data, ktFile, fileResolutionScope, bindingContext) }
        }
    }

    private fun findReference(data: KotlinReferenceData, file: KtFile, blockStart: Int): KtReference? =
        findReference(file, TextRange(data.startOffset + blockStart, data.endOffset + blockStart))

    private fun findReference(file: KtFile, desiredRange: TextRange): KtReference? {
        val element = runReadAction { file.findElementAt(desiredRange.startOffset) } ?: return null
        return findReference(element, desiredRange)
    }

    private fun findReference(element: PsiElement, desiredRange: TextRange): KtReference? = runReadAction {
        for (current in element.parentsWithSelf) {
            val range = current.range
            if (current is KtElement && range == desiredRange) {
                current.mainReference?.let { return@runReadAction it }
            }
            if (range !in desiredRange) return@runReadAction null
        }
        return@runReadAction null
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
        return runReadAction {
            if (element.isValid && element is KtNameReferenceExpression && reference is KtSimpleNameReference) {
                val classifierDescriptor =
                    bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, element]
                (classifierDescriptor ?: bindingContext[BindingContext.REFERENCE_TARGET, element])?.let { return@runReadAction listOf(it) }
            }

            reference.resolveToDescriptors(bindingContext).toList()
        }
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
        return runReadAction {
            file.resolveImportReference(fqName).filterNot {
                /*TODO: temporary hack until we don't have ability to insert qualified reference into root package*/
                DescriptorUtils.getParentOfType(it, PackageFragmentDescriptor::class.java)?.fqName?.isRoot ?: false
            }
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

        if (isUnitTestMode()) {
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

    private fun PsiElement.isInCopiedArea(fileCopiedFrom: KtFile, textRanges: List<TextRange>): Boolean =
        runReadAction {
            if (containingFile != fileCopiedFrom) false else textRanges.any { this.range in it }
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
