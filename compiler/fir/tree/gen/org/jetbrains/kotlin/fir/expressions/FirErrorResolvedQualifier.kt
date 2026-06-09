/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirIdeOnly
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.FqName

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.errorResolvedQualifier]
 */
abstract class FirErrorResolvedQualifier : FirResolvedQualifier(), FirDiagnosticHolder {
    abstract override val source: KtSourceElement?
    /**
     * For resolved qualifier, it contains either null or a simple name property access which would be used for checking
     * if context-sensitive resolution might be used instead of the owner qualifier. 
     * For example, if the owner is `MyEnum.X`, then contextSensitiveAlternative would be just `X`.
     *
     * Only used in ideMode to find out if the property access can be replaced with a simple name expression
     * via context-sensitive resolution, so the reference shortener/inspections might use this information.
     *
     * Even in ideMode, it's only initialized if there is a reason to assume that it might be the case of CSR, e.g., 
     * it should be left `null` for ContextIndependent resolution mode.
     */
    @FirIdeOnly
    abstract override val contextSensitiveAlternative: FirPropertyAccessExpression?
    @UnresolvedExpressionTypeAccess
    abstract override val coneTypeOrNull: ConeKotlinType?
    abstract override val annotations: List<FirAnnotation>
    abstract override val packageFqName: FqName
    abstract override val relativeClassFqName: FqName?
    /**
     * If not null, refers to the class or **unexpanded** typealias with the name denoted by the qualifier.
     *
     * If it's null, [this] is a package qualifier.
     */
    abstract override val qualifierSymbol: FirClassLikeSymbol<*>?
    /**
     * ### During resolution
     *
     * If the [qualifierSymbol] resolves to a named object (or a typealias of that object),
     * it's the symbol of that named object.
     *
     * If the [qualifierSymbol] resolves to a class with companion object (or a typealias of that class),
     * it's the symbol of the companion object.
     *
     * Otherwise `null`.
     *
     * A not-null value indicates that the qualifier _can_ be used as an expression.
     *
     * ### After resolution
     *
     * Same as above but **if and only if** the qualifier is used as an expression.
     */
    abstract override val accessedObjectSymbol: FirRegularClassSymbol?
    abstract override val explicitParent: FirResolvedQualifier?
    abstract override val isNullableLhsForCallableReference: Boolean
    abstract override val resolvedLhsTypeForCallableReferenceOrNull: ConeKotlinType?
    /**
     * ### During resolution
     *
     * True, if [qualifierSymbol] refers to a class (or typealias of) with a companion object.
     *
     * ### After resolution
     *
     * Same as above **and** the qualifier is used as an expression.
     *
     * Technically this property is redundant because the information can be deduced from the combination of
     * [qualifierSymbol] and [accessedObjectSymbol], but it would require carefully expanding type aliases and comparing
     * symbols.
     */
    abstract override val resolvedToCompanionObject: Boolean
    abstract override val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract override val resolvedSymbolOrigin: FirResolvedSymbolOrigin?
    abstract override val typeArguments: List<FirTypeProjection>
    abstract override val diagnostic: ConeDiagnostic

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitErrorResolvedQualifier(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformErrorResolvedQualifier(this, data) as E

    abstract override fun replaceContextSensitiveAlternative(newContextSensitiveAlternative: FirPropertyAccessExpression?)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceAccessedObjectSymbol(newAccessedObjectSymbol: FirRegularClassSymbol?)

    abstract override fun replaceIsNullableLhsForCallableReference(newIsNullableLhsForCallableReference: Boolean)

    abstract override fun replaceResolvedLhsTypeForCallableReferenceOrNull(newResolvedLhsTypeForCallableReferenceOrNull: ConeKotlinType?)

    abstract override fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract override fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>)

    abstract override fun replaceResolvedSymbolOrigin(newResolvedSymbolOrigin: FirResolvedSymbolOrigin?)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier
}
