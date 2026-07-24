/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsSymbolProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.psi.*

@KaImplementationDetail
abstract class KaBaseSymbolProvider<T : KaSession> : KaBaseSessionComponent<T>(), KaInternalsSymbolProvider {
    override fun symbol(declaration: KtDeclaration): KaDeclarationSymbol = withValidityAssertion {
        when (declaration) {
            is KtParameter -> symbol(declaration)
            is KtNamedFunction -> symbol(declaration)
            is KtConstructor<*> -> symbol(declaration)
            is KtTypeParameter -> symbol(declaration)
            is KtTypeAlias -> symbol(declaration)
            is KtEnumEntry -> symbol(declaration)
            is KtFunctionLiteral -> symbol(declaration)
            is KtProperty -> symbol(declaration)
            is KtBackingField -> symbol(declaration)
            is KtObjectDeclaration -> symbol(declaration)
            is KtClassOrObject -> classSymbol(declaration)!!
            is KtPropertyAccessor -> symbol(declaration)
            is KtClassInitializer -> symbol(declaration)
            is KtDestructuringDeclarationEntry -> symbol(declaration)
            is KtScript -> symbol(declaration)
            is KtScriptInitializer -> symbol(declaration.containingDeclaration)
            is KtDestructuringDeclaration -> symbol(declaration)
            else -> error("Cannot build symbol for ${declaration::class}")
        }
    }
}
