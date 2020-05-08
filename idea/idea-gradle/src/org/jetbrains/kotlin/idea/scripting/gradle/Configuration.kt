/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.scripting.gradle.importing.KotlinDslScriptModel

data class ConfigurationData(
    val templateClasspath: List<String>,
    val models: List<KotlinDslScriptModel>
)

class Configuration(val data: ConfigurationData) {

    private val scripts: Map<String, KotlinDslScriptModel>
    val sourcePath: MutableSet<String>

    val classFilePath: MutableSet<String> = mutableSetOf()

    init {
        val allModels = data.models

        scripts = allModels.associateBy { it.file }
        sourcePath = allModels.flatMapTo(mutableSetOf()) { it.sourcePath }

        classFilePath.addAll(data.templateClasspath)
        allModels.flatMapTo(classFilePath) { it.classPath }
    }
    fun scriptModel(file: VirtualFile): KotlinDslScriptModel? {
        return scripts[FileUtil.toSystemDependentName(file.path)]
    }

}