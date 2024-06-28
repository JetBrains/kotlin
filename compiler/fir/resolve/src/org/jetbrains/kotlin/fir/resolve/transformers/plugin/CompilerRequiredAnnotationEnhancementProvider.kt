/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.plugin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol

abstract class CompilerRequiredAnnotationEnhancementProvider : FirSessionComponent {
    abstract fun enhance(enumSymbol: FirClassSymbol<*>, enumEntrySymbol: FirEnumEntrySymbol, session: FirSession): FirEnumEntrySymbol
}

internal val FirSession.compilerRequiredAnnotationEnhancementProvider: CompilerRequiredAnnotationEnhancementProvider? by FirSession.nullableSessionComponentAccessor<CompilerRequiredAnnotationEnhancementProvider>()