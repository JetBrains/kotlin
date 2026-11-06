/*
 * Copyright 2010-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.File

internal interface FileSnapshotProvider {
    operator fun get(file: File): FileSnapshot
}

internal class SimpleFileSnapshotProviderImpl : FileSnapshotProvider {
    override fun get(file: File): FileSnapshot {
        assert(!file.isDirectory)
        val length = file.length()
        val hash = file.murmur
        return FileSnapshot(length, hash)
    }
}

private val MURMUR = Hashing.murmur3_128()

internal val File.murmur: ByteArray
    get() = Files.asByteSource(this).hash(MURMUR).asBytes()
