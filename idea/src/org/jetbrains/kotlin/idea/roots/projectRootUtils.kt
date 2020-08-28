/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ContentRootData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.service.project.manage.SourceFolderManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiCodeFragment
import com.intellij.psi.PsiFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.URLUtil
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootProperties
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRoot
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import java.util.*

private fun JpsModuleSourceRoot.getOrCreateProperties() =
    getProperties(rootType)?.also { (it as? JpsElementBase<*>)?.setParent(null) } ?: rootType.createDefaultProperties()

fun JpsModuleSourceRoot.getMigratedSourceRootTypeWithProperties(): Pair<JpsModuleSourceRootType<JpsElement>, JpsElement>? {
    val currentRootType = rootType

    @Suppress("UNCHECKED_CAST")
    val newSourceRootType: JpsModuleSourceRootType<JpsElement> = when (currentRootType) {
        JavaSourceRootType.SOURCE -> SourceKotlinRootType as JpsModuleSourceRootType<JpsElement>
        JavaSourceRootType.TEST_SOURCE -> TestSourceKotlinRootType
        JavaResourceRootType.RESOURCE -> ResourceKotlinRootType
        JavaResourceRootType.TEST_RESOURCE -> TestResourceKotlinRootType
        else -> return null
    } as JpsModuleSourceRootType<JpsElement>
    return newSourceRootType to getOrCreateProperties()
}

fun migrateNonJvmSourceFolders(modifiableRootModel: ModifiableRootModel) {
    for (contentEntry in modifiableRootModel.contentEntries) {
        for (sourceFolder in contentEntry.sourceFolders) {
            val (newSourceRootType, properties) = sourceFolder.jpsElement.getMigratedSourceRootTypeWithProperties() ?: continue
            val url = sourceFolder.url
            contentEntry.removeSourceFolder(sourceFolder)
            contentEntry.addSourceFolder(url, newSourceRootType, properties)
        }
    }
    KotlinSdkType.setUpIfNeeded()
}

private val ContentRootData.SourceRoot.pathAsUrl
    get() = VirtualFileManager.constructUrl(URLUtil.FILE_PROTOCOL, FileUtil.toSystemIndependentName(path))

fun populateNonJvmSourceRootTypes(sourceSetNode: DataNode<GradleSourceSetData>, module: Module) {
    val sourceFolderManager = SourceFolderManager.getInstance(module.project)
    val contentRootDataNodes = ExternalSystemApiUtil.findAll(sourceSetNode, ProjectKeys.CONTENT_ROOT)
    val contentRootDataList = contentRootDataNodes.mapNotNull { it.data }
    if (contentRootDataList.isEmpty()) return

    val externalToKotlinSourceTypes = mapOf(
        ExternalSystemSourceType.SOURCE to SourceKotlinRootType,
        ExternalSystemSourceType.TEST to TestSourceKotlinRootType
    )
    externalToKotlinSourceTypes.forEach { (externalType, kotlinType) ->
        val sourcesRoots = contentRootDataList.flatMap { it.getPaths(externalType) }
        sourcesRoots.forEach {
            if (!FileUtil.exists(it.path)) {
                sourceFolderManager.addSourceFolder(module, it.pathAsUrl, kotlinType)
            }
        }
    }
}

fun getKotlinAwareDestinationSourceRoots(project: Project): List<VirtualFile> {
    return ModuleManager.getInstance(project).modules.flatMap { it.collectKotlinAwareDestinationSourceRoots() }
}

private val KOTLIN_AWARE_SOURCE_ROOT_TYPES: Set<JpsModuleSourceRootType<JavaSourceRootProperties>> =
    JavaModuleSourceRootTypes.SOURCES + ALL_KOTLIN_SOURCE_ROOT_TYPES

private fun Module.collectKotlinAwareDestinationSourceRoots(): List<VirtualFile> {
    return rootManager
        .contentEntries
        .asSequence()
        .flatMap { it.getSourceFolders(KOTLIN_AWARE_SOURCE_ROOT_TYPES).asSequence() }
        .filterNot { isForGeneratedSources(it) }
        .mapNotNull { it.file }
        .toList()
}

fun isOutsideSourceRootSet(psiFile: PsiFile?, sourceRootTypes: Set<JpsModuleSourceRootType<*>>): Boolean {
    if (psiFile == null || psiFile is PsiCodeFragment) return false
    val file = psiFile.virtualFile ?: return false
    if (file.fileSystem is NonPhysicalFileSystem) return false
    val projectFileIndex = ProjectRootManager.getInstance(psiFile.project).fileIndex
    return !projectFileIndex.isUnderSourceRootOfType(file, sourceRootTypes) && !projectFileIndex.isInLibrary(file)
}

fun isOutsideKotlinAwareSourceRoot(psiFile: PsiFile?) = isOutsideSourceRootSet(psiFile, KOTLIN_AWARE_SOURCE_ROOT_TYPES)

/**
 * @return list of all java source roots in the project which can be suggested as a target directory for a class created by user
 */
fun getSuitableDestinationSourceRoots(project: Project): List<VirtualFile> {
    val roots = ArrayList<VirtualFile>()
    for (module in ModuleManager.getInstance(project).modules) {
        collectSuitableDestinationSourceRoots(module, roots)
    }
    return roots
}

fun collectSuitableDestinationSourceRoots(module: Module, result: MutableList<VirtualFile>) {
    for (entry in ModuleRootManager.getInstance(module).contentEntries) {
        for (sourceFolder in entry.getSourceFolders(KOTLIN_AWARE_SOURCE_ROOT_TYPES)) {
            if (!isForGeneratedSources(sourceFolder)) {
                ContainerUtil.addIfNotNull(result, sourceFolder.file)
            }
        }
    }
}

fun isForGeneratedSources(sourceFolder: SourceFolder): Boolean {
    val properties = sourceFolder.jpsElement.getProperties(KOTLIN_AWARE_SOURCE_ROOT_TYPES)
    val javaResourceProperties = sourceFolder.jpsElement.getProperties(JavaModuleSourceRootTypes.RESOURCES)
    val kotlinResourceProperties = sourceFolder.jpsElement.getProperties(ALL_KOTLIN_RESOURCE_ROOT_TYPES)
    return properties != null && properties.isForGeneratedSources
            || (javaResourceProperties != null && javaResourceProperties.isForGeneratedSources)
            || (kotlinResourceProperties != null && kotlinResourceProperties.isForGeneratedSources)
}
