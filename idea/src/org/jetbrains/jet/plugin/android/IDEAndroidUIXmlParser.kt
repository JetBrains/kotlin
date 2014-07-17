package org.jetbrains.jet.plugin.android

import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlParser
import com.intellij.openapi.project.Project

class IDEAndroidUIXmlParser(project: Project): AndroidUIXmlParser() {
    override val searchPath: String? = project.getBasePath() + "/res/layout/"
}

