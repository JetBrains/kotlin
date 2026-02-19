/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass

// Memory usage is around 20kb.

// TODO: consider adding box caches for unsigned types.
enum class BoxCache {
    BOOLEAN, BYTE, SHORT, CHAR, INT, LONG
}

val BoxCache.defaultRange
    get() = when (this) {
        BoxCache.BOOLEAN -> (0 to 1)
        BoxCache.BYTE -> (-128 to 127)
        BoxCache.SHORT -> (-128 to 127)
        BoxCache.CHAR -> (0 to 255)
        BoxCache.INT -> (-128 to 127)
        BoxCache.LONG -> (-128 to 127)
    }

fun IrBuiltIns.getKotlinClass(cache: BoxCache): IrClass = when (cache) {
    BoxCache.BOOLEAN -> booleanClass
    BoxCache.BYTE -> byteClass
    BoxCache.SHORT -> shortClass
    BoxCache.CHAR -> charClass
    BoxCache.INT -> intClass
    BoxCache.LONG -> longClass
}.owner
