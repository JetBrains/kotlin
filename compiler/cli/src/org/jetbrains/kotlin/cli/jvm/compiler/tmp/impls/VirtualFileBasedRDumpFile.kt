/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp.impls

import org.jetbrains.kotlin.cli.jvm.compiler.tmp.RDumpFile
import java.io.File

class FileBasedRDumpFile(val file: File, override val pathRelativeToRoot: String) : RDumpFile {
    override fun getLineNumberByOffset(offset: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getColumnNumberByOffset(offset: Int): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileBasedRDumpFile

        if (pathRelativeToRoot != other.pathRelativeToRoot) return false

        return true
    }

    override fun hashCode(): Int {
        return pathRelativeToRoot.hashCode()
    }
}