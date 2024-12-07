/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

fun isJsExportedDeclaration(declaration: FirDeclaration, session: FirSession): Boolean {
    if (declaration !is FirSimpleFunction)
        return false

    if (declaration.visibility != Visibilities.Public)
        return false

    if (declaration.hasAnnotation(WebCommonStandardClassIds.Annotations.JsExport, session))
        return true

    val containerFile = session.firProvider.getFirCallableContainerFile(declaration.symbol)
    return containerFile != null && containerFile.hasAnnotation(WebCommonStandardClassIds.Annotations.JsExport, session)
}