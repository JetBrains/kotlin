/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclaration

abstract class FirPlatformStatusProvider : FirSessionComponent {
    abstract fun <T> withCalculatedStatusOf(declaration: FirDeclaration, block: () -> T): T

    abstract fun calculateStatusFor(declaration: FirDeclaration)

    object Default : FirPlatformStatusProvider() {
        override fun <T> withCalculatedStatusOf(declaration: FirDeclaration, block: () -> T): T = block()

        override fun calculateStatusFor(declaration: FirDeclaration) {}
    }
}

val FirSession.platformStatusProvider: FirPlatformStatusProvider by FirSession.sessionComponentAccessor()
