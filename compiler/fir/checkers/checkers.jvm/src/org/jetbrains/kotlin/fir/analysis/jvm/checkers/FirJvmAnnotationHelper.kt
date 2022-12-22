/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.config.JvmDefaultMode
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.StandardClassIds

fun <D> FirBasedSymbol<out D>.isCompiledToJvmDefault(
    session: FirSession,
    jvmDefaultMode: JvmDefaultMode
): Boolean where D : FirAnnotationContainer, D : FirDeclaration {
    // TODO: Fix support for all cases
    if (getAnnotationByClassId(StandardClassIds.Annotations.JvmDefault, session) != null) return true

    return jvmDefaultMode.forAllMethodsWithBody
}

