/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.SourceFile

class VirtualFileBasedSourceElement(val virtualFile: VirtualFile) : SourceElement {
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
