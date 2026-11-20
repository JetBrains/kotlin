/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.compilation

import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrScopeCache
import org.jetbrains.kotlin.fir.backend.utils.CodeFragmentConversionData
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol

/**
 * Extensions for the code fragment context transformation.
 * Collects all local declarations of the context file to feed the code fragment transformer later.
 */
internal class CodeFragmentContextFir2IrExtensions(
    delegate: Fir2IrExtensions,
    private val contextDeclarationCache: CodeFragmentContextDeclarationCache?,
) : Fir2IrExtensions by delegate {
    override fun preserveLocalScope(symbol: IrSymbol, cache: Fir2IrScopeCache) {
        contextDeclarationCache?.registerLocalScope(symbol, cache)
    }
}

/**
 * Extensions for code fragment transformation.
 * This replaces captured value calls with injected parameter calls of the code fragment.
 */
internal class CodeFragmentFir2IrExtensions(
    delegate: Fir2IrExtensions,
    private val injectedValueProvider: InjectedSymbolProvider?,
    private val inlineLambdaParametersMapping: Map<FirValueParameterSymbol, FirExpression>,
) : Fir2IrExtensions by delegate {
    override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
        return injectedValueProvider?.invoke(calleeReference, conversionScope)
    }

    override fun codeFragmentConversionData(fragment: FirCodeFragment): CodeFragmentConversionData {
        return injectedValueProvider?.conversionData ?: errorWithFirSpecificEntries("Conversion data is not provided", fir = fragment)
    }

    override fun findInjectedInlineLambdaArgument(parameter: FirValueParameterSymbol): FirExpression? {
        return inlineLambdaParametersMapping[parameter]
    }

}
