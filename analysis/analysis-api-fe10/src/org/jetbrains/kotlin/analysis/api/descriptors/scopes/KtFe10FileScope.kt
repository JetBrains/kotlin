/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.scopes

import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtCallableSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

internal class KaFe10FileScope(
    private val ktFile: KtFile,
    private val analysisContext: Fe10AnalysisContext,
    override val token: KaLifetimeToken,
) : KaScope {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        ktFile.declarations.mapNotNullTo(mutableSetOf()) { (it as? KtCallableDeclaration)?.nameAsName }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        ktFile.declarations.mapNotNullTo(mutableSetOf()) { (it as? KtClassLikeDeclaration)?.nameAsName }
    }

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = withValidityAssertion {
        val context = analysisContext.analyze(ktFile)

        sequence {
            for (declaration in ktFile.declarations) {
                if (declaration is KtCallableDeclaration) {
                    val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? CallableDescriptor ?: continue
                    val ktSymbol = descriptor.takeIf { nameFilter(it.name) }?.toKtCallableSymbol(analysisContext) ?: continue
                    yield(ktSymbol)
                }
            }
        }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getCallableSymbols { it in namesSet }
    }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = withValidityAssertion {
        val context = analysisContext.analyze(ktFile)

        sequence {
            for (declaration in ktFile.declarations) {
                if (declaration is KtClassLikeDeclaration) {
                    val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] as? ClassifierDescriptor ?: continue
                    val ktSymbol = descriptor.takeIf { nameFilter(it.name) }?.toKtClassifierSymbol(analysisContext) ?: continue
                    yield(ktSymbol)
                }
            }
        }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return getClassifierSymbols { it in namesSet }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }
}