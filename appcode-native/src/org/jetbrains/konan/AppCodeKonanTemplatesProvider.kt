/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.application.PathManager
import com.jetbrains.cidr.xcode.templates.XcodeTemplatePathsProvider
import java.io.File

class AppCodeKonanTemplatesProvider : XcodeTemplatePathsProvider {
    override fun getTemplatePaths(): List<File> = listOf(File(PathManager.getPluginsPath() + "/Kotlin/templates/"))
}