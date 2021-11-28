/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.printer.GENERATED_MESSAGE
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import java.io.File

fun collectPreviouslyGeneratedFiles(generationPath: File): List<File> {
    return generationPath.walkTopDown().filter {
        it.isFile && it.readText().contains(GENERATED_MESSAGE)
    }.toList()
}

fun removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles: List<File>, generatedFiles: List<File>) {
    val generatedFilesPath = generatedFiles.mapTo(mutableSetOf()) { it.absolutePath }

    for (file in previouslyGeneratedFiles) {
        if (file.absolutePath !in generatedFilesPath) {
            if (GeneratorsFileUtil.failOnTeamCity("File delete `${file.absolutePath}`")) continue
            println("Deleted: ${file.absolutePath}")
            file.delete()
        }
    }
}
