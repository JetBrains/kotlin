/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.psi.KtElement

public object KaConstantValueFactory {
    public fun createConstantValue(value: Any?, expression: KtElement? = null): KaConstantValue? = when (value) {
        null -> KaConstantValue.KaNullConstantValue(expression)
        is Boolean -> KaConstantValue.KaBooleanConstantValue(value, expression)
        is Char -> KaConstantValue.KaCharConstantValue(value, expression)
        is Byte -> KaConstantValue.KaByteConstantValue(value, expression)
        is UByte -> KaConstantValue.KaUnsignedByteConstantValue(value, expression)
        is Short -> KaConstantValue.KaShortConstantValue(value, expression)
        is UShort -> KaConstantValue.KaUnsignedShortConstantValue(value, expression)
        is Int -> KaConstantValue.KaIntConstantValue(value, expression)
        is UInt -> KaConstantValue.KaUnsignedIntConstantValue(value, expression)
        is Long -> KaConstantValue.KaLongConstantValue(value, expression)
        is ULong -> KaConstantValue.KaUnsignedLongConstantValue(value, expression)
        is String -> KaConstantValue.KaStringConstantValue(value, expression)
        is Float -> KaConstantValue.KaFloatConstantValue(value, expression)
        is Double -> KaConstantValue.KaDoubleConstantValue(value, expression)
        else -> null
    }
}

public typealias KtConstantValueFactory = KaConstantValueFactory