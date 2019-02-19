package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.name.FqName

object RuntimeNames {
    val symbolName = FqName("kotlin.native.SymbolName")
    val exportForCppRuntime = FqName("kotlin.native.internal.ExportForCppRuntime")
    val exportForCompilerAnnotation = FqName("kotlin.native.internal.ExportForCompiler")
    val exportTypeInfoAnnotation = FqName("kotlin.native.internal.ExportTypeInfo")
    val cCall = FqName("kotlinx.cinterop.internal.CCall")
}
