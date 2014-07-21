package org.jetbrains.jet.lang.resolve.android

import com.intellij.openapi.vfs.VirtualFileManager
import java.util.ArrayList
import com.intellij.openapi.project.Project

class CliAndroidUIXmlParser(val project: Project, override val searchPath: String?): AndroidUIXmlParser() {

    override fun lazySetup() {
        val fileManager = VirtualFileManager.getInstance()
        val watchDir = fileManager.findFileByUrl("file://" + searchPath)
        filesToProcess.addAll(watchDir?.getChildren()?.toArrayList() ?: ArrayList(0))
    }
}

