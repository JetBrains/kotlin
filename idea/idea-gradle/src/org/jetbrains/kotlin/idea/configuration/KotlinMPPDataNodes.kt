/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.AbstractNamedData
import com.intellij.openapi.externalSystem.model.project.ExternalSystemSourceType
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.util.Key
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.idea.util.CopyableDataNodeUserDataProperty
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.Serializable
import com.intellij.openapi.externalSystem.model.Key as ExternalKey

var DataNode<out ModuleData>.kotlinSourceSet: KotlinSourceSetInfo?
        by CopyableDataNodeUserDataProperty(Key.create("KOTLIN_SOURCE_SET"))

val DataNode<ModuleData>.kotlinAndroidSourceSets: List<KotlinSourceSetInfo>?
        get() = ExternalSystemApiUtil.getChildren(this, KotlinAndroidSourceSetData.KEY).firstOrNull()?.data?.sourceSetInfos

class KotlinSourceSetInfo(val kotlinModule: KotlinModule) : Serializable {
    var moduleId: String? = null
    var gradleModuleId: String = ""

    var actualPlatforms: KotlinPlatformContainer = KotlinPlatformContainerImpl()

    @Deprecated("Returns only single TargetPlatform", ReplaceWith("actualPlatforms.actualPlatforms"), DeprecationLevel.ERROR)
    val platform: KotlinPlatform
        get() = actualPlatforms.getSinglePlatform()

    var defaultCompilerArguments: CommonCompilerArguments? = null
    var compilerArguments: CommonCompilerArguments? = null
    var dependencyClasspath: List<String> = emptyList()
    var isTestModule: Boolean = false
    var sourceSetIdsByName: MutableMap<String, String> = LinkedHashMap()
}

class KotlinAndroidSourceSetData(
    val sourceSetInfos: List<KotlinSourceSetInfo>
) : AbstractExternalEntityData(GradleConstants.SYSTEM_ID) {
    companion object {
        val KEY = ExternalKey.create(KotlinAndroidSourceSetData::class.java, KotlinTargetData.KEY.processingWeight + 1)
    }
}

class KotlinTargetData(name: String) : AbstractNamedData(GradleConstants.SYSTEM_ID, name) {
    var moduleIds: Set<String> = emptySet()
    var archiveFile: File? = null
    var konanArtifacts: Collection<KonanArtifactModel>? = null

    companion object {
        val KEY = ExternalKey.create(KotlinTargetData::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}

class KotlinOutputPathsData(val paths: MultiMap<ExternalSystemSourceType, String>) : AbstractExternalEntityData(GradleConstants.SYSTEM_ID) {
    companion object {
        val KEY = ExternalKey.create(KotlinOutputPathsData::class.java, ProjectKeys.CONTENT_ROOT.processingWeight + 1)
    }
}