/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.calls

import org.jetbrains.kotlin.analysis.api.resolution.*

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public val KaCallInfo.calls: List<KaCall> get() = calls

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public fun KaCallInfo.singleFunctionCallOrNull(): KaFunctionCall<*>? = singleFunctionCallOrNull()