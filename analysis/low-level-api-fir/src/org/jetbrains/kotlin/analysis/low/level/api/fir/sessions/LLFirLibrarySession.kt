/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.PrivateSessionConstructor

/**
 * [org.jetbrains.kotlin.fir.FirSession] responsible for all libraries analysing module transitively depends on
 */
internal class LLFirLibrarySession @PrivateSessionConstructor constructor(
    ktModule: KaModule,
    builtinTypes: BuiltinTypes,
) : LLFirLibraryLikeSession(ktModule, builtinTypes)
