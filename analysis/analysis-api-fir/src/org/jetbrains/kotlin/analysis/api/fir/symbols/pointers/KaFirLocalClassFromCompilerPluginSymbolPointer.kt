/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols.pointers

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.fir.extensions.callRefinementExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

@OptIn(FirExtensionApiInternals::class)
internal class KaFirLocalClassFromCompilerPluginSymbolPointer(
    private val psiPointer: SmartPsiElementPointer<out KtElement>,
    private val name: Name,
    private val compilerPluginOrigin: GeneratedDeclarationKey,
    originalSymbol: KaNamedClassSymbol?,
) : KaBaseCachedSymbolPointer<KaNamedClassSymbol>(originalSymbol) {
    override fun restoreIfNotCached(analysisSession: KaSession): KaNamedClassSymbol? {
        require(analysisSession is KaFirSession)
        val call = psiPointer.element?.getOrBuildFirSafe<FirFunctionCall>(analysisSession.resolutionFacade) ?: return null
        val symbol = analysisSession.firSession.extensionService.callRefinementExtensions
            .firstNotNullOfOrNull { it.restoreSymbol(call, name) } ?: return null
        return analysisSession.firSymbolBuilder.classifierBuilder.buildNamedClassSymbol(symbol)
    }

    override fun pointsToTheSameSymbolAs(other: KaSymbolPointer<KaSymbol>): Boolean {
        return this === other ||
                other is KaFirLocalClassFromCompilerPluginSymbolPointer &&
                compilerPluginOrigin == other.compilerPluginOrigin &&
                name == other.name &&
                psiPointer == other.psiPointer
    }
}