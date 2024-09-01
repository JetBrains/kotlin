/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

inline fun <reified S : KaSymbol> KaSession.getSymbolByNameSafe(scope: KtElement, name: String): S? {
    return scope.collectDescendantsOfType<KtDeclaration> { it.name == name }
        .map { it.symbol }
        .filterIsInstance<S>()
        .singleOrNull()
}

inline fun <reified S : KaSymbol> KaSession.getSymbolByName(scope: KtElement, name: String): S {
    return getSymbolByNameSafe(scope, name)
        ?: error("Symbol with $name was not found in scope")
}