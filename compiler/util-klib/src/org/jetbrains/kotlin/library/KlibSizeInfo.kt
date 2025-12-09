/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.file
import org.jetbrains.kotlin.konan.file.withZipFileSystem
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_BODIES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DEBUG_INFO_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DECLARATIONS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILE_ENTRIES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_SIGNATURES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_STRINGS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_TYPES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * [size] is always in bytes.
 */
class KlibElementWithSize private constructor(val name: String, val size: Long, val children: List<KlibElementWithSize>) {
    constructor(name: String, size: Long) : this(name, size, emptyList())
    constructor(name: String, children: List<KlibElementWithSize>) : this(name, children.sumOf { it.size }, children)
}

fun KotlinLibrary.loadSizeInfo(): KlibElementWithSize? = libraryFile.absoluteFile.loadSizeInfo()

fun KFile.loadSizeInfo(): KlibElementWithSize? {
    val libraryFile = absoluteFile

    return when {
        libraryFile.isFile -> KlibElementWithSize(
            "KLIB file cumulative size",
            libraryFile.withZipFileSystem { fs -> fs.file("/").collectTopLevelElements() }
        )

        !libraryFile.isDirectory -> null

        else -> KlibElementWithSize(
            "KLIB directory cumulative size",
            libraryFile.collectTopLevelElements()
        )
    }
}

/**
 * Returns a depth-first list of (path, sizeBytes), where path is slash-separated from the root.
 */
fun KlibElementWithSize.flatten(): List<Pair<String, Long>> = buildList {
    fun visit(node: KlibElementWithSize, path: String) {
        add(path to node.size)
        for (child in node.children) {
            visit(child, path + "/" + child.name)
        }
    }

    visit(this@flatten, this@flatten.name)
}

private fun KFile.collectTopLevelElements(): List<KlibElementWithSize> {
    var defaultEntry: KFile? = null
    val otherTopLevelEntries = ArrayList<KFile>()

    for (entry in entries) {
        // Expand the contents of the "default" directory, don't show the directory itself.
        if (entry.name.normalizeEntryName() == "default" && entry.isDirectory) {
            defaultEntry = entry
        } else {
            otherTopLevelEntries += entry
        }
    }

    // The contents of the "default" entry go the first, then everything else.
    val topLevelEntries = buildList<KFile> {
        this += defaultEntry?.entries?.sortedBy(KFile::name).orEmpty()
        this += otherTopLevelEntries.sortedBy(KFile::name)
    }

    return topLevelEntries.map { topLevelEntry ->
        when (val topLevelEntryName = topLevelEntry.name.normalizeEntryName()) {
            KLIB_IR_FOLDER_NAME -> buildIrElement(name = "IR (main)", topLevelEntry)
            KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME -> buildIrElement(name = "IR (inlinable functions)", topLevelEntry)
            KLIB_METADATA_FOLDER_NAME -> buildElement(name = "Metadata", topLevelEntry)
            KLIB_TARGETS_FOLDER_NAME -> buildElement(name = "Native-specific binary data", topLevelEntry)
            KLIB_MANIFEST_FILE_NAME -> buildElement(name = "Manifest file", topLevelEntry)
            else -> buildElement(
                name = topLevelEntryName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                topLevelEntry
            )
        }
    }
}

private val KFile.entries: List<KFile> get() = listFiles

private fun KFile.cumulativeSize(): Long = when {
    isFile -> size
    isDirectory -> entries.sumOf { it.cumulativeSize() }
    else -> 0L
}

private fun buildElement(name: String, entry: KFile): KlibElementWithSize {
    return KlibElementWithSize(name, entry.cumulativeSize())
}

private fun buildIrElement(name: String, entry: KFile): KlibElementWithSize {
    val nestedElements = ArrayList<KlibElementWithSize>()

    entry.entries.mapTo(nestedElements) { childEntry ->
        val prettyName = when (val childName = childEntry.name.normalizeEntryName()) {
            KLIB_IR_FILES_FILE_NAME -> "IR files"
            KLIB_IR_FILE_ENTRIES_FILE_NAME -> "IR file entries"
            KLIB_IR_DECLARATIONS_FILE_NAME -> "IR declarations"
            KLIB_IR_BODIES_FILE_NAME -> "IR bodies"
            KLIB_IR_TYPES_FILE_NAME -> "IR types"
            KLIB_IR_SIGNATURES_FILE_NAME -> "IR signatures"
            KLIB_IR_DEBUG_INFO_FILE_NAME -> "IR signatures (debug info)"
            KLIB_IR_STRINGS_FILE_NAME -> "IR strings"
            // TODO: add file entries here!
            else -> childName
        }

        buildElement(prettyName, childEntry)
    }

    return KlibElementWithSize(name, nestedElements.sortedBy { it.name })
}

// On Windows, entry names start with an uppercase character and end with '/' that is not trimmed by `File.name` since the separator is `\`
private fun String.normalizeEntryName() = replaceFirstChar { it.lowercase() }.trimEnd('/')