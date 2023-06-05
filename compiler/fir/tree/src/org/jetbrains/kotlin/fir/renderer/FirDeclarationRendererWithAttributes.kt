/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol

open class FirDeclarationRendererWithAttributes : FirDeclarationRenderer() {
    override fun FirDeclaration.renderDeclarationAttributes() {
        if (attributes.isNotEmpty()) {
            val attributes = getAttributesWithValues()
                .mapNotNull { (klass, value) -> value?.let { klass to value.renderAsDeclarationAttributeValue() } }
                .ifEmpty { return }
                .joinToString { (name, value) -> "$name=$value" }
            printer.print("[$attributes] ")
        }
    }

    private fun FirDeclaration.getAttributesWithValues(): List<Pair<String, Any?>> {
        return attributeTypesToIds()
            .sortedBy { it.first }
            .map { (klass, index) -> klass to attributes[index] }
    }

    protected open fun attributeTypesToIds(): List<Pair<String, Int>> {
        val attributeMap = FirDeclarationDataRegistry.allValuesThreadUnsafeForRendering()
        return attributeMap.entries
            .map { it.key.substringAfterLast(".") to it.value }
    }

    private fun Any.renderAsDeclarationAttributeValue() = when (this) {
        is FirCallableSymbol<*> -> callableId.toString()
        is FirClassLikeSymbol<*> -> classId.asString()
        is FirProperty -> symbol.callableId.toString()
        is Lazy<*> -> value.toString()
        else -> toString()
    }
}
