/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

// The purpose of utilities in this file is to support different behavior in deserialization according to the given binary file's version.
//
// For example, if we find a bug in serialization/deserialization and would like to fix it _remaining compatible_ with two latest versions
// of Kotlin, we can use methods of this class to fix deserialization of the "future" binaries, and later (in the next major version)
// fix the bug in serialization when the binary version advances to the value supported in the first bug fix.

/**
 * Before Kotlin 1.4, version requirements for nested classes were deserialized incorrectly: the version requirement table was loaded from
 * the outermost class and passed to the nested classes and their members, even though indices of their version requirements were pointing
 * to the other table stored in the nested class (which was not read by deserialization). See KT-25120 for more information
 */
fun isVersionRequirementTableWrittenCorrectly(version: BinaryVersion): Boolean =
    isKotlin1Dot4OrLater(version)

fun isKotlin1Dot4OrLater(version: BinaryVersion): Boolean {
    // All metadata versions (JVM, JS, common) will be advanced to 1.4.0 in Kotlin 1.4
    return version.major == 1 && version.minor >= 4
}
