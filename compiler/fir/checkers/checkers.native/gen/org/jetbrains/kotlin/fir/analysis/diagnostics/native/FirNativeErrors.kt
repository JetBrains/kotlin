/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.native

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirNativeErrors {
    // All
    val THROWS_LIST_EMPTY by error0<KtElement>()
    val INCOMPATIBLE_THROWS_OVERRIDE by error1<KtElement, FirRegularClassSymbol>()
    val INCOMPATIBLE_THROWS_INHERITED by error1<KtDeclaration, Collection<FirRegularClassSymbol>>()
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND by error1<KtElement, FqName>()
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY by error0<KtElement>()
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL by error0<KtElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirNativeErrorsDefaultMessages)
    }
}
