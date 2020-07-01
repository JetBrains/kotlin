package org.jetbrains.konan.gradle.execution

import com.intellij.openapi.roots.ProjectModelBuildableElement
import com.intellij.openapi.roots.ProjectModelExternalSource
import com.jetbrains.cidr.execution.CidrBuildConfiguration
import org.jetbrains.kotlin.gradle.KonanRunConfigurationModel
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
    runConfiguration: KonanRunConfigurationModel,
    val isTests: Boolean
) : Serializable, CidrBuildConfiguration, ProjectModelBuildableElement {
    val isExecutable: Boolean
        get() = targetType == CompilerOutputKind.PROGRAM

    val artifactCleanTaskPath: String? = artifactCleanTaskPath?.takeIf { it.isNotEmpty() }

    val runConfiguration: KonanRunConfigurationModel? = runConfiguration.takeIf { it.isNotEmpty() }

    override fun getName() = name

    override fun getExternalSource(): ProjectModelExternalSource? = null
}
