/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.util

import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.firProvider

fun FirDeclaration.getContainingFile(): FirFile? {
    val provider = moduleData.session.firProvider
    return when (this) {
        is FirFile -> this
        is FirCallableDeclaration<*> -> provider.getFirCallableContainerFile(symbol)
        is FirClassLikeDeclaration<*> -> provider.getFirClassifierContainerFile(symbol)
        else -> error("Unsupported declaration ${this::class.java}")
    }
}
