/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForDebugInfo
import org.jetbrains.kotlin.fir.types.ConeKotlinType

fun ConeKotlinType.renderForDebugInfo(): String {
    val builder = StringBuilder()
    ConeTypeRendererForDebugInfo(builder).render(this)
    return builder.toString()
}
