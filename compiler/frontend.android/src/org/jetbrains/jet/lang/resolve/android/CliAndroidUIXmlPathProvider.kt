package org.jetbrains.jet.lang.resolve.android

import java.io.File
import java.util.ArrayList

class CliAndroidUIXmlPathProvider(val searchPath: String?): AndroidUIXmlPathProvider {

    override fun getPaths(): MutableCollection<File> {
        return if (searchPath != null) arrayListOf(File(searchPath + "/layout/")) else ArrayList()
    }
}

