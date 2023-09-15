/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
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
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.FirPackageDirective
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.expressions.FirAnonymousObjectExpression
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.declarations.FirImport
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
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.declarations.FirErrorPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.FirDanglingModifierList
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedErrorAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirMultiDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirInaccessibleReceiverExpression
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

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirDefaultVisitor<out R, in D> : FirVisitor<R, D>() {
    override fun visitTypeRef(typeRef: FirTypeRef, data: D): R = visitAnnotationContainer(typeRef, data)

    override fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): R = visitDeclarationStatus(resolvedDeclarationStatus, data)

    override fun visitStatement(statement: FirStatement, data: D): R = visitAnnotationContainer(statement, data)

    override fun visitExpression(expression: FirExpression, data: D): R = visitStatement(expression, data)

    override fun visitLazyExpression(lazyExpression: FirLazyExpression, data: D): R = visitExpression(lazyExpression, data)

    override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): R = visitTypeParameterRefsOwner(typeParametersOwner, data)

    override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): R = visitMemberDeclaration(callableDeclaration, data)

    override fun visitConstructedClassTypeParameterRef(constructedClassTypeParameterRef: FirConstructedClassTypeParameterRef, data: D): R = visitTypeParameterRef(constructedClassTypeParameterRef, data)

    override fun visitOuterClassTypeParameterRef(outerClassTypeParameterRef: FirOuterClassTypeParameterRef, data: D): R = visitTypeParameterRef(outerClassTypeParameterRef, data)

    override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: D): R = visitAnnotationContainer(receiverParameter, data)

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): R = visitVariable(enumEntry, data)

    override fun visitRegularClass(regularClass: FirRegularClass, data: D): R = visitClass(regularClass, data)

    override fun visitScript(script: FirScript, data: D): R = visitDeclaration(script, data)

    override fun visitCodeFragment(codeFragment: FirCodeFragment, data: D): R = visitDeclaration(codeFragment, data)

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): R = visitExpression(anonymousFunctionExpression, data)

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): R = visitClass(anonymousObject, data)

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): R = visitExpression(anonymousObjectExpression, data)

    override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): R = visitImport(resolvedImport, data)

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): R = visitLoop(doWhileLoop, data)

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): R = visitLoop(whileLoop, data)

    override fun visitBlock(block: FirBlock, data: D): R = visitExpression(block, data)

    override fun visitLazyBlock(lazyBlock: FirLazyBlock, data: D): R = visitBlock(lazyBlock, data)

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): R = visitExpression(binaryLogicExpression, data)

    override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): R = visitExpression(jump, data)

    override fun visitLoopJump(loopJump: FirLoopJump, data: D): R = visitJump(loopJump, data)

    override fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): R = visitLoopJump(breakExpression, data)

    override fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): R = visitLoopJump(continueExpression, data)

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): R = visitExpression(constExpression, data)

    override fun visitStarProjection(starProjection: FirStarProjection, data: D): R = visitTypeProjection(starProjection, data)

    override fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): R = visitTypeProjection(placeholderProjection, data)

    override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): R = visitTypeProjection(typeProjectionWithVariance, data)

    override fun visitCall(call: FirCall, data: D): R = visitStatement(call, data)

    override fun visitAnnotation(annotation: FirAnnotation, data: D): R = visitExpression(annotation, data)

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): R = visitExpression(comparisonExpression, data)

    override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): R = visitStatement(assignmentOperatorStatement, data)

    override fun visitIncrementDecrementExpression(incrementDecrementExpression: FirIncrementDecrementExpression, data: D): R = visitExpression(incrementDecrementExpression, data)

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): R = visitStatement(augmentedArraySetCall, data)

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): R = visitExpression(classReferenceExpression, data)

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: D): R = visitQualifiedAccessExpression(propertyAccessExpression, data)

    override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): R = visitFunctionCall(integerLiteralOperatorCall, data)

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): R = visitFunctionCall(implicitInvokeCall, data)

    override fun visitMultiDelegatedConstructorCall(multiDelegatedConstructorCall: FirMultiDelegatedConstructorCall, data: D): R = visitDelegatedConstructorCall(multiDelegatedConstructorCall, data)

    override fun visitComponentCall(componentCall: FirComponentCall, data: D): R = visitFunctionCall(componentCall, data)

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): R = visitQualifiedAccessExpression(callableReferenceAccess, data)

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): R = visitQualifiedAccessExpression(thisReceiverExpression, data)

    override fun visitSmartCastExpression(smartCastExpression: FirSmartCastExpression, data: D): R = visitExpression(smartCastExpression, data)

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): R = visitExpression(safeCallExpression, data)

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): R = visitExpression(checkedSafeCallSubject, data)

    override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): R = visitExpression(wrappedExpression, data)

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): R = visitWrappedExpression(wrappedArgumentExpression, data)

    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): R = visitWrappedArgumentExpression(lambdaArgumentExpression, data)

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): R = visitWrappedArgumentExpression(spreadArgumentExpression, data)

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): R = visitWrappedArgumentExpression(namedArgumentExpression, data)

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): R = visitExpression(varargArgumentsExpression, data)

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): R = visitExpression(resolvedQualifier, data)

    override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): R = visitExpression(resolvedReifiedParameterReference, data)

    override fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): R = visitJump(returnExpression, data)

    override fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): R = visitExpression(throwExpression, data)

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): R = visitStatement(variableAssignment, data)

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): R = visitExpression(whenSubjectExpression, data)

    override fun visitDesugaredAssignmentValueReferenceExpression(desugaredAssignmentValueReferenceExpression: FirDesugaredAssignmentValueReferenceExpression, data: D): R = visitExpression(desugaredAssignmentValueReferenceExpression, data)

    override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): R = visitWrappedExpression(wrappedDelegateExpression, data)

    override fun visitEnumEntryDeserializedAccessExpression(enumEntryDeserializedAccessExpression: FirEnumEntryDeserializedAccessExpression, data: D): R = visitExpression(enumEntryDeserializedAccessExpression, data)

    override fun visitNamedReference(namedReference: FirNamedReference, data: D): R = visitReference(namedReference, data)

    override fun visitNamedReferenceWithCandidateBase(namedReferenceWithCandidateBase: FirNamedReferenceWithCandidateBase, data: D): R = visitNamedReference(namedReferenceWithCandidateBase, data)

    override fun visitFromMissingDependenciesNamedReference(fromMissingDependenciesNamedReference: FirFromMissingDependenciesNamedReference, data: D): R = visitNamedReference(fromMissingDependenciesNamedReference, data)

    override fun visitSuperReference(superReference: FirSuperReference, data: D): R = visitReference(superReference, data)

    override fun visitThisReference(thisReference: FirThisReference, data: D): R = visitReference(thisReference, data)

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): R = visitReference(controlFlowGraphReference, data)

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): R = visitNamedReference(resolvedNamedReference, data)

    override fun visitResolvedErrorReference(resolvedErrorReference: FirResolvedErrorReference, data: D): R = visitResolvedNamedReference(resolvedErrorReference, data)

    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): R = visitResolvedNamedReference(delegateFieldReference, data)

    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): R = visitResolvedNamedReference(backingFieldReference, data)

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): R = visitResolvedNamedReference(resolvedCallableReference, data)

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): R = visitTypeRef(resolvedTypeRef, data)

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): R = visitTypeRef(typeRefWithNullability, data)

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): R = visitTypeRefWithNullability(userTypeRef, data)

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): R = visitTypeRefWithNullability(dynamicTypeRef, data)

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): R = visitTypeRefWithNullability(functionTypeRef, data)

    override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): R = visitTypeRefWithNullability(intersectionTypeRef, data)

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): R = visitTypeRef(implicitTypeRef, data)

    override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): R = visitContractElementDeclaration(effectDeclaration, data)

    override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): R = visitContractDescription(legacyRawContractDescription, data)

    override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): R = visitContractDescription(rawContractDescription, data)

    override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): R = visitContractDescription(resolvedContractDescription, data)

}
