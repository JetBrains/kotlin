/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.FirTargetElement
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvedDeclarationStatus
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSealedClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousObject
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
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
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.FirWhenBranch
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessWithoutCallee
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirArrayOfCall
import org.jetbrains.kotlin.fir.expressions.FirArraySetCall
import org.jetbrains.kotlin.fir.expressions.FirClassReferenceExpression
import org.jetbrains.kotlin.fir.expressions.FirErrorExpression
import org.jetbrains.kotlin.fir.declarations.FirErrorFunction
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirExpressionWithSmartcast
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedExpression
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirLambdaArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirNamedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
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
import org.jetbrains.kotlin.fir.types.FirDelegatedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRefWithNullability
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.FirDynamicTypeRef
import org.jetbrains.kotlin.fir.types.FirFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirResolvedFunctionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirTransformer<in D> : FirVisitor<CompositeTransformResult<FirElement>, D>() {

    abstract fun <E : FirElement> transformElement(element: E, data: D): CompositeTransformResult<E>

    open fun transformAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): CompositeTransformResult<FirAnnotationContainer> {
        return transformElement(annotationContainer, data)
    }

    open fun transformTypeRef(typeRef: FirTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(typeRef, data)
    }

    open fun transformReference(reference: FirReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(reference, data)
    }

    open fun transformLabel(label: FirLabel, data: D): CompositeTransformResult<FirLabel> {
        return transformElement(label, data)
    }

    open fun transformImport(import: FirImport, data: D): CompositeTransformResult<FirImport> {
        return transformElement(import, data)
    }

    open fun transformResolvedImport(resolvedImport: FirResolvedImport, data: D): CompositeTransformResult<FirImport> {
        return transformElement(resolvedImport, data)
    }

    open fun <E> transformSymbolOwner(symbolOwner: FirSymbolOwner<E>, data: D): CompositeTransformResult<FirSymbolOwner<E>> where E : FirSymbolOwner<E>, E : FirDeclaration {
        return transformElement(symbolOwner, data)
    }

    open fun transformResolvable(resolvable: FirResolvable, data: D): CompositeTransformResult<FirResolvable> {
        return transformElement(resolvable, data)
    }

    open fun transformControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): CompositeTransformResult<FirControlFlowGraphOwner> {
        return transformElement(controlFlowGraphOwner, data)
    }

    open fun transformTargetElement(targetElement: FirTargetElement, data: D): CompositeTransformResult<FirTargetElement> {
        return transformElement(targetElement, data)
    }

    open fun transformDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): CompositeTransformResult<FirDeclarationStatus> {
        return transformElement(declarationStatus, data)
    }

    open fun transformResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): CompositeTransformResult<FirDeclarationStatus> {
        return transformElement(resolvedDeclarationStatus, data)
    }

    open fun transformStatement(statement: FirStatement, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(statement, data)
    }

    open fun transformExpression(expression: FirExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(expression, data)
    }

    open fun transformDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(declaration, data)
    }

    open fun transformAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(anonymousInitializer, data)
    }

    open fun transformTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(typedDeclaration, data)
    }

    open fun <F : FirCallableDeclaration<F>> transformCallableDeclaration(callableDeclaration: FirCallableDeclaration<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(callableDeclaration, data)
    }

    open fun transformNamedDeclaration(namedDeclaration: FirNamedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(namedDeclaration, data)
    }

    open fun transformTypeParameter(typeParameter: FirTypeParameter, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(typeParameter, data)
    }

    open fun transformTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): CompositeTransformResult<FirTypeParametersOwner> {
        return transformElement(typeParametersOwner, data)
    }

    open fun transformMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(memberDeclaration, data)
    }

    open fun <F : FirCallableMemberDeclaration<F>> transformCallableMemberDeclaration(callableMemberDeclaration: FirCallableMemberDeclaration<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(callableMemberDeclaration, data)
    }

    open fun <F : FirVariable<F>> transformVariable(variable: FirVariable<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(variable, data)
    }

    open fun transformValueParameter(valueParameter: FirValueParameter, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(valueParameter, data)
    }

    open fun transformProperty(property: FirProperty, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(property, data)
    }

    open fun transformField(field: FirField, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(field, data)
    }

    open fun transformEnumEntry(enumEntry: FirEnumEntry, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(enumEntry, data)
    }

    open fun <F : FirClassLikeDeclaration<F>> transformClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(classLikeDeclaration, data)
    }

    open fun <F : FirClass<F>> transformClass(klass: FirClass<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(klass, data)
    }

    open fun transformRegularClass(regularClass: FirRegularClass, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(regularClass, data)
    }

    open fun transformSealedClass(sealedClass: FirSealedClass, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(sealedClass, data)
    }

    open fun transformTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(typeAlias, data)
    }

    open fun <F : FirFunction<F>> transformFunction(function: FirFunction<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(function, data)
    }

    open fun transformContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): CompositeTransformResult<FirContractDescriptionOwner> {
        return transformElement(contractDescriptionOwner, data)
    }

    open fun <F : FirMemberFunction<F>> transformMemberFunction(memberFunction: FirMemberFunction<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(memberFunction, data)
    }

    open fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(simpleFunction, data)
    }

    open fun transformPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(propertyAccessor, data)
    }

    open fun transformConstructor(constructor: FirConstructor, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(constructor, data)
    }

    open fun transformFile(file: FirFile, data: D): CompositeTransformResult<FirDeclaration> {
        return transformElement(file, data)
    }

    open fun transformAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(anonymousFunction, data)
    }

    open fun transformAnonymousObject(anonymousObject: FirAnonymousObject, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(anonymousObject, data)
    }

    open fun transformDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): CompositeTransformResult<FirDiagnosticHolder> {
        return transformElement(diagnosticHolder, data)
    }

    open fun transformLoop(loop: FirLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(loop, data)
    }

    open fun transformErrorLoop(errorLoop: FirErrorLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(errorLoop, data)
    }

    open fun transformDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(doWhileLoop, data)
    }

    open fun transformWhileLoop(whileLoop: FirWhileLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(whileLoop, data)
    }

    open fun transformBlock(block: FirBlock, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(block, data)
    }

    open fun transformBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(binaryLogicExpression, data)
    }

    open fun <E : FirTargetElement> transformJump(jump: FirJump<E>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(jump, data)
    }

    open fun transformLoopJump(loopJump: FirLoopJump, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(loopJump, data)
    }

    open fun transformBreakExpression(breakExpression: FirBreakExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(breakExpression, data)
    }

    open fun transformContinueExpression(continueExpression: FirContinueExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(continueExpression, data)
    }

    open fun transformCatch(catch: FirCatch, data: D): CompositeTransformResult<FirCatch> {
        return transformElement(catch, data)
    }

    open fun transformTryExpression(tryExpression: FirTryExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(tryExpression, data)
    }

    open fun <T> transformConstExpression(constExpression: FirConstExpression<T>, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(constExpression, data)
    }

    open fun transformTypeProjection(typeProjection: FirTypeProjection, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformElement(typeProjection, data)
    }

    open fun transformStarProjection(starProjection: FirStarProjection, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformElement(starProjection, data)
    }

    open fun transformTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformElement(typeProjectionWithVariance, data)
    }

    open fun transformCall(call: FirCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(call, data)
    }

    open fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(annotationCall, data)
    }

    open fun transformOperatorCall(operatorCall: FirOperatorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(operatorCall, data)
    }

    open fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(typeOperatorCall, data)
    }

    open fun transformWhenExpression(whenExpression: FirWhenExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(whenExpression, data)
    }

    open fun transformWhenBranch(whenBranch: FirWhenBranch, data: D): CompositeTransformResult<FirWhenBranch> {
        return transformElement(whenBranch, data)
    }

    open fun transformQualifiedAccessWithoutCallee(qualifiedAccessWithoutCallee: FirQualifiedAccessWithoutCallee, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(qualifiedAccessWithoutCallee, data)
    }

    open fun transformQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(qualifiedAccess, data)
    }

    open fun transformCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(checkNotNullCall, data)
    }

    open fun transformArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(arrayOfCall, data)
    }

    open fun transformArraySetCall(arraySetCall: FirArraySetCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(arraySetCall, data)
    }

    open fun transformClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(classReferenceExpression, data)
    }

    open fun transformErrorExpression(errorExpression: FirErrorExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(errorExpression, data)
    }

    open fun transformErrorFunction(errorFunction: FirErrorFunction, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(errorFunction, data)
    }

    open fun transformQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(qualifiedAccessExpression, data)
    }

    open fun transformFunctionCall(functionCall: FirFunctionCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(functionCall, data)
    }

    open fun transformDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(delegatedConstructorCall, data)
    }

    open fun transformComponentCall(componentCall: FirComponentCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(componentCall, data)
    }

    open fun transformCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(callableReferenceAccess, data)
    }

    open fun transformThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(thisReceiverExpression, data)
    }

    open fun transformExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(expressionWithSmartcast, data)
    }

    open fun transformGetClassCall(getClassCall: FirGetClassCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(getClassCall, data)
    }

    open fun transformWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(wrappedExpression, data)
    }

    open fun transformWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(wrappedArgumentExpression, data)
    }

    open fun transformLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(lambdaArgumentExpression, data)
    }

    open fun transformSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(spreadArgumentExpression, data)
    }

    open fun transformNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(namedArgumentExpression, data)
    }

    open fun transformResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(resolvedQualifier, data)
    }

    open fun transformResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(resolvedReifiedParameterReference, data)
    }

    open fun transformReturnExpression(returnExpression: FirReturnExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(returnExpression, data)
    }

    open fun transformStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(stringConcatenationCall, data)
    }

    open fun transformThrowExpression(throwExpression: FirThrowExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(throwExpression, data)
    }

    open fun transformVariableAssignment(variableAssignment: FirVariableAssignment, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(variableAssignment, data)
    }

    open fun transformWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(whenSubjectExpression, data)
    }

    open fun transformWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformElement(wrappedDelegateExpression, data)
    }

    open fun transformNamedReference(namedReference: FirNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(namedReference, data)
    }

    open fun transformErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(errorNamedReference, data)
    }

    open fun transformSuperReference(superReference: FirSuperReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(superReference, data)
    }

    open fun transformThisReference(thisReference: FirThisReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(thisReference, data)
    }

    open fun transformControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(controlFlowGraphReference, data)
    }

    open fun transformResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(resolvedNamedReference, data)
    }

    open fun transformDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(delegateFieldReference, data)
    }

    open fun transformBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(backingFieldReference, data)
    }

    open fun transformResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): CompositeTransformResult<FirReference> {
        return transformElement(resolvedCallableReference, data)
    }

    open fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(resolvedTypeRef, data)
    }

    open fun transformErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(errorTypeRef, data)
    }

    open fun transformDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(delegatedTypeRef, data)
    }

    open fun transformTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(typeRefWithNullability, data)
    }

    open fun transformUserTypeRef(userTypeRef: FirUserTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(userTypeRef, data)
    }

    open fun transformDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(dynamicTypeRef, data)
    }

    open fun transformFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(functionTypeRef, data)
    }

    open fun transformResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(resolvedFunctionTypeRef, data)
    }

    open fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformElement(implicitTypeRef, data)
    }

    open fun transformContractDescription(contractDescription: FirContractDescription, data: D): CompositeTransformResult<FirContractDescription> {
        return transformElement(contractDescription, data)
    }

    final override fun visitElement(element: FirElement, data: D): CompositeTransformResult<FirElement> {
        return transformElement(element, data)
    }

    final override fun visitAnnotationContainer(annotationContainer: FirAnnotationContainer, data: D): CompositeTransformResult<FirAnnotationContainer> {
        return transformAnnotationContainer(annotationContainer, data)
    }

    final override fun visitTypeRef(typeRef: FirTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRef(typeRef, data)
    }

    final override fun visitReference(reference: FirReference, data: D): CompositeTransformResult<FirReference> {
        return transformReference(reference, data)
    }

    final override fun visitLabel(label: FirLabel, data: D): CompositeTransformResult<FirLabel> {
        return transformLabel(label, data)
    }

    final override fun visitImport(import: FirImport, data: D): CompositeTransformResult<FirImport> {
        return transformImport(import, data)
    }

    final override fun visitResolvedImport(resolvedImport: FirResolvedImport, data: D): CompositeTransformResult<FirImport> {
        return transformResolvedImport(resolvedImport, data)
    }

    final override fun <E> visitSymbolOwner(symbolOwner: FirSymbolOwner<E>, data: D): CompositeTransformResult<FirSymbolOwner<E>> where E : FirSymbolOwner<E>, E : FirDeclaration {
        return transformSymbolOwner(symbolOwner, data)
    }

    final override fun visitResolvable(resolvable: FirResolvable, data: D): CompositeTransformResult<FirResolvable> {
        return transformResolvable(resolvable, data)
    }

    final override fun visitControlFlowGraphOwner(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): CompositeTransformResult<FirControlFlowGraphOwner> {
        return transformControlFlowGraphOwner(controlFlowGraphOwner, data)
    }

    final override fun visitTargetElement(targetElement: FirTargetElement, data: D): CompositeTransformResult<FirTargetElement> {
        return transformTargetElement(targetElement, data)
    }

    final override fun visitDeclarationStatus(declarationStatus: FirDeclarationStatus, data: D): CompositeTransformResult<FirDeclarationStatus> {
        return transformDeclarationStatus(declarationStatus, data)
    }

    final override fun visitResolvedDeclarationStatus(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): CompositeTransformResult<FirDeclarationStatus> {
        return transformResolvedDeclarationStatus(resolvedDeclarationStatus, data)
    }

    final override fun visitStatement(statement: FirStatement, data: D): CompositeTransformResult<FirStatement> {
        return transformStatement(statement, data)
    }

    final override fun visitExpression(expression: FirExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformExpression(expression, data)
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformDeclaration(declaration, data)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): CompositeTransformResult<FirDeclaration> {
        return transformAnonymousInitializer(anonymousInitializer, data)
    }

    final override fun visitTypedDeclaration(typedDeclaration: FirTypedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformTypedDeclaration(typedDeclaration, data)
    }

    final override fun <F : FirCallableDeclaration<F>> visitCallableDeclaration(callableDeclaration: FirCallableDeclaration<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformCallableDeclaration(callableDeclaration, data)
    }

    final override fun visitNamedDeclaration(namedDeclaration: FirNamedDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformNamedDeclaration(namedDeclaration, data)
    }

    final override fun visitTypeParameter(typeParameter: FirTypeParameter, data: D): CompositeTransformResult<FirDeclaration> {
        return transformTypeParameter(typeParameter, data)
    }

    final override fun visitTypeParametersOwner(typeParametersOwner: FirTypeParametersOwner, data: D): CompositeTransformResult<FirTypeParametersOwner> {
        return transformTypeParametersOwner(typeParametersOwner, data)
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): CompositeTransformResult<FirDeclaration> {
        return transformMemberDeclaration(memberDeclaration, data)
    }

    final override fun <F : FirCallableMemberDeclaration<F>> visitCallableMemberDeclaration(callableMemberDeclaration: FirCallableMemberDeclaration<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformCallableMemberDeclaration(callableMemberDeclaration, data)
    }

    final override fun <F : FirVariable<F>> visitVariable(variable: FirVariable<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformVariable(variable, data)
    }

    final override fun visitValueParameter(valueParameter: FirValueParameter, data: D): CompositeTransformResult<FirStatement> {
        return transformValueParameter(valueParameter, data)
    }

    final override fun visitProperty(property: FirProperty, data: D): CompositeTransformResult<FirDeclaration> {
        return transformProperty(property, data)
    }

    final override fun visitField(field: FirField, data: D): CompositeTransformResult<FirDeclaration> {
        return transformField(field, data)
    }

    final override fun visitEnumEntry(enumEntry: FirEnumEntry, data: D): CompositeTransformResult<FirDeclaration> {
        return transformEnumEntry(enumEntry, data)
    }

    final override fun <F : FirClassLikeDeclaration<F>> visitClassLikeDeclaration(classLikeDeclaration: FirClassLikeDeclaration<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformClassLikeDeclaration(classLikeDeclaration, data)
    }

    final override fun <F : FirClass<F>> visitClass(klass: FirClass<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformClass(klass, data)
    }

    final override fun visitRegularClass(regularClass: FirRegularClass, data: D): CompositeTransformResult<FirStatement> {
        return transformRegularClass(regularClass, data)
    }

    final override fun visitSealedClass(sealedClass: FirSealedClass, data: D): CompositeTransformResult<FirStatement> {
        return transformSealedClass(sealedClass, data)
    }

    final override fun visitTypeAlias(typeAlias: FirTypeAlias, data: D): CompositeTransformResult<FirDeclaration> {
        return transformTypeAlias(typeAlias, data)
    }

    final override fun <F : FirFunction<F>> visitFunction(function: FirFunction<F>, data: D): CompositeTransformResult<FirStatement> {
        return transformFunction(function, data)
    }

    final override fun visitContractDescriptionOwner(contractDescriptionOwner: FirContractDescriptionOwner, data: D): CompositeTransformResult<FirContractDescriptionOwner> {
        return transformContractDescriptionOwner(contractDescriptionOwner, data)
    }

    final override fun <F : FirMemberFunction<F>> visitMemberFunction(memberFunction: FirMemberFunction<F>, data: D): CompositeTransformResult<FirDeclaration> {
        return transformMemberFunction(memberFunction, data)
    }

    final override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: D): CompositeTransformResult<FirDeclaration> {
        return transformSimpleFunction(simpleFunction, data)
    }

    final override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: D): CompositeTransformResult<FirStatement> {
        return transformPropertyAccessor(propertyAccessor, data)
    }

    final override fun visitConstructor(constructor: FirConstructor, data: D): CompositeTransformResult<FirDeclaration> {
        return transformConstructor(constructor, data)
    }

    final override fun visitFile(file: FirFile, data: D): CompositeTransformResult<FirDeclaration> {
        return transformFile(file, data)
    }

    final override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: D): CompositeTransformResult<FirStatement> {
        return transformAnonymousFunction(anonymousFunction, data)
    }

    final override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: D): CompositeTransformResult<FirStatement> {
        return transformAnonymousObject(anonymousObject, data)
    }

    final override fun visitDiagnosticHolder(diagnosticHolder: FirDiagnosticHolder, data: D): CompositeTransformResult<FirDiagnosticHolder> {
        return transformDiagnosticHolder(diagnosticHolder, data)
    }

    final override fun visitLoop(loop: FirLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformLoop(loop, data)
    }

    final override fun visitErrorLoop(errorLoop: FirErrorLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformErrorLoop(errorLoop, data)
    }

    final override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformDoWhileLoop(doWhileLoop, data)
    }

    final override fun visitWhileLoop(whileLoop: FirWhileLoop, data: D): CompositeTransformResult<FirStatement> {
        return transformWhileLoop(whileLoop, data)
    }

    final override fun visitBlock(block: FirBlock, data: D): CompositeTransformResult<FirStatement> {
        return transformBlock(block, data)
    }

    final override fun visitBinaryLogicExpression(binaryLogicExpression: FirBinaryLogicExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformBinaryLogicExpression(binaryLogicExpression, data)
    }

    final override fun <E : FirTargetElement> visitJump(jump: FirJump<E>, data: D): CompositeTransformResult<FirStatement> {
        return transformJump(jump, data)
    }

    final override fun visitLoopJump(loopJump: FirLoopJump, data: D): CompositeTransformResult<FirStatement> {
        return transformLoopJump(loopJump, data)
    }

    final override fun visitBreakExpression(breakExpression: FirBreakExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformBreakExpression(breakExpression, data)
    }

    final override fun visitContinueExpression(continueExpression: FirContinueExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformContinueExpression(continueExpression, data)
    }

    final override fun visitCatch(catch: FirCatch, data: D): CompositeTransformResult<FirCatch> {
        return transformCatch(catch, data)
    }

    final override fun visitTryExpression(tryExpression: FirTryExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformTryExpression(tryExpression, data)
    }

    final override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: D): CompositeTransformResult<FirStatement> {
        return transformConstExpression(constExpression, data)
    }

    final override fun visitTypeProjection(typeProjection: FirTypeProjection, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformTypeProjection(typeProjection, data)
    }

    final override fun visitStarProjection(starProjection: FirStarProjection, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformStarProjection(starProjection, data)
    }

    final override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): CompositeTransformResult<FirTypeProjection> {
        return transformTypeProjectionWithVariance(typeProjectionWithVariance, data)
    }

    final override fun visitCall(call: FirCall, data: D): CompositeTransformResult<FirStatement> {
        return transformCall(call, data)
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): CompositeTransformResult<FirStatement> {
        return transformAnnotationCall(annotationCall, data)
    }

    final override fun visitOperatorCall(operatorCall: FirOperatorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformOperatorCall(operatorCall, data)
    }

    final override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformTypeOperatorCall(typeOperatorCall, data)
    }

    final override fun visitWhenExpression(whenExpression: FirWhenExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWhenExpression(whenExpression, data)
    }

    final override fun visitWhenBranch(whenBranch: FirWhenBranch, data: D): CompositeTransformResult<FirWhenBranch> {
        return transformWhenBranch(whenBranch, data)
    }

    final override fun visitQualifiedAccessWithoutCallee(qualifiedAccessWithoutCallee: FirQualifiedAccessWithoutCallee, data: D): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessWithoutCallee(qualifiedAccessWithoutCallee, data)
    }

    final override fun visitQualifiedAccess(qualifiedAccess: FirQualifiedAccess, data: D): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccess(qualifiedAccess, data)
    }

    final override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: D): CompositeTransformResult<FirStatement> {
        return transformCheckNotNullCall(checkNotNullCall, data)
    }

    final override fun visitArrayOfCall(arrayOfCall: FirArrayOfCall, data: D): CompositeTransformResult<FirStatement> {
        return transformArrayOfCall(arrayOfCall, data)
    }

    final override fun visitArraySetCall(arraySetCall: FirArraySetCall, data: D): CompositeTransformResult<FirStatement> {
        return transformArraySetCall(arraySetCall, data)
    }

    final override fun visitClassReferenceExpression(classReferenceExpression: FirClassReferenceExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformClassReferenceExpression(classReferenceExpression, data)
    }

    final override fun visitErrorExpression(errorExpression: FirErrorExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformErrorExpression(errorExpression, data)
    }

    final override fun visitErrorFunction(errorFunction: FirErrorFunction, data: D): CompositeTransformResult<FirStatement> {
        return transformErrorFunction(errorFunction, data)
    }

    final override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformQualifiedAccessExpression(qualifiedAccessExpression, data)
    }

    final override fun visitFunctionCall(functionCall: FirFunctionCall, data: D): CompositeTransformResult<FirStatement> {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): CompositeTransformResult<FirStatement> {
        return transformDelegatedConstructorCall(delegatedConstructorCall, data)
    }

    final override fun visitComponentCall(componentCall: FirComponentCall, data: D): CompositeTransformResult<FirStatement> {
        return transformComponentCall(componentCall, data)
    }

    final override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: D): CompositeTransformResult<FirStatement> {
        return transformCallableReferenceAccess(callableReferenceAccess, data)
    }

    final override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformThisReceiverExpression(thisReceiverExpression, data)
    }

    final override fun visitExpressionWithSmartcast(expressionWithSmartcast: FirExpressionWithSmartcast, data: D): CompositeTransformResult<FirStatement> {
        return transformExpressionWithSmartcast(expressionWithSmartcast, data)
    }

    final override fun visitGetClassCall(getClassCall: FirGetClassCall, data: D): CompositeTransformResult<FirStatement> {
        return transformGetClassCall(getClassCall, data)
    }

    final override fun visitWrappedExpression(wrappedExpression: FirWrappedExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedExpression(wrappedExpression, data)
    }

    final override fun visitWrappedArgumentExpression(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedArgumentExpression(wrappedArgumentExpression, data)
    }

    final override fun visitLambdaArgumentExpression(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformLambdaArgumentExpression(lambdaArgumentExpression, data)
    }

    final override fun visitSpreadArgumentExpression(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformSpreadArgumentExpression(spreadArgumentExpression, data)
    }

    final override fun visitNamedArgumentExpression(namedArgumentExpression: FirNamedArgumentExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformNamedArgumentExpression(namedArgumentExpression, data)
    }

    final override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: D): CompositeTransformResult<FirStatement> {
        return transformResolvedQualifier(resolvedQualifier, data)
    }

    final override fun visitResolvedReifiedParameterReference(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): CompositeTransformResult<FirStatement> {
        return transformResolvedReifiedParameterReference(resolvedReifiedParameterReference, data)
    }

    final override fun visitReturnExpression(returnExpression: FirReturnExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformReturnExpression(returnExpression, data)
    }

    final override fun visitStringConcatenationCall(stringConcatenationCall: FirStringConcatenationCall, data: D): CompositeTransformResult<FirStatement> {
        return transformStringConcatenationCall(stringConcatenationCall, data)
    }

    final override fun visitThrowExpression(throwExpression: FirThrowExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformThrowExpression(throwExpression, data)
    }

    final override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: D): CompositeTransformResult<FirStatement> {
        return transformVariableAssignment(variableAssignment, data)
    }

    final override fun visitWhenSubjectExpression(whenSubjectExpression: FirWhenSubjectExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWhenSubjectExpression(whenSubjectExpression, data)
    }

    final override fun visitWrappedDelegateExpression(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): CompositeTransformResult<FirStatement> {
        return transformWrappedDelegateExpression(wrappedDelegateExpression, data)
    }

    final override fun visitNamedReference(namedReference: FirNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformNamedReference(namedReference, data)
    }

    final override fun visitErrorNamedReference(errorNamedReference: FirErrorNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformErrorNamedReference(errorNamedReference, data)
    }

    final override fun visitSuperReference(superReference: FirSuperReference, data: D): CompositeTransformResult<FirReference> {
        return transformSuperReference(superReference, data)
    }

    final override fun visitThisReference(thisReference: FirThisReference, data: D): CompositeTransformResult<FirReference> {
        return transformThisReference(thisReference, data)
    }

    final override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference, data: D): CompositeTransformResult<FirReference> {
        return transformControlFlowGraphReference(controlFlowGraphReference, data)
    }

    final override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference, data: D): CompositeTransformResult<FirReference> {
        return transformResolvedNamedReference(resolvedNamedReference, data)
    }

    final override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference, data: D): CompositeTransformResult<FirReference> {
        return transformDelegateFieldReference(delegateFieldReference, data)
    }

    final override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference, data: D): CompositeTransformResult<FirReference> {
        return transformBackingFieldReference(backingFieldReference, data)
    }

    final override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: D): CompositeTransformResult<FirReference> {
        return transformResolvedCallableReference(resolvedCallableReference, data)
    }

    final override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformResolvedTypeRef(resolvedTypeRef, data)
    }

    final override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformErrorTypeRef(errorTypeRef, data)
    }

    final override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformDelegatedTypeRef(delegatedTypeRef, data)
    }

    final override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: D): CompositeTransformResult<FirTypeRef> {
        return transformTypeRefWithNullability(typeRefWithNullability, data)
    }

    final override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformUserTypeRef(userTypeRef, data)
    }

    final override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformDynamicTypeRef(dynamicTypeRef, data)
    }

    final override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformFunctionTypeRef(functionTypeRef, data)
    }

    final override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformResolvedFunctionTypeRef(resolvedFunctionTypeRef, data)
    }

    final override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): CompositeTransformResult<FirTypeRef> {
        return transformImplicitTypeRef(implicitTypeRef, data)
    }

    final override fun visitContractDescription(contractDescription: FirContractDescription, data: D): CompositeTransformResult<FirContractDescription> {
        return transformContractDescription(contractDescription, data)
    }

}
