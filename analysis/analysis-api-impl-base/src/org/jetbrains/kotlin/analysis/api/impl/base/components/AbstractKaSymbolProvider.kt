/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolProvider
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter

@KaImplementationDetail
abstract class AbstractKaSymbolProvider<T : KaSession> : KaSessionComponent<T>(), KaSymbolProvider {
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
                is KtClassOrObject -> {
                    val literalExpression = (this as? KtObjectDeclaration)?.parent as? KtObjectLiteralExpression
                    literalExpression?.symbol ?: classSymbol!!
                }
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