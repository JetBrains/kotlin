/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.types` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaTypeProjection = KaTypeProjection

@Deprecated("Use 'KaTypeProjection' instead", ReplaceWith("KaTypeProjection"))
public typealias KtTypeProjection = KaTypeProjection

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.types` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaStarTypeProjection = KaStarTypeProjection

@Deprecated("Use 'KaTypeArgumentWithVariance' instead", ReplaceWith("KaTypeArgumentWithVariance"))
public typealias KtStarTypeProjection = KaStarTypeProjection

@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.types` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaTypeArgumentWithVariance = KaTypeArgumentWithVariance

@Deprecated("Use 'KaTypeArgumentWithVariance' instead", ReplaceWith("KaTypeArgumentWithVariance"))
public typealias KtTypeArgumentWithVariance = KaTypeArgumentWithVariance