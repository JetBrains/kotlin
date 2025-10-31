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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor

/**
 * Represents a common base for all declarations in FIR, that can be class member declarations (though in fact can be
 * declared e.g. at top-level). This includes all class-like declarations (see [FirClassLikeDeclaration] and
 * all callable declarations (see [FirCallableDeclaration]).
 *
 * Notable properties:
 * - [symbol] — the symbol which serves as a pointer to this declaration.
 * - [typeParameters] — type parameter references declared for this declaration, if any.
 * In certain situations, references to type parameters of its outer classes may also be present in the list. 
 * - [isLocal] — the declaration is non-local (isLocal = false) iff all its ancestors (containing declarations) are
 * either files (see [FirFile]) or classes. A property accessor or a backing field inherits isLocal from its owner property, 
 * otherwise with any callable or anonymous initializer among ancestors, the declaration is local (isLocal = true).
 * In particular, it means that any member declaration of a local class is also local. 
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.memberDeclaration]
 */
sealed class FirMemberDeclaration : FirDeclaration(), FirTypeParameterRefsOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val symbol: FirBasedSymbol<FirDeclaration>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val typeParameters: List<FirTypeParameterRef>
    abstract val status: FirDeclarationStatus
    abstract val isLocal: Boolean

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitMemberDeclaration(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformMemberDeclaration(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirMemberDeclaration

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirMemberDeclaration

    abstract fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirMemberDeclaration
}
