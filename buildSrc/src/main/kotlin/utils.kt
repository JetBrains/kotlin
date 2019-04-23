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
import org.w3c.dom.Attr
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

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

// extract all the necessary XML elements from the file
// fail if either: one of elements not found or some element found more than once
internal fun File.extractXmlElements(elementTagNames: Set<String>): Map<String, Pair<String, Map<String, String>>> {
    if (elementTagNames.isEmpty()) return emptyMap()

    val result = mutableMapOf<String, Pair<String, Map<String, String>>>()

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
    for (tagName in elementTagNames) {
        val elements = document.getElementsByTagName(tagName)
        check(elements.length == 1) { "$this: ${elements.length} \"$tagName\" elements found, expected amount: 1" }

        val element = elements.item(0) as Element

        val attributes = mutableMapOf<String, String>()
        result[tagName] = element.textContent to attributes

        with (element.attributes) {
            for (i in 0 until length) {
                with (item(i) as Attr) {
                    attributes[name] = value
                }
            }
        }
    }

    return result
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
