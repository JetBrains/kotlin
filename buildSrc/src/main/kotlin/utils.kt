/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("UltimateBuildSrc")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ultimate

import org.gradle.api.Action
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractCopyTask
import java.io.File
import java.util.regex.Pattern

// an adapter to use `from()` in pure Kotlin code in the same way as it is used in *.gradle.kts
internal fun AbstractCopyTask.from(
        sourcePath: Any?,
        configureAction: CopySpec.() -> Unit
): CopySourceSpec = from(sourcePath as Any, Action { configureAction() })

// comment out lines in XML files
internal fun AbstractCopyTask.commentXmlFiles(fileToMarkers: Map<String, List<String>>) {
    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }

        logger.kotlinInfo {
            "File \"${this.path}\" in task ${this@commentXmlFiles.path} has been patched to comment lines with the following items: $markers"
        }
    }

    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

// property-like access to Gradle `Property` instance
internal var Property<String>.value
    get() = get()
    set(value) = set(value)

internal var DirectoryProperty.value: File
    get() = get().asFile
    set(value) = set(value)

internal fun Logger.kotlinInfo(message: () -> String) {
    if (isInfoEnabled) { info("[KOTLIN] ${message()}") }
}
