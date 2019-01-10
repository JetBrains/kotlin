package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName

object RuntimeNames {
    val symbolName = FqName("kotlin.native.SymbolName")
    val exportForCppRuntime = FqName("kotlin.native.internal.ExportForCppRuntime")
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
}
