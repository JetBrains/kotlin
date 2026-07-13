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
import org.jetbrains.kotlin.fir.resolve.FirResolvedSymbolOrigin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.FqName

/**
 * A class or package qualifier.
 *
 * If [qualifierSymbol] is `null`, [this] is a package qualifier, otherwise this is a class qualifier.
 *
 * The [coneTypeOrNull] is `Unit` when [qualifierSymbol] is `null` or resolves to a class without companion object.
 * Otherwise, it's the type of the resolved object or the class' companion object.
 *
 * Note that even when the qualifier is **not** used as an expression ([accessedObjectSymbol] is `null`), but the resolved class
 * has a companion object, the [coneTypeOrNull] is the type of the companion object
 * This is relied upon by AA & the IDE plugin (mostly in completion) and is difficult to change.
 * Because this can lead to subtle bugs, the overload of |[org.jetbrains.kotlin.fir.types.resolvedType]
 * with a [FirResolvedQualifier] receiver requires an opt-in.
 *
 * TODO for fixing it: KT-87036.
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.resolvedQualifier]
 */
abstract class FirResolvedQualifier : FirExpression(), FirQualifierWithContextSensitiveAlternative {
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
    abstract val packageFqName: FqName
    abstract val relativeClassFqName: FqName?
    /**
     * If not null, refers to the class or **unexpanded** typealias with the name denoted by the qualifier.
     *
     * If it's null, [this] is a package qualifier.
     */
    abstract val qualifierSymbol: FirClassLikeSymbol<*>?
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
    abstract val accessedObjectSymbol: FirRegularClassSymbol?
    abstract val explicitParent: FirResolvedQualifier?
    abstract val isNullableLhsForCallableReference: Boolean
    abstract val resolvedLhsTypeForCallableReferenceOrNull: ConeKotlinType?
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
    abstract val resolvedToCompanionObject: Boolean
    abstract val nonFatalDiagnostics: List<ConeDiagnostic>
    abstract val resolvedSymbolOrigin: FirResolvedSymbolOrigin?
    abstract val typeArguments: List<FirTypeProjection>

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitResolvedQualifier(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformResolvedQualifier(this, data) as E

    abstract override fun replaceContextSensitiveAlternative(newContextSensitiveAlternative: FirPropertyAccessExpression?)

    abstract override fun replaceConeTypeOrNull(newConeTypeOrNull: ConeKotlinType?)

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract fun replaceAccessedObjectSymbol(newAccessedObjectSymbol: FirRegularClassSymbol?)

    abstract fun replaceIsNullableLhsForCallableReference(newIsNullableLhsForCallableReference: Boolean)

    abstract fun replaceResolvedLhsTypeForCallableReferenceOrNull(newResolvedLhsTypeForCallableReferenceOrNull: ConeKotlinType?)

    abstract fun replaceResolvedToCompanionObject(newResolvedToCompanionObject: Boolean)

    abstract fun replaceNonFatalDiagnostics(newNonFatalDiagnostics: List<ConeDiagnostic>)

    abstract fun replaceResolvedSymbolOrigin(newResolvedSymbolOrigin: FirResolvedSymbolOrigin?)

    abstract fun replaceTypeArguments(newTypeArguments: List<FirTypeProjection>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirResolvedQualifier

    abstract fun <D> transformTypeArguments(transformer: FirTransformer<D>, data: D): FirResolvedQualifier
}
