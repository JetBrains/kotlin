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

abstract class FirTransformer<in D> : FirVisitor<FirElement, D>() {

    abstract fun <E : FirElement> transformElement(element: E, data: D): E

    open fun transformAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): FirAnnotationContainer {
        return transformElement(annotationContainer, data)
    }

    open fun transformTypeRef(typeRef: FirTypeRef, data: D): FirTypeRef {
        return transformElement(typeRef, data)
    }

    open fun transformReference(reference: FirReference, data: D): FirReference {
        return transformElement(reference, data)
    }

    open fun transformLabel(label: FirLabel, data: D): FirLabel {
        return transformElement(label, data)
    }

    open fun transformResolvable(resolvable: FirResolvable, data: D): FirResolvable {
        return transformElement(resolvable, data)
    }

    open fun transformTargetElement(targetElement: FirTargetElement, data: D): FirTargetElement {
        return transformElement(targetElement, data)
    }

    open fun transformDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): FirDeclarationStatus {
        return transformElement(declarationStatus, data)
    }

    open fun transformResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): FirDeclarationStatus {
        return transformElement(resolvedDeclarationStatus, data)
    }

    open fun transformControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): FirControlFlowGraphOwner {
        return transformElement(controlFlowGraphOwner, data)
    }

    open fun transformStatement(statement: FirStatement, data: D): FirStatement {
        return transformElement(statement, data)
    }

    open fun transformExpression(expression: FirExpression, data: D): FirStatement {
        return transformElement(expression, data)
    }

    open fun transformDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformElement(declaration, data)
    }

    open fun transformAnnotatedDeclaration(annotatedDeclaration: FirAnnotatedDeclaration, data: D): FirAnnotatedDeclaration {
        return transformElement(annotatedDeclaration, data)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): FirAnonymousInitializer {
        return transformElement(anonymousInitializer, data)
    }

    open fun transformTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): FirTypedDeclaration {
        return transformElement(typedDeclaration, data)
    }

    open fun transformTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): FirTypeParameterRefsOwner {
        return transformElement(typeParameterRefsOwner, data)
    }

    open fun transformTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): FirTypeParametersOwner {
        return transformElement(typeParametersOwner, data)
    }

    open fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): FirMemberDeclaration {
        return transformElement(memberDeclaration, data)
    }

    open fun transformCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): FirCallableDeclaration {
        return transformElement(callableDeclaration, data)
    }

    open fun transformTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: D): FirTypeParameterRef {
        return transformElement(typeParameterRef, data)
    }

    open fun transformTypeParameter(typeParameter: FirTypeParameter, data: D): FirTypeParameterRef {
        return transformElement(typeParameter, data)
    }

    open fun transformVariable(variable: FirVariable, data: D): FirStatement {
        return transformElement(variable, data)
    }

    open fun transformValueParameter(valueParameter: FirValueParameter, data: D): FirStatement {
        return transformElement(valueParameter, data)
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

    open fun transformContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): FirContractDescriptionOwner {
        return transformElement(contractDescriptionOwner, data)
    }

    open fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: D): FirStatement {
        return transformElement(simpleFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): FirStatement {
        return transformElement(propertyAccessor, data)
    }

    open fun transformConstructor(constructor: FirConstructor, data: D): FirStatement {
        return transformElement(constructor, data)
    }

    open fun transformFile(file: FirFile, data: D): FirFile {
        return transformElement(file, data)
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

    open fun transformDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): FirDiagnosticHolder {
        return transformElement(diagnosticHolder, data)
    }

    open fun transformImport(import: FirImport, data: D): FirImport {
        return transformElement(import, data)
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

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): FirTypeProjection {
        return transformElement(typeProjectionWithVariance, data)
    }

    open fun transformArgumentList(argumentList: FirArgumentList, data: D): FirArgumentList {
        return transformElement(argumentList, data)
    }

    open fun transformCall(call: FirCall, data: D): FirStatement {
        return transformElement(call, data)
    }

    open fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirStatement {
        return transformElement(annotationCall, data)
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

    open fun transformEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): FirStatement {
        return transformElement(equalityOperatorCall, data)
    }

    open fun transformWhenExpression(whenExpression: FirWhenExpression, data: D): FirStatement {
        return transformElement(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: FirWhenBranch, data: D): FirWhenBranch {
        return transformElement(whenBranch, data)
    }

    open fun transformQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): FirStatement {
        return transformElement(qualifiedAccess, data)
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

    open fun transformQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): FirStatement {
        return transformElement(qualifiedAccessExpression, data)
    }

    open fun transformFunctionCall(functionCall: FirFunctionCall, data: D): FirStatement {
        return transformElement(functionCall, data)
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

    open fun transformExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: D): FirStatement {
        return transformElement(expressionWithSmartcast, data)
    }

    open fun transformExpressionWithSmartcastToNull(expressionWithSmartcastToNull: FirExpressionWithSmartcastToNull, data: D): FirStatement {
        return transformElement(expressionWithSmartcastToNull, data)
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

    open fun transformWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): FirStatement {
        return transformElement(wrappedDelegateExpression, data)
    }

    open fun transformNamedReference(namedReference: FirNamedReference, data: D): FirReference {
        return transformElement(namedReference, data)
    }

    open fun transformErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): FirReference {
        return transformElement(errorNamedReference, data)
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

    open fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformElement(implicitTypeRef, data)
    }

    open fun transformEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): FirEffectDeclaration {
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

    final override fun visitElement(element: FirElement, data: D): FirElement {
        return transformElement(element, data)
    }

    final override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): FirAnnotationContainer {
        return transformAnnotationContainer(annotationContainer, data)
    }

    final override fun visitTypeRef(typeRef: FirTypeRef, data: D): FirTypeRef {
        return transformTypeRef(typeRef, data)
    }

    final override fun visitReference(reference: FirReference, data: D): FirReference {
        return transformReference(reference, data)
    }

    final override fun visitLabel(label: FirLabel, data: D): FirLabel {
        return transformLabel(label, data)
    }

    final override fun visitResolvable(resolvable: FirResolvable, data: D): FirResolvable {
        return transformResolvable(resolvable, data)
    }

    final override fun visitTargetElement(targetElement: FirTargetElement, data: D): FirTargetElement {
        return transformTargetElement(targetElement, data)
    }

    final override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): FirDeclarationStatus {
        return transformDeclarationStatus(declarationStatus, data)
    }

    final override fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): FirDeclarationStatus {
        return transformResolvedDeclarationStatus(resolvedDeclarationStatus, data)
    }

    final override fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): FirControlFlowGraphOwner {
        return transformControlFlowGraphOwner(controlFlowGraphOwner, data)
    }

    final override fun visitStatement(statement: FirStatement, data: D): FirStatement {
        return transformStatement(statement, data)
    }

    final override fun visitExpression(expression: FirExpression, data: D): FirStatement {
        return transformExpression(expression, data)
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformDeclaration(declaration, data)
    }

    final override fun visitAnnotatedDeclaration(annotatedDeclaration: FirAnnotatedDeclaration, data: D): FirAnnotatedDeclaration {
        return transformAnnotatedDeclaration(annotatedDeclaration, data)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): FirAnonymousInitializer {
        return transformAnonymousInitializer(anonymousInitializer, data)
    }

    final override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): FirTypedDeclaration {
        return transformTypedDeclaration(typedDeclaration, data)
    }

    final override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): FirTypeParameterRefsOwner {
        return transformTypeParameterRefsOwner(typeParameterRefsOwner, data)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): FirTypeParametersOwner {
        return transformTypeParametersOwner(typeParametersOwner, data)
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): FirMemberDeclaration {
        return transformMemberDeclaration(memberDeclaration, data)
    }

    final override fun visitCallableDeclaration(callableDeclaration: FirCallableDeclaration, data: D): FirCallableDeclaration {
        return transformCallableDeclaration(callableDeclaration, data)
    }

    final override fun visitTypeParameterRef(typeParameterRef: FirTypeParameterRef, data: D): FirTypeParameterRef {
        return transformTypeParameterRef(typeParameterRef, data)
    }

    final override fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): FirTypeParameterRef {
        return transformTypeParameter(typeParameter, data)
    }

    final override fun visitVariable(variable: FirVariable, data: D): FirStatement {
        return transformVariable(variable, data)
    }

    final override fun visitValueParameter(valueParameter: FirValueParameter, data: D): FirStatement {
        return transformValueParameter(valueParameter, data)
    }

    final override fun visitProperty(property: FirProperty, data: D): FirStatement {
        return transformProperty(property, data)
    }

    final override fun visitField(field: FirField, data: D): FirStatement {
        return transformField(field, data)
    }

    final override fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): FirStatement {
        return transformEnumEntry(enumEntry, data)
    }

    final override fun visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration, data: D): FirStatement {
        return transformClassLikeDeclaration(classLikeDeclaration, data)
    }

    final override fun visitClass(klass: FirClass, data: D): FirStatement {
        return transformClass(klass, data)
    }

    final override fun visitRegularClass(regularClass: FirRegularClass, data: D): FirStatement {
        return transformRegularClass(regularClass, data)
    }

    final override fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): FirStatement {
        return transformTypeAlias(typeAlias, data)
    }

    final override fun visitFunction(function: FirFunction, data: D): FirStatement {
        return transformFunction(function, data)
    }

    final override fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): FirContractDescriptionOwner {
        return transformContractDescriptionOwner(contractDescriptionOwner, data)
    }

    final override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: D): FirStatement {
        return transformSimpleFunction(simpleFunction, data)
    }

    final override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): FirStatement {
        return transformPropertyAccessor(propertyAccessor, data)
    }

    final override fun visitConstructor(constructor: FirConstructor, data: D): FirStatement {
        return transformConstructor(constructor, data)
    }

    final override fun visitFile(file: FirFile, data: D): FirFile {
        return transformFile(file, data)
    }

    final override fun visitPackageDirective(packageDirective: FirPackageDirective, data: D): FirPackageDirective {
        return transformPackageDirective(packageDirective, data)
    }

    final override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): FirStatement {
        return transformAnonymousFunction(anonymousFunction, data)
    }

    final override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): FirStatement {
        return transformAnonymousFunctionExpression(anonymousFunctionExpression, data)
    }

    final override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): FirStatement {
        return transformAnonymousObject(anonymousObject, data)
    }

    final override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): FirStatement {
        return transformAnonymousObjectExpression(anonymousObjectExpression, data)
    }

    final override fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): FirDiagnosticHolder {
        return transformDiagnosticHolder(diagnosticHolder, data)
    }

    final override fun visitImport(import: FirImport, data: D): FirImport {
        return transformImport(import, data)
    }

    final override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): FirImport {
        return transformResolvedImport(resolvedImport, data)
    }

    final override fun visitErrorImport(errorImport: FirErrorImport, data: D): FirImport {
        return transformErrorImport(errorImport, data)
    }

    final override fun visitLoop(loop: FirLoop, data: D): FirStatement {
        return transformLoop(loop, data)
    }

    final override fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): FirStatement {
        return transformErrorLoop(errorLoop, data)
    }

    final override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): FirStatement {
        return transformDoWhileLoop(doWhileLoop, data)
    }

    final override fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): FirStatement {
        return transformWhileLoop(whileLoop, data)
    }

    final override fun visitBlock(block: FirBlock, data: D): FirStatement {
        return transformBlock(block, data)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): FirStatement {
        return transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    final override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): FirStatement {
        return transformJump(jump, data)
    }

    final override fun visitLoopJump(loopJump: FirLoopJump, data: D): FirStatement {
        return transformLoopJump(loopJump, data)
    }

    final override fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): FirStatement {
        return transformBreakExpression(breakExpression, data)
    }

    final override fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): FirStatement {
        return transformContinueExpression(continueExpression, data)
    }

    final override fun visitCatch(catch: FirCatch, data: D): FirCatch {
        return transformCatch(catch, data)
    }

    final override fun visitTryExpression(tryExpression: FirTryExpression, data: D): FirStatement {
        return transformTryExpression(tryExpression, data)
    }

    final override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): FirStatement {
        return transformConstExpression(constExpression, data)
    }

    final override fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): FirTypeProjection {
        return transformTypeProjection(typeProjection, data)
    }

    final override fun visitStarProjection(starProjection: FirStarProjection, data: D): FirTypeProjection {
        return transformStarProjection(starProjection, data)
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): FirTypeProjection {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance, data)
    }

    final override fun visitArgumentList(argumentList: FirArgumentList, data: D): FirArgumentList {
        return transformArgumentList(argumentList, data)
    }

    final override fun visitCall(call: FirCall, data: D): FirStatement {
        return transformCall(call, data)
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirStatement {
        return transformAnnotationCall(annotationCall, data)
    }

    final override fun visitComparisonExpression(comparisonExpression: FirComparisonExpression, data: D): FirStatement {
        return transformComparisonExpression(comparisonExpression, data)
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): FirStatement {
        return transformTypeOperatorCall(typeOperatorCall, data)
    }

    final override fun visitAssignmentOperatorStatement(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): FirStatement {
        return transformAssignmentOperatorStatement(assignmentOperatorStatement, data)
    }

    final override fun visitEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall, data: D): FirStatement {
        return transformEqualityOperatorCall(equalityOperatorCall, data)
    }

    final override fun visitWhenExpression(whenExpression: FirWhenExpression, data: D): FirStatement {
        return transformWhenExpression(whenExpression, data)
    }

    final override fun visitWhenBranch(whenBranch: FirWhenBranch, data: D): FirWhenBranch {
        return transformWhenBranch(whenBranch, data)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): FirStatement {
        return transformQualifiedAccess(qualifiedAccess, data)
    }

    final override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): FirStatement {
        return transformCheckNotNullCall(checkNotNullCall, data)
    }

    final override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: D): FirStatement {
        return transformElvisExpression(elvisExpression, data)
    }

    final override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): FirStatement {
        return transformArrayOfCall(arrayOfCall, data)
    }

    final override fun visitAugmentedArraySetCall(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): FirStatement {
        return transformAugmentedArraySetCall(augmentedArraySetCall, data)
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): FirStatement {
        return transformClassReferenceExpression(classReferenceExpression, data)
    }

    final override fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): FirStatement {
        return transformErrorExpression(errorExpression, data)
    }

    final override fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): FirStatement {
        return transformErrorFunction(errorFunction, data)
    }

    final override fun visitErrorProperty(errorProperty: FirErrorProperty, data: D): FirStatement {
        return transformErrorProperty(errorProperty, data)
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): FirStatement {
        return transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    final override fun visitFunctionCall(functionCall: FirFunctionCall, data: D): FirStatement {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitImplicitInvokeCall(implicitInvokeCall: FirImplicitInvokeCall, data: D): FirStatement {
        return transformImplicitInvokeCall(implicitInvokeCall, data)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): FirStatement {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    final override fun visitComponentCall(componentCall: FirComponentCall, data: D): FirStatement {
        return transformComponentCall(componentCall, data)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): FirStatement {
        return transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): FirStatement {
        return transformThisReceiverExpression(thisReceiverExpression, data)
    }

    final override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: D): FirStatement {
        return transformExpressionWithSmartcast(expressionWithSmartcast, data)
    }

    final override fun visitExpressionWithSmartcastToNull(expressionWithSmartcastToNull: FirExpressionWithSmartcastToNull, data: D): FirStatement {
        return transformExpressionWithSmartcastToNull(expressionWithSmartcastToNull, data)
    }

    final override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: D): FirStatement {
        return transformSafeCallExpression(safeCallExpression, data)
    }

    final override fun visitCheckedSafeCallSubject(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): FirStatement {
        return transformCheckedSafeCallSubject(checkedSafeCallSubject, data)
    }

    final override fun visitGetClassCall(getClassCall: FirGetClassCall, data: D): FirStatement {
        return transformGetClassCall(getClassCall, data)
    }

    final override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): FirStatement {
        return transformWrappedExpression(wrappedExpression, data)
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): FirStatement {
        return transformWrappedArgumentExpression(wrappedArgumentExpression, data)
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): FirStatement {
        return transformLambdaArgumentExpression(lambdaArgumentExpression, data)
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): FirStatement {
        return transformSpreadArgumentExpression(spreadArgumentExpression, data)
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): FirStatement {
        return transformNamedArgumentExpression(namedArgumentExpression, data)
    }

    final override fun visitVarargArgumentsExpression(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): FirStatement {
        return transformVarargArgumentsExpression(varargArgumentsExpression, data)
    }

    final override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): FirStatement {
        return transformResolvedQualifier(resolvedQualifier, data)
    }

    final override fun visitErrorResolvedQualifier(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): FirStatement {
        return transformErrorResolvedQualifier(errorResolvedQualifier, data)
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): FirStatement {
        return transformResolvedReifiedParameterReference(resolvedReifiedParameterReference, data)
    }

    final override fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): FirStatement {
        return transformReturnExpression(returnExpression, data)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): FirStatement {
        return transformStringConcatenationCall(stringConcatenationCall, data)
    }

    final override fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): FirStatement {
        return transformThrowExpression(throwExpression, data)
    }

    final override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): FirStatement {
        return transformVariableAssignment(variableAssignment, data)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): FirStatement {
        return transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): FirStatement {
        return transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    final override fun visitNamedReference(namedReference: FirNamedReference, data: D): FirReference {
        return transformNamedReference(namedReference, data)
    }

    final override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): FirReference {
        return transformErrorNamedReference(errorNamedReference, data)
    }

    final override fun visitSuperReference(superReference: FirSuperReference, data: D): FirReference {
        return transformSuperReference(superReference, data)
    }

    final override fun visitThisReference(thisReference: FirThisReference, data: D): FirReference {
        return transformThisReference(thisReference, data)
    }

    final override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): FirReference {
        return transformControlFlowGraphReference(controlFlowGraphReference, data)
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): FirReference {
        return transformResolvedNamedReference(resolvedNamedReference, data)
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): FirReference {
        return transformDelegateFieldReference(delegateFieldReference, data)
    }

    final override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): FirReference {
        return transformBackingFieldReference(backingFieldReference, data)
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): FirReference {
        return transformResolvedCallableReference(resolvedCallableReference, data)
    }

    final override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): FirTypeRef {
        return transformResolvedTypeRef(resolvedTypeRef, data)
    }

    final override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): FirTypeRef {
        return transformErrorTypeRef(errorTypeRef, data)
    }

    final override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): FirTypeRef {
        return transformTypeRefWithNullability(typeRefWithNullability, data)
    }

    final override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): FirTypeRef {
        return transformUserTypeRef(userTypeRef, data)
    }

    final override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): FirTypeRef {
        return transformDynamicTypeRef(dynamicTypeRef, data)
    }

    final override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): FirTypeRef {
        return transformFunctionTypeRef(functionTypeRef, data)
    }

    final override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformImplicitTypeRef(implicitTypeRef, data)
    }

    final override fun visitEffectDeclaration(effectDeclaration: FirEffectDeclaration, data: D): FirEffectDeclaration {
        return transformEffectDeclaration(effectDeclaration, data)
    }

    final override fun visitContractDescription(contractDescription: FirContractDescription, data: D): FirContractDescription {
        return transformContractDescription(contractDescription, data)
    }

    final override fun visitLegacyRawContractDescription(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): FirContractDescription {
        return transformLegacyRawContractDescription(legacyRawContractDescription, data)
    }

    final override fun visitRawContractDescription(rawContractDescription: FirRawContractDescription, data: D): FirContractDescription {
        return transformRawContractDescription(rawContractDescription, data)
    }

    final override fun visitResolvedContractDescription(resolvedContractDescription: FirResolvedContractDescription, data: D): FirContractDescription {
        return transformResolvedContractDescription(resolvedContractDescription, data)
    }

}
