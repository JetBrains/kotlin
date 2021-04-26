/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

/**
 * KtType to string renderer options
 * @see KtType
 * @see KtSymbolDeclarationRendererProvider.render
 */
data class KtTypeRendererOptions(
    /**
     * Render type name without package name for not local types
     */
    val shortQualifiedNames: Boolean = false,
    /**
     * Render function types FunctionN using Kotlin function type syntax
     * @see Function
     * @sample Function0<Int> returns () -> Int
     */
    val renderFunctionType: Boolean = true,
) {
    companion object {
        val DEFAULT = KtTypeRendererOptions()
        val SHORT_NAMES = DEFAULT.copy(shortQualifiedNames = true)
    }
}

/**
 * KtSymbol to string renderer options
 * @see KtSymbol
 * @see KtSymbolDeclarationRendererProvider.render
 */
data class KtDeclarationRendererOptions(
    /**
     * Set of modifiers that needed to be rendered
     * @see RendererModifier
     */
    val modifiers: Set<RendererModifier> = RendererModifier.ALL,
    /**
     * Type render options @see KtTypeRendererOptions
     * @see KtTypeRendererOptions
     */
    val typeRendererOptions: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT,
    /**
     * Render Unit return type for functions
     */
    val renderUnitReturnType: Boolean = false,
    /**
     * Normalize java-specific visibilities for java declaration
     */
    val normalizedVisibilities: Boolean = false,
    /**
     * Render containing declarations
     */
    val renderContainingDeclarations: Boolean = false,
    /**
     * Approximate Kotlin not-denotable types into denotable for declarations return type
     */
    val approximateTypes: Boolean = false,
) {
    companion object {
        val DEFAULT = KtDeclarationRendererOptions()
    }
}

enum class RendererModifier(val includeByDefault: Boolean) {
    VISIBILITY(true),
    MODALITY(true),
    OVERRIDE(true),
    ANNOTATIONS(false),
    INNER(true),
    DATA(true),
    INLINE(true),
    EXPECT(true),
    ACTUAL(true),
    CONST(true),
    LATEINIT(true),
    FUN(true),
    VALUE(true)
    ;

    companion object {
        val ALL = values().toSet()
    }
}

abstract class KtSymbolDeclarationRendererProvider : KtAnalysisSessionComponent() {
    abstract fun render(symbol: KtSymbol, options: KtDeclarationRendererOptions): String
    abstract fun render(type: KtType, options: KtTypeRendererOptions): String
}

/**
 * Provides services for rendering Symbols and Types into the Kotlin strings
 */
interface KtSymbolDeclarationRendererMixIn : KtAnalysisSessionMixIn {
    /**
     * Render symbol into the representable Kotlin string
     */
    fun KtSymbol.render(options: KtDeclarationRendererOptions = KtDeclarationRendererOptions.DEFAULT): String =
        analysisSession.symbolDeclarationRendererProvider.render(this, options)

    /**
     * Render kotlin type into the representable Kotlin type string
     */
    fun KtType.render(options: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT): String =
        analysisSession.symbolDeclarationRendererProvider.render(this, options)
}