/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.symbols.impl.FirAnonymousFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeSimpleKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * Represents an anonymous function or lambda expression in FIR.
 * Unlike [FirNamedFunction], this declaration has no name. Lambdas are represented
 * as anonymous functions with [isLambda] set to `true`.
 *
 * Notable properties:
 * - [typeRef] — the functional type of this anonymous function.
 * - [symbol] — the symbol which serves as a pointer to this anonymous function.
 * - [label] — an optional label attached to the anonymous function (e.g., `label@ { ... }`).
 * - [invocationKind] — how many times the function is expected to be invoked (see [org.jetbrains.kotlin.contracts.description.EventOccurrencesRange]).
 * - [inlineStatus] — information about inlining status of this function (inline, noinline, or crossinline).
 * - [isLambda] — whether this anonymous function originates from a lambda expression or not.
 * - [typeParameters] — type parameters declared for the anonymous function, if any.
 * (always empty for a green code, but technically they can exist).
 * - [hasExplicitParameterList] — whether the parameter list is explicitly specified (affects implicit `it`).
 * - [valueParameters] — the list of the function's value parameters
 * (for a lambda, the list can be empty at creation and filled later during resolution).
 * - [contextParameters] — context parameters of the function, if any.
 * - [receiverParameter] — the extension receiver parameter if present, otherwise null.
 * - [returnTypeRef] — the declared return type of the function
 * (if type is assumed to be inferred, [FirImplicitTypeRef] is used here).
 * - [body] — the function body, if present, otherwise null.
 * - [contractDescription] — contract description for the function, if present (see [FirContractDescription] and its inheritors).
 * - [annotations] — annotations present on the function, if any.
 * - [isLocal] — always true for anonymous functions. 
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTree.anonymousFunction]
 */
abstract class FirAnonymousFunction : FirFunction(), FirTypeParametersOwner, FirContractDescriptionOwner {
    abstract override val source: KtSourceElement?
    abstract override val annotations: List<FirAnnotation>
    abstract override val moduleData: FirModuleData
    abstract override val origin: FirDeclarationOrigin
    abstract override val attributes: FirDeclarationAttributes
    abstract override val status: FirDeclarationStatus
    abstract override val isLocal: Boolean
    abstract override val returnTypeRef: FirTypeRef
    abstract override val receiverParameter: FirReceiverParameter?
    abstract override val deprecationsProvider: DeprecationsProvider
    abstract override val containerSource: DeserializedContainerSource?
    abstract override val dispatchReceiverType: ConeSimpleKotlinType?
    abstract override val contextParameters: List<FirValueParameter>
    abstract override val controlFlowGraphReference: FirControlFlowGraphReference?
    abstract override val valueParameters: List<FirValueParameter>
    abstract override val body: FirBlock?
    abstract override val contractDescription: FirContractDescription?
    abstract override val symbol: FirAnonymousFunctionSymbol
    abstract val label: FirLabel?
    abstract val invocationKind: EventOccurrencesRange?
    abstract val inlineStatus: InlineStatus
    abstract val isLambda: Boolean
    abstract val hasExplicitParameterList: Boolean
    abstract override val typeParameters: List<FirTypeParameter>
    abstract val typeRef: FirTypeRef

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R =
        visitor.visitAnonymousFunction(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E : FirElement, D> transform(transformer: FirTransformer<D>, data: D): E =
        transformer.transformAnonymousFunction(this, data) as E

    abstract override fun replaceAnnotations(newAnnotations: List<FirAnnotation>)

    abstract override fun replaceStatus(newStatus: FirDeclarationStatus)

    abstract override fun replaceReturnTypeRef(newReturnTypeRef: FirTypeRef)

    abstract override fun replaceReceiverParameter(newReceiverParameter: FirReceiverParameter?)

    abstract override fun replaceDeprecationsProvider(newDeprecationsProvider: DeprecationsProvider)

    abstract override fun replaceContextParameters(newContextParameters: List<FirValueParameter>)

    abstract override fun replaceControlFlowGraphReference(newControlFlowGraphReference: FirControlFlowGraphReference?)

    abstract override fun replaceValueParameters(newValueParameters: List<FirValueParameter>)

    abstract override fun replaceBody(newBody: FirBlock?)

    abstract override fun replaceContractDescription(newContractDescription: FirContractDescription?)

    abstract fun replaceInvocationKind(newInvocationKind: EventOccurrencesRange?)

    abstract fun replaceInlineStatus(newInlineStatus: InlineStatus)

    abstract fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformStatus(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReturnTypeRef(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformReceiverParameter(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformContextParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformValueParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformBody(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformContractDescription(transformer: FirTransformer<D>, data: D): FirAnonymousFunction

    abstract override fun <D> transformTypeParameters(transformer: FirTransformer<D>, data: D): FirAnonymousFunction
}
