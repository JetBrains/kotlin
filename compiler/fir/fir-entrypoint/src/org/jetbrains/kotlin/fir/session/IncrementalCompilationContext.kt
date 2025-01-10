/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.session.environment.AbstractProjectFileSearchScope
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

data class IncrementalCompilationContext(
    // assuming that providers here do not intersect with the one being built from precompiled binaries
    // (maybe easiest way to achieve is to delete libraries
    // TODO: consider passing something more abstract instead of precompiler component, in order to avoid file ops here
    val previousFirSessionsSymbolProviders: Collection<FirSymbolProvider>,
    val precompiledBinariesPackagePartProvider: PackagePartProvider?,
    val precompiledBinariesFileScope: AbstractProjectFileSearchScope?
)
