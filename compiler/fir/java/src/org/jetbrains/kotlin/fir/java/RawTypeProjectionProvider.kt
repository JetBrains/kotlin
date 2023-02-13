/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.eraseToUpperBounds

abstract class RawTypeProjectionProvider : FirSessionComponent {
    abstract fun typeParameters(classSymbol: FirRegularClassSymbol, session: FirSession): Array<ConeTypeProjection>
}

object CompilerRawTypeProjectionProvider : RawTypeProjectionProvider() {
    override fun typeParameters(classSymbol: FirRegularClassSymbol, session: FirSession): Array<ConeTypeProjection> {
        return classSymbol.typeParameterSymbols.eraseToUpperBounds(session)
    }
}

val FirSession.rawTypeProjectionProvider: RawTypeProjectionProvider by FirSession.sessionComponentAccessor<RawTypeProjectionProvider>()