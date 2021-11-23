/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.base

import org.jetbrains.kotlin.psi.KtElement

public object KtConstantValueFactory {
    public fun createConstantValue(value: Any?, expression: KtElement? = null): KtConstantValue? = when (value) {
        null -> KtConstantValue.KtNullConstantValue(expression)
        is Boolean -> KtConstantValue.KtBooleanConstantValue(value, expression)
        is Char -> KtConstantValue.KtCharConstantValue(value, expression)
        is Byte -> KtConstantValue.KtByteConstantValue(value, expression)
        is UByte -> KtConstantValue.KtUnsignedByteConstantValue(value, expression)
        is Short -> KtConstantValue.KtShortConstantValue(value, expression)
        is UShort -> KtConstantValue.KtUnsignedShortConstantValue(value, expression)
        is Int -> KtConstantValue.KtIntConstantValue(value, expression)
        is UInt -> KtConstantValue.KtUnsignedIntConstantValue(value, expression)
        is Long -> KtConstantValue.KtLongConstantValue(value, expression)
        is ULong -> KtConstantValue.KtUnsignedLongConstantValue(value, expression)
        is String -> KtConstantValue.KtStringConstantValue(value, expression)
        is Float -> KtConstantValue.KtFloatConstantValue(value, expression)
        is Double -> KtConstantValue.KtDoubleConstantValue(value, expression)
        else -> null
    }
}