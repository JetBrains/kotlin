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
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.ClassId
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
    abstract override val classId: ClassId?
    abstract override val symbol: FirClassLikeSymbol<*>?
    abstract override val explicitParent: FirResolvedQualifier?
    abstract override val isNullableLHSForCallableReference: Boolean
    abstract override val resolvedLHSTypeForCallableReferenceOrNull: ConeKotlinType?
    abstract override val resolvedToCompanionObject: Boolean
    /**
     * If true, the qualifier is resolved to an object or companion object and can be used as an expression.
     */
    abstract override val canBeValue: Boolean
    abstract override val isFullyQualified: Boolean
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

    abstract override fun replaceIsNullableLHSForCallableReference(newIsNullableLHSForCallableReference: Boolean)

    abstract override fun replaceResolvedLHSTypeForCallableReferenceOrNull(newResolvedLHSTypeForCallableReferenceOrNull: ConeKotlinType?)

    abstract override fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract override fun replaceCanBeValue(newCanBeValue: Boolean)

    abstract override fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>)

    abstract override fun replaceResolvedSymbolOrigin(newResolvedSymbolOrigin: FirResolvedSymbolOrigin?)

    abstract override fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier

    abstract override fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirErrorResolvedQualifier
}
