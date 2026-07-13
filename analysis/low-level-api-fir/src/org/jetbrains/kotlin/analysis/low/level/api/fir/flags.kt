/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

// TODO (marco): Document.
// TODO (marco): Turn this into a registry flag (cheap to check since it controls the symbol ID factory, not read during symbol ID
//  creation).
internal const val ENABLE_SOURCE_BASED_SYMBOL_IDS = true
