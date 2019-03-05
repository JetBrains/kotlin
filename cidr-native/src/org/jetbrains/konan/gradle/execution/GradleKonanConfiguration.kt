/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import org.jetbrains.kotlin.konan.target.CompilerOutputKind

import java.io.File
import java.io.Serializable

/**
 * @author Vladislav.Soroka
 */
class GradleKonanConfiguration(
    val id: String,
    private val name: String,
    val profileName: String,
    val productFile: File?,
    val targetType: CompilerOutputKind?,
    val artifactBuildTaskPath: String,
    artifactCleanTaskPath: String?,
    val projectPath: String,
    val isTests: Boolean
) : Serializable, CidrBuildConfiguration, ProjectModelBuildableElement {
    val isExecutable: Boolean
        get() = targetType == CompilerOutputKind.PROGRAM

    val artifactCleanTaskPath: String? = artifactCleanTaskPath?.takeIf { it.isNotEmpty() }

    override fun getName() = name

    override fun getExternalSource(): ProjectModelExternalSource? = null
}
