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
import org.jetbrains.kotlin.fir.declarations.FirBackingField
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
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
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
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
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

abstract class FirDefaultVisitorVoid : FirVisitorVoid() {
    override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer)  = visitElement(annotationContainer)

    override fun visitTypeRef(typeRef: FirTypeRef)  = visitAnnotationContainer(typeRef)

    override fun visitReference(reference: FirReference)  = visitElement(reference)

    override fun visitLabel(label: FirLabel)  = visitElement(label)

    override fun visitResolvable(resolvable: FirResolvable)  = visitElement(resolvable)

    override fun visitTargetElement(targetElement: FirTargetElement)  = visitElement(targetElement)

    override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus)  = visitElement(declarationStatus)

    override fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus)  = visitDeclarationStatus(resolvedDeclarationStatus)

    override fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner)  = visitElement(controlFlowGraphOwner)

    override fun visitStatement(statement: FirStatement)  = visitAnnotationContainer(statement)

    override fun visitExpression(expression: FirExpression)  = visitStatement(expression)

    override fun visitDeclaration(declaration: FirDeclaration)  = visitElement(declaration)

    override fun visitAnnotatedDeclaration(annotatedDeclaration: FirAnnotatedDeclaration)  = visitDeclaration(annotatedDeclaration)

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer)  = visitDeclaration(anonymousInitializer)

    override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration)  = visitAnnotatedDeclaration(typedDeclaration)

    override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner)  = visitElement(typeParameterRefsOwner)

    override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner)  = visitTypeParameterRefsOwner(typeParametersOwner)

    override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration)  = visitTypeParameterRefsOwner(memberDeclaration)

    override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration)  = visitTypedDeclaration(callableDeclaration)

    override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef)  = visitElement(typeParameterRef)

    override fun visitTypeParameter(typeParameter: FirTypeParameter)  = visitTypeParameterRef(typeParameter)

    override fun visitVariable(variable: FirVariable)  = visitCallableDeclaration(variable)

    override fun visitValueParameter(valueParameter: FirValueParameter)  = visitVariable(valueParameter)

    override fun visitProperty(property: FirProperty)  = visitVariable(property)

    override fun visitField(field: FirField)  = visitVariable(field)

    override fun visitEnumEntry(enumEntry: FirEnumEntry)  = visitVariable(enumEntry)

    override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration)  = visitAnnotatedDeclaration(classLikeDeclaration)

    override fun visitClass(klass: FirClass)  = visitClassLikeDeclaration(klass)

    override fun visitRegularClass(regularClass: FirRegularClass)  = visitClass(regularClass)

    override fun visitTypeAlias(typeAlias: FirTypeAlias)  = visitClassLikeDeclaration(typeAlias)

    override fun visitFunction(function: FirFunction)  = visitCallableDeclaration(function)

    override fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner)  = visitElement(contractDescriptionOwner)

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction)  = visitFunction(simpleFunction)

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor)  = visitFunction(propertyAccessor)

    override fun visitBackingField(backingField: FirBackingField)  = visitVariable(backingField)

    override fun visitConstructor(constructor: FirConstructor)  = visitFunction(constructor)

    override fun visitFile(file: FirFile)  = visitAnnotatedDeclaration(file)

    override fun visitPackageDirective(packageDirective: FirPackageDirective)  = visitElement(packageDirective)

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction)  = visitFunction(anonymousFunction)

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression)  = visitExpression(anonymousFunctionExpression)

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject)  = visitClass(anonymousObject)

    override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression)  = visitExpression(anonymousObjectExpression)

    override fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder)  = visitElement(diagnosticHolder)

    override fun visitImport(import: FirImport)  = visitElement(import)

    override fun visitResolvedImport(resolvedImport: FirResolvedImport)  = visitImport(resolvedImport)

    override fun visitErrorImport(errorImport: FirErrorImport)  = visitImport(errorImport)

    override fun visitLoop(loop: FirLoop)  = visitStatement(loop)

    override fun visitErrorLoop(errorLoop: FirErrorLoop)  = visitLoop(errorLoop)

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop)  = visitLoop(doWhileLoop)

    override fun visitWhileLoop(whileLoop: FirWhileLoop)  = visitLoop(whileLoop)

    override fun visitBlock(block: FirBlock)  = visitExpression(block)

    override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression)  = visitExpression(binaryLogicExpression)

    override fun <E : FirTargetElement> visitJump(jump: FirJump<E>)  = visitExpression(jump)

    override fun visitLoopJump(loopJump: FirLoopJump)  = visitJump(loopJump)

    override fun visitBreakExpression(breakExpression: FirBreakExpression)  = visitLoopJump(breakExpression)

    override fun visitContinueExpression(continueExpression: FirContinueExpression)  = visitLoopJump(continueExpression)

    override fun visitCatch(catch: FirCatch)  = visitElement(catch)

    override fun visitTryExpression(tryExpression: FirTryExpression)  = visitExpression(tryExpression)

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>)  = visitExpression(constExpression)

    override fun visitTypeProjection(typeProjection: FirTypeProjection)  = visitElement(typeProjection)

    override fun visitStarProjection(starProjection: FirStarProjection)  = visitTypeProjection(starProjection)

    override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance)  = visitTypeProjection(typeProjectionWithVariance)

    override fun visitArgumentList(argumentList: FirArgumentList)  = visitElement(argumentList)

    override fun visitCall(call: FirCall)  = visitStatement(call)

    override fun visitAnnotation(annotation: FirAnnotation)  = visitExpression(annotation)

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall)  = visitAnnotation(annotationCall)

    override fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping)  = visitElement(annotationArgumentMapping)

    override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression)  = visitExpression(comparisonExpression)

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall)  = visitExpression(typeOperatorCall)

    override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement)  = visitStatement(assignmentOperatorStatement)

    override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall)  = visitExpression(equalityOperatorCall)

    override fun visitWhenExpression(whenExpression: FirWhenExpression)  = visitExpression(whenExpression)

    override fun visitWhenBranch(whenBranch: FirWhenBranch)  = visitElement(whenBranch)

    override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess)  = visitResolvable(qualifiedAccess)

    override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall)  = visitExpression(checkNotNullCall)

    override fun visitElvisExpression(elvisExpression: FirElvisExpression)  = visitExpression(elvisExpression)

    override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall)  = visitExpression(arrayOfCall)

    override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall)  = visitStatement(augmentedArraySetCall)

    override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression)  = visitExpression(classReferenceExpression)

    override fun visitErrorExpression(errorExpression: FirErrorExpression)  = visitExpression(errorExpression)

    override fun visitErrorFunction(errorFunction: FirErrorFunction)  = visitFunction(errorFunction)

    override fun visitErrorProperty(errorProperty: FirErrorProperty)  = visitVariable(errorProperty)

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression)  = visitExpression(qualifiedAccessExpression)

    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression)  = visitQualifiedAccessExpression(propertyAccessExpression)

    override fun visitFunctionCall(functionCall: FirFunctionCall)  = visitQualifiedAccessExpression(functionCall)

    override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall)  = visitFunctionCall(implicitInvokeCall)

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall)  = visitResolvable(delegatedConstructorCall)

    override fun visitComponentCall(componentCall: FirComponentCall)  = visitFunctionCall(componentCall)

    override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess)  = visitQualifiedAccessExpression(callableReferenceAccess)

    override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression)  = visitQualifiedAccessExpression(thisReceiverExpression)

    override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast)  = visitQualifiedAccessExpression(expressionWithSmartcast)

    override fun visitExpressionWithSmartcastToNull(expressionWithSmartcastToNull: FirExpressionWithSmartcastToNull)  = visitExpressionWithSmartcast(expressionWithSmartcastToNull)

    override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression)  = visitExpression(safeCallExpression)

    override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject)  = visitExpression(checkedSafeCallSubject)

    override fun visitGetClassCall(getClassCall: FirGetClassCall)  = visitExpression(getClassCall)

    override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression)  = visitExpression(wrappedExpression)

    override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression)  = visitWrappedExpression(wrappedArgumentExpression)

    override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression)  = visitWrappedArgumentExpression(lambdaArgumentExpression)

    override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression)  = visitWrappedArgumentExpression(spreadArgumentExpression)

    override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression)  = visitWrappedArgumentExpression(namedArgumentExpression)

    override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression)  = visitExpression(varargArgumentsExpression)

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier)  = visitExpression(resolvedQualifier)

    override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier)  = visitResolvedQualifier(errorResolvedQualifier)

    override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference)  = visitExpression(resolvedReifiedParameterReference)

    override fun visitReturnExpression(returnExpression: FirReturnExpression)  = visitJump(returnExpression)

    override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall)  = visitCall(stringConcatenationCall)

    override fun visitThrowExpression(throwExpression: FirThrowExpression)  = visitExpression(throwExpression)

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment)  = visitQualifiedAccess(variableAssignment)

    override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression)  = visitExpression(whenSubjectExpression)

    override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression)  = visitWrappedExpression(wrappedDelegateExpression)

    override fun visitNamedReference(namedReference: FirNamedReference)  = visitReference(namedReference)

    override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference)  = visitNamedReference(errorNamedReference)

    override fun visitSuperReference(superReference: FirSuperReference)  = visitReference(superReference)

    override fun visitThisReference(thisReference: FirThisReference)  = visitReference(thisReference)

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference)  = visitReference(controlFlowGraphReference)

    override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference)  = visitNamedReference(resolvedNamedReference)

    override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference)  = visitResolvedNamedReference(delegateFieldReference)

    override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference)  = visitResolvedNamedReference(backingFieldReference)

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference)  = visitResolvedNamedReference(resolvedCallableReference)

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef)  = visitTypeRef(resolvedTypeRef)

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef)  = visitResolvedTypeRef(errorTypeRef)

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability)  = visitTypeRef(typeRefWithNullability)

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef)  = visitTypeRefWithNullability(userTypeRef)

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef)  = visitTypeRefWithNullability(dynamicTypeRef)

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef)  = visitTypeRefWithNullability(functionTypeRef)

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef)  = visitTypeRef(implicitTypeRef)

    override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration)  = visitElement(effectDeclaration)

    override fun visitContractDescription(contractDescription: FirContractDescription)  = visitElement(contractDescription)

    override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription)  = visitContractDescription(legacyRawContractDescription)

    override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription)  = visitContractDescription(rawContractDescription)

    override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription)  = visitContractDescription(resolvedContractDescription)

}
