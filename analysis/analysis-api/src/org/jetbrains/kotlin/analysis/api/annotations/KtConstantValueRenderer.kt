/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.types.ConstantValueKind

public object KtConstantValueRenderer {
    public fun render(value: KtConstantValue): String = buildString {
        when (value) {
            is KtConstantValue.KtStringConstantValue -> {
                append('"')
                append(value.value)
                append('"')
            }
            is KtConstantValue.KtCharConstantValue -> {
                append("'")
                append(value.value)
                append("'")
            }
            is KtConstantValue.KtErrorConstantValue -> {
                append("error(\"${value.errorMessage}\")")
            }
            else -> {
                append(value.value)
            }
        }
    }
}