/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import com.intellij.util.text.NameUtilCore
import java.util.*

// Copied from intellij build scripts
object MavenArtifactsBuilder {
    fun generateMavenCoordinates(moduleName: String): MavenArtifact {
        val names = moduleName.split(".")
        if (names.size < 2) {
            error("Cannot generate Maven artifacts: incorrect module name '${moduleName}'")
        }
        val groupId = names.take(2).joinToString(separator = ".")
        val firstMeaningful = if (names.size > 2 && COMMON_GROUP_NAMES.contains(names[1])) 2 else 1
        val artifactId = names.drop(firstMeaningful).flatMap {
            splitByCamelHumpsMergingNumbers(it).map { it.lowercase(Locale.US) }
        }.joinToString(separator = "-")
        return MavenArtifact(groupId, artifactId)
    }

    private fun splitByCamelHumpsMergingNumbers(s: String): List<String> {
        val words: List<String> = NameUtilCore.splitNameIntoWords(s).toList()

        val result = ArrayList<String>()
        var i = 0
        while (i < words.size) {
            val next: String
            if (i < words.size - 1 && Character.isDigit(words[i + 1][0])) {
                next = words[i] + words[i + 1]
                i++
            } else {
                next = words[i]
            }
            result += next
            i++
        }
        return result
    }

    private val COMMON_GROUP_NAMES = setOf("platform", "vcs", "tools", "clouds")
}

data class MavenArtifact(
    val groupId: String,
    val artifactId: String
)