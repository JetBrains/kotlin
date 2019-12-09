/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.util.prefixBaseNameIfNot
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import org.jetbrains.kotlin.util.suffixIfNot
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.visibleName
import kotlin.random.Random


/**
 * Creates and stores terminal compiler outputs.
 */
class OutputFiles(outputPath: String?, target: KonanTarget, val produce: CompilerOutputKind) {

    private val prefix = produce.prefix(target)
    private val suffix = produce.suffix(target)

    val outputName = outputPath?.removeSuffixIfPresent(suffix) ?: produce.visibleName

    /**
     * Header file for dynamic library.
     */
    val cAdapterHeader      by lazy { File("${outputName}_api.h") }
    val cAdapterDef         by lazy { File("${outputName}.def") }

    /**
     * Main compiler's output file.
     */
    val mainFile = outputName
            .prefixBaseNameIfNeeded(prefix)
            .suffixIfNeeded(suffix)

    val mainFileMangled = if (!produce.isCache) mainFile else {
        (outputName + Random.nextLong().toString())
                .prefixBaseNameIfNeeded(prefix)
                .suffixIfNeeded(suffix)
    }

    private fun String.prefixBaseNameIfNeeded(prefix: String): String {
        return if (produce.isCache)
            prefixBaseNameAlways(prefix)
        else prefixBaseNameIfNot(prefix)
    }

    private fun String.suffixIfNeeded(prefix: String): String {
        return if (produce.isCache)
            suffixAlways(prefix)
        else suffixIfNot(prefix)
    }

    private fun String.prefixBaseNameAlways(prefix: String): String {
        val file = File(this).absoluteFile
        val name = file.name
        val directory = file.parent
        return "$directory/$prefix$name"
    }

    private fun String.suffixAlways(suffix: String) = "$this$suffix"
}