/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

import java.io.File

interface ResolutionDumper {
    fun dumpResolutionResult(dumpForFile: RDumpForFile)
}

class ResolutionDumperImpl(private val serializer: ResolutionDumpSerializer, private val root: File) : ResolutionDumper {
    override fun dumpResolutionResult(dumpForFile: RDumpForFile) {
        val destinationFile = File(root, dumpForFile.ownerFile.pathRelativeToRoot)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        destinationFile.createNewFile()

        val serializedResolutionResult = serializer.serialize(dumpForFile)

        destinationFile.outputStream().use {
            it.write(serializedResolutionResult)
        }
    }
}