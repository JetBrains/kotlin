/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle

/** The source element of a Java class read from a binary class file: the class file it was read from. */
class BinaryClassSourceElement(val classFile: BinaryClassFileHandle) : SourceElement {
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE

    override fun toString(): String = classFile.pathForDiagnostics
}
