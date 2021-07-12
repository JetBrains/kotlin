/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
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
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirComparisonExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirAssignmentOperatorStatement
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirAugmentedArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.declarations.FirErrorProperty
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcastToNull
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
import org.jetbrains.kotlin.fir.expressions.FirWrappedDelegateExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirDelegateFieldReference
import org.jetbrains.kotlin.fir.references.FirBackingFieldReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.FirDynamicTypeRef
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirVisitor<out R, in D> {
    abstract fun visitElement(element: FirElement, data: D): R

    open fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): R  = visitElement(annotationContainer, data)

    open fun visitTypeRef(typeRef: FirTypeRef, data: D): R  = visitElement(typeRef, data)

    open fun visitReference(reference: FirReference, data: D): R  = visitElement(reference, data)

    open fun visitLabel(label: FirLabel, data: D): R  = visitElement(label, data)

    open fun visitResolvable(resolvable: FirResolvable, data: D): R  = visitElement(resolvable, data)

    open fun visitTargetElement(targetElement: FirTargetElement, data: D): R  = visitElement(targetElement, data)

    open fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): R  = visitElement(declarationStatus, data)

    open fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): R  = visitElement(resolvedDeclarationStatus, data)

    open fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): R  = visitElement(controlFlowGraphOwner, data)

    open fun visitStatement(statement: FirStatement, data: D): R  = visitElement(statement, data)

    open fun visitExpression(expression: FirExpression, data: D): R  = visitElement(expression, data)

    open fun visitDeclaration(declaration: FirDeclaration, data: D): R  = visitElement(declaration, data)

    open fun visitAnnotatedDeclaration(annotatedDeclaration: FirAnnotatedDeclaration, data: D): R  = visitElement(annotatedDeclaration, data)

    open fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): R  = visitElement(anonymousInitializer, data)

    open fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): R  = visitElement(typedDeclaration, data)

    open fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): R  = visitElement(typeParameterRefsOwner, data)

    open fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): R  = visitElement(typeParametersOwner, data)

    open fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): R  = visitElement(memberDeclaration, data)

    open fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): R  = visitElement(callableDeclaration, data)

    open fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: D): R  = visitElement(typeParameterRef, data)

    open fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): R  = visitElement(typeParameter, data)

    open fun visitVariable(variable: FirVariable, data: D): R  = visitElement(variable, data)

    open fun visitValueParameter(valueParameter: FirValueParameter, data: D): R  = visitElement(valueParameter, data)

    open fun visitProperty(property: FirProperty, data: D): R  = visitElement(property, data)

    open fun visitField(field: FirField, data: D): R  = visitElement(field, data)

    open fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): R  = visitElement(enumEntry, data)

    open fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: D): R  = visitElement(classLikeDeclaration, data)

    open fun visitClass(klass: FirClass, data: D): R  = visitElement(klass, data)

    open fun visitRegularClass(regularClass: FirRegularClass, data: D): R  = visitElement(regularClass, data)

    open fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): R  = visitElement(typeAlias, data)

    open fun visitFunction(function: FirFunction, data: D): R  = visitElement(function, data)

    open fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): R  = visitElement(contractDescriptionOwner, data)

    open fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: D): R  = visitElement(simpleFunction, data)

    open fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): R  = visitElement(propertyAccessor, data)

    open fun visitConstructor(constructor: FirConstructor, data: D): R  = visitElement(constructor, data)

    open fun visitFile(file: FirFile, data: D): R  = visitElement(file, data)

    open fun visitPackageDirective(packageDirective: FirPackageDirective, data: D): R  = visitElement(packageDirective, data)

    open fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): R  = visitElement(anonymousFunction, data)

    open fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): R  = visitElement(anonymousFunctionExpression, data)

    open fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): R  = visitElement(anonymousObject, data)

    open fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): R  = visitElement(anonymousObjectExpression, data)

    open fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): R  = visitElement(diagnosticHolder, data)

    open fun visitImport(import: FirImport, data: D): R  = visitElement(import, data)

    open fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): R  = visitElement(resolvedImport, data)

    open fun visitErrorImport(errorImport: FirErrorImport, data: D): R  = visitElement(errorImport, data)

    open fun visitLoop(loop: FirLoop, data: D): R  = visitElement(loop, data)

    open fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): R  = visitElement(errorLoop, data)

    open fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): R  = visitElement(doWhileLoop, data)

    open fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): R  = visitElement(whileLoop, data)

    open fun visitBlock(block: FirBlock, data: D): R  = visitElement(block, data)

    open fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): R  = visitElement(binaryLogicExpression, data)

    open fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): R  = visitElement(jump, data)

    open fun visitLoopJump(loopJump: FirLoopJump, data: D): R  = visitElement(loopJump, data)

    open fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): R  = visitElement(breakExpression, data)

    open fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): R  = visitElement(continueExpression, data)

    open fun visitCatch(catch: FirCatch, data: D): R  = visitElement(catch, data)

    open fun visitTryExpression(tryExpression: FirTryExpression, data: D): R  = visitElement(tryExpression, data)

    open fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): R  = visitElement(constExpression, data)

    open fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): R  = visitElement(typeProjection, data)

    open fun visitStarProjection(starProjection: FirStarProjection, data: D): R  = visitElement(starProjection, data)

    open fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): R  = visitElement(typeProjectionWithVariance, data)

    open fun visitArgumentList(argumentList: FirArgumentList, data: D): R  = visitElement(argumentList, data)

    open fun visitCall(call: FirCall, data: D): R  = visitElement(call, data)

    open fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): R  = visitElement(annotationCall, data)

    open fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): R  = visitElement(comparisonExpression, data)

    open fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): R  = visitElement(typeOperatorCall, data)

    open fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): R  = visitElement(assignmentOperatorStatement, data)

    open fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): R  = visitElement(equalityOperatorCall, data)

    open fun visitWhenExpression(whenExpression: FirWhenExpression, data: D): R  = visitElement(whenExpression, data)

    open fun visitWhenBranch(whenBranch: FirWhenBranch, data: D): R  = visitElement(whenBranch, data)

    open fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): R  = visitElement(qualifiedAccess, data)

    open fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): R  = visitElement(checkNotNullCall, data)

    open fun visitElvisExpression(elvisExpression: FirElvisExpression, data: D): R  = visitElement(elvisExpression, data)

    open fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): R  = visitElement(arrayOfCall, data)

    open fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): R  = visitElement(augmentedArraySetCall, data)

    open fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): R  = visitElement(classReferenceExpression, data)

    open fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): R  = visitElement(errorExpression, data)

    open fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): R  = visitElement(errorFunction, data)

    open fun visitErrorProperty(errorProperty: FirErrorProperty, data: D): R  = visitElement(errorProperty, data)

    open fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): R  = visitElement(qualifiedAccessExpression, data)

    open fun visitFunctionCall(functionCall: FirFunctionCall, data: D): R  = visitElement(functionCall, data)

    open fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): R  = visitElement(implicitInvokeCall, data)

    open fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): R  = visitElement(delegatedConstructorCall, data)

    open fun visitComponentCall(componentCall: FirComponentCall, data: D): R  = visitElement(componentCall, data)

    open fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): R  = visitElement(callableReferenceAccess, data)

    open fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): R  = visitElement(thisReceiverExpression, data)

    open fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: D): R  = visitElement(expressionWithSmartcast, data)

    open fun visitExpressionWithSmartcastToNull(expressionWithSmartcastToNull: FirExpressionWithSmartcastToNull, data: D): R  = visitElement(expressionWithSmartcastToNull, data)

    open fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): R  = visitElement(safeCallExpression, data)

    open fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): R  = visitElement(checkedSafeCallSubject, data)

    open fun visitGetClassCall(getClassCall: FirGetClassCall, data: D): R  = visitElement(getClassCall, data)

    open fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): R  = visitElement(wrappedExpression, data)

    open fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): R  = visitElement(wrappedArgumentExpression, data)

    open fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): R  = visitElement(lambdaArgumentExpression, data)

    open fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): R  = visitElement(spreadArgumentExpression, data)

    open fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): R  = visitElement(namedArgumentExpression, data)

    open fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): R  = visitElement(varargArgumentsExpression, data)

    open fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): R  = visitElement(resolvedQualifier, data)

    open fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): R  = visitElement(errorResolvedQualifier, data)

    open fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): R  = visitElement(resolvedReifiedParameterReference, data)

    open fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): R  = visitElement(returnExpression, data)

    open fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): R  = visitElement(stringConcatenationCall, data)

    open fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): R  = visitElement(throwExpression, data)

    open fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): R  = visitElement(variableAssignment, data)

    open fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): R  = visitElement(whenSubjectExpression, data)

    open fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): R  = visitElement(wrappedDelegateExpression, data)

    open fun visitNamedReference(namedReference: FirNamedReference, data: D): R  = visitElement(namedReference, data)

    open fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): R  = visitElement(errorNamedReference, data)

    open fun visitSuperReference(superReference: FirSuperReference, data: D): R  = visitElement(superReference, data)

    open fun visitThisReference(thisReference: FirThisReference, data: D): R  = visitElement(thisReference, data)

    open fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): R  = visitElement(controlFlowGraphReference, data)

    open fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): R  = visitElement(resolvedNamedReference, data)

    open fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): R  = visitElement(delegateFieldReference, data)

    open fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): R  = visitElement(backingFieldReference, data)

    open fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): R  = visitElement(resolvedCallableReference, data)

    open fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): R  = visitElement(resolvedTypeRef, data)

    open fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): R  = visitElement(errorTypeRef, data)

    open fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): R  = visitElement(typeRefWithNullability, data)

    open fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): R  = visitElement(userTypeRef, data)

    open fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): R  = visitElement(dynamicTypeRef, data)

    open fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): R  = visitElement(functionTypeRef, data)

    open fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): R  = visitElement(implicitTypeRef, data)

    open fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): R  = visitElement(effectDeclaration, data)

    open fun visitContractDescription(contractDescription: FirContractDescription, data: D): R  = visitElement(contractDescription, data)

    open fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): R  = visitElement(legacyRawContractDescription, data)

    open fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): R  = visitElement(rawContractDescription, data)

    open fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): R  = visitElement(resolvedContractDescription, data)

}
