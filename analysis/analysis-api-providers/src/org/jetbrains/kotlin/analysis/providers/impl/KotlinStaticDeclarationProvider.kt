/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.io.URLUtil.JAR_SEPARATOR
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProviderFactory
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.*
import org.jetbrains.kotlin.analysis.utils.collections.ConcurrentMapBasedCache
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.util.concurrent.ConcurrentHashMap

public class KotlinStaticDeclarationProvider(
    private val project: Project,
    scope: GlobalSearchScope,
    sourceKtFiles: Collection<KtFile>,
    private val libraryModules: Collection<KtLibraryModule> = emptyList(),
    private val jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
) : KotlinDeclarationProvider() {
    private val sourceFilesInScope = sourceKtFiles.filter { scope.contains(it.virtualFile) }
    private val ktClsFilesByPackageCache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<FqName, List<KtClsFile>>(ConcurrentHashMap())
    }
    private val psiManager by lazy { PsiManager.getInstance(project) }

    private fun filesByPackage(packageFqName: FqName): Sequence<KtFile> =
        sourceFilesInScope.asSequence()
            .filter { it.packageFqName == packageFqName }
            .ifEmpty { getOrCreateKtClsFilesByPackage(packageFqName).asSequence() }

    private fun getOrCreateKtClsFilesByPackage(packageFqName: FqName): List<KtClsFile> {
        return ktClsFilesByPackageCache.getOrPut(packageFqName) {
            ktClsFilesByPackage(it)
        }
    }

    private fun ktClsFilesByPackage(packageFqName: FqName): List<KtClsFile> {
        val packageFqNameString = packageFqName.asString()
        val fs = StandardFileSystems.local()
        return libraryModules
            .flatMap { libModule ->
                libModule.getBinaryRoots().flatMap cls@{ rootPath ->
                    val root = if (rootPath.toFile().isDirectory) {
                        fs.findFileByPath(rootPath.toAbsolutePath().toString())
                    } else {
                        jarFileSystem.refreshAndFindFileByPath(rootPath.toAbsolutePath().toString() + JAR_SEPARATOR)
                    } ?: return@cls emptySet()
                    val files = mutableSetOf<VirtualFile>()
                    VfsUtilCore.iterateChildrenRecursively(
                        root,
                        /*filter=*/{
                            // Return `false` will skip the children.
                            // If it is a directory, then check if its path ends with package fq name of interest
                            // Otherwise, i.e., if it is a file, we are already in that matched directory.
                            // But, for files at the top-level, double-check if its parent (dir) and package fq name of interest matches.
                            it == root ||
                                    (it.isDirectory && packageFqNameString.startsWith(packageFqNameAfterJarSeparator(it))) ||
                                    packageFqNameAfterJarSeparator(it.parent).endsWith(packageFqNameString)
                        },
                        /*iterator=*/{
                            // We reach here after filtering above.
                            // Directories in the middle, e.g., com/android, can reach too.
                            if (!it.isDirectory &&
                                // Double-check if its parent (dir) and package fq name of interest matches.
                                packageFqNameAfterJarSeparator(it.parent).endsWith(packageFqNameString) &&
                                isDecompiledKtFile(it)
                            ) {
                                files.add(it)
                            }
                            true
                        }
                    )
                    files
                }
            }
            .mapNotNull {
                getDecompiledKtFileFromVirtualFile(it)
            }
    }

    private fun packageFqNameAfterJarSeparator(
        virtualFile: VirtualFile,
    ): String {
        return virtualFile.path.split(JAR_SEPARATOR).lastOrNull()?.replace("/", ".")
            ?: JAR_SEPARATOR // random string that will bother membership test.
    }

    private fun isDecompiledKtFile(
        virtualFile: VirtualFile,
    ): Boolean {
        if (!virtualFile.name.endsWith(".class")) return false
        return psiManager.findViewProvider(virtualFile)?.baseLanguage == KotlinLanguage.INSTANCE
    }

    private fun getDecompiledKtFileFromVirtualFile(
        virtualFile: VirtualFile,
    ): KtClsFile? {
        return psiManager.findFile(virtualFile) as? KtClsFile
    }

    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        filesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtClassOrObject> { ktClass ->
                ktClass.getClassId() == classId
            }
        }.toList()

    override fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        filesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtTypeAlias> { typeAlias ->
                typeAlias.getClassId() == classId
            }
        }.toList()

    override fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtTypeAlias>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile> =
        filesByPackage(packageFqName)
            .filter { file -> file.hasTopLevelCallables() }
            .toSet()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return getFacadeFilesInPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        filesByPackage(callableId.packageName)
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        filesByPackage(callableId.packageName)
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()

    override fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        filesByPackage(packageFqName)
            .flatMap { it.declarations }
            .filterIsInstance<KtClassOrObject>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }
}

public class KotlinStaticDeclarationProviderFactory(
    private val project: Project,
    private val sourceKtFiles: Collection<KtFile>,
    private val libraryModules: Collection<KtLibraryModule> = emptyList(),
    private val jarFileSystem: CoreJarFileSystem = CoreJarFileSystem(),
) : KotlinDeclarationProviderFactory() {
    private val cache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<GlobalSearchScope, KotlinDeclarationProvider>(ConcurrentHashMap())
    }

    private fun getOrCreateDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return cache.getOrPut(searchScope) {
            KotlinStaticDeclarationProvider(
                project,
                it,
                sourceKtFiles,
                libraryModules,
                jarFileSystem
            )
        }
    }

    override fun createDeclarationProvider(searchScope: GlobalSearchScope): KotlinDeclarationProvider {
        return getOrCreateDeclarationProvider(searchScope)
    }
}
