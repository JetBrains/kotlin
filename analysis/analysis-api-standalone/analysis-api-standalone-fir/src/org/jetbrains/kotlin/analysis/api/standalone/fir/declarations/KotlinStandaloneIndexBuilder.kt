/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.analysis.api.standalone.fir.declarations

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.standalone.fir.declarations.KotlinStandaloneIndexCache.SharedIndexableDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KotlinKlibMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.*
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPlatformInterface
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deepCopy

internal class KotlinStandaloneIndexBuilder private constructor(
    project: Project,
    private val shouldBuildStubsForDecompiledFiles: Boolean,
) {
    class IndexData(
        decompiledFilePackageNamesComputer: () -> Map<VirtualFile, FqName>,
        val index: KotlinStandaloneDeclarationIndex,
    ) {
        /**
         * Package names of the decompiled files created for builtins and binary roots, computed on the first access.
         */
        val decompiledFilePackageNames: Map<VirtualFile, FqName> by lazy(decompiledFilePackageNamesComputer)
    }

    private val psiManager = PsiManager.getInstance(project)
    private val cacheService = ApplicationManager.getApplication().serviceOrNull<KotlinStandaloneIndexCache>()

    /**
     * Synchronization is needed since [SharedIndexedFile]s bind their stubs on demand from different threads.
     */
    private val setStubTreeMethod by lazy {
        val setStubTreeMethodName = "setStubTree"

        PsiFileImpl::class
            .java
            .declaredMethods
            .find { it.name == setStubTreeMethodName && it.parameterCount == 1 }
            ?.also { it.isAccessible = true }
            ?: error("'${PsiFileImpl::class.simpleName}.$setStubTreeMethodName' method is not found")
    }

    fun build(postponeIndexing: Boolean): IndexData {
        val initializedIndex = if (postponeIndexing) {
            KotlinStandaloneLazyDeclarationIndexImpl(lazy(::index))
        } else {
            index()
        }

        return IndexData(
            decompiledFilePackageNamesComputer = ::computeDecompiledFilePackageNames,
            index = initializedIndex,
        )
    }

    private fun computeDecompiledFilePackageNames(): Map<VirtualFile, FqName> = buildMap {
        for (indexableFile in decompiledFilesFromBuiltins + decompiledFilesFromBinaryRoots) {
            put(indexableFile.virtualFile, indexableFile.packageFqName)
        }
    }

    /**
     * Shared files take their package name from [cacheService], since [KtFile.packageFqName] of a decompiled file builds its stub,
     * which would otherwise happen in each project.
     */
    private val IndexableFile.packageFqName: FqName
        get() = if (isShared && cacheService != null) {
            cacheService.getOrComputePackageName(virtualFile) { ktFile.packageFqName }
        } else {
            ktFile.packageFqName
        }

    private fun index(): KotlinStandaloneDeclarationIndex {
        val rawIndex = KotlinStandaloneDeclarationIndexImpl()

        decompiledFilesFromBuiltins.forEach { rawIndex.indexDecompiledFile(it) }

        val [decompiledBuiltinsFilesFromBinaryRoots, decompiledFilesFromOtherFiles] = decompiledFilesFromBinaryRoots.partition { entry ->
            entry.virtualFile.fileType == KotlinBuiltInFileType
        }

        decompiledFilesFromOtherFiles.forEach { rawIndex.indexDecompiledFile(it) }

        // Due to KT-78748, we have to index builtin declarations last so that class declarations are preferred. Note that this currently
        // only affects Analysis API tests, since production Standalone doesn't index binary declarations as stubs.
        decompiledBuiltinsFilesFromBinaryRoots.forEach { rawIndex.indexDecompiledFile(it) }

        for (file in providedSourceFiles) when {
            !file.isCompiled -> {
                val stub = file.stub
                if (stub != null) {
                    rawIndex.indexStubEagerly(file, stub)
                } else {
                    val astBasedIndexer = KotlinStandaloneAstBasedIndexer()
                    file.accept(astBasedIndexer)

                    val indexedFile = KotlinStandaloneEagerIndexedFile(file.virtualFile, astBasedIndexer.elements)
                    rawIndex.indexFile(indexedFile, astBasedIndexer.records)
                }
            }

            shouldBuildStubsForDecompiledFiles -> rawIndex.indexStubEagerly(file, file.forcedStub)
        }

        return rawIndex
    }

    private fun KotlinStandaloneDeclarationIndexImpl.indexDecompiledFile(indexableFile: IndexableFile) {
        if (indexableFile.isShared && cacheService != null) {
            val virtualFile = indexableFile.virtualFile
            val cachedRecords = cacheService.getIndexRecords(virtualFile)
            val sharedFile = SharedIndexedFile(indexableFile, cacheService, cachedRecords?.indexedElementCount)

            val fileRecords = cachedRecords ?: sharedFile.computeIndexRecords().also { cacheService.putIndexRecords(virtualFile, it) }
            indexFile(sharedFile, fileRecords.records)
        } else {
            indexStubEagerly(indexableFile.ktFile, indexableFile.ktFile.forcedStub)
        }
    }

    private fun KotlinStandaloneDeclarationIndexImpl.indexStubEagerly(file: KtFile, fileStub: StubElement<*>) {
        val indexedStubs = collectIndexedStubs(fileStub)
        indexFile(KotlinStandaloneEagerIndexedFile(file.virtualFile, indexedStubs.map { it.psi }), collectIndexRecords(indexedStubs))
    }

    /**
     * A file shared between projects, whose stub is bound to its [KtFile] only when one of its elements is requested.
     *
     * The binding copies the cached stub, so binding all library files would take most of the time of a test, while a test usually
     * needs only a few of them. Index records of the file don't depend on the project, so they are taken from [KotlinStandaloneIndexCache].
     *
     * @param expectedIndexedElementCount the number of indexed elements of the cached records, if the file is indexed with them.
     *  The cached records refer to the elements by their ordinals, so a mismatch would silently give the PSI of another declaration.
     */
    private inner class SharedIndexedFile(
        private val indexableFile: IndexableFile,
        private val cacheService: KotlinStandaloneIndexCache,
        private val expectedIndexedElementCount: Int?,
    ) : KotlinStandaloneIndexedFile(indexableFile.virtualFile) {
        private val indexedStubs: List<StubElement<*>> by lazy {
            val indexedStubs = collectIndexedStubs(bindStub())
            if (expectedIndexedElementCount != null) {
                check(indexedStubs.size == expectedIndexedElementCount) {
                    "`$virtualFile` has ${indexedStubs.size} indexed elements," +
                            " but its cached index records were computed for $expectedIndexedElementCount elements"
                }
            }

            // The PSI is created right away, so it's created only once even if the elements are requested from different threads
            indexedStubs.onEach { it.psi }
        }

        override fun getElement(ordinal: Int): PsiElement = indexedStubs[ordinal].psi

        fun computeIndexRecords(): KotlinStandaloneFileIndexRecords =
            KotlinStandaloneFileIndexRecords(collectIndexRecords(indexedStubs), indexedStubs.size)

        private fun bindStub(): KotlinFileStubImpl {
            val ktFile = indexableFile.ktFile
            val stub = cacheService.getOrBuildStub(indexableFile.virtualFile) {
                ktFile.forcedStub
            }

            if (stub.psi != ktFile) {
                val clonedStub = stub.deepCopy()

                // A hack to avoid costly stub builder execution
                setStubTreeMethod.invoke(ktFile, clonedStub)
            }

            return ktFile.forcedStub
        }
    }

    private val decompiledFilesFromBinaryRoots = mutableSetOf<IndexableFile>()

    fun collectDecompiledFilesFromBinaryRoot(binaryRoot: VirtualFile, isSharedRoot: Boolean) {
        if (isSharedRoot && cacheService != null) {
            val sharedFiles = cacheService.getOrProcessBinaryRoot(binaryRoot) { root ->
                buildSet {
                    processDecompiledFilesFromBinaryRoot(root) { sharedFile, _ -> add(sharedFile) }
                }
            }

            sharedFiles.mapTo(decompiledFilesFromBinaryRoots, this::materializeSharedDecompiledFile)
        } else {
            processDecompiledFilesFromBinaryRoot(binaryRoot) { sharedFile, ktFile ->
                decompiledFilesFromBinaryRoots += IndexableFile(
                    virtualFile = sharedFile.virtualFile,
                    ktFile = ktFile,
                    isShared = isSharedRoot,
                )
            }
        }
    }

    private fun processDecompiledFilesFromBinaryRoot(root: VirtualFile, processor: (SharedIndexableDecompiledFile, KtFile) -> Unit) {
        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory) {
                    val [sharedFile, ktFile] = findSharedDecompiledFile(file) ?: return true
                    processor(sharedFile, ktFile)
                }

                return true
            }
        })
    }

    /**
     * The function imitates [PsiManager.findFile] behavior for a file inside a binary root
     */
    private fun findSharedDecompiledFile(virtualFile: VirtualFile): Pair<SharedIndexableDecompiledFile, KtFile>? {
        val decompiler = ClassFileDecompilers.getInstance().find(virtualFile, ClassFileDecompilers.Full::class.java) ?: return null

        val viewProvider = decompiler.kotlinDecompiledFileViewProvider(virtualFile)
        val ktFile = viewProvider?.getPsi(viewProvider.baseLanguage) as? KtFile ?: return null

        // Synthetic class parts are not supposed to be indexed to avoid duplicates
        // The information about virtual files is already cached after the previous line
        if (ClsClassFinder.isMultifileClassPartFile(virtualFile)) {
            return null
        }

        return SharedIndexableDecompiledFile(virtualFile, decompiler) to ktFile
    }

    private fun ClassFileDecompilers.Full.kotlinDecompiledFileViewProvider(virtualFile: VirtualFile): KotlinDecompiledFileViewProvider? =
        createFileViewProvider(
            /* file = */ virtualFile,
            /* manager = */ psiManager,
            /* physical = */ !LightVirtualFile.shouldSkipEventSystem(virtualFile),
        ) as? KotlinDecompiledFileViewProvider

    /**
     * This function is the main reason why [SharedIndexableDecompiledFile] exists.
     * Since we cannot share PSI between tests (because they are bound to a project),
     * we need to recreate the PSI for all files so that all files are indexed.
     *
     * Usually, this [VirtualFile] -> [KtDecompiledFile] mapping happens via [PsiManager.findFile]
     * which involves the costly internal machinery to iterate through all registered decompilers and find a match.
     *
     * Thanks to [findSharedDecompiledFile], we imitate the same behavior once for each [VirtualFile] and
     * cache the decompiler that results in a [KtDecompiledFile] via [SharedIndexableDecompiledFile].
     *
     * Since we know all possible [SharedIndexableDecompiledFile.kotlinDecompiler] values,
     * we can safely skipp all checks on the way and directly instantiate required [KtDecompiledFile] files.
     */
    private fun materializeSharedDecompiledFile(file: SharedIndexableDecompiledFile): IndexableFile {
        val virtualFile = file.virtualFile
        val kotlinDecompiler = file.kotlinDecompiler
        val viewProvider = kotlinDecompiler.kotlinDecompiledFileViewProvider(virtualFile)
        requireNotNull(viewProvider) { "Something is wrong – the decompiler must be a Kotlin one" }

        // A hack to avoid costly checks inside createFile
        val ktFile = when (kotlinDecompiler) {
            is KotlinClassFileDecompiler -> KtClsFile(viewProvider)
            is KotlinKlibMetadataDecompiler -> KlibDecompiledFile(viewProvider)
            is KotlinBuiltInDecompiler -> KotlinBuiltinsDecompiledFile(viewProvider)
            else -> error("Unexpected decompiler: ${kotlinDecompiler::class.simpleName}")
        }

        // This call is required to bind the view provider, its file and the psi manager together
        return ApplicationManager.getApplication().runWriteAction<_> {
            viewProvider.forceCachedPsi(ktFile)
            IndexableFile(virtualFile = virtualFile, ktFile = ktFile, isShared = true)
        }
    }

    private val decompiledFilesFromBuiltins = mutableSetOf<IndexableFile>()

    @OptIn(KtPlatformInterface::class)
    fun collectDecompiledFilesFromBuiltins() {
        for (virtualFile in BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()) {
            if (cacheService != null) {
                val sharedFiles = cacheService.getOrProcessBinaryRoot(virtualFile) { root ->
                    setOfNotNull(findSharedDecompiledFile(root)?.first)
                }

                sharedFiles.mapTo(decompiledFilesFromBuiltins, this::materializeSharedDecompiledFile)
            } else {
                val [_, ktFile] = findSharedDecompiledFile(virtualFile) ?: continue
                decompiledFilesFromBuiltins += IndexableFile(virtualFile, ktFile, isShared = true)
            }
        }
    }

    private val providedSourceFiles = mutableSetOf<KtFile>()

    fun collectSourceFiles(sourceFiles: Collection<KtFile>) {
        providedSourceFiles += sourceFiles
    }

    companion object {
        operator fun invoke(
            project: Project,
            shouldBuildStubsForDecompiledFiles: Boolean,
            postponeIndexing: Boolean,
            configurator: KotlinStandaloneIndexBuilder.() -> Unit,
        ): IndexData = KotlinStandaloneIndexBuilder(
            project = project,
            shouldBuildStubsForDecompiledFiles = shouldBuildStubsForDecompiledFiles,
        ).apply(configurator).build(postponeIndexing = postponeIndexing)
    }
}

private class IndexableFile(
    /**
     * The virtual file of the file that is being indexed.
     */
    val virtualFile: VirtualFile,

    /**
     * The [KtFile] associated with the [virtualFile].
     */
    val ktFile: KtFile,

    /**
     * Whether the file is shared between multiple projects.
     */
    val isShared: Boolean,
) {
    override fun equals(other: Any?): Boolean = this === other || other is IndexableFile && virtualFile == other.virtualFile
    override fun hashCode(): Int = virtualFile.hashCode()
    override fun toString(): String = virtualFile.toString()
}

/**
 * An already available stub tree is cheaper than [PsiFileImpl.calcStubTree] since it doesn't require computing the AST tree
 */
private val KtFile.forcedStub: KotlinFileStubImpl
    get() {
        val stubTree = stubTreeOrFileElement.first ?: calcStubTree()
        return stubTree.root as KotlinFileStubImpl
    }
