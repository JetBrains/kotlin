/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

fun methodModelLocator(
    rootDir: File,
    file: File,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    tags: List<String>,
    nativeTestInNonNativeTestInfra: Boolean
): List<MethodModel> = SimpleTestMethodModel(
    rootDir,
    file,
    filenamePattern,
    checkFilenameStartsLowerCase,
    targetBackend,
    tags,
    nativeTestInNonNativeTestInfra
).let { methodModel ->
    listOf(methodModel)
}
