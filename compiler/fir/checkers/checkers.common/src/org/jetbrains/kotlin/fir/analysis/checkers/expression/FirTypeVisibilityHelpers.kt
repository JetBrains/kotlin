/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.isClassLikeVisible
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.visibilityChecker

@OptIn(SymbolInternals::class)
context(context: CheckerContext)
internal fun ConeKotlinType.isTypeVisibilityBroken(
    checkTypeArguments: Boolean,
): Boolean {

    val visibilityChecker = context.session.visibilityChecker
    val classSymbol = toClassSymbol()
    val containingFile = context.containingFileSymbol
    if (classSymbol == null || containingFile == null) return false
    if (
        !visibilityChecker.isClassLikeVisible(
            symbol = classSymbol,
            session = context.session,
            useSiteFileSymbol = containingFile,
            containingDeclarations = context.containingDeclarations
        )
    ) {
        return true
    }
    if (checkTypeArguments) {
        for (typeArgument in typeArguments) {
            if (typeArgument is ConeKotlinTypeProjection) {
                if (typeArgument.type.isTypeVisibilityBroken(checkTypeArguments = true)) return true
            }
        }
    }
    return false
}
