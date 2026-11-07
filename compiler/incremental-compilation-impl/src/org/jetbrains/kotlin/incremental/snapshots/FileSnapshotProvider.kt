/*
 * Copyright 2010-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.snapshots

import com.google.common.hash.Hashing
import com.google.common.io.Files
import java.io.File

private val FARM = Hashing.farmHashFingerprint64()

internal interface FileSnapshotProvider {
    operator fun get(file: File): FileSnapshot
}

internal class SimpleFileSnapshotProviderImpl : FileSnapshotProvider {
    override fun get(file: File): FileSnapshot {
        assert(!file.isDirectory)
        val length = file.length()
        val hash = Files.asByteSource(file).hash(FARM).asBytes()
        return FileSnapshot(length, hash)
    }
}
