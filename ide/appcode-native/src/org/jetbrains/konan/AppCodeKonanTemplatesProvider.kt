package org.jetbrains.konan

import com.intellij.openapi.application.PathManager
import com.jetbrains.cidr.xcode.templates.XcodeTemplatePathsProvider
import java.io.File

class AppCodeKonanTemplatesProvider : XcodeTemplatePathsProvider {
    override fun getTemplatePaths(): List<File> = listOf(File(PathManager.getPluginsPath() + "/Kotlin/templates/"))
}