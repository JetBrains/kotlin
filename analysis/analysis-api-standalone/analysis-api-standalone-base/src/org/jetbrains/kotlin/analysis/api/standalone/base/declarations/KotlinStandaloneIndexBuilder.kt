/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.declarations

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneIndexCache.SharedIndexableDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KotlinKlibMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.*
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deepCopy

internal class KotlinStandaloneIndexBuilder private constructor(
    project: Project,
    private val shouldBuildStubsForDecompiledFiles: Boolean,
) {
    class IndexData(val fakeKtFiles: List<KtFile>, val index: KotlinStandaloneDeclarationIndex)

    private val psiManager = PsiManager.getInstance(project)
    private val cacheService = ApplicationManager.getApplication().serviceOrNull<KotlinStandaloneIndexCache>()

    /**
     * Synchronization is not needed since either this code is executed in a single thread right away
     * or guarded by the next synchronized `lazyIndex` property
     */
    private val setStubTreeMethod by lazy(LazyThreadSafetyMode.NONE) {
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
            fakeKtFiles = decompiledFilesFromBuiltins.map(IndexableFile::ktFile) + decompiledFilesFromBinaryRoots.map(IndexableFile::ktFile),
            index = initializedIndex,
        )
    }

    private fun index(): KotlinStandaloneDeclarationIndex {
        val rawIndex = KotlinStandaloneDeclarationIndexImpl()

        decompiledFilesFromBuiltins.forEach { rawIndex.indexStubRecursively(it) }

        val (decompiledBuiltinsFilesFromBinaryRoots, decompiledFilesFromOtherFiles) = decompiledFilesFromBinaryRoots.partition { entry ->
            entry.virtualFile.fileType == KotlinBuiltInFileType
        }

        decompiledFilesFromOtherFiles.forEach { rawIndex.indexStubRecursively(it) }

        // Due to KT-78748, we have to index builtin declarations last so that class declarations are preferred. Note that this currently
        // only affects Analysis API tests, since production Standalone doesn't index binary declarations as stubs.
        decompiledBuiltinsFilesFromBinaryRoots.forEach { rawIndex.indexStubRecursively(it) }

        val astBasedIndexer = rawIndex.AstBasedIndexer()

        for (file in providedSourceFiles) when {
            !file.isCompiled -> {
                val stub = file.stub
                if (stub != null) {
                    rawIndex.indexStubRecursively(stub)
                } else {
                    file.accept(astBasedIndexer)
                }
            }

            shouldBuildStubsForDecompiledFiles -> rawIndex.indexStubRecursively(file.forcedStub)
        }

        return rawIndex
    }

    private fun KotlinStandaloneDeclarationIndexImpl.indexStubRecursively(indexableFile: IndexableFile) {
        val virtualFile = indexableFile.virtualFile
        val ktFile = indexableFile.ktFile

        // Stub calculation
        if (indexableFile.isShared && cacheService != null) {
            val stub = cacheService.getOrBuildStub(virtualFile) {
                ktFile.forcedStub
            }

            if (stub.psi != ktFile) {
                @OptIn(KtImplementationDetail::class)
                val clonedStub = stub.deepCopy()

                // A hack to avoid costly stub builder execution
                setStubTreeMethod.invoke(ktFile, clonedStub)
            }
        }

        val stub = ktFile.forcedStub
        indexStubRecursively(stub)
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
                    val (sharedFile, ktFile) = findSharedDecompiledFile(file) ?: return true
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
        viewProvider.forceCachedPsi(ktFile)
        return IndexableFile(virtualFile = virtualFile, ktFile = ktFile, isShared = true)
    }

    private val decompiledFilesFromBuiltins = mutableSetOf<IndexableFile>()

    fun collectDecompiledFilesFromBuiltins() {
        for (virtualFile in BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()) {
            if (cacheService != null) {
                val sharedFiles = cacheService.getOrProcessBinaryRoot(virtualFile) { root ->
                    setOfNotNull(findSharedDecompiledFile(root)?.first)
                }

                sharedFiles.mapTo(decompiledFilesFromBuiltins, this::materializeSharedDecompiledFile)
            } else {
                val (_, ktFile) = findSharedDecompiledFile(virtualFile) ?: continue
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
 * [PsiFileImpl.getGreenStubTree] is cheaper if it is available since it doesn't require computing the AST tree
 */
private val KtFile.forcedStub: KotlinFileStubImpl
    get() {
        @Suppress("DEPRECATION") // KT-78356
        val stubTree = greenStubTree ?: calcStubTree()
        return stubTree.root as KotlinFileStubImpl
    }
