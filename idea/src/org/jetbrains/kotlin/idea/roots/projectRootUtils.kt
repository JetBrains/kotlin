/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.roots

import com.intellij.openapi.roots.ModifiableRootModel
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.module.JpsModuleSourceRoot
import org.jetbrains.jps.model.module.JpsModuleSourceRootType
import org.jetbrains.kotlin.config.KotlinResourceRootType
import org.jetbrains.kotlin.config.KotlinSourceRootType
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform

private fun JpsModuleSourceRoot.getOrCreateProperties() =
    getProperties(rootType)?.also { (it as? JpsElementBase<*>)?.setParent(null) } ?: rootType.createDefaultProperties()

fun JpsModuleSourceRoot.getMigratedSourceRootTypeWithProperties(): Pair<JpsModuleSourceRootType<JpsElement>, JpsElement>? {
    val currentRootType = rootType
    @Suppress("UNCHECKED_CAST")
    val newSourceRootType: JpsModuleSourceRootType<JpsElement> = when (currentRootType) {
        JavaSourceRootType.SOURCE -> KotlinSourceRootType.Source
        JavaSourceRootType.TEST_SOURCE -> KotlinSourceRootType.TestSource
        JavaResourceRootType.RESOURCE -> KotlinResourceRootType.Resource
        JavaResourceRootType.TEST_RESOURCE -> KotlinResourceRootType.TestResource
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
}

fun TargetPlatformKind<*>.asTargetPlatform() = when (this) {
    is TargetPlatformKind.Jvm -> JvmPlatform
    is TargetPlatformKind.JavaScript -> JsPlatform
    is TargetPlatformKind.Common -> TargetPlatform.Common
}