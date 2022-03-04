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
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsClassImpl
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.decompiled.light.classes.ClsJavaStubByVirtualFileCache
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.providers.CompiledDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.CompiledDeclarationProviderFactory
import org.jetbrains.kotlin.asJava.builder.ClsWrapperStubPsiFactory
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Path

internal class StaticCompiledDeclarationProvider(
    private val project: Project,
    private val scope: GlobalSearchScope,
    private val libraryModules: Collection<KtLibraryModule>,
    private val jarFileSystem: CoreJarFileSystem,
) : CompiledDeclarationProvider() {
    private val psiManager by lazy { PsiManager.getInstance(project) }

    private fun clsClassImplsByFqName(
        fqName: FqName,
        isPackageName: Boolean = true,
    ): Collection<ClsClassImpl> {
        return libraryModules
            .flatMap {
                virtualFilesFromModule(it, fqName, isPackageName)
            }
            .mapNotNull {
                createClsJavaClassFromVirtualFile(it)
            }
    }

    private fun virtualFilesFromModule(
        libraryModule: KtLibraryModule,
        fqName: FqName,
        isPackageName: Boolean,
    ): Collection<VirtualFile> {
        val fqNameString = fqName.asString()
        val fs = StandardFileSystems.local()
        return libraryModule.getBinaryRoots().flatMap r@{ rootPath ->
            val root = findRoot(rootPath, fs) ?: return@r emptySet()
            val files = mutableSetOf<VirtualFile>()
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
                        files.add(it)
                    }
                    true
                }
            )
            files
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

    private fun createClsJavaClassFromVirtualFile(
        classFile: VirtualFile,
    ): ClsClassImpl? {
        val javaFileStub = ClsJavaStubByVirtualFileCache.getInstance(project).get(classFile) ?: return null
        javaFileStub.psiFactory = ClsWrapperStubPsiFactory.INSTANCE
        val fakeFile = object : ClsFileImpl(ClassFileViewProvider(psiManager, classFile)) {
            override fun getStub() = javaFileStub

            override fun isPhysical() = false
        }
        javaFileStub.psi = fakeFile
        return fakeFile.classes.single() as ClsClassImpl
    }

    override fun getClassesByClassId(classId: ClassId): Collection<ClsClassImpl> {
        return clsClassImplsByFqName(classId.asSingleFqName(), isPackageName = false)
    }

    override fun getProperties(callableId: CallableId): Collection<PsiField> {
        val classes = callableId.classId?.let { classId ->
            getClassesByClassId(classId)
        } ?: clsClassImplsByFqName(callableId.packageName)
        return classes.flatMap { psiClass ->
            psiClass.fields.filter { psiField ->
                psiField.name == callableId.callableName.identifier
            }
        }.toList()
    }

    override fun getFunctions(callableId: CallableId): Collection<PsiMethod> {
        val classes = callableId.classId?.let { classId ->
            getClassesByClassId(classId)
        } ?: clsClassImplsByFqName(callableId.packageName)
        return classes.flatMap { psiClass ->
            psiClass.methods.filter { psiMethod ->
                psiMethod.name == callableId.callableName.identifier
            }
        }.toList()
    }
}

internal class StaticCompiledDeclarationProviderFactory(
    private val project: Project,
    private val libraryModules: Collection<KtLibraryModule>,
    private val jarFileSystem: CoreJarFileSystem,
) : CompiledDeclarationProviderFactory() {
    override fun createCompiledDeclarationProvider(searchScope: GlobalSearchScope): CompiledDeclarationProvider {
        return StaticCompiledDeclarationProvider(project, searchScope, libraryModules, jarFileSystem)
    }
}
