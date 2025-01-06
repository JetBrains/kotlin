/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

/**
 * [LLBuiltinSymbolProviderMarker] is used to mark builtin
 * [FirSymbolProvider][org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider]s defined in LL FIR.
 *
 * The marker interface helps to discover builtin symbol providers in the workaround for KT-72390. It should be removed again with that
 * workaround.
 */
interface LLBuiltinSymbolProviderMarker
