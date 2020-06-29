package com.jetbrains.cidr.apple.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.modulemap.resolve.ModuleMapManagerRequestor

class GradleAppleModuleMapManagerRequestor : ModuleMapManagerRequestor {
    override fun shouldBuildModuleMaps(project: Project): Boolean = AppleProjectDataService.hasAnyProject(project)
}
