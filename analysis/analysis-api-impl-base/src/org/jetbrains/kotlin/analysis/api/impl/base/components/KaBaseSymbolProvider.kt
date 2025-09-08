/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider
import org.jetbrains.kotlin.psi.*

@KaImplementationDetail
abstract class KaBaseSymbolProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaSymbolProvider {
    override val KtDeclaration.symbol: KaDeclarationSymbol
        get() = withValidityAssertion {
            when (this) {
                is KtParameter -> symbol
                is KtNamedFunction -> symbol
                is KtConstructor<*> -> symbol
                is KtTypeParameter -> symbol
                is KtTypeAlias -> symbol
                is KtEnumEntry -> symbol
                is KtFunctionLiteral -> symbol
                is KtProperty -> symbol
                is KtObjectDeclaration -> symbol
                is KtClassOrObject -> classSymbol!!
                is KtPropertyAccessor -> symbol
                is KtClassInitializer -> symbol
                is KtDestructuringDeclarationEntry -> symbol
                is KtScript -> symbol
                is KtScriptInitializer -> containingDeclaration.symbol
                is KtDestructuringDeclaration -> symbol
                else -> error("Cannot build symbol for ${this::class}")
            }
        }
}
