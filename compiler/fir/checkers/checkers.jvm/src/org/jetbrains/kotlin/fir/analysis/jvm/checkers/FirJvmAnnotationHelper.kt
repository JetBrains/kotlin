/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.config.jvmDefaultMode
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotationWithClassId
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.JvmStandardClassIds

/**
 * The containing symbol is resolved using the declaration-site session.
 */
fun <D> FirBasedSymbol<D>.isCompiledToJvmDefault(
    session: FirSession,
): Boolean where D : FirAnnotationContainer, D : FirDeclaration {
    if (hasAnnotationWithClassId(JvmStandardClassIds.Annotations.JvmDefault, session)) return true

    val container = getContainingClassSymbol()
    if (container !is FirRegularClassSymbol) return session.languageVersionSettings.jvmDefaultMode.isEnabled

    // A source interface is compiled with the mode of its own module, which differs from the use-site one when the IDE analyzes
    // a dependency module from sources.
    if (container.origin.fromSource) return container.moduleData.session.languageVersionSettings.jvmDefaultMode.isEnabled

    // Opt-in is fine here because this flag is only possible for deserialized declarations, and it's set during deserialization.
    @OptIn(SymbolInternals::class)
    return container.fir.isNewPlaceForBodyGeneration == true
}
