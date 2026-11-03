/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.syntax.FirModifier
import org.jetbrains.kotlin.fir.syntax.FirModifierList
import org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.syntax.SyntaxId

operator fun FirModifierList?.contains(tokenId: SyntaxId): Boolean = this?.contains(tokenId) == true

context(sessionHolder: SessionHolder)
fun KtSourceElement?.getModifierList(): FirModifierList? = this?.let { withSyntax { it.findModifierList() } }

context(sessionHolder: SessionHolder)
fun FirElement.getModifierList(): FirModifierList? = when {
    source?.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor && source?.syntaxId != KotlinSyntaxNodeTypeIds.PROPERTY_ACCESSOR -> null
    else -> source.getModifierList()
}

context(sessionHolder: SessionHolder)
fun FirElement.getModifier(tokenId: SyntaxId): FirModifier? = getModifierList()?.get(tokenId)

context(sessionHolder: SessionHolder)
fun FirElement.hasModifier(tokenId: SyntaxId): Boolean = tokenId in getModifierList()

@OptIn(SymbolInternals::class)
context(sessionHolder: SessionHolder)
fun FirBasedSymbol<*>.hasModifier(tokenId: SyntaxId): Boolean = fir.hasModifier(tokenId)

internal enum class ValOrVarKeyword(val keyword: String) {
    VAL("val"),
    VAR("var")
}

context(syntax: FirSyntaxComponent<*>)
internal val KtSourceElement?.valOrVarKeyword: ValOrVarKeyword?
    get() = when (with(syntax) { this@valOrVarKeyword?.findValOrVarKeywordSource() }?.syntaxId) {
        KotlinSyntaxTokenIds.VAL_KEYWORD -> ValOrVarKeyword.VAL
        KotlinSyntaxTokenIds.VAR_KEYWORD -> ValOrVarKeyword.VAR
        else -> null
    }
