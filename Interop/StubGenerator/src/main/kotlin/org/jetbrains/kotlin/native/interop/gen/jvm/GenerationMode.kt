package org.jetbrains.kotlin.native.interop.gen.jvm

import org.jetbrains.kotlin.native.interop.tool.CommonInteropArguments

enum class GenerationMode {
    SOURCE_CODE, METADATA
}

fun parseGenerationMode(mode: String): GenerationMode? = when(mode) {
    CommonInteropArguments.MODE_METADATA -> GenerationMode.METADATA
    CommonInteropArguments.MODE_SOURCECODE -> GenerationMode.SOURCE_CODE
    else -> null
}