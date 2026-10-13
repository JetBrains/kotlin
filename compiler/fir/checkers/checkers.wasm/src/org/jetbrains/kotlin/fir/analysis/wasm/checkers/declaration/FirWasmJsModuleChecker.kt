/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.web.common.checkers.declaration.FirWebCommonJsModuleChecker
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.WebCommonStandardClassIds.Annotations.JsModule

object FirWasmJsModuleChecker : FirWebCommonJsModuleChecker() {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    override fun FirBasedSymbol<*>.findModuleAnnotation(session: FirSession): FirAnnotation? =
        resolvedAnnotationsWithClassIds.getAnnotationByClassId(JsModule, session)
}
