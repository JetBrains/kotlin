/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TestDataVariants")

package org.jetbrains.kotlin.test

import java.io.File
import java.util.regex.Pattern

/**
 * Whether a file [name] is selected as test data by [filenamePattern] and [excludedPattern].
 */
fun matchesTestDataPattern(name: String, filenamePattern: Pattern, excludedPattern: Pattern?): Boolean =
    filenamePattern.matcher(name).matches() && excludedPattern?.matcher(name)?.matches() != true

/**
 * Whether a file [name] is a variant of another test data file in the same directory, i.e., a part of that test rather than a test of
 * its own. That is the case when removing one or more consecutive dot-separated parts from the part of [name] matched by group 1 of
 * [filenamePattern] gives the name of one of [siblingFileNames] which matches [filenamePattern]. What [name] has before and after group 1
 * stays as it is. For example, `foo.ll.kt` and `foo.ll.wasmJs.kt` are variants of `foo.kt`, and `foo.ll.test.kts` is a variant of
 * `foo.test.kts`. The variants of test data excluded from a test suite are not its tests either.
 *
 * Some parts of group 1 are never removed:
 * - the first part, which ends at the first dot after the first character, so `.ll.kt` is not a variant of `.kt`;
 * - the last part if nothing follows group 1, as it is the extension then: under `^(.*)$`, `foo.ll.kt` is a variant of `foo.kt`, but
 *   `foo.kt` is not a variant of `foo`;
 * - a part not starting with a letter: a part starting with a digit numbers independent test data, e.g., `kt385.109.441.kt`,
 *   `hiddenSuperOverrideIn1.0.kt` or the steps `l1a.4.kt` and `l1.0_init.kt` of an incremental compilation test.
 */
fun isTestDataVariant(name: String, siblingFileNames: Set<String>, filenamePattern: Pattern): Boolean {
    val matcher = filenamePattern.matcher(name)
    if (!matcher.matches() || matcher.groupCount() < 1 || matcher.start(1) < 0) return false

    val prefix = name.substring(0, matcher.start(1))
    val stem = matcher.group(1)
    val extension = name.substring(matcher.end(1))
    val firstDot = stem.indexOf('.', startIndex = 1)
    if (firstDot < 0) return false

    val firstPart = stem.substring(0, firstDot)
    val otherParts = stem.substring(firstDot + 1).split('.')
    val removablePartCount = if (extension.isEmpty()) otherParts.size - 1 else otherParts.size
    for (from in 0 until removablePartCount) {
        for (to in from until removablePartCount) {
            if (!otherParts[to].isVariantPart()) break

            val baseName = buildString {
                append(prefix).append(firstPart)
                for (part in otherParts.subList(0, from)) append('.').append(part)
                for (part in otherParts.subList(to + 1, otherParts.size)) append('.').append(part)
                append(extension)
            }
            if (baseName in siblingFileNames && filenamePattern.matcher(baseName).matches()) return true
        }
    }

    return false
}

private fun String.isVariantPart(): Boolean = isNotEmpty() && Character.isLetter(codePointAt(0))

/**
 * Whether [file], an entry of a directory whose file names are [siblingFileNames] (see [testDataFileNames]), is a variant of another test
 * data file there. A directory is never a variant, as its name is not among [siblingFileNames].
 */
fun isTestDataVariant(file: File, siblingFileNames: Set<String>, filenamePattern: Pattern): Boolean =
    file.name in siblingFileNames && isTestDataVariant(file.name, siblingFileNames, filenamePattern)

/**
 * The names of the files among the [entries] of a directory, i.e., the test data that can be the base of a variant. A directory never is.
 */
fun testDataFileNames(entries: Array<out File>): Set<String> =
    entries.filter { it.isFile }.mapTo(HashSet()) { it.name }
