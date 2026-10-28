/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassFileHandle

/**
 * The source element of a Java class read from [virtualFile]. For a class read from a binary class file, [classFile] is the
 * handle it was read from, which also knows the classpath root the file was found in.
 */
class VirtualFileBasedSourceElement @JvmOverloads constructor(
    val virtualFile: VirtualFile,
    val classFile: BinaryClassFileHandle? = null,
) : SourceElement {
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
