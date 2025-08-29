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
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneIndexCache.SharedIndexableFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltinsVirtualFileProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinClassFileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtDecompiledFile
import org.jetbrains.kotlin.analysis.decompiler.psi.file.deepCopy
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl

internal class KotlinStandaloneIndexBuilder(
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
                    processBinaryRoot(root) { sharedFile, _ -> add(sharedFile) }
                }
            }

            sharedFiles.mapTo(decompiledFilesFromBinaryRoots) { sharedFile ->
                val virtualFile = sharedFile.virtualFile
                val kotlinDecompiler = sharedFile.kotlinDecompiler
                val viewProvider = kotlinDecompiler.kotlinDecompiledFileViewProvider(virtualFile)!!

                // A hack to avoid costly checks inside createFile
                val ktFile = when (kotlinDecompiler) {
                    is KotlinClassFileDecompiler -> KtClsFile(viewProvider)

                    // Exceptions are acceptable since the files don't use those lambdas with stub-based decompilers
                    is KlibMetadataDecompiler<*> -> KlibDecompiledFile(viewProvider) { throw UnsupportedOperationException() }
                    is KotlinMetadataDecompiler<*> -> KtDecompiledFile(viewProvider) { throw UnsupportedOperationException() }
                    else -> error("Unexpected decompiler: ${kotlinDecompiler::class.simpleName}")
                }

                // This call is required to bind the view provider, its file and the psi manager together
                viewProvider.forceCachedPsi(ktFile)
                IndexableFile(virtualFile = virtualFile, ktFile = ktFile, isShared = true)
            }
        } else {
            processBinaryRoot(binaryRoot) { sharedFile, ktFile ->
                decompiledFilesFromBinaryRoots += IndexableFile(
                    virtualFile = sharedFile.virtualFile,
                    ktFile = ktFile,
                    isShared = isSharedRoot,
                )
            }
        }
    }

    private fun processBinaryRoot(root: VirtualFile, action: (SharedIndexableFile, KtFile) -> Unit) {
        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory) {
                    val (sharedFile, ktFile) = findSharedFile(file) ?: return true
                    action(sharedFile, ktFile)
                }

                return true
            }
        })
    }

    /**
     * The function imitates [PsiManager.findFile] behavior for a file inside a binary root
     */
    private fun findSharedFile(virtualFile: VirtualFile): Pair<SharedIndexableFile, KtFile>? {
        val decompiler = ClassFileDecompilers.getInstance().find(virtualFile, ClassFileDecompilers.Full::class.java) ?: return null

        val viewProvider = decompiler.kotlinDecompiledFileViewProvider(virtualFile)
        val ktFile = viewProvider?.getPsi(viewProvider.baseLanguage) as? KtFile ?: return null

        // Synthetic class parts are not supposed to be indexed to avoid duplicates
        // The information about virtual files is already cached after the previous line
        if (ClsClassFinder.isMultifileClassPartFile(virtualFile)) {
            return null
        }

        return SharedIndexableFile(virtualFile, decompiler) to ktFile
    }

    private fun ClassFileDecompilers.Full.kotlinDecompiledFileViewProvider(virtualFile: VirtualFile): KotlinDecompiledFileViewProvider? =
        createFileViewProvider(
            /* file = */ virtualFile,
            /* manager = */ psiManager,
            /* physical = */ !LightVirtualFile.shouldSkipEventSystem(virtualFile),
        ) as? KotlinDecompiledFileViewProvider

    private val decompiledFilesFromBuiltins = mutableSetOf<IndexableFile>()

    fun collectDecompiledFilesFromBuiltins() {
        for (virtualFile in BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()) {
            val decompiledFile = psiManager.findFile(virtualFile) as? KtFile ?: continue
            decompiledFilesFromBuiltins += IndexableFile(virtualFile, decompiledFile, isShared = true)
        }
    }

    private val providedSourceFiles = mutableSetOf<KtFile>()

    fun collectSourceFiles(sourceFiles: Collection<KtFile>) {
        providedSourceFiles += sourceFiles
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
        val stubTree = greenStubTree ?: calcStubTree()
        return stubTree.root as KotlinFileStubImpl
    }
