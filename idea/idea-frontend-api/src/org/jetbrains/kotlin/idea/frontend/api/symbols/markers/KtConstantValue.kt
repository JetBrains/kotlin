/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols.markers

sealed class KtConstantValue
object KtUnsupportedConstantValue : KtConstantValue()

data class KtSimpleConstantValue<T>(val constant: T?) : KtConstantValue()

// If it it UBite 200 then the runtimeConstant would be (-56).toByte()
data class KtUnsignedConstantValue<T>(val runtimeConstant: T?) : KtConstantValue()