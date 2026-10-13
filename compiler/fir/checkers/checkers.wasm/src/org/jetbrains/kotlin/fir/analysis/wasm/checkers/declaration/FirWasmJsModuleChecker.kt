/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonJsModuleChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule

object FirWasmJsModuleChecker : FirWebCommonJsModuleChecker() {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    override fun FirBasedSymbol<*>.hasModuleAnnotation(session: FirSession): Boolean =
        hasAnnotation(JsModule, session)

    context(context: CheckerContext)
    override fun isExternal(declaration: FirDeclaration): Boolean =
        declaration.symbol.isEffectivelyExternal(context.session)
}
