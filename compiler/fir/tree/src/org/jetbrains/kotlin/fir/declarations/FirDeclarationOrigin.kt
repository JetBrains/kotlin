/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

sealed class FirDeclarationOrigin {
    object Source : FirDeclarationOrigin()
    object Library : FirDeclarationOrigin()
    object Java : FirDeclarationOrigin()
    object Synthetic : FirDeclarationOrigin()
    object SamConstructor : FirDeclarationOrigin()
    object FakeOverride : FirDeclarationOrigin()
    object Enhancement : FirDeclarationOrigin()

    class Plugin(val key: FirPluginKey) : FirDeclarationOrigin()
}

abstract class FirPluginKey