/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.debugger

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider
import org.jetbrains.kotlin.idea.KotlinFileType

class KonanLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
  override fun getFileTypes() = setOf(KotlinFileType.INSTANCE as FileType)
}