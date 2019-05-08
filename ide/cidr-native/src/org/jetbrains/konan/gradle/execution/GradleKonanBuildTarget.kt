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

    /**
     * Each non-test build target should have null recorded in [baseBuildTarget].
     *
     * Each test build target should have non-null [baseBuildTarget] pointing to non-test build target created
     * for the same MPP Native target.
     *
     * This is necessary to avoid creating run configurations for test executables if there are non-test
     * artifacts (executables, libraries, etc) created for the same MPP Native target. In such case run configuration
     * will be created only for non-test artifact. But user may choose to edit run configuration and switch
     * targets/executables between non-test and test ones.
     *
     * Example 1: "Hello World" project. One MPP Native target. Three executable artifacts:
     * 1. application (release)
     * 2. application (debug)
     * 3. test (debug)
     *
     * The first two artifacts will be united in a single "non-test" [GradleKonanBuildTarget] that will have null
     * in [baseBuildTarget]. The third artifact will be added to the second "test" [GradleKonanBuildTarget] who's
     * [baseBuildTarget] will reference to just created "non-test" [GradleKonanBuildTarget].
     *
     * The logic that creates run configurations will look at [baseBuildTarget] of every [GradleKonanBuildTarget],
     * and if there is null it will take the [GradleKonanBuildTarget] itself. Finally, resulting in creation
     * of a run configuration only for "non-test" [GradleKonanBuildTarget].
     *
     * Example 2: A project with the single MPP Native target that does not define any output kinds. Only single
     * artifact will be produced by such project - test executable.
     *
     * Only one "test" [GradleKonanBuildTarget] will be created for such project. And as far as there is no the
     * appropriate "non-test" [GradleKonanBuildTarget] the [baseBuildTarget] property will be left null.
     *
     * As a result the single run configuration will be created that will run test executable.
     */
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
