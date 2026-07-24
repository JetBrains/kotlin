/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.io.withZipFileSystem
import org.jetbrains.kotlin.library.KlibConstants.KLIB_MANIFEST_FILE_NAME
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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * [size] is always in bytes.
 */
class KlibElementWithSize private constructor(
    val name: String,
    val size: Long,
    val children: List<KlibElementWithSize>
) {
    var parent: KlibElementWithSize? = null
        private set

    val fullName: String
        get() = parent?.let { "${it.fullName}/$name" } ?: name

    init {
        for (child in children) {
            child.parent = this
        }
    }

    constructor(name: String, size: Long) : this(name, size, emptyList())
    constructor(name: String, children: List<KlibElementWithSize>) : this(name, children.sumOf { it.size }, children)

    /**
     * Recursively collects this element and all its children into a flat list.
     *
     * Each entry in the result is a pair consisting of:
     *  - the element full name
     *  - the element size
     */
    fun flatten(): List<Pair<String, Long>> = listOf(fullName to size) + children.flatMap { it.flatten() }
}

fun loadSizeInfo(klibPath: Path): KlibElementWithSize? {
    return when {
        klibPath.isRegularFile() -> KlibElementWithSize(
            "KLIB file cumulative size",
            klibPath.withZipFileSystem { fs -> fs.getPath("/").collectTopLevelElements() }
        )

        !klibPath.isDirectory() -> null

        else -> KlibElementWithSize(
            "KLIB directory cumulative size",
            klibPath.collectTopLevelElements()
        )
    }
}

private fun Path.collectTopLevelElements(): List<KlibElementWithSize> {
    var defaultEntry: Path? = null
    val otherTopLevelEntries = ArrayList<Path>()

    for (entry: Path in entries) {
        // Expand the contents of the "default" directory, don't show the directory itself.
        if (entry.name.normalizeEntryName() == "default" && entry.isDirectory()) {
            defaultEntry = entry
        } else {
            otherTopLevelEntries.add(entry)
        }
    }

    // The contents of the "default" entry go the first, then everything else.
    val topLevelEntries = buildList<Path> {
        this += defaultEntry?.entries?.sortedBy(Path::name).orEmpty()
        this += otherTopLevelEntries.sortedBy(Path::name)
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

private val Path.entries: List<Path> get() = listDirectoryEntries()

private fun Path.cumulativeSize(): Long = when {
    isRegularFile() -> Files.size(this)
    isDirectory() -> entries.sumOf { it.cumulativeSize() }
    else -> 0L
}

private fun buildElement(name: String, entry: Path): KlibElementWithSize {
    return KlibElementWithSize(name, entry.cumulativeSize())
}

private fun buildIrElement(name: String, entry: Path): KlibElementWithSize {
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

// Entry names start with an uppercase character and end with '/'.
private fun String.normalizeEntryName(): String {
    return replaceFirstChar { it.lowercase() }.trimEnd('/')
}
