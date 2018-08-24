/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler.tmp

class RDumpForFile(val ownerFile: RDumpFile, val results: List<RDumpElement>)


// ====== Hierarchy ==========

interface RDumpElement {
    val offset: Int
    val presentableText: String
}

interface RDumpExpression : RDumpElement {
    val type: RDumpType
}


// ======== Utility =============

interface RDumpFile {
    val pathRelativeToRoot: String

    fun getLineNumberByOffset(offset: Int): Int
    fun getColumnNumberByOffset(offset: Int): Int
}

// TODO: full descriptor?
interface RDumpDescriptor {
    val typeParameters: List<RDumpDescriptor>
}

// TODO: full type?
interface RDumpType

// TODO: full diagnostic?
interface RDumpDiagnostic