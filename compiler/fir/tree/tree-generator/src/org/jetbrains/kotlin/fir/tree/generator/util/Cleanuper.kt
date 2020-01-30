/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.printer.GENERATED_MESSAGE
import java.io.File

fun removePreviousGeneratedFiles(generationPath: File) {
    generationPath.walkTopDown().forEach {
        if (it.isFile && it.readText().contains(GENERATED_MESSAGE)) {
            it.delete()
        }
    }
}