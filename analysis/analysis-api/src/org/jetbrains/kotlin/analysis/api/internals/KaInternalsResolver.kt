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

    public fun resolveSuccessfulSymbols(resolvable: KtResolvable): Collection<KaSymbol>

    public fun resolveSuccessfulSymbol(resolvable: KtResolvable): KaSymbol?

    public fun resolveSuccessfulSymbol(annotationEntry: KtAnnotationEntry): KaConstructorSymbol?

    public fun resolveSuccessfulSymbol(superTypeCallEntry: KtSuperTypeCallEntry): KaConstructorSymbol?

    public fun resolveSuccessfulSymbol(constructorDelegationCall: KtConstructorDelegationCall): KaConstructorSymbol?

    public fun resolveSuccessfulSymbol(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaConstructorSymbol?

    public fun resolveSuccessfulSymbol(callElement: KtCallElement): KaFunctionSymbol?

    public fun resolveSuccessfulSymbol(callableReferenceExpression: KtCallableReferenceExpression): KaCallableSymbol?

    public fun resolveSuccessfulSymbol(arrayAccessExpression: KtArrayAccessExpression): KaNamedFunctionSymbol?

    public fun resolveSuccessfulSymbol(collectionLiteralExpression: KtCollectionLiteralExpression): KaNamedFunctionSymbol?

    public fun resolveSuccessfulSymbol(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaNamedClassSymbol?

    public fun resolveSuccessfulSymbol(labelReferenceExpression: KtLabelReferenceExpression): KaDeclarationSymbol?

    public fun resolveSuccessfulSymbol(returnExpression: KtReturnExpression): KaFunctionSymbol?

    public fun resolveSuccessfulSymbol(whenConditionInRange: KtWhenConditionInRange): KaNamedFunctionSymbol?

    public fun resolveSuccessfulSymbol(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaCallableSymbol?

    public fun resolveSuccessfulSymbol(qualifiedExpression: KtQualifiedExpression): KaCallableSymbol?

    public fun resolveSuccessfulSymbol(constructorCalleeExpression: KtConstructorCalleeExpression): KaConstructorSymbol?

    public fun resolveSuccessfulSymbol(instanceExpressionWithLabel: KtInstanceExpressionWithLabel): KaDeclarationSymbol?

    public fun resolveSuccessfulSymbol(nullableType: KtNullableType): KaClassifierSymbol?

    public fun resolveSuccessfulSymbol(functionType: KtFunctionType): KaClassSymbol?

    public fun resolveSuccessfulSymbol(typeReference: KtTypeReference): KaClassifierSymbol?

    public fun resolveSuccessfulSymbol(classLiteralExpression: KtClassLiteralExpression): KaClassifierSymbol?

    public fun resolveSuccessfulSymbol(superTypeEntry: KtSuperTypeEntry): KaClassifierSymbol?

    public fun resolveSuccessfulSymbol(delegatedSuperTypeEntry: KtDelegatedSuperTypeEntry): KaClassifierSymbol?

    public fun tryResolveCall(resolvableCall: KtResolvableCall): KaCallResolutionAttempt?

    public fun tryResolveCall(forExpression: KtForExpression): KaForLoopCallResolutionAttempt?

    public fun tryResolveCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCallResolutionAttempt?

    public fun resolveSuccessfulCall(resolvableCall: KtResolvableCall): KaSimpleOrMultiCall?

    public fun resolveSuccessfulCall(annotationEntry: KtAnnotationEntry): KaAnnotationCall?

    public fun resolveSuccessfulCall(superTypeCallEntry: KtSuperTypeCallEntry): KaFunctionCall<KaConstructorSymbol>?

    public fun resolveSuccessfulCall(constructorDelegationCall: KtConstructorDelegationCall): KaDelegatedConstructorCall?

    public fun resolveSuccessfulCall(constructorDelegationReferenceExpression: KtConstructorDelegationReferenceExpression): KaDelegatedConstructorCall?

    public fun resolveSuccessfulCall(callElement: KtCallElement): KaFunctionCall<*>?

    public fun resolveSuccessfulCall(callableReferenceExpression: KtCallableReferenceExpression): KaCallableReferenceCall<*, *>?

    public fun resolveSuccessfulCall(arrayAccessExpression: KtArrayAccessExpression): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveSuccessfulCall(collectionLiteralExpression: KtCollectionLiteralExpression): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveSuccessfulCall(enumEntrySuperclassReferenceExpression: KtEnumEntrySuperclassReferenceExpression): KaDelegatedConstructorCall?

    public fun resolveSuccessfulCall(whenConditionInRange: KtWhenConditionInRange): KaFunctionCall<KaNamedFunctionSymbol>?

    public fun resolveSuccessfulCall(destructuringDeclarationEntry: KtDestructuringDeclarationEntry): KaSimpleCall<*, *>?

    public fun resolveSuccessfulCall(qualifiedExpression: KtQualifiedExpression): KaSimpleCall<*, *>?

    public fun resolveSuccessfulCall(forExpression: KtForExpression): KaForLoopCall?

    public fun resolveSuccessfulCall(propertyDelegate: KtPropertyDelegate): KaDelegatedPropertyCall?

    public fun resolveSuccessfulCall(constructorCalleeExpression: KtConstructorCalleeExpression): KaFunctionCall<KaConstructorSymbol>?

    public fun resolveSuccessfulCall(nameReferenceExpression: KtNameReferenceExpression): KaSimpleCall<*, *>?

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
