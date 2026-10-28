/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.structure.impl.classFiles

import com.intellij.openapi.vfs.VirtualFile

/**
 * A [BinaryClassFileHandle] over a [VirtualFile].
 *
 * Only the PSI Java view and K1 reach the file through a binary class (see [BinaryJavaClass.virtualFile]); the K2 CLI
 * path never does.
 */
interface VirtualFileBackedClassFileHandle : BinaryClassFileHandle {
    val virtualFile: VirtualFile
}
