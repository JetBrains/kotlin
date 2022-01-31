/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.ModuleFileCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.LLFirProvider
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.resolve.providers.firProvider

internal abstract class LLFirResolvableModuleSession(
    builtinTypes: BuiltinTypes,
) : LLFirModuleSession(builtinTypes, Kind.Source) {
    internal val cache: ModuleFileCache get() = (firProvider as LLFirProvider).cache
    abstract val firFileBuilder: FirFileBuilder
}