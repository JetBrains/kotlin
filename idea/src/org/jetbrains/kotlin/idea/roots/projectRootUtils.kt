/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.JavaProjectRootsUtil.isForGeneratedSources
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
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

private fun JpsModuleSourceRoot.getOrCreateProperties() =
    getProperties(rootType)?.also { (it as? JpsElementBase<*>)?.setParent(null) } ?: rootType.createDefaultProperties()

fun JpsModuleSourceRoot.getMigratedSourceRootTypeWithProperties(): Pair<JpsModuleSourceRootType<JpsElement>, JpsElement>? {
    val currentRootType = rootType
    @Suppress("UNCHECKED_CAST")
    val newSourceRootType: JpsModuleSourceRootType<JpsElement> = when (currentRootType) {
        JavaSourceRootType.SOURCE -> SourceKotlinRootType
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