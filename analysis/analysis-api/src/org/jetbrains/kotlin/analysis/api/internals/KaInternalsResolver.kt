/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaExperimentalApi::class, KtExperimentalApi::class)

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolution.KtResolvable
import org.jetbrains.kotlin.resolution.KtResolvableCall

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsResolver {
    public fun tryResolveSymbols(resolvable: KtResolvable): KaSymbolResolutionAttempt?

    public fun resolveSymbols(resolvable: KtResolvable): Collection<KaSymbol>

    public fun resolveSymbol(resolvable: KtResolvable): KaSymbol?

    public fun resolveSymbol(annotationEntry: KtAnnotationEntry): KaConstructorSymbol?

    public fun resolveSymbol(superTypeCallEntry: KtSuperTypeCallEntry): KaConstructorSymbol?

    public fun resolveSymbol(constructorDelegationCall: KtConstructorDelegationCall): KaConstructorSymbol?

    public fun resolveSymbol(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaConstructorSymbol?

    public fun resolveSymbol(callElement: KtCallElement): KaFunctionSymbol?

    public fun resolveSymbol(callableReferenceExpression: KtCallableReferenceExpression): KaCallableSymbol?

    public fun resolveSymbol(arrayAccessExpression: KtArrayAccessExpression): KaNamedFunctionSymbol?

    public fun resolveSymbol(collectionLiteralExpression: KtCollectionLiteralExpression): KaNamedFunctionSymbol?

    public fun resolveSymbol(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaNamedClassSymbol?

    public fun resolveSymbol(labelReferenceExpression: KtLabelReferenceExpression): KaDeclarationSymbol?

    public fun resolveSymbol(returnExpression: KtReturnExpression): KaFunctionSymbol?

    public fun resolveSymbol(whenConditionInRange: KtWhenConditionInRange): KaNamedFunctionSymbol?

    public fun resolveSymbol(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaCallableSymbol?

    public fun resolveSymbol(qualifiedExpression: KtQualifiedExpression): KaCallableSymbol?

    public fun resolveSymbol(constructorCalleeExpression: KtConstructorCalleeExpression): KaConstructorSymbol?

    public fun resolveSymbol(instanceExpressionWithLabel: KtInstanceExpressionWithLabel): KaDeclarationSymbol?

    public fun resolveSymbol(nullableType: KtNullableType): KaClassifierSymbol?

    public fun resolveSymbol(functionType: KtFunctionType): KaClassSymbol?

    public fun resolveSymbol(typeReference: KtTypeReference): KaClassifierSymbol?

    public fun resolveSymbol(classLiteralExpression: KtClassLiteralExpression): KaClassifierSymbol?

    public fun resolveSymbol(superTypeEntry: KtSuperTypeEntry): KaClassifierSymbol?

    public fun resolveSymbol(delegatedSuperTypeEntry: KtDelegatedSuperTypeEntry): KaClassifierSymbol?

    public fun tryResolveCall(resolvableCall: KtResolvableCall): KaCallResolutionAttempt?

    public fun tryResolveCall(forExpression: KtForExpression): KaForLoopCallResolutionAttempt?

    public fun tryResolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCallResolutionAttempt?

    public fun resolveCall(resolvableCall: KtResolvableCall): KaSingleOrMultiCall?

    public fun resolveCall(annotationEntry: KtAnnotationEntry): KaAnnotationCall?

    public fun resolveCall(superTypeCallEntry: KtSuperTypeCallEntry): KaFunctionCall<KaConstructorSymbol>?

    public fun resolveCall(constructorDelegationCall: KtConstructorDelegationCall): KaDelegatedConstructorCall?

    public fun resolveCall(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaDelegatedConstructorCall?

    public fun resolveCall(callElement: KtCallElement): KaFunctionCall<*>?

    public fun resolveCall(callableReferenceExpression: KtCallableReferenceExpression): KaCallableReferenceCall<*, *>?

    public fun resolveCall(arrayAccessExpression: KtArrayAccessExpression): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveCall(collectionLiteralExpression: KtCollectionLiteralExpression): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveCall(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaDelegatedConstructorCall?

    public fun resolveCall(whenConditionInRange: KtWhenConditionInRange): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveCall(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaSingleCall<*, *>?

    public fun resolveCall(qualifiedExpression: KtQualifiedExpression): KaSingleCall<*, *>?

    public fun resolveCall(forExpression: KtForExpression): KaForLoopCall?

    public fun resolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCall?

    public fun resolveCall(constructorCalleeExpression: KtConstructorCalleeExpression): KaFunctionCall<KaConstructorSymbol>?

    public fun resolveCall(nameReferenceExpression: KtNameReferenceExpression): KaSingleCall<*, *>?

    public fun collectCallCandidates(resolvableCall: KtResolvableCall): List<KaCallCandidate>

    public fun resolveToCall(element: KtElement): KaCallInfo?

    public fun resolveToCallCandidates(element: KtElement): List<KaCallCandidateInfo>

    public fun isImplicitReferenceToCompanion(simpleNameExpression: KtSimpleNameExpression): Boolean

    public fun contextSensitiveResolutionStatus(simpleNameExpression: KtSimpleNameExpression): KaContextSensitiveResolutionStatus

    public fun resolveToSymbols(reference: KtReference): Collection<KaSymbol>

    public fun resolveToSymbol(reference: KtReference): KaSymbol?

    public fun isImplicitReferenceToCompanion(reference: KtReference): Boolean

    public fun usesContextSensitiveResolution(reference: KtReference): Boolean

    public fun usesContextSensitiveResolution(simpleNameExpression: KtSimpleNameExpression): Boolean
}
