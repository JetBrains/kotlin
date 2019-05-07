package org.jetbrains.konan.gradle.execution

import com.intellij.icons.AllIcons
import com.jetbrains.cidr.execution.CidrBuildTarget
import org.jetbrains.konan.CidrNativeIconProvider
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import javax.swing.Icon

class GradleKonanBuildTarget(
        val id: String,
        private val name: String,
        private val projectName: String,
        private val configurations: List<GradleKonanConfiguration>
) : java.io.Serializable, CidrBuildTarget<GradleKonanConfiguration> {

    var baseBuildTarget: GradleKonanBuildTarget? = null

    override fun getName() = name
    override fun getProjectName() = projectName
    override fun getBuildConfigurations() = configurations

    override fun getIcon(): Icon? {
        val targetType = configurations.firstOrNull()?.targetType ?: return null
        return when (targetType) {
            PROGRAM -> EXECUTABLE_ICON
            LIBRARY, STATIC, DYNAMIC, FRAMEWORK -> LIBRARY_ICON
            else -> null
        }
    }

    override fun isExecutable() = configurations.any { it.isExecutable }

    override fun toString() = "$name [${configurations.size} configs]"

    companion object {
        val EXECUTABLE_ICON: Icon = CidrNativeIconProvider.getInstance().getExecutableIcon()
        val LIBRARY_ICON: Icon = AllIcons.Nodes.PpLib
    }
}
