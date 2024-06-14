/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolBasedReference

@OptIn(KaAnalysisApiInternals::class)
@Deprecated(
    "The API has been moved into `org.jetbrains.kotlin.analysis.api.resolution` package",
    level = DeprecationLevel.HIDDEN,
)
public typealias KaSymbolBasedReference = KaSymbolBasedReference

@OptIn(KaAnalysisApiInternals::class)
@Deprecated("Use 'KaSymbolBasedReference' instead", ReplaceWith("KaSymbolBasedReference"))
public typealias KtSymbolBasedReference = KaSymbolBasedReference