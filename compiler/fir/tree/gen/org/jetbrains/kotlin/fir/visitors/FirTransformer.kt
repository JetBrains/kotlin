/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElementInterface
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLazyExpression
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirFileAnnotationsContainer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirConstructedClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.FirFunctionTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirBackingField
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirImportBase
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.FirErrorImport
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirErrorLoop
import org.jetbrains.kotlin.fir.expressions.FirDoWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirWhileLoop
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirLazyBlock
import org.jetbrains.kotlin.fir.expressions.FirBinaryLogicExpression
import org.jetbrains.kotlin.fir.expressions.FirJump
import org.jetbrains.kotlin.fir.expressions.FirLoopJump
import org.jetbrains.kotlin.fir.expressions.FirBreakExpression
import org.jetbrains.kotlin.fir.expressions.FirContinueExpression
import org.jetbrains.kotlin.fir.expressions.FirCatch
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirStarProjection
import org.jetbrains.kotlin.fir.types.FirPlaceholderProjection
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirErrorAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirIncrementDecrementExpression
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirContextReceiverArgumentListOwner
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedErrorAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirErrorResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirResolvedReifiedParameterReference
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStringConcatenationCall
import org.jetbrains.kotlin.fir.expressions.FirThrowExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.FirWhenSubjectExpression
import org.jetbrains.kotlin.fir.expressions.FirDesugaredAssignmentValueReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.expressions.FirEnumEntryDeserializedAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirNamedReferenceWithCandidateBase
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirFromMissingDependenciesNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedErrorReference
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.FirDynamicTypeRef
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.contracts.FirContractElementDeclaration
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.FirElement

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTransformer<in D> : FirVisitor<FirElement, D>() {

    abstract fun <E : FirElement> transformElement(element: E, data: D): E

    open fun transformTypeRef(typeRef: FirTypeRef, data: D): FirTypeRef {
        return transformElement(typeRef, data)
    }

    open fun transformReference(reference: FirReference, data: D): FirReference {
        return transformElement(reference, data)
    }

    open fun transformLabel(label: FirLabel, data: D): FirLabel {
        return transformElement(label, data)
    }

    open fun transformExpression(expression: FirExpression, data: D): FirStatement {
        return transformElement(expression, data)
    }

    open fun transformLazyExpression(lazyExpression: FirLazyExpression, data: D): FirStatement {
        return transformElement(lazyExpression, data)
    }

    open fun transformContextReceiver(contextReceiver: FirContextReceiver, data: D): FirContextReceiver {
        return transformElement(contextReceiver, data)
    }

    open fun transformElementWithResolveState(elementWithResolveState: FirElementWithResolveState, data: D): FirElementWithResolveState {
        return transformElement(elementWithResolveState, data)
    }

    open fun transformFileAnnotationsContainer(fileAnnotationsContainer: FirFileAnnotationsContainer, data: D): FirFileAnnotationsContainer {
        return transformElement(fileAnnotationsContainer, data)
    }

    open fun transformDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformElement(declaration, data)
    }

    open fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): FirMemberDeclaration {
        return transformElement(memberDeclaration, data)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): FirAnonymousInitializer {
        return transformElement(anonymousInitializer, data)
    }

    open fun transformCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): FirCallableDeclaration {
        return transformElement(callableDeclaration, data)
    }

    open fun transformTypeParameter(typeParameter: FirTypeParameter, data: D): FirTypeParameterRef {
        return transformElement(typeParameter, data)
    }

    open fun transformConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef, data: D): FirTypeParameterRef {
        return transformElement(constructedClassTypeParameterRef, data)
    }

    open fun transformOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef, data: D): FirTypeParameterRef {
        return transformElement(outerClassTypeParameterRef, data)
    }

    open fun transformVariable(variable: FirVariable, data: D): FirStatement {
        return transformElement(variable, data)
    }

    open fun transformValueParameter(valueParameter: FirValueParameter, data: D): FirStatement {
        return transformElement(valueParameter, data)
    }

    open fun transformReceiverParameter(receiverParameter: FirReceiverParameter, data: D): FirReceiverParameter {
        return transformElement(receiverParameter, data)
    }

    open fun transformProperty(property: FirProperty, data: D): FirStatement {
        return transformElement(property, data)
    }

    open fun transformField(field: FirField, data: D): FirStatement {
        return transformElement(field, data)
    }

    open fun transformEnumEntry(enumEntry: FirEnumEntry, data: D): FirStatement {
        return transformElement(enumEntry, data)
    }

    open fun transformFunctionTypeParameter(functionTypeParameter: FirFunctionTypeParameter, data: D): FirFunctionTypeParameter {
        return transformElement(functionTypeParameter, data)
    }

    open fun transformClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: D): FirStatement {
        return transformElement(classLikeDeclaration, data)
    }

    open fun transformClass(klass: FirClass, data: D): FirStatement {
        return transformElement(klass, data)
    }

    open fun transformRegularClass(regularClass: FirRegularClass, data: D): FirStatement {
        return transformElement(regularClass, data)
    }

    open fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): FirStatement {
        return transformElement(typeAlias, data)
    }

    open fun transformFunction(function: FirFunction, data: D): FirStatement {
        return transformElement(function, data)
    }

    open fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: D): FirStatement {
        return transformElement(simpleFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): FirStatement {
        return transformElement(propertyAccessor, data)
    }

    open fun transformBackingField(backingField: FirBackingField, data: D): FirStatement {
        return transformElement(backingField, data)
    }

    open fun transformConstructor(constructor: FirConstructor, data: D): FirStatement {
        return transformElement(constructor, data)
    }

    open fun transformFile(file: FirFile, data: D): FirFile {
        return transformElement(file, data)
    }

    open fun transformScript(script: FirScript, data: D): FirScript {
        return transformElement(script, data)
    }

    open fun transformPackageDirective(packageDirective: FirPackageDirective, data: D): FirPackageDirective {
        return transformElement(packageDirective, data)
    }

    open fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): FirStatement {
        return transformElement(anonymousFunction, data)
    }

    open fun transformAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): FirStatement {
        return transformElement(anonymousFunctionExpression, data)
    }

    open fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: D): FirStatement {
        return transformElement(anonymousObject, data)
    }

    open fun transformAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): FirStatement {
        return transformElement(anonymousObjectExpression, data)
    }

    open fun transformImportBase(importBase: FirImportBase, data: D): FirImport {
        return transformElement(importBase, data)
    }

    open fun transformResolvedImport(resolvedImport: FirResolvedImport, data: D): FirImport {
        return transformElement(resolvedImport, data)
    }

    open fun transformErrorImport(errorImport: FirErrorImport, data: D): FirImport {
        return transformElement(errorImport, data)
    }

    open fun transformLoop(loop: FirLoop, data: D): FirStatement {
        return transformElement(loop, data)
    }

    open fun transformErrorLoop(errorLoop: FirErrorLoop, data: D): FirStatement {
        return transformElement(errorLoop, data)
    }

    open fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): FirStatement {
        return transformElement(doWhileLoop, data)
    }

    open fun transformWhileLoop(whileLoop: FirWhileLoop, data: D): FirStatement {
        return transformElement(whileLoop, data)
    }

    open fun transformBlock(block: FirBlock, data: D): FirStatement {
        return transformElement(block, data)
    }

    open fun transformLazyBlock(lazyBlock: FirLazyBlock, data: D): FirStatement {
        return transformElement(lazyBlock, data)
    }

    open fun transformBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): FirStatement {
        return transformElement(binaryLogicExpression, data)
    }

    open fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: D): FirStatement {
        return transformElement(jump, data)
    }

    open fun transformLoopJump(loopJump: FirLoopJump, data: D): FirStatement {
        return transformElement(loopJump, data)
    }

    open fun transformBreakExpression(breakExpression: FirBreakExpression, data: D): FirStatement {
        return transformElement(breakExpression, data)
    }

    open fun transformContinueExpression(continueExpression: FirContinueExpression, data: D): FirStatement {
        return transformElement(continueExpression, data)
    }

    open fun transformCatch(catch: FirCatch, data: D): FirCatch {
        return transformElement(catch, data)
    }

    open fun transformTryExpression(tryExpression: FirTryExpression, data: D): FirStatement {
        return transformElement(tryExpression, data)
    }

    open fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: D): FirStatement {
        return transformElement(constExpression, data)
    }

    open fun transformTypeProjection(typeProjection: FirTypeProjection, data: D): FirTypeProjection {
        return transformElement(typeProjection, data)
    }

    open fun transformStarProjection(starProjection: FirStarProjection, data: D): FirTypeProjection {
        return transformElement(starProjection, data)
    }

    open fun transformPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): FirTypeProjection {
        return transformElement(placeholderProjection, data)
    }

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): FirTypeProjection {
        return transformElement(typeProjectionWithVariance, data)
    }

    open fun transformArgumentList(argumentList: FirArgumentList, data: D): FirArgumentList {
        return transformElement(argumentList, data)
    }

    open fun transformAnnotation(annotation: FirAnnotation, data: D): FirStatement {
        return transformElement(annotation, data)
    }

    open fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirStatement {
        return transformElement(annotationCall, data)
    }

    open fun transformAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): FirAnnotationArgumentMapping {
        return transformElement(annotationArgumentMapping, data)
    }

    open fun transformErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: D): FirStatement {
        return transformElement(errorAnnotationCall, data)
    }

    open fun transformComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): FirStatement {
        return transformElement(comparisonExpression, data)
    }

    open fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): FirStatement {
        return transformElement(typeOperatorCall, data)
    }

    open fun transformAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): FirStatement {
        return transformElement(assignmentOperatorStatement, data)
    }

    open fun transformIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression, data: D): FirStatement {
        return transformElement(incrementDecrementExpression, data)
    }

    open fun transformEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): FirStatement {
        return transformElement(equalityOperatorCall, data)
    }

    open fun transformWhenExpression(whenExpression: FirWhenExpression, data: D): FirStatement {
        return transformElement(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: FirWhenBranch, data: D): FirWhenBranch {
        return transformElement(whenBranch, data)
    }

    open fun transformCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): FirStatement {
        return transformElement(checkNotNullCall, data)
    }

    open fun transformElvisExpression(elvisExpression: FirElvisExpression, data: D): FirStatement {
        return transformElement(elvisExpression, data)
    }

    open fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): FirStatement {
        return transformElement(arrayOfCall, data)
    }

    open fun transformAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): FirStatement {
        return transformElement(augmentedArraySetCall, data)
    }

    open fun transformClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): FirStatement {
        return transformElement(classReferenceExpression, data)
    }

    open fun transformErrorExpression(errorExpression: FirErrorExpression, data: D): FirStatement {
        return transformElement(errorExpression, data)
    }

    open fun transformErrorFunction(errorFunction: FirErrorFunction, data: D): FirStatement {
        return transformElement(errorFunction, data)
    }

    open fun transformErrorProperty(errorProperty: FirErrorProperty, data: D): FirStatement {
        return transformElement(errorProperty, data)
    }

    open fun transformDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: D): FirDanglingModifierList {
        return transformElement(danglingModifierList, data)
    }

    open fun transformQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): FirStatement {
        return transformElement(qualifiedAccessExpression, data)
    }

    open fun transformQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression, data: D): FirStatement {
        return transformElement(qualifiedErrorAccessExpression, data)
    }

    open fun transformPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: D): FirStatement {
        return transformElement(propertyAccessExpression, data)
    }

    open fun transformFunctionCall(functionCall: FirFunctionCall, data: D): FirStatement {
        return transformElement(functionCall, data)
    }

    open fun transformIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): FirStatement {
        return transformElement(integerLiteralOperatorCall, data)
    }

    open fun transformImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): FirStatement {
        return transformElement(implicitInvokeCall, data)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): FirStatement {
        return transformElement(delegatedConstructorCall, data)
    }

    open fun transformComponentCall(componentCall: FirComponentCall, data: D): FirStatement {
        return transformElement(componentCall, data)
    }

    open fun transformCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): FirStatement {
        return transformElement(callableReferenceAccess, data)
    }

    open fun transformThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): FirStatement {
        return transformElement(thisReceiverExpression, data)
    }

    open fun transformSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: D): FirStatement {
        return transformElement(smartCastExpression, data)
    }

    open fun transformSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): FirStatement {
        return transformElement(safeCallExpression, data)
    }

    open fun transformCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): FirStatement {
        return transformElement(checkedSafeCallSubject, data)
    }

    open fun transformGetClassCall(getClassCall: FirGetClassCall, data: D): FirStatement {
        return transformElement(getClassCall, data)
    }

    open fun transformWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): FirStatement {
        return transformElement(wrappedExpression, data)
    }

    open fun transformWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): FirStatement {
        return transformElement(wrappedArgumentExpression, data)
    }

    open fun transformLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): FirStatement {
        return transformElement(lambdaArgumentExpression, data)
    }

    open fun transformSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): FirStatement {
        return transformElement(spreadArgumentExpression, data)
    }

    open fun transformNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): FirStatement {
        return transformElement(namedArgumentExpression, data)
    }

    open fun transformVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): FirStatement {
        return transformElement(varargArgumentsExpression, data)
    }

    open fun transformResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): FirStatement {
        return transformElement(resolvedQualifier, data)
    }

    open fun transformErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): FirStatement {
        return transformElement(errorResolvedQualifier, data)
    }

    open fun transformResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): FirStatement {
        return transformElement(resolvedReifiedParameterReference, data)
    }

    open fun transformReturnExpression(returnExpression: FirReturnExpression, data: D): FirStatement {
        return transformElement(returnExpression, data)
    }

    open fun transformStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): FirStatement {
        return transformElement(stringConcatenationCall, data)
    }

    open fun transformThrowExpression(throwExpression: FirThrowExpression, data: D): FirStatement {
        return transformElement(throwExpression, data)
    }

    open fun transformVariableAssignment(variableAssignment: FirVariableAssignment, data: D): FirStatement {
        return transformElement(variableAssignment, data)
    }

    open fun transformWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): FirStatement {
        return transformElement(whenSubjectExpression, data)
    }

    open fun transformDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression, data: D): FirStatement {
        return transformElement(desugaredAssignmentValueReferenceExpression, data)
    }

    open fun transformWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): FirStatement {
        return transformElement(wrappedDelegateExpression, data)
    }

    open fun transformEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression, data: D): FirStatement {
        return transformElement(enumEntryDeserializedAccessExpression, data)
    }

    open fun transformNamedReference(namedReference: FirNamedReference, data: D): FirReference {
        return transformElement(namedReference, data)
    }

    open fun transformNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase, data: D): FirReference {
        return transformElement(namedReferenceWithCandidateBase, data)
    }

    open fun transformErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): FirReference {
        return transformElement(errorNamedReference, data)
    }

    open fun transformFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference: FirFromMissingDependenciesNamedReference, data: D): FirReference {
        return transformElement(fromMissingDependenciesNamedReference, data)
    }

    open fun transformSuperReference(superReference: FirSuperReference, data: D): FirReference {
        return transformElement(superReference, data)
    }

    open fun transformThisReference(thisReference: FirThisReference, data: D): FirReference {
        return transformElement(thisReference, data)
    }

    open fun transformControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): FirReference {
        return transformElement(controlFlowGraphReference, data)
    }

    open fun transformResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): FirReference {
        return transformElement(resolvedNamedReference, data)
    }

    open fun transformResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference, data: D): FirReference {
        return transformElement(resolvedErrorReference, data)
    }

    open fun transformDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): FirReference {
        return transformElement(delegateFieldReference, data)
    }

    open fun transformBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): FirReference {
        return transformElement(backingFieldReference, data)
    }

    open fun transformResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): FirReference {
        return transformElement(resolvedCallableReference, data)
    }

    open fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): FirTypeRef {
        return transformElement(resolvedTypeRef, data)
    }

    open fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): FirTypeRef {
        return transformElement(errorTypeRef, data)
    }

    open fun transformTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): FirTypeRef {
        return transformElement(typeRefWithNullability, data)
    }

    open fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: D): FirTypeRef {
        return transformElement(userTypeRef, data)
    }

    open fun transformDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): FirTypeRef {
        return transformElement(dynamicTypeRef, data)
    }

    open fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): FirTypeRef {
        return transformElement(functionTypeRef, data)
    }

    open fun transformIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirTypeRef {
        return transformElement(intersectionTypeRef, data)
    }

    open fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformElement(implicitTypeRef, data)
    }

    open fun transformContractElementDeclaration(contractElementDeclaration: FirContractElementDeclaration, data: D): FirContractElementDeclaration {
        return transformElement(contractElementDeclaration, data)
    }

    open fun transformEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): FirContractElementDeclaration {
        return transformElement(effectDeclaration, data)
    }

    open fun transformContractDescription(contractDescription: FirContractDescription, data: D): FirContractDescription {
        return transformElement(contractDescription, data)
    }

    open fun transformLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): FirContractDescription {
        return transformElement(legacyRawContractDescription, data)
    }

    open fun transformRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): FirContractDescription {
        return transformElement(rawContractDescription, data)
    }

    open fun transformResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): FirContractDescription {
        return transformElement(resolvedContractDescription, data)
    }

    open fun transformStubStatement(stubStatement: FirStubStatement, data: D): FirStubStatement {
        return transformElement(stubStatement, data)
    }

    open fun transformDeclarationStatusImpl(declarationStatusImpl: FirDeclarationStatusImpl, data: D): FirDeclarationStatusImpl {
        return transformElement(declarationStatusImpl, data)
    }

    final override fun visitElement(element: FirElement, data: D): FirElement {
        return transformElement(element, data)
    }

    final override fun visitTypeRef(typeRef: FirTypeRef, data: D): FirElement {
        return transformTypeRef(typeRef, data) as FirElement
    }

    final override fun visitReference(reference: FirReference, data: D): FirElement {
        return transformReference(reference, data) as FirElement
    }

    final override fun visitLabel(label: FirLabel, data: D): FirElement {
        return transformLabel(label, data) as FirElement
    }

    final override fun visitExpression(expression: FirExpression, data: D): FirElement {
        return transformExpression(expression, data) as FirElement
    }

    final override fun visitLazyExpression(lazyExpression: FirLazyExpression, data: D): FirElement {
        return transformLazyExpression(lazyExpression, data) as FirElement
    }

    final override fun visitContextReceiver(contextReceiver: FirContextReceiver, data: D): FirElement {
        return transformContextReceiver(contextReceiver, data) as FirElement
    }

    final override fun visitElementWithResolveState(elementWithResolveState: FirElementWithResolveState, data: D): FirElement {
        return transformElementWithResolveState(elementWithResolveState, data) as FirElement
    }

    final override fun visitFileAnnotationsContainer(fileAnnotationsContainer: FirFileAnnotationsContainer, data: D): FirElement {
        return transformFileAnnotationsContainer(fileAnnotationsContainer, data) as FirElement
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: D): FirElement {
        return transformDeclaration(declaration, data) as FirElement
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): FirElement {
        return transformMemberDeclaration(memberDeclaration, data) as FirElement
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): FirElement {
        return transformAnonymousInitializer(anonymousInitializer, data) as FirElement
    }

    final override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): FirElement {
        return transformCallableDeclaration(callableDeclaration, data) as FirElement
    }

    final override fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): FirElement {
        return transformTypeParameter(typeParameter, data) as FirElement
    }

    final override fun visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef, data: D): FirElement {
        return transformConstructedClassTypeParameterRef(constructedClassTypeParameterRef, data) as FirElement
    }

    final override fun visitOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef, data: D): FirElement {
        return transformOuterClassTypeParameterRef(outerClassTypeParameterRef, data) as FirElement
    }

    final override fun visitVariable(variable: FirVariable, data: D): FirElement {
        return transformVariable(variable, data) as FirElement
    }

    final override fun visitValueParameter(valueParameter: FirValueParameter, data: D): FirElement {
        return transformValueParameter(valueParameter, data) as FirElement
    }

    final override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: D): FirElement {
        return transformReceiverParameter(receiverParameter, data) as FirElement
    }

    final override fun visitProperty(property: FirProperty, data: D): FirElement {
        return transformProperty(property, data) as FirElement
    }

    final override fun visitField(field: FirField, data: D): FirElement {
        return transformField(field, data) as FirElement
    }

    final override fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): FirElement {
        return transformEnumEntry(enumEntry, data) as FirElement
    }

    final override fun visitFunctionTypeParameter(functionTypeParameter: FirFunctionTypeParameter, data: D): FirElement {
        return transformFunctionTypeParameter(functionTypeParameter, data) as FirElement
    }

    final override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: D): FirElement {
        return transformClassLikeDeclaration(classLikeDeclaration, data) as FirElement
    }

    final override fun visitClass(klass: FirClass, data: D): FirElement {
        return transformClass(klass, data) as FirElement
    }

    final override fun visitRegularClass(regularClass: FirRegularClass, data: D): FirElement {
        return transformRegularClass(regularClass, data) as FirElement
    }

    final override fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): FirElement {
        return transformTypeAlias(typeAlias, data) as FirElement
    }

    final override fun visitFunction(function: FirFunction, data: D): FirElement {
        return transformFunction(function, data) as FirElement
    }

    final override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: D): FirElement {
        return transformSimpleFunction(simpleFunction, data) as FirElement
    }

    final override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): FirElement {
        return transformPropertyAccessor(propertyAccessor, data) as FirElement
    }

    final override fun visitBackingField(backingField: FirBackingField, data: D): FirElement {
        return transformBackingField(backingField, data) as FirElement
    }

    final override fun visitConstructor(constructor: FirConstructor, data: D): FirElement {
        return transformConstructor(constructor, data) as FirElement
    }

    final override fun visitFile(file: FirFile, data: D): FirElement {
        return transformFile(file, data) as FirElement
    }

    final override fun visitScript(script: FirScript, data: D): FirElement {
        return transformScript(script, data) as FirElement
    }

    final override fun visitPackageDirective(packageDirective: FirPackageDirective, data: D): FirElement {
        return transformPackageDirective(packageDirective, data) as FirElement
    }

    final override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): FirElement {
        return transformAnonymousFunction(anonymousFunction, data) as FirElement
    }

    final override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): FirElement {
        return transformAnonymousFunctionExpression(anonymousFunctionExpression, data) as FirElement
    }

    final override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): FirElement {
        return transformAnonymousObject(anonymousObject, data) as FirElement
    }

    final override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): FirElement {
        return transformAnonymousObjectExpression(anonymousObjectExpression, data) as FirElement
    }

    final override fun visitImportBase(importBase: FirImportBase, data: D): FirElement {
        return transformImportBase(importBase, data) as FirElement
    }

    final override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): FirElement {
        return transformResolvedImport(resolvedImport, data) as FirElement
    }

    final override fun visitErrorImport(errorImport: FirErrorImport, data: D): FirElement {
        return transformErrorImport(errorImport, data) as FirElement
    }

    final override fun visitLoop(loop: FirLoop, data: D): FirElement {
        return transformLoop(loop, data) as FirElement
    }

    final override fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): FirElement {
        return transformErrorLoop(errorLoop, data) as FirElement
    }

    final override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): FirElement {
        return transformDoWhileLoop(doWhileLoop, data) as FirElement
    }

    final override fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): FirElement {
        return transformWhileLoop(whileLoop, data) as FirElement
    }

    final override fun visitBlock(block: FirBlock, data: D): FirElement {
        return transformBlock(block, data) as FirElement
    }

    final override fun visitLazyBlock(lazyBlock: FirLazyBlock, data: D): FirElement {
        return transformLazyBlock(lazyBlock, data) as FirElement
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): FirElement {
        return transformBinaryLogicExpression(binaryLogicExpression, data) as FirElement
    }

    final override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): FirElement {
        return transformJump(jump, data) as FirElement
    }

    final override fun visitLoopJump(loopJump: FirLoopJump, data: D): FirElement {
        return transformLoopJump(loopJump, data) as FirElement
    }

    final override fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): FirElement {
        return transformBreakExpression(breakExpression, data) as FirElement
    }

    final override fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): FirElement {
        return transformContinueExpression(continueExpression, data) as FirElement
    }

    final override fun visitCatch(catch: FirCatch, data: D): FirElement {
        return transformCatch(catch, data) as FirElement
    }

    final override fun visitTryExpression(tryExpression: FirTryExpression, data: D): FirElement {
        return transformTryExpression(tryExpression, data) as FirElement
    }

    final override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): FirElement {
        return transformConstExpression(constExpression, data) as FirElement
    }

    final override fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): FirElement {
        return transformTypeProjection(typeProjection, data) as FirElement
    }

    final override fun visitStarProjection(starProjection: FirStarProjection, data: D): FirElement {
        return transformStarProjection(starProjection, data) as FirElement
    }

    final override fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): FirElement {
        return transformPlaceholderProjection(placeholderProjection, data) as FirElement
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): FirElement {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance, data) as FirElement
    }

    final override fun visitArgumentList(argumentList: FirArgumentList, data: D): FirElement {
        return transformArgumentList(argumentList, data) as FirElement
    }

    final override fun visitAnnotation(annotation: FirAnnotation, data: D): FirElement {
        return transformAnnotation(annotation, data) as FirElement
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirElement {
        return transformAnnotationCall(annotationCall, data) as FirElement
    }

    final override fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): FirElement {
        return transformAnnotationArgumentMapping(annotationArgumentMapping, data) as FirElement
    }

    final override fun visitErrorAnnotationCall(errorAnnotationCall: FirErrorAnnotationCall, data: D): FirElement {
        return transformErrorAnnotationCall(errorAnnotationCall, data) as FirElement
    }

    final override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): FirElement {
        return transformComparisonExpression(comparisonExpression, data) as FirElement
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): FirElement {
        return transformTypeOperatorCall(typeOperatorCall, data) as FirElement
    }

    final override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): FirElement {
        return transformAssignmentOperatorStatement(assignmentOperatorStatement, data) as FirElement
    }

    final override fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression, data: D): FirElement {
        return transformIncrementDecrementExpression(incrementDecrementExpression, data) as FirElement
    }

    final override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): FirElement {
        return transformEqualityOperatorCall(equalityOperatorCall, data) as FirElement
    }

    final override fun visitWhenExpression(whenExpression: FirWhenExpression, data: D): FirElement {
        return transformWhenExpression(whenExpression, data) as FirElement
    }

    final override fun visitWhenBranch(whenBranch: FirWhenBranch, data: D): FirElement {
        return transformWhenBranch(whenBranch, data) as FirElement
    }

    final override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): FirElement {
        return transformCheckNotNullCall(checkNotNullCall, data) as FirElement
    }

    final override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: D): FirElement {
        return transformElvisExpression(elvisExpression, data) as FirElement
    }

    final override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): FirElement {
        return transformArrayOfCall(arrayOfCall, data) as FirElement
    }

    final override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): FirElement {
        return transformAugmentedArraySetCall(augmentedArraySetCall, data) as FirElement
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): FirElement {
        return transformClassReferenceExpression(classReferenceExpression, data) as FirElement
    }

    final override fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): FirElement {
        return transformErrorExpression(errorExpression, data) as FirElement
    }

    final override fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): FirElement {
        return transformErrorFunction(errorFunction, data) as FirElement
    }

    final override fun visitErrorProperty(errorProperty: FirErrorProperty, data: D): FirElement {
        return transformErrorProperty(errorProperty, data) as FirElement
    }

    final override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: D): FirElement {
        return transformDanglingModifierList(danglingModifierList, data) as FirElement
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): FirElement {
        return transformQualifiedAccessExpression(qualifiedAccessExpression, data) as FirElement
    }

    final override fun visitQualifiedErrorAccessExpression(qualifiedErrorAccessExpression: FirQualifiedErrorAccessExpression, data: D): FirElement {
        return transformQualifiedErrorAccessExpression(qualifiedErrorAccessExpression, data) as FirElement
    }

    final override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: D): FirElement {
        return transformPropertyAccessExpression(propertyAccessExpression, data) as FirElement
    }

    final override fun visitFunctionCall(functionCall: FirFunctionCall, data: D): FirElement {
        return transformFunctionCall(functionCall, data) as FirElement
    }

    final override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): FirElement {
        return transformIntegerLiteralOperatorCall(integerLiteralOperatorCall, data) as FirElement
    }

    final override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): FirElement {
        return transformImplicitInvokeCall(implicitInvokeCall, data) as FirElement
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): FirElement {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data) as FirElement
    }

    final override fun visitComponentCall(componentCall: FirComponentCall, data: D): FirElement {
        return transformComponentCall(componentCall, data) as FirElement
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): FirElement {
        return transformCallableReferenceAccess(callableReferenceAccess, data) as FirElement
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): FirElement {
        return transformThisReceiverExpression(thisReceiverExpression, data) as FirElement
    }

    final override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: D): FirElement {
        return transformSmartCastExpression(smartCastExpression, data) as FirElement
    }

    final override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): FirElement {
        return transformSafeCallExpression(safeCallExpression, data) as FirElement
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): FirElement {
        return transformCheckedSafeCallSubject(checkedSafeCallSubject, data) as FirElement
    }

    final override fun visitGetClassCall(getClassCall: FirGetClassCall, data: D): FirElement {
        return transformGetClassCall(getClassCall, data) as FirElement
    }

    final override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): FirElement {
        return transformWrappedExpression(wrappedExpression, data) as FirElement
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): FirElement {
        return transformWrappedArgumentExpression(wrappedArgumentExpression, data) as FirElement
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): FirElement {
        return transformLambdaArgumentExpression(lambdaArgumentExpression, data) as FirElement
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): FirElement {
        return transformSpreadArgumentExpression(spreadArgumentExpression, data) as FirElement
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): FirElement {
        return transformNamedArgumentExpression(namedArgumentExpression, data) as FirElement
    }

    final override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): FirElement {
        return transformVarargArgumentsExpression(varargArgumentsExpression, data) as FirElement
    }

    final override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): FirElement {
        return transformResolvedQualifier(resolvedQualifier, data) as FirElement
    }

    final override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): FirElement {
        return transformErrorResolvedQualifier(errorResolvedQualifier, data) as FirElement
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): FirElement {
        return transformResolvedReifiedParameterReference(resolvedReifiedParameterReference, data) as FirElement
    }

    final override fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): FirElement {
        return transformReturnExpression(returnExpression, data) as FirElement
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): FirElement {
        return transformStringConcatenationCall(stringConcatenationCall, data) as FirElement
    }

    final override fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): FirElement {
        return transformThrowExpression(throwExpression, data) as FirElement
    }

    final override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): FirElement {
        return transformVariableAssignment(variableAssignment, data) as FirElement
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): FirElement {
        return transformWhenSubjectExpression(whenSubjectExpression, data) as FirElement
    }

    final override fun visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression, data: D): FirElement {
        return transformDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression, data) as FirElement
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): FirElement {
        return transformWrappedDelegateExpression(wrappedDelegateExpression, data) as FirElement
    }

    final override fun visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression, data: D): FirElement {
        return transformEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression, data) as FirElement
    }

    final override fun visitNamedReference(namedReference: FirNamedReference, data: D): FirElement {
        return transformNamedReference(namedReference, data) as FirElement
    }

    final override fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase, data: D): FirElement {
        return transformNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase, data) as FirElement
    }

    final override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): FirElement {
        return transformErrorNamedReference(errorNamedReference, data) as FirElement
    }

    final override fun visitFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference: FirFromMissingDependenciesNamedReference, data: D): FirElement {
        return transformFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference, data) as FirElement
    }

    final override fun visitSuperReference(superReference: FirSuperReference, data: D): FirElement {
        return transformSuperReference(superReference, data) as FirElement
    }

    final override fun visitThisReference(thisReference: FirThisReference, data: D): FirElement {
        return transformThisReference(thisReference, data) as FirElement
    }

    final override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): FirElement {
        return transformControlFlowGraphReference(controlFlowGraphReference, data) as FirElement
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): FirElement {
        return transformResolvedNamedReference(resolvedNamedReference, data) as FirElement
    }

    final override fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference, data: D): FirElement {
        return transformResolvedErrorReference(resolvedErrorReference, data) as FirElement
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): FirElement {
        return transformDelegateFieldReference(delegateFieldReference, data) as FirElement
    }

    final override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): FirElement {
        return transformBackingFieldReference(backingFieldReference, data) as FirElement
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): FirElement {
        return transformResolvedCallableReference(resolvedCallableReference, data) as FirElement
    }

    final override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): FirElement {
        return transformResolvedTypeRef(resolvedTypeRef, data) as FirElement
    }

    final override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): FirElement {
        return transformErrorTypeRef(errorTypeRef, data) as FirElement
    }

    final override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): FirElement {
        return transformTypeRefWithNullability(typeRefWithNullability, data) as FirElement
    }

    final override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): FirElement {
        return transformUserTypeRef(userTypeRef, data) as FirElement
    }

    final override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): FirElement {
        return transformDynamicTypeRef(dynamicTypeRef, data) as FirElement
    }

    final override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): FirElement {
        return transformFunctionTypeRef(functionTypeRef, data) as FirElement
    }

    final override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirElement {
        return transformIntersectionTypeRef(intersectionTypeRef, data) as FirElement
    }

    final override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirElement {
        return transformImplicitTypeRef(implicitTypeRef, data) as FirElement
    }

    final override fun visitContractElementDeclaration(contractElementDeclaration: FirContractElementDeclaration, data: D): FirElement {
        return transformContractElementDeclaration(contractElementDeclaration, data) as FirElement
    }

    final override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): FirElement {
        return transformEffectDeclaration(effectDeclaration, data) as FirElement
    }

    final override fun visitContractDescription(contractDescription: FirContractDescription, data: D): FirElement {
        return transformContractDescription(contractDescription, data) as FirElement
    }

    final override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): FirElement {
        return transformLegacyRawContractDescription(legacyRawContractDescription, data) as FirElement
    }

    final override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): FirElement {
        return transformRawContractDescription(rawContractDescription, data) as FirElement
    }

    final override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): FirElement {
        return transformResolvedContractDescription(resolvedContractDescription, data) as FirElement
    }

    final override fun visitStubStatement(stubStatement: FirStubStatement, data: D): FirElement {
        return transformStubStatement(stubStatement, data)
    }

    final override fun visitDeclarationStatusImpl(declarationStatusImpl: FirDeclarationStatusImpl, data: D): FirElement {
        return transformDeclarationStatusImpl(declarationStatusImpl, data)
    }

}
