/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.FileContentImpl
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinClsStubBuilder
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.providers.KotlinCompiledDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinCompiledDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.*
import org.jetbrains.kotlin.analysis.utils.collections.ConcurrentMapBasedCache
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

internal class KotlinStaticCompiledDeclarationProvider(
    private val project: Project,
    private val scope: GlobalSearchScope,
    private val libraryModules: Collection<KtLibraryModule>,
    private val jarFileSystem: CoreJarFileSystem,
) : KotlinCompiledDeclarationProvider() {
    private val psiManager by lazy { PsiManager.getInstance(project) }
    private val stubBuilder by lazy { KotlinClsStubBuilder() }
    private val clsKotlinBinaryClassCache by lazy { ClsKotlinBinaryClassCache.getInstance() }

    private val kotlinFileStubsByFqNameCache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<FqName, Collection<KotlinFileStubImpl>>(ConcurrentHashMap())
    }

    private fun ktFileStubsByFqName(
        fqName: FqName,
        isPackageName: Boolean = true,
    ): Collection<KotlinFileStubImpl> {
        return kotlinFileStubsByFqNameCache.getOrPut(fqName) {
            libraryModules
                .flatMap {
                    fileContentsFromModule(it, fqName, isPackageName)
                }
                .mapNotNull {
                    createKotlinFileStub(it)
                }
        }
    }

    private fun fileContentsFromModule(
        libraryModule: KtLibraryModule,
        fqName: FqName,
        isPackageName: Boolean,
    ): Collection<FileContent> {
        val fqNameString = fqName.asString()
        val fs = StandardFileSystems.local()
        return libraryModule.getBinaryRoots().flatMap r@{ rootPath ->
            val root = findRoot(rootPath, fs) ?: return@r emptySet()
            val fileContents = mutableSetOf<FileContent>()
            VfsUtilCore.iterateChildrenRecursively(
                root,
                /*filter=*/filter@{
                    // Return `false` will skip the children.
                    if (it == root) return@filter true
                    // If it is a directory, then check if its path starts with fq name of interest
                    val relativeFqName = relativeFqName(root, it)
                    if (it.isDirectory && fqNameString.startsWith(relativeFqName)) {
                        return@filter true
                    }
                    // Otherwise, i.e., if it is a file, we are already in that matched directory (or directory in the middle).
                    // But, for files at the top-level, double-check if its parent (dir) and fq name of interest match.
                    if (isPackageName)
                        relativeFqName(root, it.parent).endsWith(fqNameString)
                    else // exact class fq name
                        relativeFqName == fqNameString
                },
                /*iterator=*/{
                    // We reach here after filtering above.
                    // Directories in the middle, e.g., com/android, can reach too.
                    if (!it.isDirectory &&
                        isCompiledFile(it) &&
                        it in scope
                    ) {
                        val fileContent = FileContentImpl.createByFile(it, project)
                        if (isKtCompiledFile(fileContent)) {
                            fileContents.add(fileContent)
                        }
                    }
                    true
                }
            )
            fileContents
        }
    }

    private fun findRoot(
        rootPath: Path,
        fs: VirtualFileSystem,
    ): VirtualFile? {
        return if (rootPath.toFile().isDirectory) {
            fs.findFileByPath(rootPath.toAbsolutePath().toString())
        } else {
            jarFileSystem.refreshAndFindFileByPath(rootPath.toAbsolutePath().toString() + JAR_SEPARATOR)
        }
    }

    private fun relativeFqName(
        root: VirtualFile,
        virtualFile: VirtualFile,
    ): String {
        return if (root.isDirectory) {
            val fragments = buildList {
                var cur = virtualFile
                while (cur != root) {
                    add(cur.nameWithoutExtension)
                    cur = cur.parent
                }
            }
            fragments.reversed().joinToString(".")
        } else {
            virtualFile.path.split(JAR_SEPARATOR).lastOrNull()?.replace("/", ".")
                ?: JAR_SEPARATOR // random string that will bother membership test.
        }
    }

    private fun isCompiledFile(
        virtualFile: VirtualFile,
    ): Boolean {
        return virtualFile.extension?.endsWith(JavaClassFileType.INSTANCE.defaultExtension) == true
    }

    private fun isKtCompiledFile(
        fileContent: FileContent,
    ): Boolean {
        return clsKotlinBinaryClassCache.isKotlinJvmCompiledFile(fileContent.file, fileContent.content)
    }

    private fun createKotlinFileStub(
        fileContent: FileContent,
    ): KotlinFileStubImpl? {
        val ktFileStub = stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl ?: return null
        val fakeFile = object : KtFile(KtClassFileViewProvider(psiManager, fileContent.file), isCompiled = true) {
            override fun getStub() = ktFileStub

            override fun isPhysical() = false
        }
        ktFileStub.psi = fakeFile
        return ktFileStub
    }

    private class KtClassFileViewProvider(
        psiManager: PsiManager,
        virtualFile: VirtualFile,
    ) : SingleRootFileViewProvider(psiManager, virtualFile, true, KotlinLanguage.INSTANCE)

    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> {
        return ktFileStubsByFqName(classId.asSingleFqName(), isPackageName = false).flatMap { stubFile ->
            stubFile.childrenStubs.asSequence()
                .filterIsInstance<KotlinClassStubImpl>()
                .filter { classStub -> classStub.getClassId() == classId }
                .map { it.psi }
        }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> {
        return ktFileStubsByFqName(callableId.packageName).flatMap { stubFile ->
            stubFile.childrenStubs.asSequence()
                .filterIsInstance<KotlinPropertyStubImpl>()
                .map { it.psi }
                .filterIsInstance<KtProperty>()
                .filter { it.nameAsName == callableId.callableName }
                .toList()
        }
    }

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> {
        return ktFileStubsByFqName(callableId.packageName).flatMap { stubFile ->
            stubFile.childrenStubs.asSequence()
                .filterIsInstance<KotlinFunctionStubImpl>()
                .map { it.psi }
                .filterIsInstance<KtNamedFunction>()
                .filter { it.nameAsName == callableId.callableName }
                .toList()
        }
    }

}

public class KotlinStaticCompiledDeclarationProviderFactory(
    private val project: Project,
    private val libraryModules: Collection<KtLibraryModule>,
    private val jarFileSystem: CoreJarFileSystem,
) : KotlinCompiledDeclarationProviderFactory() {
    private val cache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<GlobalSearchScope, KotlinStaticCompiledDeclarationProvider>(ConcurrentHashMap())
    }

    private fun getOrCreateCompiledDeclarationProvider(searchScope: GlobalSearchScope): KotlinCompiledDeclarationProvider {
        return cache.getOrPut(searchScope) {
            KotlinStaticCompiledDeclarationProvider(
                project,
                searchScope,
                libraryModules,
                jarFileSystem
            )
        }
    }

    override fun createCompiledDeclarationProvider(searchScope: GlobalSearchScope): KotlinCompiledDeclarationProvider {
        return getOrCreateCompiledDeclarationProvider(searchScope)
    }
}
