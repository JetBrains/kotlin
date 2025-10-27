/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.scopes.FirScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Represents a common base for all class-like declarations in FIR.
 * This includes named and anonymous classes (see [FirClass] and its inheritors
 * [FirRegularClass] and [FirAnonymousObject]) as well as type aliases (see [FirTypeAlias]).
 *
 * Notable properties:
 * - [symbol] — the symbol which serves as a pointer to this class-like declaration.
 * - [typeParameters] — type parameter references declared for this class-like declaration, if any.
 * - [scopeProvider] — a provider used to get different kind of scopes, like a use-site scope, a static scope, or a nested classifier scope
 * (see [FirScopeProvider], [org.jetbrains.kotlin.fir.scopes.FirScope]) for names resolution. There are two main providers used (Kotlin and Java ones).
 * - [isLocal] — the class-like is non-local (isLocal = false) iff all its ancestors (containing declarations) are
 * either files (see [FirFile]) or classes. With any function-like among ancestors, the class-like is local (isLocal = true).
 * In particular, it means that any class-like declared inside a local class is also local. 
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.classLikeDeclaration]
 */
sealed class FirClassLikeDeclaration : FirMemberDeclaration(), FirStatement, FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract override val status: FirDeclarationStatus
    abstract override val isLocal: Boolean
    abstract override val symbol: FirClassLikeSymbol<FirClassLikeDeclaration>
    abstract val deprecationsProvider: DeprecationsProvider
    abstract val scopeProvider: FirScopeProvider

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitClassLikeDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformClassLikeDeclaration(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirClassLikeDeclaration
}
