/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.kmm.wizard

import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import java.io.File

private val TO_DELETE_LIST = listOf(
    "build.gradle",
    "build.gradle.kts",
    "gradle.properties",
    "local.properties",
    "settings.gradle",
    "settings.gradle.kts"
)

// some object is required to distinguish vfs operations called from IDE and other
private object KmmWizardVfsRequestor

internal fun safelyRemove(file: File) {
    val virtualFile = VfsUtil.findFile(file.toPath(), true) ?: return

    runWriteAction {
        virtualFile.delete(KmmWizardVfsRequestor)
    }
}

internal fun cleanUpRootDir(dir: File) {
    val virtualDir = VfsUtil.findFile(dir.toPath(), true) ?: return

    for (file in virtualDir.children) {
        if (TO_DELETE_LIST.any { file.name == it }) {
            runWriteAction {
                file.delete(KmmWizardVfsRequestor)
            }
        }
    }
}

fun scanForProjectName(lines: List<String>): String? {
    var name: String? = null
    for (line in lines) {
        val trimmed = line.trim()
        if (!trimmed.startsWith("rootProject.name")) continue

        val quoteChar = trimmed.last()
        val firstQuoteIndex = trimmed.indexOfFirst { ch -> ch == quoteChar }

        val nameBegin = firstQuoteIndex + 1
        val nameEnd = trimmed.length - 1

        if (nameBegin > nameEnd) continue

        val nameCandidate = trimmed
            .subSequence(nameBegin, nameEnd)
            .filter { ch -> !ch.isWhitespace() }
            .toString()

        if (nameCandidate.isNotEmpty()) {
            name = nameCandidate
        }
    }

    return name
}
