/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

fun TestEntityModel.containsWithoutJvmInline(backend: TargetBackend): Boolean = backend == TargetBackend.JVM_IR && when (this) {
    is ClassModel -> methods.any { it.containsWithoutJvmInline(backend) } || innerTestClasses.any { it.containsWithoutJvmInline(backend) }
    is SimpleTestMethodModel -> file.readLines().any { Regex("^\\s*//\\s*WORKS_WHEN_VALUE_CLASS\\s*$").matches(it) }
    else -> false
}

fun methodModelLocator(
    rootDir: File,
    file: File,
    filenamePattern: Pattern,
    checkFilenameStartsLowerCase: Boolean?,
    targetBackend: TargetBackend,
    skipIgnored: Boolean,
    tags: List<String>
): List<MethodModel> = SimpleTestMethodModel(
    rootDir,
    file,
    filenamePattern,
    checkFilenameStartsLowerCase,
    targetBackend,
    skipIgnored,
    tags
).let { methodModel ->
    if (methodModel.containsWithoutJvmInline(targetBackend)) listOf(true, false).map { WithoutJvmInlineTestMethodModel(methodModel, it) }
    else listOf(methodModel)
}