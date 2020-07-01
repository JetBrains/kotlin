package com.jetbrains.konan.debugger

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.cidr.execution.debugger.breakpoints.CidrLineBreakpointFileTypesProvider
import org.jetbrains.kotlin.idea.KotlinFileType

class KonanLineBreakpointFileTypesProvider : CidrLineBreakpointFileTypesProvider {
    override fun getFileTypes() = setOf(KotlinFileType.INSTANCE as FileType)
}
