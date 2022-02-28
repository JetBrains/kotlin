/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import com.intellij.psi.PsiElement
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
    private val ktClsFilesByFqNameCache by softCachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker()
    ) {
        ConcurrentMapBasedCache<FqName, List<KtClsFile>>(ConcurrentHashMap())
    }
    private val psiManager by lazy { PsiManager.getInstance(project) }

    private fun sourceFilesByPackage(packageFqName: FqName): Sequence<KtFile> =
        sourceFilesInScope.asSequence()
            .filter { it.packageFqName == packageFqName }

    private fun ktClsFilesByFqName(
        fqName: FqName,
        isPackageName: Boolean = true,
    ): Sequence<KtClsFile> {
        return ktClsFilesByFqNameCache.getOrPut(fqName) {
            decompileKtClsFilesByFqName(it, isPackageName)
        }.asSequence()
    }

    private fun decompileKtClsFilesByFqName(
        fqName: FqName,
        isPackageName: Boolean,
    ): List<KtClsFile> {
        val fqNameString = fqName.asString()
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

    private fun isDecompiledKtFile(
        virtualFile: VirtualFile,
    ): Boolean {
        if (virtualFile.extension?.endsWith(JavaClassFileType.INSTANCE.defaultExtension) != true) return false
        return psiManager.findViewProvider(virtualFile)?.baseLanguage == KotlinLanguage.INSTANCE
    }

    private fun getDecompiledKtFileFromVirtualFile(
        virtualFile: VirtualFile,
    ): KtClsFile? {
        return psiManager.findFile(virtualFile) as? KtClsFile
    }

    private fun canGoInsideForClassIdLookup(psiElement: PsiElement): Boolean {
        // Classes and type aliases in members are local, hence not available via [ClassId]
        return psiElement is KtFile || psiElement is KtClassOrObject || psiElement is KtTypeAlias
    }

    override fun getClassesByClassId(classId: ClassId): Collection<KtClassOrObject> =
        sourceFilesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtClassOrObject>(::canGoInsideForClassIdLookup) { ktClass ->
                ktClass.getClassId() == classId
            }
        }.ifEmpty {
            ktClsFilesByFqName(classId.asSingleFqName(), isPackageName = false).flatMap { file ->
                file.collectDescendantsOfType<KtClassOrObject>(::canGoInsideForClassIdLookup) { ktClass ->
                    ktClass.getClassId() == classId
                }
            }
        }.toList()

    override fun getTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias> =
        sourceFilesByPackage(classId.packageFqName).flatMap { file ->
            file.collectDescendantsOfType<KtTypeAlias>(::canGoInsideForClassIdLookup) { typeAlias ->
                typeAlias.getClassId() == classId
            }
        }.ifEmpty {
            ktClsFilesByFqName(classId.asSingleFqName(), isPackageName = false).flatMap { file ->
                file.collectDescendantsOfType<KtTypeAlias>(::canGoInsideForClassIdLookup) { typeAlias ->
                    typeAlias.getClassId() == classId
                }
            }
        }.toList()

    override fun getTypeAliasNamesInPackage(packageFqName: FqName): Set<Name> =
        sourceFilesByPackage(packageFqName)
            .ifEmpty { ktClsFilesByFqName(packageFqName) }
            .flatMap { it.declarations }
            .filterIsInstance<KtTypeAlias>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getPropertyNamesInPackage(packageFqName: FqName): Set<Name> =
        sourceFilesByPackage(packageFqName)
            .ifEmpty { ktClsFilesByFqName(packageFqName) }
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFunctionsNamesInPackage(packageFqName: FqName): Set<Name> =
        sourceFilesByPackage(packageFqName)
            .ifEmpty { ktClsFilesByFqName(packageFqName) }
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .mapNotNullTo(mutableSetOf()) { it.nameAsName }

    override fun getFacadeFilesInPackage(packageFqName: FqName): Collection<KtFile> =
        sourceFilesByPackage(packageFqName)
            .ifEmpty { ktClsFilesByFqName(packageFqName) }
            .filter { file -> file.hasTopLevelCallables() }
            .toSet()

    override fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile> {
        if (facadeFqName.shortNameOrSpecial().isSpecial) return emptyList()
        return getFacadeFilesInPackage(facadeFqName.parent()) //TODO Not work correctly for classes with JvmPackageName
            .filter { it.javaFileFacadeFqName == facadeFqName }
    }

    override fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty> =
        sourceFilesByPackage(callableId.packageName)
            .ifEmpty {
                // TODO: this triggers de-compilation of all files in the package, which is too expensive.
                //   Decompiling facade files only would be optimal.
                ktClsFilesByFqName(callableId.packageName)
            }
            .flatMap { it.declarations }
            .filterIsInstance<KtProperty>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()

    override fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction> =
        sourceFilesByPackage(callableId.packageName)
            .ifEmpty {
                // TODO: this triggers de-compilation of all files in the package, which is too expensive.
                //   Decompiling facade files only would be optimal.
                ktClsFilesByFqName(callableId.packageName)
            }
            .flatMap { it.declarations }
            .filterIsInstance<KtNamedFunction>()
            .filter { it.nameAsName == callableId.callableName }
            .toList()

    override fun getClassNamesInPackage(packageFqName: FqName): Set<Name> =
        sourceFilesByPackage(packageFqName)
            .ifEmpty { ktClsFilesByFqName(packageFqName) }
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
