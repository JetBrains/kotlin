/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.kdoc.psi.api.KDocCommentDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNonPublicApi

/**
 * Component that locates KDoc for [KtDeclaration] and [KaDeclarationSymbol].
 *
 * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
 * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
 */
@KaNonPublicApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaKDocProvider : KaSessionComponent {

    /**
     * Resolves KDoc for this [KtDeclaration].
     *
     * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
     * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
     *
     * [KDocCommentDescriptor] for the given [KtDeclaration] is resolved using the following algorithm:
     *
     * 1. [KtDeclaration] has its own KDoc, i.e. [KtDeclaration.getDocComment] returns non-null value.
     * In this case, [KDocCommentDescriptor.primaryTag] is the KDoc's default section and [KDocCommentDescriptor.additionalSections]
     * contain all sections of that KDoc (including the default one).
     *
     * 2. [KtDeclaration] does not have its own KDoc, but its documentation can be derived from its parent's KDoc.
     * This rule is applied in four different cases:
     *
     *         - [KtDeclaration] is a primary constructor and the enclosing class KDoc has a `@constructor` section.
     *         That section is used as [KaKDocComment.primaryTag], and [KaKDocComment.additionalSections] contains
     *         the sections that include `@param` tags from the class's KDoc.
     *
     *         - [KtDeclaration] is a [KtParameter] of the primary constructor. [KaKDocComment.primaryTag] is resolved
     *         from the `@property` or `@param` tags of the class's KDoc. [KaKDocComment.additionalSections] is empty.
     *
     *         - [KtDeclaration] is a [KtParameter] or [KtTypeParameter]. [KaKDocComment.primaryTag] is resolved
     *         from the `@param` tags of the parent's KDoc. [KaKDocComment.additionalSections] is empty.
     *
     *         - [KtDeclaration] is a [KtProperty] that is declared inside a class/object with a KDoc, but referenced
     *         with a `@property` tag in the class/object KDoc. [KaKDocComment.primaryTag] is the `@property` tag,
     *         and [KaKDocComment.additionalSections] is empty.
     *
     *  3. In all other cases this method returns `null`
     *
     * Notes:
     * - KDoc for an element can be resolved whenever we have sources for it. Including external libraries.
     * - This method only resolves KDoc for Kotlin elements. Java elements and Javadoc are not supported.
     */
    @KaNonPublicApi
    @KtNonPublicApi
    public fun KtDeclaration.findKDoc(): KDocCommentDescriptor?


    /**
     * Resolves KDoc for this [KaDeclarationSymbol].
     *
     * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
     * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
     *
     * [KDocCommentDescriptor] for the given [KaDeclarationSymbol] is resolved using the following algorithm:
     *
     * 1. Try resolving the KDoc of the symbol's PSI navigation element via [KaKDocProvider.findKDoc].
     *
     * 2. If the symbol is a callable, walk its [KaCallableSymbol.allOverriddenSymbols] in order and
     * recursively call [findKDoc] on each of them. Return the first non-null result.
     *
     * 3. Resolve the symbol's `expect` declarations via [getExpectsForActual]. Recursively call [findKDoc]
     * on the first (non-null) element of the list. Return the first non-null result.
     *
     *  4. In all other cases this method returns `null`.
     *
     * Notes:
     * - In case of ambiguity, the first match is returned; if nothing is found, `null` is returned;
     * - KDoc for a [KaDeclarationSymbol] can only be resolved when it has a corresponding PSI element.
     *
     * @see [KaKDocProvider.findKDoc] for the implementation details.
     */
    @KaNonPublicApi
    @KtNonPublicApi
    public fun KaDeclarationSymbol.findKDoc(): KDocCommentDescriptor?
}

/**
 * Resolves KDoc for this [KtDeclaration].
 *
 * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
 * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
 *
 * [KDocCommentDescriptor] for the given [KtDeclaration] is resolved using the following algorithm:
 *
 * 1. [KtDeclaration] has its own KDoc, i.e. [KtDeclaration.getDocComment] returns non-null value.
 * In this case, [KDocCommentDescriptor.primaryTag] is the KDoc's default section and [KDocCommentDescriptor.additionalSections]
 * contain all sections of that KDoc (including the default one).
 *
 * 2. [KtDeclaration] does not have its own KDoc, but its documentation can be derived from its parent's KDoc.
 * This rule is applied in four different cases:
 *
 *         - [KtDeclaration] is a primary constructor and the enclosing class KDoc has a `@constructor` section.
 *         That section is used as [KaKDocComment.primaryTag], and [KaKDocComment.additionalSections] contains
 *         the sections that include `@param` tags from the class's KDoc.
 *
 *         - [KtDeclaration] is a [KtParameter] of the primary constructor. [KaKDocComment.primaryTag] is resolved
 *         from the `@property` or `@param` tags of the class's KDoc. [KaKDocComment.additionalSections] is empty.
 *
 *         - [KtDeclaration] is a [KtParameter] or [KtTypeParameter]. [KaKDocComment.primaryTag] is resolved
 *         from the `@param` tags of the parent's KDoc. [KaKDocComment.additionalSections] is empty.
 *
 *         - [KtDeclaration] is a [KtProperty] that is declared inside a class/object with a KDoc, but referenced
 *         with a `@property` tag in the class/object KDoc. [KaKDocComment.primaryTag] is the `@property` tag,
 *         and [KaKDocComment.additionalSections] is empty.
 *
 *  3. In all other cases this method returns `null`
 *
 * Notes:
 * - KDoc for an element can be resolved whenever we have sources for it. Including external libraries.
 * - This method only resolves KDoc for Kotlin elements. Java elements and Javadoc are not supported.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaNonPublicApi
@KtNonPublicApi
@KaContextParameterApi
context(s: KaSession)
public fun KtDeclaration.findKDoc(): KDocCommentDescriptor? {
    return with(s) {
        findKDoc()
    }
}

/**
 * Resolves KDoc for this [KaDeclarationSymbol].
 *
 * This is part of a non-public API intended for use only by IDE and Dokka. Current implementation
 * is based on the original `findKDoc` logic from Kotlin IDE Plugin.
 *
 * [KDocCommentDescriptor] for the given [KaDeclarationSymbol] is resolved using the following algorithm:
 *
 * 1. Try resolving the KDoc of the symbol's PSI navigation element via [KaKDocProvider.findKDoc].
 *
 * 2. If the symbol is a callable, walk its [KaCallableSymbol.allOverriddenSymbols] in order and
 * recursively call [findKDoc] on each of them. Return the first non-null result.
 *
 * 3. Resolve the symbol's `expect` declarations via [getExpectsForActual]. Recursively call [findKDoc]
 * on the first (non-null) element of the list. Return the first non-null result.
 *
 *  4. In all other cases this method returns `null`.
 *
 * Notes:
 * - In case of ambiguity, the first match is returned; if nothing is found, `null` is returned;
 * - KDoc for a [KaDeclarationSymbol] can only be resolved when it has a corresponding PSI element.
 *
 * @see [KaKDocProvider.findKDoc] for the implementation details.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaNonPublicApi
@KtNonPublicApi
@KaContextParameterApi
context(s: KaSession)
public fun KaDeclarationSymbol.findKDoc(): KDocCommentDescriptor? {
    return with(s) {
        findKDoc()
    }
}
