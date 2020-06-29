package org.jetbrains.konan.resolve.symbols

import com.jetbrains.cidr.lang.symbols.OCSymbolOffsetUtil
import org.jetbrains.kotlin.backend.konan.objcexport.Stub

val Stub<*>.offset: Long
    get() = psi?.let { OCSymbolOffsetUtil.getComplexOffset(it.textOffset, 0) } ?: 0
