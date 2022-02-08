/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

internal class KtFe10FileScope(
    private val ktFile: KtFile,
    private val analysisContext: Fe10AnalysisContext,
    override val token: ValidityToken,
) : KtScope {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        ktFile.declarations.mapNotNullTo(mutableSetOf()) { (it as? KtCallableDeclaration)?.nameAsName }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        ktFile.declarations.mapNotNullTo(mutableSetOf()) { (it as? KtClassLikeDeclaration)?.nameAsName }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        val context = analysisContext.analyze(ktFile)

        sequence {
            for (declaration in ktFile.declarations) {
                if (declaration is KtCallableDeclaration) {
                    val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? CallableDescriptor ?: continue
                    val ktSymbol = descriptor.toKtCallableSymbol(analysisContext) ?: continue
                    yield(ktSymbol)
                }
            }
        }
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        val context = analysisContext.analyze(ktFile)

        sequence {
            for (declaration in ktFile.declarations) {
                if (declaration is KtClassLikeDeclaration) {
                    val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? ClassifierDescriptor ?: continue
                    val ktSymbol = descriptor.toKtClassifierSymbol(analysisContext) ?: continue
                    yield(ktSymbol)
                }
            }
        }
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}