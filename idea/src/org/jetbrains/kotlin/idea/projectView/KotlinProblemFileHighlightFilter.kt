/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.projectView

import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType

class KotlinProblemFileHighlightFilter : Condition<VirtualFile> {
    override fun value(t: VirtualFile): Boolean = t.fileType == KotlinFileType.INSTANCE
}
