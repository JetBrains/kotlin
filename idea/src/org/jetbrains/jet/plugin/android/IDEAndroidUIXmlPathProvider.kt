package org.jetbrains.jet.plugin.android

import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlPathProvider
import java.io.File

class IDEAndroidUIXmlPathProvider(val project: Project): AndroidUIXmlPathProvider {

    override fun getPaths(): MutableCollection<File> {
        return arrayListOf(File("/tmp/android/"))
    }
}