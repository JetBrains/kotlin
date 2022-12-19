/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.config.LanguageVersion
import java.io.File

class AdditionalArgs {
    companion object {
        private const val ARGUMENTS_FILE_NAME = "args.txt"
        private val substitutionMap = mapOf("\$PREVIEW_LANGUAGE_VERSION\$" to LanguageVersion.PREVIEW.toString())

        fun parseAdditionalArgs(testDir: File): List<String> =
            File(testDir, ARGUMENTS_FILE_NAME)
                .takeIf { it.exists() }
                ?.readText()
                ?.split(" ", "\n")
                ?.filter { it.isNotBlank() }
                ?.map {
                    if (it in substitutionMap.keys) {
                        substitutionMap[it]!!
                    } else {
                        it
                    }
                }
                ?: emptyList()
    }
}