/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType

/**
 * KtType to string renderer options
 * @see KtType
 * @see KtSymbolDeclarationRendererProvider.render
 */
public data class KtTypeRendererOptions(
    /**
     * Render type name without package name for not local types
     */
    public val shortQualifiedNames: Boolean = false,
    /**
     * Render public function types public functionN using Kotlin public function type syntax
     * @see public function
     * @sample public function0<Int> returns () -> Int
     */
    public val renderFunctionType: Boolean = true,

    /**
     * When met type with unresolved qualifier, render it as it is resolved
     * When `true` will render as `UnresolvedQualifier`
     * When `false` will render as "ERROR_TYPE <symbol not found for UnresolvedQualifier>"
     */
    public val renderUnresolvedTypeAsResolved: Boolean = true,

    /**
     * Should annotations on types be rendered.
     */
    public val renderTypeAnnotations: Boolean = true
) {
    public companion object {
        public val DEFAULT: KtTypeRendererOptions = KtTypeRendererOptions()
        public val SHORT_NAMES: KtTypeRendererOptions = DEFAULT.copy(shortQualifiedNames = true)
    }
}

/**
 * KtSymbol to string renderer options
 * @see KtSymbol
 * @see KtSymbolDeclarationRendererProvider.render
 */
public data class KtDeclarationRendererOptions(
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
     * Render Unit return type for public functions
     */
    val renderUnitReturnType: Boolean = false,
    /**
     * Normalize java-specific visibilities for java declaration
     */
    val normalizedVisibilities: Boolean = false,
    /**
     * Render members of a classes and objects if any
     */
    val renderClassMembers: Boolean = false,
    /**
     * Approximate Kotlin not-denotable types into denotable for declarations return type
     */
    val approximateTypes: Boolean = false,

    /**
     * Declaration header is something like `public abstract class`, `public fun`, or `private public interface ` in a declaration.
     */
    val renderDeclarationHeader: Boolean = true,

    /**
     * Whether to forcefully add `override` modifier when rendering public functions or properties. Note that the [modifiers] option still
     * controls whether `override` is rendered. That is, if [modifiers] don't contain `override`, then this flag does not have any effect.
     */
    val forceRenderingOverrideModifier: Boolean = false,

    val renderDefaultParameterValue: Boolean = true,

    /**
     * Sort nested declarations by kind (enum values, then constructors, then properties, then functions) and name.
     */
    val sortNestedDeclarations: Boolean = false,
) {
    public companion object {
        public val DEFAULT: KtDeclarationRendererOptions = KtDeclarationRendererOptions()
    }
}

public enum class RendererModifier(public val includeByDefault: Boolean) {
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
    VALUE(true),
    OPERATOR(true)
    ;

    public companion object {
        public val ALL: Set<RendererModifier> = values().toSet()
        public val DEFAULT: Set<RendererModifier> = values().filterTo(mutableSetOf()) { it.includeByDefault }
        public val NONE: Set<RendererModifier> = emptySet()
    }
}

public abstract class KtSymbolDeclarationRendererProvider : KtAnalysisSessionComponent() {
    public abstract fun renderDeclaration(symbol: KtDeclarationSymbol, options: KtDeclarationRendererOptions): String

    public abstract fun render(type: KtType, options: KtTypeRendererOptions): String
}

/**
 * Provides services for rendering Symbols and Types into the Kotlin strings
 */
public interface KtSymbolDeclarationRendererMixIn : KtAnalysisSessionMixIn {
    /**
     * Render symbol into the representable Kotlin string
     */
    public fun KtDeclarationSymbol.render(options: KtDeclarationRendererOptions = KtDeclarationRendererOptions.DEFAULT): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.renderDeclaration(this, options) }

    /**
     * Render kotlin type into the representable Kotlin type string
     */
    public fun KtType.render(options: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT): String =
        withValidityAssertion { analysisSession.symbolDeclarationRendererProvider.render(this, options) }
}