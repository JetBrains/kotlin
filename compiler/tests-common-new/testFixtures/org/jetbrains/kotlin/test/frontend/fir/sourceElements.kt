/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.utils.checkDistinctKeys
import org.jetbrains.kotlin.util.toDebugLocationDescription

/**
 * Checks that all FIR declarations reachable from the given [roots] have distinct [KtSourceElement]s. This is a requirement of FIR as Data
 * (KT-84343) and checked in various tests. Roots are usually [FirFile][org.jetbrains.kotlin.fir.declarations.FirFile]s, but may also be
 * other cache roots such as top-level library declarations.
 *
 * @param lazyErrorTitle A title for the error message if the check fails. The parameters are the two FIR declarations that have conflicting
 *  source elements.
 */
fun checkDistinctSourceElements(roots: List<FirElement>, lazyErrorTitle: (FirDeclaration, FirDeclaration) -> String) {
    checkDistinctKeys(
        roots,
        keyExtractor = { it.symbol.source },
        lazyErrorTitle = lazyErrorTitle,
        formatLocation = { it.source.toDebugLocationDescription() },
    )
}
