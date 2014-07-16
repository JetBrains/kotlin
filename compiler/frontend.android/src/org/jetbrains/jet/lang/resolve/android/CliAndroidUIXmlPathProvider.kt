package org.jetbrains.jet.lang.resolve.android

import java.io.File

class CliAndroidUIXmlPathProvider: AndroidUIXmlPathProvider {

    override fun getPaths(): MutableCollection<File> {
        throw UnsupportedOperationException()
    }
}

