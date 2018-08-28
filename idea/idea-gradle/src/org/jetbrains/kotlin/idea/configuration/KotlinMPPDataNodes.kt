/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractNamedData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.KotlinModule
import org.jetbrains.kotlin.gradle.KotlinPlatform
import org.jetbrains.kotlin.gradle.KotlinSourceSet
import org.jetbrains.kotlin.idea.util.CopyableDataNodeUserDataProperty
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import com.intellij.openapi.externalSystem.model.Key as ExternalKey

var DataNode<out ModuleData>.kotlinSourceSet: KotlinSourceSetInfo?
        by CopyableDataNodeUserDataProperty(Key.create("KOTLIN_SOURCE_SET"))

var DataNode<out ModuleData>.kotlinTargetDataNode: DataNode<KotlinTargetData>?
        by CopyableDataNodeUserDataProperty(Key.create("KOTLIN_TARGET_DATA_NODE"))

class KotlinSourceSetInfo(val kotlinModule: KotlinModule) {
    var moduleId: String? = null
    var platform: KotlinPlatform = KotlinPlatform.COMMON
    var defaultCompilerArguments: CommonCompilerArguments? = null
    var compilerArguments: CommonCompilerArguments? = null
    var dependencyClasspath: List<String> = emptyList()
    var isTestModule: Boolean = false
    var sourceSetIdsByName: Map<String, String> = emptyMap()
}

class KotlinTargetData(name: String) : AbstractNamedData(GradleConstants.SYSTEM_ID, name) {
    var moduleIds: Set<String> = emptySet()
    var archiveFile: File? = null

    companion object {
        val KEY = ExternalKey.create(KotlinTargetData::class.java, ProjectKeys.MODULE.processingWeight + 1)
    }
}