package org.jetbrains.jet.lang.resolve.android

import java.io.File
import com.intellij.openapi.project.Project
import com.intellij.openapi.components.ServiceManager

trait AndroidUIXmlPathProvider {
    class object {
        fun getInstance(project: Project) = ServiceManager.getService(project, javaClass<AndroidUIXmlPathProvider>())!!
    }
    public fun getPaths(): Collection<File>
}
