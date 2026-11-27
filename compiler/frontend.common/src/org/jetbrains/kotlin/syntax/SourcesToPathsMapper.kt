/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceFileLinesMapping

/** Maps source elements to the paths of the files containing them. */
abstract class SourcesToPathsMapper {
    /**
     * Returns the path of the file containing [sourceElement], or `null` if it's unknown.
     * The element must belong to the parse tree that the mapper is created for.
     */
    abstract fun getSourceFilePath(sourceElement: KtSourceElement): String?
}

/**
 * Unlike a PSI element, an element of such a source doesn't know its file, so the files have to be registered with
 * [registerFileSource] first. A registered file is identified by the [FileKey] that [fileKey] returns for its elements.
 * Besides the paths, the mapper keeps the lines mappings of the registered files.
 */
abstract class NonPsiSourcesToPathsMapper<FileKey : Any> : SourcesToPathsMapper() {
    private val sourcesToPath = mutableMapOf<FileKey, String>()
    private val sourcesToLinesMapping = mutableMapOf<FileKey, KtSourceFileLinesMapping>()

    /**
     * The key identifying the file that contains the source element, e.g., the root node of the light tree.
     * All the elements of a file must have equal keys, and the elements of different files must have different ones.
     */
    protected abstract val KtSourceElement.fileKey: FileKey

    final override fun getSourceFilePath(sourceElement: KtSourceElement): String? {
        return sourcesToPath[sourceElement.fileKey]
    }

    /**
     * Returns the lines mapping of the file containing [sourceElement],
     * or `null` if the file isn't registered or is registered without a mapping.
     */
    fun getSourceFileLinesMapping(sourceElement: KtSourceElement): KtSourceFileLinesMapping? {
        return sourcesToLinesMapping[sourceElement.fileKey]
    }

    /**
     * Registers the file with the given [sourceElement] (the source of the file itself), so its [path] and [linesMapping]
     * can be found by the elements of the file later.
     */
    fun registerFileSource(sourceElement: KtSourceElement, path: String, linesMapping: KtSourceFileLinesMapping?) {
        val fileKey = sourceElement.fileKey
        sourcesToPath[fileKey] = path
        if (linesMapping != null) {
            sourcesToLinesMapping[fileKey] = linesMapping
        }
    }
}
