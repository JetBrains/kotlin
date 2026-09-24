/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonVarInJsModuleFileChecker
import org.jetbrains.kotlin.fir.declarations.utils.isNativeObject
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JsStandardClassIds

object FirJsVarInJsModuleFileChecker : FirWebCommonVarInJsModuleFileChecker() {
    override val moduleAnnotations: List<ClassId>
        get() = listOf(JsStandardClassIds.Annotations.JsModule, JsStandardClassIds.Annotations.JsNonModule)

    override fun isExternalLike(symbol: FirBasedSymbol<*>, session: FirSession): Boolean {
        return symbol.isNativeObject(session)
    }
}
