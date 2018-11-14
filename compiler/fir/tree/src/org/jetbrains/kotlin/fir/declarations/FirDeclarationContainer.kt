/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

interface FirDeclarationContainer {
    // May be only member declarations should be here
    // ?: FirAnonymousInitializer
    val declarations: List<FirDeclaration>
}