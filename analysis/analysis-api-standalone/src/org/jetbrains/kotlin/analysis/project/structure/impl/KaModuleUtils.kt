/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure.impl

import com.intellij.ide.highlighter.JavaFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension

/**
 * Collect source file path from the given [root]
 *
 * E.g., for `project/app/src` as a [root], this will walk the file tree and
 * collect all `.kt`, `.kts`, and `.java` files under that folder.
 *
 * Note that this util gracefully skips [IOException] during file tree traversal.
 * Also, there are no guarantees about the iteration order that subdirectories are visited.
 */
internal fun collectSourceFilePaths(root: Path): List<Path> {
    // NB: [Files#walk] throws an exception if there is an issue during IO.
    // With [Files#walkFileTree] with a custom visitor, we can take control of exception handling.
    val result = mutableListOf<Path>()
    Files.walkFileTree(
        /* start = */ root,
        /* options = */ setOf(FileVisitOption.FOLLOW_LINKS),
        /* maxDepth = */ Int.MAX_VALUE,
        /* visitor = */ object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                return if (Files.isReadable(dir))
                    FileVisitResult.CONTINUE
                else
                    FileVisitResult.SKIP_SUBTREE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (!Files.isRegularFile(file) || !Files.isReadable(file))
                    return FileVisitResult.CONTINUE
                if (file.hasSuitableExtensionToAnalyse()) {
                    result.add(file)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                // TODO: report or log [IOException]?
                // NB: this intentionally swallows the exception, hence fail-safe.
                // Skipping subtree doesn't make any sense, since this is not a directory.
                // Skipping sibling may drop valid file paths afterward, so we just continue.
                return FileVisitResult.CONTINUE
            }
        }
    )
    return result
}

internal fun Path.hasSuitableExtensionToAnalyse(): Boolean {
    val extension = extension

    return extension == KotlinFileType.EXTENSION ||
            extension == KotlinParserDefinition.STD_SCRIPT_SUFFIX ||
            extension == JavaFileType.DEFAULT_EXTENSION
}
