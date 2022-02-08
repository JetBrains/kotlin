/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import org.jetbrains.kotlin.generators.model.*
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

const val WORKS_WHEN_VALUE_CLASS = "WORKS_WHEN_VALUE_CLASS"
// will replace OPTIONAL_JVM_INLINE_ANNOTATION with @JvmInline or remove it depending on compiler backend
// for JVM IR both ones are generated according to value classes feature (https://github.com/Kotlin/KEEP/issues/237)

fun TestEntityModel.containsWithoutJvmInline(): Boolean = when (this) {
    is ClassModel -> methods.any { it.containsWithoutJvmInline() } || innerTestClasses.any { it.containsWithoutJvmInline() }
    is SimpleTestMethodModel -> file.isFile && file.readLines().any { Regex("^\\s*//\\s*$WORKS_WHEN_VALUE_CLASS\\s*$").matches(it) }
    else -> false
}

private fun TargetBackend.isRecursivelyCompatibleWith(targetBackend: TargetBackend): Boolean =
    this == targetBackend || this != TargetBackend.ANY && this.compatibleWith.isRecursivelyCompatibleWith(targetBackend)

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
    if (methodModel.containsWithoutJvmInline()) {
        val isWithAnnotationAndIsWithPostfix = when {
            targetBackend.isRecursivelyCompatibleWith(TargetBackend.JVM) -> listOf(true to false)
            targetBackend == TargetBackend.ANY -> listOf(null to false)
            else -> listOf(false to false)
        }
        isWithAnnotationAndIsWithPostfix.map { (ann, post) -> WithoutJvmInlineTestMethodModel(methodModel, ann, post) }
    } else listOf(methodModel)
}