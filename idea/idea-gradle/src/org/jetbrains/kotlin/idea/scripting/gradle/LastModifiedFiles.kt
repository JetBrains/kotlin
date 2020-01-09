/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

/**
 * Optimized collection for storing last modified files with ability to
 * get time of last modified file expect given one ([lastModifiedTimeStampExcept]).
 *
 * This is required since Gradle scripts configurations should be updated on
 * each other script changes (but not on the given script changes itself).
 *
 * Actually works by storing two last timestamps with the set of files modified at this times.
 */
class LastModifiedFiles {
    private var last: SimultaneouslyChangedFiles = SimultaneouslyChangedFiles()
    private var previous: SimultaneouslyChangedFiles = SimultaneouslyChangedFiles()

    class SimultaneouslyChangedFiles(
        val ts: Long = Long.MIN_VALUE,
        val fileIds: MutableSet<String> = mutableSetOf()
    )

    @Synchronized
    fun fileChanged(ts: Long, fileId: String) {
        when {
            ts > last.ts -> {
                previous = last
                last = SimultaneouslyChangedFiles(ts, hashSetOf(fileId))
            }
            ts == last.ts -> last.fileIds.add(fileId)
            ts == previous.ts -> previous.fileIds.add(fileId)
        }
    }

    @Synchronized
    fun lastModifiedTimeStampExcept(fileId: String): Long = when {
        last.fileIds.size == 1 && last.fileIds.contains(fileId) -> previous.ts
        else -> last.ts
    }
}