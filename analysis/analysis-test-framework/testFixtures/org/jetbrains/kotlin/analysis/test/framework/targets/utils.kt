/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.targets

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.CallableId

internal fun KaSession.findMatchingCallableSymbols(callableId: CallableId, classSymbol: KaClassSymbol): List<KaCallableSymbol> {
    val declaredSymbols = classSymbol.combinedDeclaredMemberScope
        .callables(callableId.callableName)
        .toList()

    if (declaredSymbols.isNotEmpty()) {
        return declaredSymbols
    }

    // Fake overrides are absent in the declared member scope.
    return classSymbol.combinedMemberScope
        .callables(callableId.callableName)
        .filter { it.containingDeclaration == classSymbol }
        .toList()
}
