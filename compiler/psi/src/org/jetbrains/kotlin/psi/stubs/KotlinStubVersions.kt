/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs

object KotlinStubVersions {

    // Source stub version should be increased if stub format (org.jetbrains.kotlin.psi.stubs.impl) is changed
    //      or changes are made to the parser that can potentially modify the psi structure of kotlin source code.
    // Though only kotlin declarations (no code in the bodies) are stubbed, please do increase this version
    //      if you are not 100% sure it can be avoided.
    // Increasing this version will lead to reindexing of all kotlin source files on the first IDE startup with the new version.
    const val SOURCE_STUB_VERSION = 159

    // Binary stub version should be increased if stub format (org.jetbrains.kotlin.psi.stubs.impl) is changed
    //      or changes are made to the core stub building code (org.jetbrains.kotlin.idea.decompiler.stubBuilder).
    // Increasing this version will lead to reindexing of all binary files that are potentially kotlin binaries (including all class files).
    private const val BINARY_STUB_VERSION = 97

    // Classfile stub version should be increased if changes are made to classfile stub building subsystem (org.jetbrains.kotlin.idea.decompiler.classFile)
    // Increasing this version will lead to reindexing of all classfiles.
    const val CLASSFILE_STUB_VERSION = BINARY_STUB_VERSION + 0

    // BuiltIn stub version should be increased if changes are made to builtIn stub building subsystem (org.jetbrains.kotlin.idea.decompiler.builtIns)
    // Increasing this version will lead to reindexing of all builtIn files (see KotlinBuiltInFileType).
    const val BUILTIN_STUB_VERSION = BINARY_STUB_VERSION + 5

    // JS stub version should be increased if changes are made to js stub building subsystem (org.jetbrains.kotlin.idea.decompiler.js)
    // Increasing this version will lead to reindexing of js binary files (see KotlinJavaScriptMetaFileType).
    const val JS_STUB_VERSION = BINARY_STUB_VERSION + 4

    // K/N stub version should be increased if changes are made to K/N stub building subsystem.
    // Increasing this version will lead to reindexing of K/N binary files (see KlibMetaFileType).
    const val KOTLIN_NATIVE_STUB_VERSION = BINARY_STUB_VERSION + 7
}
