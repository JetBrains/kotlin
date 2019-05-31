/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core

import com.intellij.ide.util.DirectoryChooserUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModulePackageIndex
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Query
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.SourceKotlinRootType
import org.jetbrains.kotlin.config.TestSourceKotlinRootType
import org.jetbrains.kotlin.idea.caches.PerModulePackageCacheService
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.roots.invalidateProjectRoots
import org.jetbrains.kotlin.idea.util.rootManager
import org.jetbrains.kotlin.idea.util.sourceRoot
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

fun PsiDirectory.getPackage(): PsiPackage? = JavaDirectoryService.getInstance()!!.getPackage(this)

fun PsiFile.getFqNameByDirectory(): FqName {
    val qualifiedNameByDirectory = parent?.getPackage()?.qualifiedName
    return qualifiedNameByDirectory?.let(::FqName) ?: FqName.ROOT
}

fun PsiDirectory.getFqNameWithImplicitPrefix(): FqName {
    val packageFqName = getPackage()?.qualifiedName?.let(::FqName) ?: FqName.ROOT
    sourceRoot?.takeIf { !it.hasExplicitPackagePrefix(project) }?.let { sourceRoot ->
        val implicitPrefix = PerModulePackageCacheService.getInstance(project).getImplicitPackagePrefix(sourceRoot)
        return FqName.fromSegments((implicitPrefix.pathSegments() + packageFqName.pathSegments()).map { it.asString() })
    }
    return packageFqName
}

private fun VirtualFile.hasExplicitPackagePrefix(project: Project): Boolean =
    toPsiDirectory(project)?.getPackage()?.qualifiedName?.isNotEmpty() == true

fun KtFile.packageMatchesDirectory(): Boolean = packageFqName == getFqNameByDirectory()

fun KtFile.packageMatchesDirectoryOrImplicit() =
    packageFqName == getFqNameByDirectory() || packageFqName == parent?.getFqNameWithImplicitPrefix()

private fun getWritableModuleDirectory(vFiles: Query<VirtualFile>, module: Module, manager: PsiManager): PsiDirectory? {
    for (vFile in vFiles) {
        if (ModuleUtil.findModuleForFile(vFile, module.project) !== module) continue
        val directory = manager.findDirectory(vFile)
        if (directory != null && directory.isValid && directory.isWritable) {
            return directory
        }
    }
    return null
}

private fun findLongestExistingPackage(module: Module, packageName: String): PsiPackage? {
    val manager = PsiManager.getInstance(module.project)

    var nameToMatch = packageName
    while (true) {
        val vFiles = ModulePackageIndex.getInstance(module).getDirsByPackageName(nameToMatch, false)
        val directory = getWritableModuleDirectory(vFiles, module, manager)
        if (directory != null) return directory.getPackage()

        val lastDotIndex = nameToMatch.lastIndexOf('.')
        if (lastDotIndex < 0) {
            return null
        }
        nameToMatch = nameToMatch.substring(0, lastDotIndex)
    }
}

private val kotlinSourceRootTypes: Set<JpsModuleSourceRootType<JavaSourceRootProperties>> =
    setOf(SourceKotlinRootType, TestSourceKotlinRootType) + JavaModuleSourceRootTypes.SOURCES

private fun Module.getNonGeneratedKotlinSourceRoots(): List<VirtualFile> {
    val result = mutableListOf<VirtualFile>()
    val rootManager = ModuleRootManager.getInstance(this)
    for (contentEntry in rootManager.contentEntries) {
        val sourceFolders = contentEntry.getSourceFolders(kotlinSourceRootTypes)
        for (sourceFolder in sourceFolders) {
            if (sourceFolder.jpsElement.getProperties(kotlinSourceRootTypes)?.isForGeneratedSources == true) {
                continue
            }
            result.addIfNotNull(sourceFolder.file)
        }
    }
    return result
}

private fun Module.getOrConfigureKotlinSourceRoots(): List<VirtualFile> {
    val sourceRoots = getNonGeneratedKotlinSourceRoots()
    if (sourceRoots.isNotEmpty()) {
        return sourceRoots
    }
    return runWriteAction {
        val rootDir = rootManager.contentRoots.firstOrNull()
        rootDir?.createChildDirectory(project, "kotlin")
        project.invalidateProjectRoots()
        getNonGeneratedKotlinSourceRoots()
    }
}

private fun getPackageDirectoriesInModule(rootPackage: PsiPackage, module: Module): Array<PsiDirectory> =
    rootPackage.getDirectories(GlobalSearchScope.moduleScope(module))

// This is Kotlin version of PackageUtil.findOrCreateDirectoryForPackage
fun findOrCreateDirectoryForPackage(module: Module, packageName: String): PsiDirectory? {
    val project = module.project
    var existingDirectoryByPackage: PsiDirectory? = null
    var restOfName = packageName
    if (packageName.isNotEmpty()) {
        val rootPackage = findLongestExistingPackage(module, packageName)
        if (rootPackage != null) {
            val beginIndex = rootPackage.qualifiedName.length + 1
            val subPackageName = if (beginIndex < packageName.length) packageName.substring(beginIndex) else ""
            var postfixToShow = subPackageName.replace('.', File.separatorChar)
            if (subPackageName.isNotEmpty()) {
                postfixToShow = File.separatorChar + postfixToShow
            }
            val moduleDirectories = getPackageDirectoriesInModule(rootPackage, module)
            existingDirectoryByPackage =
                DirectoryChooserUtil.selectDirectory(project, moduleDirectories, null, postfixToShow) ?: return null
            restOfName = subPackageName
        }
    }

    val existingDirectory = existingDirectoryByPackage ?: run {
        val sourceRoots = module.getOrConfigureKotlinSourceRoots()
        if (sourceRoots.isEmpty()) {
            return null
        }
        val directoryList = mutableListOf<PsiDirectory>()
        for (sourceRoot in sourceRoots) {
            val directory = PsiManager.getInstance(project).findDirectory(sourceRoot) ?: continue
            directoryList += directory
        }
        val sourceDirectories = directoryList.toTypedArray()
        DirectoryChooserUtil.selectDirectory(
            project, sourceDirectories, null,
            File.separatorChar + packageName.replace('.', File.separatorChar)
        ) ?: return null
    }

    fun getLeftPart(packageName: String): String {
        val index = packageName.indexOf('.')
        return if (index > -1) packageName.substring(0, index) else packageName
    }

    fun cutLeftPart(packageName: String): String {
        val index = packageName.indexOf('.')
        return if (index > -1) packageName.substring(index + 1) else ""
    }

    var psiDirectory = existingDirectory
    while (restOfName.isNotEmpty()) {
        val name = getLeftPart(restOfName)
        val foundExistingDirectory = psiDirectory.findSubdirectory(name)
        psiDirectory = foundExistingDirectory ?: WriteAction.compute<PsiDirectory, Exception> { psiDirectory.createSubdirectory(name) }
        restOfName = cutLeftPart(restOfName)
    }
    return psiDirectory
}