/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")
package org.jetbrains.kotlin.fir.visitors

import org.jetbrains.kotlin.fir.visitors.FirElementKind.*
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
import org.jetbrains.kotlin.fir.expressions.FirStatementStub
import org.jetbrains.kotlin.fir.declarations.FirContextReceiver
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
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
import org.jetbrains.kotlin.fir.types.FirPlaceholderProjection
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
import org.jetbrains.kotlin.fir.expressions.FirContextReceiverArgumentListOwner
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
import org.jetbrains.kotlin.fir.expressions.FirIntegerLiteralOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirComponentCall
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
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
import org.jetbrains.kotlin.fir.types.FirIntersectionTypeRef
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.FirSmartCastedTypeRef
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirLegacyRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirRawContractDescription
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.transformAnnotations
import org.jetbrains.kotlin.fir.types.transformAnnotations
import org.jetbrains.kotlin.fir.expressions.transformCalleeReference
import org.jetbrains.kotlin.fir.declarations.transformControlFlowGraphReference
import org.jetbrains.kotlin.fir.expressions.transformAnnotations
import org.jetbrains.kotlin.fir.expressions.transformTypeRef
import org.jetbrains.kotlin.fir.declarations.transformTypeRef
import org.jetbrains.kotlin.fir.declarations.transformAnnotations
import org.jetbrains.kotlin.fir.declarations.transformTypeParameters
import org.jetbrains.kotlin.fir.declarations.transformStatus
import org.jetbrains.kotlin.fir.declarations.transformBody
import org.jetbrains.kotlin.fir.declarations.transformReturnTypeRef
import org.jetbrains.kotlin.fir.declarations.transformReceiverTypeRef
import org.jetbrains.kotlin.fir.declarations.transformContextReceivers
import org.jetbrains.kotlin.fir.declarations.transformBounds
import org.jetbrains.kotlin.fir.declarations.transformInitializer
import org.jetbrains.kotlin.fir.declarations.transformDelegate
import org.jetbrains.kotlin.fir.declarations.transformGetter
import org.jetbrains.kotlin.fir.declarations.transformSetter
import org.jetbrains.kotlin.fir.declarations.transformBackingField
import org.jetbrains.kotlin.fir.declarations.transformDefaultValue
import org.jetbrains.kotlin.fir.declarations.transformSuperTypeRefs
import org.jetbrains.kotlin.fir.declarations.transformDeclarations
import org.jetbrains.kotlin.fir.declarations.transformExpandedTypeRef
import org.jetbrains.kotlin.fir.declarations.transformValueParameters
import org.jetbrains.kotlin.fir.declarations.transformContractDescription
import org.jetbrains.kotlin.fir.declarations.transformDelegatedConstructor
import org.jetbrains.kotlin.fir.declarations.transformPackageDirective
import org.jetbrains.kotlin.fir.declarations.transformImports
import org.jetbrains.kotlin.fir.declarations.transformLabel
import org.jetbrains.kotlin.fir.expressions.transformAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.transformAnonymousObject
import org.jetbrains.kotlin.fir.expressions.transformBlock
import org.jetbrains.kotlin.fir.expressions.transformCondition
import org.jetbrains.kotlin.fir.expressions.transformLabel
import org.jetbrains.kotlin.fir.expressions.transformStatements
import org.jetbrains.kotlin.fir.expressions.transformLeftOperand
import org.jetbrains.kotlin.fir.expressions.transformRightOperand
import org.jetbrains.kotlin.fir.expressions.transformParameter
import org.jetbrains.kotlin.fir.expressions.transformTryBlock
import org.jetbrains.kotlin.fir.expressions.transformCatches
import org.jetbrains.kotlin.fir.expressions.transformFinallyBlock
import org.jetbrains.kotlin.fir.types.transformTypeRef
import org.jetbrains.kotlin.fir.expressions.transformArguments
import org.jetbrains.kotlin.fir.expressions.transformArgumentList
import org.jetbrains.kotlin.fir.expressions.transformAnnotationTypeRef
import org.jetbrains.kotlin.fir.expressions.transformArgumentMapping
import org.jetbrains.kotlin.fir.expressions.transformTypeArguments
import org.jetbrains.kotlin.fir.expressions.transformCompareToCall
import org.jetbrains.kotlin.fir.expressions.transformConversionTypeRef
import org.jetbrains.kotlin.fir.expressions.transformLeftArgument
import org.jetbrains.kotlin.fir.expressions.transformRightArgument
import org.jetbrains.kotlin.fir.expressions.transformSubject
import org.jetbrains.kotlin.fir.expressions.transformSubjectVariable
import org.jetbrains.kotlin.fir.expressions.transformBranches
import org.jetbrains.kotlin.fir.expressions.transformResult
import org.jetbrains.kotlin.fir.expressions.transformContextReceiverArguments
import org.jetbrains.kotlin.fir.expressions.transformExplicitReceiver
import org.jetbrains.kotlin.fir.expressions.transformDispatchReceiver
import org.jetbrains.kotlin.fir.expressions.transformExtensionReceiver
import org.jetbrains.kotlin.fir.expressions.transformLhs
import org.jetbrains.kotlin.fir.expressions.transformRhs
import org.jetbrains.kotlin.fir.expressions.transformLhsGetCall
import org.jetbrains.kotlin.fir.expressions.transformClassTypeRef
import org.jetbrains.kotlin.fir.expressions.transformExpression
import org.jetbrains.kotlin.fir.expressions.transformConstructedTypeRef
import org.jetbrains.kotlin.fir.expressions.transformReceiver
import org.jetbrains.kotlin.fir.expressions.transformSelector
import org.jetbrains.kotlin.fir.expressions.transformArgument
import org.jetbrains.kotlin.fir.expressions.transformVarargElementType
import org.jetbrains.kotlin.fir.expressions.transformException
import org.jetbrains.kotlin.fir.expressions.transformLValue
import org.jetbrains.kotlin.fir.expressions.transformLValueTypeRef
import org.jetbrains.kotlin.fir.expressions.transformRValue
import org.jetbrains.kotlin.fir.expressions.transformDelegateProvider
import org.jetbrains.kotlin.fir.references.transformSuperTypeRef
import org.jetbrains.kotlin.fir.types.transformDelegatedTypeRef
import org.jetbrains.kotlin.fir.types.transformReceiverTypeRef
import org.jetbrains.kotlin.fir.types.transformValueParameters
import org.jetbrains.kotlin.fir.types.transformReturnTypeRef
import org.jetbrains.kotlin.fir.types.transformContextReceiverTypeRefs
import org.jetbrains.kotlin.fir.types.transformLeftType
import org.jetbrains.kotlin.fir.types.transformRightType
import org.jetbrains.kotlin.fir.contracts.transformContractCall
import org.jetbrains.kotlin.fir.contracts.transformRawEffects
import org.jetbrains.kotlin.fir.contracts.transformEffects
import org.jetbrains.kotlin.fir.contracts.transformUnresolvedEffects

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

    open fun transformStatementStub(statementStub: FirStatementStub, data: D): FirStatement {
        return transformElement(statementStub, data)
    }

    open fun transformContextReceiver(contextReceiver: FirContextReceiver, data: D): FirContextReceiver {
        return transformElement(contextReceiver, data)
    }

    open fun transformDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformElement(declaration, data)
    }

    open fun transformTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): FirTypeParameterRefsOwner {
        return transformElement(typeParameterRefsOwner, data)
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

    open fun transformBackingField(backingField: FirBackingField, data: D): FirStatement {
        return transformElement(backingField, data)
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

    open fun transformPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): FirTypeProjection {
        return transformElement(placeholderProjection, data)
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

    open fun transformAnnotation(annotation: FirAnnotation, data: D): FirStatement {
        return transformElement(annotation, data)
    }

    open fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirStatement {
        return transformElement(annotationCall, data)
    }

    open fun transformAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): FirAnnotationArgumentMapping {
        return transformElement(annotationArgumentMapping, data)
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

    open fun transformContextReceiverArgumentListOwner(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: D): FirContextReceiverArgumentListOwner {
        return transformElement(contextReceiverArgumentListOwner, data)
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

    open fun transformIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirTypeRef {
        return transformElement(intersectionTypeRef, data)
    }

    open fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformElement(implicitTypeRef, data)
    }

    open fun transformSmartCastedTypeRef(smartCastedTypeRef: FirSmartCastedTypeRef, data: D): FirTypeRef {
        return transformElement(smartCastedTypeRef, data)
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

    final override fun visitStatementStub(statementStub: FirStatementStub, data: D): FirStatement {
        return transformStatementStub(statementStub, data)
    }

    final override fun visitContextReceiver(contextReceiver: FirContextReceiver, data: D): FirContextReceiver {
        return transformContextReceiver(contextReceiver, data)
    }

    final override fun visitDeclaration(declaration: FirDeclaration, data: D): FirDeclaration {
        return transformDeclaration(declaration, data)
    }

    final override fun visitTypeParameterRefsOwner(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): FirTypeParameterRefsOwner {
        return transformTypeParameterRefsOwner(typeParameterRefsOwner, data)
    }

    final override fun visitMemberDeclaration(memberDeclaration: FirMemberDeclaration, data: D): FirMemberDeclaration {
        return transformMemberDeclaration(memberDeclaration, data)
    }

    final override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: D): FirAnonymousInitializer {
        return transformAnonymousInitializer(anonymousInitializer, data)
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

    final override fun visitBackingField(backingField: FirBackingField, data: D): FirStatement {
        return transformBackingField(backingField, data)
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

    final override fun visitPlaceholderProjection(placeholderProjection: FirPlaceholderProjection, data: D): FirTypeProjection {
        return transformPlaceholderProjection(placeholderProjection, data)
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

    final override fun visitAnnotation(annotation: FirAnnotation, data: D): FirStatement {
        return transformAnnotation(annotation, data)
    }

    final override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: D): FirStatement {
        return transformAnnotationCall(annotationCall, data)
    }

    final override fun visitAnnotationArgumentMapping(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): FirAnnotationArgumentMapping {
        return transformAnnotationArgumentMapping(annotationArgumentMapping, data)
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

    final override fun visitContextReceiverArgumentListOwner(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: D): FirContextReceiverArgumentListOwner {
        return transformContextReceiverArgumentListOwner(contextReceiverArgumentListOwner, data)
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

    final override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression, data: D): FirStatement {
        return transformPropertyAccessExpression(propertyAccessExpression, data)
    }

    final override fun visitFunctionCall(functionCall: FirFunctionCall, data: D): FirStatement {
        return transformFunctionCall(functionCall, data)
    }

    final override fun visitIntegerLiteralOperatorCall(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): FirStatement {
        return transformIntegerLiteralOperatorCall(integerLiteralOperatorCall, data)
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

    final override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirTypeRef {
        return transformIntersectionTypeRef(intersectionTypeRef, data)
    }

    final override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        return transformImplicitTypeRef(implicitTypeRef, data)
    }

    final override fun visitSmartCastedTypeRef(smartCastedTypeRef: FirSmartCastedTypeRef, data: D): FirTypeRef {
        return transformSmartCastedTypeRef(smartCastedTypeRef, data)
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

    fun transformElementChildren(element: FirElement, data: D): FirElement {
        return element
    }

    fun transformAnnotationContainerChildren(annotationContainer: FirAnnotationContainer, data: D): FirAnnotationContainer {
        annotationContainer.transformAnnotations(this, data)
        return annotationContainer
    }

    fun transformTypeRefChildren(typeRef: FirTypeRef, data: D): FirTypeRef {
        typeRef.transformAnnotations(this, data)
        return typeRef
    }

    fun transformReferenceChildren(reference: FirReference, data: D): FirReference {
        return reference
    }

    fun transformLabelChildren(label: FirLabel, data: D): FirLabel {
        return label
    }

    fun transformResolvableChildren(resolvable: FirResolvable, data: D): FirResolvable {
        resolvable.transformCalleeReference(this, data)
        return resolvable
    }

    fun transformTargetElementChildren(targetElement: FirTargetElement, data: D): FirTargetElement {
        return targetElement
    }

    fun transformDeclarationStatusChildren(declarationStatus: FirDeclarationStatus, data: D): FirDeclarationStatus {
        return declarationStatus
    }

    fun transformResolvedDeclarationStatusChildren(resolvedDeclarationStatus: FirResolvedDeclarationStatus, data: D): FirDeclarationStatus {
        return resolvedDeclarationStatus
    }

    fun transformControlFlowGraphOwnerChildren(controlFlowGraphOwner: FirControlFlowGraphOwner, data: D): FirControlFlowGraphOwner {
        controlFlowGraphOwner.transformControlFlowGraphReference(this, data)
        return controlFlowGraphOwner
    }

    fun transformStatementChildren(statement: FirStatement, data: D): FirStatement {
        statement.transformAnnotations(this, data)
        return statement
    }

    fun transformExpressionChildren(expression: FirExpression, data: D): FirStatement {
        expression.transformTypeRef(this, data)
        expression.transformAnnotations(this, data)
        return expression
    }

    fun transformStatementStubChildren(statementStub: FirStatementStub, data: D): FirStatement {
        statementStub.transformAnnotations(this, data)
        return statementStub
    }

    fun transformContextReceiverChildren(contextReceiver: FirContextReceiver, data: D): FirContextReceiver {
        contextReceiver.transformTypeRef(this, data)
        return contextReceiver
    }

    fun transformDeclarationChildren(declaration: FirDeclaration, data: D): FirDeclaration {
        declaration.transformAnnotations(this, data)
        return declaration
    }

    fun transformTypeParameterRefsOwnerChildren(typeParameterRefsOwner: FirTypeParameterRefsOwner, data: D): FirTypeParameterRefsOwner {
        typeParameterRefsOwner.transformTypeParameters(this, data)
        return typeParameterRefsOwner
    }

    fun transformMemberDeclarationChildren(memberDeclaration: FirMemberDeclaration, data: D): FirMemberDeclaration {
        memberDeclaration.transformAnnotations(this, data)
        memberDeclaration.transformTypeParameters(this, data)
        memberDeclaration.transformStatus(this, data)
        return memberDeclaration
    }

    fun transformAnonymousInitializerChildren(anonymousInitializer: FirAnonymousInitializer, data: D): FirAnonymousInitializer {
        anonymousInitializer.transformAnnotations(this, data)
        anonymousInitializer.transformControlFlowGraphReference(this, data)
        anonymousInitializer.transformBody(this, data)
        return anonymousInitializer
    }

    fun transformCallableDeclarationChildren(callableDeclaration: FirCallableDeclaration, data: D): FirCallableDeclaration {
        callableDeclaration.transformAnnotations(this, data)
        callableDeclaration.transformTypeParameters(this, data)
        callableDeclaration.transformStatus(this, data)
        callableDeclaration.transformReturnTypeRef(this, data)
        callableDeclaration.transformReceiverTypeRef(this, data)
        callableDeclaration.transformContextReceivers(this, data)
        return callableDeclaration
    }

    fun transformTypeParameterRefChildren(typeParameterRef: FirTypeParameterRef, data: D): FirTypeParameterRef {
        return typeParameterRef
    }

    fun transformTypeParameterChildren(typeParameter: FirTypeParameter, data: D): FirTypeParameterRef {
        typeParameter.transformBounds(this, data)
        typeParameter.transformAnnotations(this, data)
        return typeParameter
    }

    fun transformVariableChildren(variable: FirVariable, data: D): FirStatement {
        variable.transformTypeParameters(this, data)
        variable.transformStatus(this, data)
        variable.transformReturnTypeRef(this, data)
        variable.transformReceiverTypeRef(this, data)
        variable.transformContextReceivers(this, data)
        variable.transformInitializer(this, data)
        variable.transformDelegate(this, data)
        variable.transformGetter(this, data)
        variable.transformSetter(this, data)
        variable.transformBackingField(this, data)
        variable.transformAnnotations(this, data)
        return variable
    }

    fun transformValueParameterChildren(valueParameter: FirValueParameter, data: D): FirStatement {
        valueParameter.transformTypeParameters(this, data)
        valueParameter.transformStatus(this, data)
        valueParameter.transformReturnTypeRef(this, data)
        valueParameter.transformReceiverTypeRef(this, data)
        valueParameter.transformContextReceivers(this, data)
        valueParameter.transformInitializer(this, data)
        valueParameter.transformDelegate(this, data)
        valueParameter.transformGetter(this, data)
        valueParameter.transformSetter(this, data)
        valueParameter.transformBackingField(this, data)
        valueParameter.transformAnnotations(this, data)
        valueParameter.transformControlFlowGraphReference(this, data)
        valueParameter.transformDefaultValue(this, data)
        return valueParameter
    }

    fun transformPropertyChildren(property: FirProperty, data: D): FirStatement {
        property.transformStatus(this, data)
        property.transformReturnTypeRef(this, data)
        property.transformReceiverTypeRef(this, data)
        property.transformContextReceivers(this, data)
        property.transformInitializer(this, data)
        property.transformDelegate(this, data)
        property.transformGetter(this, data)
        property.transformSetter(this, data)
        property.transformBackingField(this, data)
        property.transformAnnotations(this, data)
        property.transformControlFlowGraphReference(this, data)
        property.transformTypeParameters(this, data)
        return property
    }

    fun transformFieldChildren(field: FirField, data: D): FirStatement {
        field.transformTypeParameters(this, data)
        field.transformStatus(this, data)
        field.transformReturnTypeRef(this, data)
        field.transformReceiverTypeRef(this, data)
        field.transformContextReceivers(this, data)
        field.transformInitializer(this, data)
        field.transformDelegate(this, data)
        field.transformGetter(this, data)
        field.transformSetter(this, data)
        field.transformBackingField(this, data)
        field.transformAnnotations(this, data)
        field.transformControlFlowGraphReference(this, data)
        return field
    }

    fun transformEnumEntryChildren(enumEntry: FirEnumEntry, data: D): FirStatement {
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformStatus(this, data)
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformReceiverTypeRef(this, data)
        enumEntry.transformContextReceivers(this, data)
        enumEntry.transformInitializer(this, data)
        enumEntry.transformDelegate(this, data)
        enumEntry.transformGetter(this, data)
        enumEntry.transformSetter(this, data)
        enumEntry.transformBackingField(this, data)
        enumEntry.transformAnnotations(this, data)
        return enumEntry
    }

    fun transformClassLikeDeclarationChildren(classLikeDeclaration: FirClassLikeDeclaration, data: D): FirStatement {
        classLikeDeclaration.transformAnnotations(this, data)
        classLikeDeclaration.transformTypeParameters(this, data)
        classLikeDeclaration.transformStatus(this, data)
        return classLikeDeclaration
    }

    fun transformClassChildren(klass: FirClass, data: D): FirStatement {
        klass.transformTypeParameters(this, data)
        klass.transformStatus(this, data)
        klass.transformSuperTypeRefs(this, data)
        klass.transformDeclarations(this, data)
        klass.transformAnnotations(this, data)
        return klass
    }

    fun transformRegularClassChildren(regularClass: FirRegularClass, data: D): FirStatement {
        regularClass.transformTypeParameters(this, data)
        regularClass.transformStatus(this, data)
        regularClass.transformDeclarations(this, data)
        regularClass.transformAnnotations(this, data)
        regularClass.transformControlFlowGraphReference(this, data)
        regularClass.transformSuperTypeRefs(this, data)
        regularClass.transformContextReceivers(this, data)
        return regularClass
    }

    fun transformTypeAliasChildren(typeAlias: FirTypeAlias, data: D): FirStatement {
        typeAlias.transformStatus(this, data)
        typeAlias.transformTypeParameters(this, data)
        typeAlias.transformExpandedTypeRef(this, data)
        typeAlias.transformAnnotations(this, data)
        return typeAlias
    }

    fun transformFunctionChildren(function: FirFunction, data: D): FirStatement {
        function.transformAnnotations(this, data)
        function.transformTypeParameters(this, data)
        function.transformStatus(this, data)
        function.transformReturnTypeRef(this, data)
        function.transformReceiverTypeRef(this, data)
        function.transformContextReceivers(this, data)
        function.transformControlFlowGraphReference(this, data)
        function.transformValueParameters(this, data)
        function.transformBody(this, data)
        return function
    }

    fun transformContractDescriptionOwnerChildren(contractDescriptionOwner: FirContractDescriptionOwner, data: D): FirContractDescriptionOwner {
        contractDescriptionOwner.transformContractDescription(this, data)
        return contractDescriptionOwner
    }

    fun transformSimpleFunctionChildren(simpleFunction: FirSimpleFunction, data: D): FirStatement {
        simpleFunction.transformStatus(this, data)
        simpleFunction.transformReturnTypeRef(this, data)
        simpleFunction.transformReceiverTypeRef(this, data)
        simpleFunction.transformContextReceivers(this, data)
        simpleFunction.transformControlFlowGraphReference(this, data)
        simpleFunction.transformValueParameters(this, data)
        simpleFunction.transformBody(this, data)
        simpleFunction.transformContractDescription(this, data)
        simpleFunction.transformAnnotations(this, data)
        simpleFunction.transformTypeParameters(this, data)
        return simpleFunction
    }

    fun transformPropertyAccessorChildren(propertyAccessor: FirPropertyAccessor, data: D): FirStatement {
        propertyAccessor.transformStatus(this, data)
        propertyAccessor.transformReturnTypeRef(this, data)
        propertyAccessor.transformReceiverTypeRef(this, data)
        propertyAccessor.transformContextReceivers(this, data)
        propertyAccessor.transformControlFlowGraphReference(this, data)
        propertyAccessor.transformValueParameters(this, data)
        propertyAccessor.transformBody(this, data)
        propertyAccessor.transformContractDescription(this, data)
        propertyAccessor.transformAnnotations(this, data)
        propertyAccessor.transformTypeParameters(this, data)
        return propertyAccessor
    }

    fun transformBackingFieldChildren(backingField: FirBackingField, data: D): FirStatement {
        backingField.transformReturnTypeRef(this, data)
        backingField.transformReceiverTypeRef(this, data)
        backingField.transformContextReceivers(this, data)
        backingField.transformDelegate(this, data)
        backingField.transformGetter(this, data)
        backingField.transformSetter(this, data)
        backingField.transformBackingField(this, data)
        backingField.transformInitializer(this, data)
        backingField.transformAnnotations(this, data)
        backingField.transformTypeParameters(this, data)
        backingField.transformStatus(this, data)
        return backingField
    }

    fun transformConstructorChildren(constructor: FirConstructor, data: D): FirStatement {
        constructor.transformTypeParameters(this, data)
        constructor.transformStatus(this, data)
        constructor.transformReturnTypeRef(this, data)
        constructor.transformReceiverTypeRef(this, data)
        constructor.transformContextReceivers(this, data)
        constructor.transformControlFlowGraphReference(this, data)
        constructor.transformValueParameters(this, data)
        constructor.transformAnnotations(this, data)
        constructor.transformDelegatedConstructor(this, data)
        constructor.transformBody(this, data)
        return constructor
    }

    fun transformFileChildren(file: FirFile, data: D): FirFile {
        file.transformAnnotations(this, data)
        file.transformPackageDirective(this, data)
        file.transformImports(this, data)
        file.transformDeclarations(this, data)
        return file
    }

    fun transformPackageDirectiveChildren(packageDirective: FirPackageDirective, data: D): FirPackageDirective {
        return packageDirective
    }

    fun transformAnonymousFunctionChildren(anonymousFunction: FirAnonymousFunction, data: D): FirStatement {
        anonymousFunction.transformAnnotations(this, data)
        anonymousFunction.transformStatus(this, data)
        anonymousFunction.transformReturnTypeRef(this, data)
        anonymousFunction.transformReceiverTypeRef(this, data)
        anonymousFunction.transformContextReceivers(this, data)
        anonymousFunction.transformControlFlowGraphReference(this, data)
        anonymousFunction.transformValueParameters(this, data)
        anonymousFunction.transformBody(this, data)
        anonymousFunction.transformLabel(this, data)
        anonymousFunction.transformTypeParameters(this, data)
        anonymousFunction.transformTypeRef(this, data)
        return anonymousFunction
    }

    fun transformAnonymousFunctionExpressionChildren(anonymousFunctionExpression: FirAnonymousFunctionExpression, data: D): FirStatement {
        anonymousFunctionExpression.transformTypeRef(this, data)
        anonymousFunctionExpression.transformAnnotations(this, data)
        anonymousFunctionExpression.transformAnonymousFunction(this, data)
        return anonymousFunctionExpression
    }

    fun transformAnonymousObjectChildren(anonymousObject: FirAnonymousObject, data: D): FirStatement {
        anonymousObject.transformTypeParameters(this, data)
        anonymousObject.transformStatus(this, data)
        anonymousObject.transformSuperTypeRefs(this, data)
        anonymousObject.transformDeclarations(this, data)
        anonymousObject.transformAnnotations(this, data)
        anonymousObject.transformControlFlowGraphReference(this, data)
        return anonymousObject
    }

    fun transformAnonymousObjectExpressionChildren(anonymousObjectExpression: FirAnonymousObjectExpression, data: D): FirStatement {
        anonymousObjectExpression.transformTypeRef(this, data)
        anonymousObjectExpression.transformAnnotations(this, data)
        anonymousObjectExpression.transformAnonymousObject(this, data)
        return anonymousObjectExpression
    }

    fun transformDiagnosticHolderChildren(diagnosticHolder: FirDiagnosticHolder, data: D): FirDiagnosticHolder {
        return diagnosticHolder
    }

    fun transformImportChildren(import: FirImport, data: D): FirImport {
        return import
    }

    fun transformResolvedImportChildren(resolvedImport: FirResolvedImport, data: D): FirImport {
        resolvedImport.transformDelegate(this, data)
        return resolvedImport
    }

    fun transformErrorImportChildren(errorImport: FirErrorImport, data: D): FirImport {
        errorImport.transformDelegate(this, data)
        return errorImport
    }

    fun transformLoopChildren(loop: FirLoop, data: D): FirStatement {
        loop.transformAnnotations(this, data)
        loop.transformBlock(this, data)
        loop.transformCondition(this, data)
        loop.transformLabel(this, data)
        return loop
    }

    fun transformErrorLoopChildren(errorLoop: FirErrorLoop, data: D): FirStatement {
        errorLoop.transformAnnotations(this, data)
        errorLoop.transformBlock(this, data)
        errorLoop.transformCondition(this, data)
        errorLoop.transformLabel(this, data)
        return errorLoop
    }

    fun transformDoWhileLoopChildren(doWhileLoop: FirDoWhileLoop, data: D): FirStatement {
        doWhileLoop.transformAnnotations(this, data)
        doWhileLoop.transformBlock(this, data)
        doWhileLoop.transformCondition(this, data)
        doWhileLoop.transformLabel(this, data)
        return doWhileLoop
    }

    fun transformWhileLoopChildren(whileLoop: FirWhileLoop, data: D): FirStatement {
        whileLoop.transformAnnotations(this, data)
        whileLoop.transformLabel(this, data)
        whileLoop.transformCondition(this, data)
        whileLoop.transformBlock(this, data)
        return whileLoop
    }

    fun transformBlockChildren(block: FirBlock, data: D): FirStatement {
        block.transformAnnotations(this, data)
        block.transformStatements(this, data)
        block.transformTypeRef(this, data)
        return block
    }

    fun transformBinaryLogicExpressionChildren(binaryLogicExpression: FirBinaryLogicExpression, data: D): FirStatement {
        binaryLogicExpression.transformTypeRef(this, data)
        binaryLogicExpression.transformAnnotations(this, data)
        binaryLogicExpression.transformLeftOperand(this, data)
        binaryLogicExpression.transformRightOperand(this, data)
        return binaryLogicExpression
    }

    fun <E : FirTargetElement> transformJumpChildren(jump: FirJump<E>, data: D): FirStatement {
        jump.transformTypeRef(this, data)
        jump.transformAnnotations(this, data)
        return jump
    }

    fun transformLoopJumpChildren(loopJump: FirLoopJump, data: D): FirStatement {
        loopJump.transformTypeRef(this, data)
        loopJump.transformAnnotations(this, data)
        return loopJump
    }

    fun transformBreakExpressionChildren(breakExpression: FirBreakExpression, data: D): FirStatement {
        breakExpression.transformTypeRef(this, data)
        breakExpression.transformAnnotations(this, data)
        return breakExpression
    }

    fun transformContinueExpressionChildren(continueExpression: FirContinueExpression, data: D): FirStatement {
        continueExpression.transformTypeRef(this, data)
        continueExpression.transformAnnotations(this, data)
        return continueExpression
    }

    fun transformCatchChildren(catch: FirCatch, data: D): FirCatch {
        catch.transformParameter(this, data)
        catch.transformBlock(this, data)
        return catch
    }

    fun transformTryExpressionChildren(tryExpression: FirTryExpression, data: D): FirStatement {
        tryExpression.transformTypeRef(this, data)
        tryExpression.transformAnnotations(this, data)
        tryExpression.transformCalleeReference(this, data)
        tryExpression.transformTryBlock(this, data)
        tryExpression.transformCatches(this, data)
        tryExpression.transformFinallyBlock(this, data)
        return tryExpression
    }

    fun <T> transformConstExpressionChildren(constExpression: FirConstExpression<T>, data: D): FirStatement {
        constExpression.transformTypeRef(this, data)
        constExpression.transformAnnotations(this, data)
        return constExpression
    }

    fun transformTypeProjectionChildren(typeProjection: FirTypeProjection, data: D): FirTypeProjection {
        return typeProjection
    }

    fun transformStarProjectionChildren(starProjection: FirStarProjection, data: D): FirTypeProjection {
        return starProjection
    }

    fun transformPlaceholderProjectionChildren(placeholderProjection: FirPlaceholderProjection, data: D): FirTypeProjection {
        return placeholderProjection
    }

    fun transformTypeProjectionWithVarianceChildren(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: D): FirTypeProjection {
        typeProjectionWithVariance.transformTypeRef(this, data)
        return typeProjectionWithVariance
    }

    fun transformArgumentListChildren(argumentList: FirArgumentList, data: D): FirArgumentList {
        argumentList.transformArguments(this, data)
        return argumentList
    }

    fun transformCallChildren(call: FirCall, data: D): FirStatement {
        call.transformAnnotations(this, data)
        call.transformArgumentList(this, data)
        return call
    }

    fun transformAnnotationChildren(annotation: FirAnnotation, data: D): FirStatement {
        annotation.transformTypeRef(this, data)
        annotation.transformAnnotations(this, data)
        annotation.transformAnnotationTypeRef(this, data)
        annotation.transformArgumentMapping(this, data)
        annotation.transformTypeArguments(this, data)
        return annotation
    }

    fun transformAnnotationCallChildren(annotationCall: FirAnnotationCall, data: D): FirStatement {
        annotationCall.transformTypeRef(this, data)
        annotationCall.transformAnnotations(this, data)
        annotationCall.transformAnnotationTypeRef(this, data)
        annotationCall.transformTypeArguments(this, data)
        annotationCall.transformArgumentList(this, data)
        annotationCall.transformCalleeReference(this, data)
        annotationCall.transformArgumentMapping(this, data)
        return annotationCall
    }

    fun transformAnnotationArgumentMappingChildren(annotationArgumentMapping: FirAnnotationArgumentMapping, data: D): FirAnnotationArgumentMapping {
        return annotationArgumentMapping
    }

    fun transformComparisonExpressionChildren(comparisonExpression: FirComparisonExpression, data: D): FirStatement {
        comparisonExpression.transformTypeRef(this, data)
        comparisonExpression.transformAnnotations(this, data)
        comparisonExpression.transformCompareToCall(this, data)
        return comparisonExpression
    }

    fun transformTypeOperatorCallChildren(typeOperatorCall: FirTypeOperatorCall, data: D): FirStatement {
        typeOperatorCall.transformTypeRef(this, data)
        typeOperatorCall.transformAnnotations(this, data)
        typeOperatorCall.transformArgumentList(this, data)
        typeOperatorCall.transformConversionTypeRef(this, data)
        return typeOperatorCall
    }

    fun transformAssignmentOperatorStatementChildren(assignmentOperatorStatement: FirAssignmentOperatorStatement, data: D): FirStatement {
        assignmentOperatorStatement.transformAnnotations(this, data)
        assignmentOperatorStatement.transformLeftArgument(this, data)
        assignmentOperatorStatement.transformRightArgument(this, data)
        return assignmentOperatorStatement
    }

    fun transformEqualityOperatorCallChildren(equalityOperatorCall: FirEqualityOperatorCall, data: D): FirStatement {
        equalityOperatorCall.transformTypeRef(this, data)
        equalityOperatorCall.transformAnnotations(this, data)
        equalityOperatorCall.transformArgumentList(this, data)
        return equalityOperatorCall
    }

    fun transformWhenExpressionChildren(whenExpression: FirWhenExpression, data: D): FirStatement {
        whenExpression.transformTypeRef(this, data)
        whenExpression.transformAnnotations(this, data)
        whenExpression.transformCalleeReference(this, data)
        whenExpression.transformSubject(this, data)
        whenExpression.transformBranches(this, data)
        return whenExpression
    }

    fun transformWhenBranchChildren(whenBranch: FirWhenBranch, data: D): FirWhenBranch {
        whenBranch.transformCondition(this, data)
        whenBranch.transformResult(this, data)
        return whenBranch
    }

    fun transformContextReceiverArgumentListOwnerChildren(contextReceiverArgumentListOwner: FirContextReceiverArgumentListOwner, data: D): FirContextReceiverArgumentListOwner {
        contextReceiverArgumentListOwner.transformContextReceiverArguments(this, data)
        return contextReceiverArgumentListOwner
    }

    fun transformQualifiedAccessChildren(qualifiedAccess: FirQualifiedAccess, data: D): FirStatement {
        qualifiedAccess.transformCalleeReference(this, data)
        qualifiedAccess.transformAnnotations(this, data)
        qualifiedAccess.transformContextReceiverArguments(this, data)
        qualifiedAccess.transformTypeArguments(this, data)
        qualifiedAccess.transformExplicitReceiver(this, data)
        if (qualifiedAccess.dispatchReceiver !== qualifiedAccess.explicitReceiver) {
              qualifiedAccess.transformDispatchReceiver(this, data)
        }
        if (qualifiedAccess.extensionReceiver !== qualifiedAccess.explicitReceiver && qualifiedAccess.extensionReceiver !== qualifiedAccess.dispatchReceiver) {
            qualifiedAccess.transformExtensionReceiver(this, data)
        }
        return qualifiedAccess
    }

    fun transformCheckNotNullCallChildren(checkNotNullCall: FirCheckNotNullCall, data: D): FirStatement {
        checkNotNullCall.transformTypeRef(this, data)
        checkNotNullCall.transformAnnotations(this, data)
        checkNotNullCall.transformArgumentList(this, data)
        checkNotNullCall.transformCalleeReference(this, data)
        return checkNotNullCall
    }

    fun transformElvisExpressionChildren(elvisExpression: FirElvisExpression, data: D): FirStatement {
        elvisExpression.transformTypeRef(this, data)
        elvisExpression.transformAnnotations(this, data)
        elvisExpression.transformCalleeReference(this, data)
        elvisExpression.transformLhs(this, data)
        elvisExpression.transformRhs(this, data)
        return elvisExpression
    }

    fun transformArrayOfCallChildren(arrayOfCall: FirArrayOfCall, data: D): FirStatement {
        arrayOfCall.transformTypeRef(this, data)
        arrayOfCall.transformAnnotations(this, data)
        arrayOfCall.transformArgumentList(this, data)
        return arrayOfCall
    }

    fun transformAugmentedArraySetCallChildren(augmentedArraySetCall: FirAugmentedArraySetCall, data: D): FirStatement {
        augmentedArraySetCall.transformAnnotations(this, data)
        augmentedArraySetCall.transformLhsGetCall(this, data)
        augmentedArraySetCall.transformRhs(this, data)
        augmentedArraySetCall.transformCalleeReference(this, data)
        return augmentedArraySetCall
    }

    fun transformClassReferenceExpressionChildren(classReferenceExpression: FirClassReferenceExpression, data: D): FirStatement {
        classReferenceExpression.transformTypeRef(this, data)
        classReferenceExpression.transformAnnotations(this, data)
        classReferenceExpression.transformClassTypeRef(this, data)
        return classReferenceExpression
    }

    fun transformErrorExpressionChildren(errorExpression: FirErrorExpression, data: D): FirStatement {
        errorExpression.transformTypeRef(this, data)
        errorExpression.transformAnnotations(this, data)
        errorExpression.transformExpression(this, data)
        return errorExpression
    }

    fun transformErrorFunctionChildren(errorFunction: FirErrorFunction, data: D): FirStatement {
        errorFunction.transformAnnotations(this, data)
        errorFunction.transformStatus(this, data)
        errorFunction.transformReturnTypeRef(this, data)
        errorFunction.transformReceiverTypeRef(this, data)
        errorFunction.transformContextReceivers(this, data)
        errorFunction.transformControlFlowGraphReference(this, data)
        errorFunction.transformValueParameters(this, data)
        errorFunction.transformBody(this, data)
        errorFunction.transformTypeParameters(this, data)
        return errorFunction
    }

    fun transformErrorPropertyChildren(errorProperty: FirErrorProperty, data: D): FirStatement {
        errorProperty.transformTypeParameters(this, data)
        errorProperty.transformStatus(this, data)
        errorProperty.transformReturnTypeRef(this, data)
        errorProperty.transformReceiverTypeRef(this, data)
        errorProperty.transformContextReceivers(this, data)
        errorProperty.transformInitializer(this, data)
        errorProperty.transformDelegate(this, data)
        errorProperty.transformGetter(this, data)
        errorProperty.transformSetter(this, data)
        errorProperty.transformBackingField(this, data)
        errorProperty.transformAnnotations(this, data)
        return errorProperty
    }

    fun transformQualifiedAccessExpressionChildren(qualifiedAccessExpression: FirQualifiedAccessExpression, data: D): FirStatement {
        qualifiedAccessExpression.transformTypeRef(this, data)
        qualifiedAccessExpression.transformAnnotations(this, data)
        qualifiedAccessExpression.transformCalleeReference(this, data)
        qualifiedAccessExpression.transformContextReceiverArguments(this, data)
        qualifiedAccessExpression.transformTypeArguments(this, data)
        qualifiedAccessExpression.transformExplicitReceiver(this, data)
        if (qualifiedAccessExpression.dispatchReceiver !== qualifiedAccessExpression.explicitReceiver) {
              qualifiedAccessExpression.transformDispatchReceiver(this, data)
        }
        if (qualifiedAccessExpression.extensionReceiver !== qualifiedAccessExpression.explicitReceiver && qualifiedAccessExpression.extensionReceiver !== qualifiedAccessExpression.dispatchReceiver) {
            qualifiedAccessExpression.transformExtensionReceiver(this, data)
        }
        return qualifiedAccessExpression
    }

    fun transformPropertyAccessExpressionChildren(propertyAccessExpression: FirPropertyAccessExpression, data: D): FirStatement {
        propertyAccessExpression.transformTypeRef(this, data)
        propertyAccessExpression.transformAnnotations(this, data)
        propertyAccessExpression.transformCalleeReference(this, data)
        propertyAccessExpression.transformContextReceiverArguments(this, data)
        propertyAccessExpression.transformTypeArguments(this, data)
        propertyAccessExpression.transformExplicitReceiver(this, data)
        if (propertyAccessExpression.dispatchReceiver !== propertyAccessExpression.explicitReceiver) {
              propertyAccessExpression.transformDispatchReceiver(this, data)
        }
        if (propertyAccessExpression.extensionReceiver !== propertyAccessExpression.explicitReceiver && propertyAccessExpression.extensionReceiver !== propertyAccessExpression.dispatchReceiver) {
            propertyAccessExpression.transformExtensionReceiver(this, data)
        }
        return propertyAccessExpression
    }

    fun transformFunctionCallChildren(functionCall: FirFunctionCall, data: D): FirStatement {
        functionCall.transformTypeRef(this, data)
        functionCall.transformAnnotations(this, data)
        functionCall.transformContextReceiverArguments(this, data)
        functionCall.transformTypeArguments(this, data)
        functionCall.transformExplicitReceiver(this, data)
        if (functionCall.dispatchReceiver !== functionCall.explicitReceiver) {
              functionCall.transformDispatchReceiver(this, data)
        }
        if (functionCall.extensionReceiver !== functionCall.explicitReceiver && functionCall.extensionReceiver !== functionCall.dispatchReceiver) {
            functionCall.transformExtensionReceiver(this, data)
        }
        functionCall.transformArgumentList(this, data)
        functionCall.transformCalleeReference(this, data)
        return functionCall
    }

    fun transformIntegerLiteralOperatorCallChildren(integerLiteralOperatorCall: FirIntegerLiteralOperatorCall, data: D): FirStatement {
        integerLiteralOperatorCall.transformTypeRef(this, data)
        integerLiteralOperatorCall.transformAnnotations(this, data)
        integerLiteralOperatorCall.transformContextReceiverArguments(this, data)
        integerLiteralOperatorCall.transformTypeArguments(this, data)
        integerLiteralOperatorCall.transformExplicitReceiver(this, data)
        if (integerLiteralOperatorCall.dispatchReceiver !== integerLiteralOperatorCall.explicitReceiver) {
              integerLiteralOperatorCall.transformDispatchReceiver(this, data)
        }
        if (integerLiteralOperatorCall.extensionReceiver !== integerLiteralOperatorCall.explicitReceiver && integerLiteralOperatorCall.extensionReceiver !== integerLiteralOperatorCall.dispatchReceiver) {
            integerLiteralOperatorCall.transformExtensionReceiver(this, data)
        }
        integerLiteralOperatorCall.transformArgumentList(this, data)
        integerLiteralOperatorCall.transformCalleeReference(this, data)
        return integerLiteralOperatorCall
    }

    fun transformImplicitInvokeCallChildren(implicitInvokeCall: FirImplicitInvokeCall, data: D): FirStatement {
        implicitInvokeCall.transformTypeRef(this, data)
        implicitInvokeCall.transformAnnotations(this, data)
        implicitInvokeCall.transformContextReceiverArguments(this, data)
        implicitInvokeCall.transformTypeArguments(this, data)
        implicitInvokeCall.transformExplicitReceiver(this, data)
        if (implicitInvokeCall.dispatchReceiver !== implicitInvokeCall.explicitReceiver) {
              implicitInvokeCall.transformDispatchReceiver(this, data)
        }
        if (implicitInvokeCall.extensionReceiver !== implicitInvokeCall.explicitReceiver && implicitInvokeCall.extensionReceiver !== implicitInvokeCall.dispatchReceiver) {
            implicitInvokeCall.transformExtensionReceiver(this, data)
        }
        implicitInvokeCall.transformArgumentList(this, data)
        implicitInvokeCall.transformCalleeReference(this, data)
        return implicitInvokeCall
    }

    fun transformDelegatedConstructorCallChildren(delegatedConstructorCall: FirDelegatedConstructorCall, data: D): FirStatement {
        delegatedConstructorCall.transformAnnotations(this, data)
        delegatedConstructorCall.transformArgumentList(this, data)
        delegatedConstructorCall.transformContextReceiverArguments(this, data)
        delegatedConstructorCall.transformConstructedTypeRef(this, data)
        delegatedConstructorCall.transformCalleeReference(this, data)
        return delegatedConstructorCall
    }

    fun transformComponentCallChildren(componentCall: FirComponentCall, data: D): FirStatement {
        componentCall.transformTypeRef(this, data)
        componentCall.transformAnnotations(this, data)
        componentCall.transformContextReceiverArguments(this, data)
        componentCall.transformTypeArguments(this, data)
        componentCall.transformArgumentList(this, data)
        componentCall.transformCalleeReference(this, data)
        componentCall.transformExplicitReceiver(this, data)
        if (componentCall.dispatchReceiver !== componentCall.explicitReceiver) {
              componentCall.transformDispatchReceiver(this, data)
        }
        if (componentCall.extensionReceiver !== componentCall.explicitReceiver && componentCall.extensionReceiver !== componentCall.dispatchReceiver) {
            componentCall.transformExtensionReceiver(this, data)
        }
        return componentCall
    }

    fun transformCallableReferenceAccessChildren(callableReferenceAccess: FirCallableReferenceAccess, data: D): FirStatement {
        callableReferenceAccess.transformTypeRef(this, data)
        callableReferenceAccess.transformAnnotations(this, data)
        callableReferenceAccess.transformContextReceiverArguments(this, data)
        callableReferenceAccess.transformTypeArguments(this, data)
        callableReferenceAccess.transformExplicitReceiver(this, data)
        if (callableReferenceAccess.dispatchReceiver !== callableReferenceAccess.explicitReceiver) {
              callableReferenceAccess.transformDispatchReceiver(this, data)
        }
        if (callableReferenceAccess.extensionReceiver !== callableReferenceAccess.explicitReceiver && callableReferenceAccess.extensionReceiver !== callableReferenceAccess.dispatchReceiver) {
            callableReferenceAccess.transformExtensionReceiver(this, data)
        }
        callableReferenceAccess.transformCalleeReference(this, data)
        return callableReferenceAccess
    }

    fun transformThisReceiverExpressionChildren(thisReceiverExpression: FirThisReceiverExpression, data: D): FirStatement {
        thisReceiverExpression.transformTypeRef(this, data)
        thisReceiverExpression.transformAnnotations(this, data)
        thisReceiverExpression.transformContextReceiverArguments(this, data)
        thisReceiverExpression.transformTypeArguments(this, data)
        thisReceiverExpression.transformExplicitReceiver(this, data)
        if (thisReceiverExpression.dispatchReceiver !== thisReceiverExpression.explicitReceiver) {
              thisReceiverExpression.transformDispatchReceiver(this, data)
        }
        if (thisReceiverExpression.extensionReceiver !== thisReceiverExpression.explicitReceiver && thisReceiverExpression.extensionReceiver !== thisReceiverExpression.dispatchReceiver) {
            thisReceiverExpression.transformExtensionReceiver(this, data)
        }
        thisReceiverExpression.transformCalleeReference(this, data)
        return thisReceiverExpression
    }

    fun transformSafeCallExpressionChildren(safeCallExpression: FirSafeCallExpression, data: D): FirStatement {
        safeCallExpression.transformTypeRef(this, data)
        safeCallExpression.transformAnnotations(this, data)
        safeCallExpression.transformReceiver(this, data)
        safeCallExpression.transformSelector(this, data)
        return safeCallExpression
    }

    fun transformCheckedSafeCallSubjectChildren(checkedSafeCallSubject: FirCheckedSafeCallSubject, data: D): FirStatement {
        checkedSafeCallSubject.transformTypeRef(this, data)
        checkedSafeCallSubject.transformAnnotations(this, data)
        return checkedSafeCallSubject
    }

    fun transformGetClassCallChildren(getClassCall: FirGetClassCall, data: D): FirStatement {
        getClassCall.transformTypeRef(this, data)
        getClassCall.transformAnnotations(this, data)
        getClassCall.transformArgumentList(this, data)
        return getClassCall
    }

    fun transformWrappedExpressionChildren(wrappedExpression: FirWrappedExpression, data: D): FirStatement {
        wrappedExpression.transformTypeRef(this, data)
        wrappedExpression.transformAnnotations(this, data)
        wrappedExpression.transformExpression(this, data)
        return wrappedExpression
    }

    fun transformWrappedArgumentExpressionChildren(wrappedArgumentExpression: FirWrappedArgumentExpression, data: D): FirStatement {
        wrappedArgumentExpression.transformTypeRef(this, data)
        wrappedArgumentExpression.transformAnnotations(this, data)
        wrappedArgumentExpression.transformExpression(this, data)
        return wrappedArgumentExpression
    }

    fun transformLambdaArgumentExpressionChildren(lambdaArgumentExpression: FirLambdaArgumentExpression, data: D): FirStatement {
        lambdaArgumentExpression.transformTypeRef(this, data)
        lambdaArgumentExpression.transformAnnotations(this, data)
        lambdaArgumentExpression.transformExpression(this, data)
        return lambdaArgumentExpression
    }

    fun transformSpreadArgumentExpressionChildren(spreadArgumentExpression: FirSpreadArgumentExpression, data: D): FirStatement {
        spreadArgumentExpression.transformTypeRef(this, data)
        spreadArgumentExpression.transformAnnotations(this, data)
        spreadArgumentExpression.transformExpression(this, data)
        return spreadArgumentExpression
    }

    fun transformNamedArgumentExpressionChildren(namedArgumentExpression: FirNamedArgumentExpression, data: D): FirStatement {
        namedArgumentExpression.transformTypeRef(this, data)
        namedArgumentExpression.transformAnnotations(this, data)
        namedArgumentExpression.transformExpression(this, data)
        return namedArgumentExpression
    }

    fun transformVarargArgumentsExpressionChildren(varargArgumentsExpression: FirVarargArgumentsExpression, data: D): FirStatement {
        varargArgumentsExpression.transformTypeRef(this, data)
        varargArgumentsExpression.transformAnnotations(this, data)
        varargArgumentsExpression.transformArguments(this, data)
        varargArgumentsExpression.transformVarargElementType(this, data)
        return varargArgumentsExpression
    }

    fun transformResolvedQualifierChildren(resolvedQualifier: FirResolvedQualifier, data: D): FirStatement {
        resolvedQualifier.transformTypeRef(this, data)
        resolvedQualifier.transformAnnotations(this, data)
        resolvedQualifier.transformTypeArguments(this, data)
        return resolvedQualifier
    }

    fun transformErrorResolvedQualifierChildren(errorResolvedQualifier: FirErrorResolvedQualifier, data: D): FirStatement {
        errorResolvedQualifier.transformTypeRef(this, data)
        errorResolvedQualifier.transformAnnotations(this, data)
        errorResolvedQualifier.transformTypeArguments(this, data)
        return errorResolvedQualifier
    }

    fun transformResolvedReifiedParameterReferenceChildren(resolvedReifiedParameterReference: FirResolvedReifiedParameterReference, data: D): FirStatement {
        resolvedReifiedParameterReference.transformTypeRef(this, data)
        resolvedReifiedParameterReference.transformAnnotations(this, data)
        return resolvedReifiedParameterReference
    }

    fun transformReturnExpressionChildren(returnExpression: FirReturnExpression, data: D): FirStatement {
        returnExpression.transformTypeRef(this, data)
        returnExpression.transformAnnotations(this, data)
        returnExpression.transformResult(this, data)
        return returnExpression
    }

    fun transformStringConcatenationCallChildren(stringConcatenationCall: FirStringConcatenationCall, data: D): FirStatement {
        stringConcatenationCall.transformAnnotations(this, data)
        stringConcatenationCall.transformArgumentList(this, data)
        stringConcatenationCall.transformTypeRef(this, data)
        return stringConcatenationCall
    }

    fun transformThrowExpressionChildren(throwExpression: FirThrowExpression, data: D): FirStatement {
        throwExpression.transformTypeRef(this, data)
        throwExpression.transformAnnotations(this, data)
        throwExpression.transformException(this, data)
        return throwExpression
    }

    fun transformVariableAssignmentChildren(variableAssignment: FirVariableAssignment, data: D): FirStatement {
        variableAssignment.transformCalleeReference(this, data)
        variableAssignment.transformAnnotations(this, data)
        variableAssignment.transformContextReceiverArguments(this, data)
        variableAssignment.transformTypeArguments(this, data)
        variableAssignment.transformExplicitReceiver(this, data)
        if (variableAssignment.dispatchReceiver !== variableAssignment.explicitReceiver) {
              variableAssignment.transformDispatchReceiver(this, data)
        }
        if (variableAssignment.extensionReceiver !== variableAssignment.explicitReceiver && variableAssignment.extensionReceiver !== variableAssignment.dispatchReceiver) {
            variableAssignment.transformExtensionReceiver(this, data)
        }
        variableAssignment.transformLValueTypeRef(this, data)
        variableAssignment.transformRValue(this, data)
        return variableAssignment
    }

    fun transformWhenSubjectExpressionChildren(whenSubjectExpression: FirWhenSubjectExpression, data: D): FirStatement {
        whenSubjectExpression.transformTypeRef(this, data)
        whenSubjectExpression.transformAnnotations(this, data)
        return whenSubjectExpression
    }

    fun transformWrappedDelegateExpressionChildren(wrappedDelegateExpression: FirWrappedDelegateExpression, data: D): FirStatement {
        wrappedDelegateExpression.transformTypeRef(this, data)
        wrappedDelegateExpression.transformAnnotations(this, data)
        wrappedDelegateExpression.transformExpression(this, data)
        wrappedDelegateExpression.transformDelegateProvider(this, data)
        return wrappedDelegateExpression
    }

    fun transformNamedReferenceChildren(namedReference: FirNamedReference, data: D): FirReference {
        return namedReference
    }

    fun transformErrorNamedReferenceChildren(errorNamedReference: FirErrorNamedReference, data: D): FirReference {
        return errorNamedReference
    }

    fun transformSuperReferenceChildren(superReference: FirSuperReference, data: D): FirReference {
        superReference.transformSuperTypeRef(this, data)
        return superReference
    }

    fun transformThisReferenceChildren(thisReference: FirThisReference, data: D): FirReference {
        return thisReference
    }

    fun transformControlFlowGraphReferenceChildren(controlFlowGraphReference: FirControlFlowGraphReference, data: D): FirReference {
        return controlFlowGraphReference
    }

    fun transformResolvedNamedReferenceChildren(resolvedNamedReference: FirResolvedNamedReference, data: D): FirReference {
        return resolvedNamedReference
    }

    fun transformDelegateFieldReferenceChildren(delegateFieldReference: FirDelegateFieldReference, data: D): FirReference {
        return delegateFieldReference
    }

    fun transformBackingFieldReferenceChildren(backingFieldReference: FirBackingFieldReference, data: D): FirReference {
        return backingFieldReference
    }

    fun transformResolvedCallableReferenceChildren(resolvedCallableReference: FirResolvedCallableReference, data: D): FirReference {
        return resolvedCallableReference
    }

    fun transformResolvedTypeRefChildren(resolvedTypeRef: FirResolvedTypeRef, data: D): FirTypeRef {
        resolvedTypeRef.transformAnnotations(this, data)
        resolvedTypeRef.transformDelegatedTypeRef(this, data)
        return resolvedTypeRef
    }

    fun transformErrorTypeRefChildren(errorTypeRef: FirErrorTypeRef, data: D): FirTypeRef {
        errorTypeRef.transformAnnotations(this, data)
        errorTypeRef.transformDelegatedTypeRef(this, data)
        return errorTypeRef
    }

    fun transformTypeRefWithNullabilityChildren(typeRefWithNullability: FirTypeRefWithNullability, data: D): FirTypeRef {
        typeRefWithNullability.transformAnnotations(this, data)
        return typeRefWithNullability
    }

    fun transformUserTypeRefChildren(userTypeRef: FirUserTypeRef, data: D): FirTypeRef {
        for (part in userTypeRef.qualifier) {
    (part.typeArgumentList.typeArguments as MutableList<FirTypeProjection>).transformInplace(this, data)
}
        userTypeRef.transformAnnotations(this, data)
        return userTypeRef
    }

    fun transformDynamicTypeRefChildren(dynamicTypeRef: FirDynamicTypeRef, data: D): FirTypeRef {
        dynamicTypeRef.transformAnnotations(this, data)
        return dynamicTypeRef
    }

    fun transformFunctionTypeRefChildren(functionTypeRef: FirFunctionTypeRef, data: D): FirTypeRef {
        functionTypeRef.transformAnnotations(this, data)
        functionTypeRef.transformReceiverTypeRef(this, data)
        functionTypeRef.transformValueParameters(this, data)
        functionTypeRef.transformReturnTypeRef(this, data)
        functionTypeRef.transformContextReceiverTypeRefs(this, data)
        return functionTypeRef
    }

    fun transformIntersectionTypeRefChildren(intersectionTypeRef: FirIntersectionTypeRef, data: D): FirTypeRef {
        intersectionTypeRef.transformAnnotations(this, data)
        intersectionTypeRef.transformLeftType(this, data)
        intersectionTypeRef.transformRightType(this, data)
        return intersectionTypeRef
    }

    fun transformImplicitTypeRefChildren(implicitTypeRef: FirImplicitTypeRef, data: D): FirTypeRef {
        implicitTypeRef.transformAnnotations(this, data)
        return implicitTypeRef
    }

    fun transformSmartCastedTypeRefChildren(smartCastedTypeRef: FirSmartCastedTypeRef, data: D): FirTypeRef {
        smartCastedTypeRef.transformAnnotations(this, data)
        smartCastedTypeRef.transformDelegatedTypeRef(this, data)
        return smartCastedTypeRef
    }

    fun transformEffectDeclarationChildren(effectDeclaration: FirEffectDeclaration, data: D): FirEffectDeclaration {
        return effectDeclaration
    }

    fun transformContractDescriptionChildren(contractDescription: FirContractDescription, data: D): FirContractDescription {
        return contractDescription
    }

    fun transformLegacyRawContractDescriptionChildren(legacyRawContractDescription: FirLegacyRawContractDescription, data: D): FirContractDescription {
        legacyRawContractDescription.transformContractCall(this, data)
        return legacyRawContractDescription
    }

    fun transformRawContractDescriptionChildren(rawContractDescription: FirRawContractDescription, data: D): FirContractDescription {
        rawContractDescription.transformRawEffects(this, data)
        return rawContractDescription
    }

    fun transformResolvedContractDescriptionChildren(resolvedContractDescription: FirResolvedContractDescription, data: D): FirContractDescription {
        resolvedContractDescription.transformEffects(this, data)
        resolvedContractDescription.transformUnresolvedEffects(this, data)
        return resolvedContractDescription
    }

    fun dispatchTransformChildren(element: FirElement, data: D): FirElement {
        return when (element.elementKind) {
            Reference -> transformReferenceChildren(element as FirReference, data)
            Label -> transformLabelChildren(element as FirLabel, data)
            DeclarationStatus -> transformDeclarationStatusChildren(element as FirDeclarationStatus, data)
            ResolvedDeclarationStatus -> transformResolvedDeclarationStatusChildren(element as FirResolvedDeclarationStatus, data)
            Expression -> transformExpressionChildren(element as FirExpression, data)
            StatementStub -> transformStatementStubChildren(element as FirStatementStub, data)
            ContextReceiver -> transformContextReceiverChildren(element as FirContextReceiver, data)
            AnonymousInitializer -> transformAnonymousInitializerChildren(element as FirAnonymousInitializer, data)
            TypeParameterRef -> transformTypeParameterRefChildren(element as FirTypeParameterRef, data)
            TypeParameter -> transformTypeParameterChildren(element as FirTypeParameter, data)
            ValueParameter -> transformValueParameterChildren(element as FirValueParameter, data)
            Property -> transformPropertyChildren(element as FirProperty, data)
            Field -> transformFieldChildren(element as FirField, data)
            EnumEntry -> transformEnumEntryChildren(element as FirEnumEntry, data)
            RegularClass -> transformRegularClassChildren(element as FirRegularClass, data)
            TypeAlias -> transformTypeAliasChildren(element as FirTypeAlias, data)
            SimpleFunction -> transformSimpleFunctionChildren(element as FirSimpleFunction, data)
            PropertyAccessor -> transformPropertyAccessorChildren(element as FirPropertyAccessor, data)
            BackingField -> transformBackingFieldChildren(element as FirBackingField, data)
            Constructor -> transformConstructorChildren(element as FirConstructor, data)
            File -> transformFileChildren(element as FirFile, data)
            PackageDirective -> transformPackageDirectiveChildren(element as FirPackageDirective, data)
            AnonymousFunction -> transformAnonymousFunctionChildren(element as FirAnonymousFunction, data)
            AnonymousFunctionExpression -> transformAnonymousFunctionExpressionChildren(element as FirAnonymousFunctionExpression, data)
            AnonymousObject -> transformAnonymousObjectChildren(element as FirAnonymousObject, data)
            AnonymousObjectExpression -> transformAnonymousObjectExpressionChildren(element as FirAnonymousObjectExpression, data)
            Import -> transformImportChildren(element as FirImport, data)
            ResolvedImport -> transformResolvedImportChildren(element as FirResolvedImport, data)
            ErrorImport -> transformErrorImportChildren(element as FirErrorImport, data)
            ErrorLoop -> transformErrorLoopChildren(element as FirErrorLoop, data)
            DoWhileLoop -> transformDoWhileLoopChildren(element as FirDoWhileLoop, data)
            WhileLoop -> transformWhileLoopChildren(element as FirWhileLoop, data)
            Block -> transformBlockChildren(element as FirBlock, data)
            BinaryLogicExpression -> transformBinaryLogicExpressionChildren(element as FirBinaryLogicExpression, data)
            BreakExpression -> transformBreakExpressionChildren(element as FirBreakExpression, data)
            ContinueExpression -> transformContinueExpressionChildren(element as FirContinueExpression, data)
            Catch -> transformCatchChildren(element as FirCatch, data)
            TryExpression -> transformTryExpressionChildren(element as FirTryExpression, data)
            ConstExpression -> transformConstExpressionChildren(element as FirConstExpression<*> , data)
            StarProjection -> transformStarProjectionChildren(element as FirStarProjection, data)
            PlaceholderProjection -> transformPlaceholderProjectionChildren(element as FirPlaceholderProjection, data)
            TypeProjectionWithVariance -> transformTypeProjectionWithVarianceChildren(element as FirTypeProjectionWithVariance, data)
            ArgumentList -> transformArgumentListChildren(element as FirArgumentList, data)
            Annotation -> transformAnnotationChildren(element as FirAnnotation, data)
            AnnotationCall -> transformAnnotationCallChildren(element as FirAnnotationCall, data)
            AnnotationArgumentMapping -> transformAnnotationArgumentMappingChildren(element as FirAnnotationArgumentMapping, data)
            ComparisonExpression -> transformComparisonExpressionChildren(element as FirComparisonExpression, data)
            TypeOperatorCall -> transformTypeOperatorCallChildren(element as FirTypeOperatorCall, data)
            AssignmentOperatorStatement -> transformAssignmentOperatorStatementChildren(element as FirAssignmentOperatorStatement, data)
            EqualityOperatorCall -> transformEqualityOperatorCallChildren(element as FirEqualityOperatorCall, data)
            WhenExpression -> transformWhenExpressionChildren(element as FirWhenExpression, data)
            WhenBranch -> transformWhenBranchChildren(element as FirWhenBranch, data)
            CheckNotNullCall -> transformCheckNotNullCallChildren(element as FirCheckNotNullCall, data)
            ElvisExpression -> transformElvisExpressionChildren(element as FirElvisExpression, data)
            ArrayOfCall -> transformArrayOfCallChildren(element as FirArrayOfCall, data)
            AugmentedArraySetCall -> transformAugmentedArraySetCallChildren(element as FirAugmentedArraySetCall, data)
            ClassReferenceExpression -> transformClassReferenceExpressionChildren(element as FirClassReferenceExpression, data)
            ErrorExpression -> transformErrorExpressionChildren(element as FirErrorExpression, data)
            ErrorFunction -> transformErrorFunctionChildren(element as FirErrorFunction, data)
            ErrorProperty -> transformErrorPropertyChildren(element as FirErrorProperty, data)
            PropertyAccessExpression -> transformPropertyAccessExpressionChildren(element as FirPropertyAccessExpression, data)
            FunctionCall -> transformFunctionCallChildren(element as FirFunctionCall, data)
            IntegerLiteralOperatorCall -> transformIntegerLiteralOperatorCallChildren(element as FirIntegerLiteralOperatorCall, data)
            ImplicitInvokeCall -> transformImplicitInvokeCallChildren(element as FirImplicitInvokeCall, data)
            DelegatedConstructorCall -> transformDelegatedConstructorCallChildren(element as FirDelegatedConstructorCall, data)
            ComponentCall -> transformComponentCallChildren(element as FirComponentCall, data)
            CallableReferenceAccess -> transformCallableReferenceAccessChildren(element as FirCallableReferenceAccess, data)
            ThisReceiverExpression -> transformThisReceiverExpressionChildren(element as FirThisReceiverExpression, data)
            SafeCallExpression -> transformSafeCallExpressionChildren(element as FirSafeCallExpression, data)
            CheckedSafeCallSubject -> transformCheckedSafeCallSubjectChildren(element as FirCheckedSafeCallSubject, data)
            GetClassCall -> transformGetClassCallChildren(element as FirGetClassCall, data)
            LambdaArgumentExpression -> transformLambdaArgumentExpressionChildren(element as FirLambdaArgumentExpression, data)
            SpreadArgumentExpression -> transformSpreadArgumentExpressionChildren(element as FirSpreadArgumentExpression, data)
            NamedArgumentExpression -> transformNamedArgumentExpressionChildren(element as FirNamedArgumentExpression, data)
            VarargArgumentsExpression -> transformVarargArgumentsExpressionChildren(element as FirVarargArgumentsExpression, data)
            ResolvedQualifier -> transformResolvedQualifierChildren(element as FirResolvedQualifier, data)
            ErrorResolvedQualifier -> transformErrorResolvedQualifierChildren(element as FirErrorResolvedQualifier, data)
            ResolvedReifiedParameterReference -> transformResolvedReifiedParameterReferenceChildren(element as FirResolvedReifiedParameterReference, data)
            ReturnExpression -> transformReturnExpressionChildren(element as FirReturnExpression, data)
            StringConcatenationCall -> transformStringConcatenationCallChildren(element as FirStringConcatenationCall, data)
            ThrowExpression -> transformThrowExpressionChildren(element as FirThrowExpression, data)
            VariableAssignment -> transformVariableAssignmentChildren(element as FirVariableAssignment, data)
            WhenSubjectExpression -> transformWhenSubjectExpressionChildren(element as FirWhenSubjectExpression, data)
            WrappedDelegateExpression -> transformWrappedDelegateExpressionChildren(element as FirWrappedDelegateExpression, data)
            NamedReference -> transformNamedReferenceChildren(element as FirNamedReference, data)
            ErrorNamedReference -> transformErrorNamedReferenceChildren(element as FirErrorNamedReference, data)
            SuperReference -> transformSuperReferenceChildren(element as FirSuperReference, data)
            ThisReference -> transformThisReferenceChildren(element as FirThisReference, data)
            ControlFlowGraphReference -> transformControlFlowGraphReferenceChildren(element as FirControlFlowGraphReference, data)
            ResolvedNamedReference -> transformResolvedNamedReferenceChildren(element as FirResolvedNamedReference, data)
            DelegateFieldReference -> transformDelegateFieldReferenceChildren(element as FirDelegateFieldReference, data)
            BackingFieldReference -> transformBackingFieldReferenceChildren(element as FirBackingFieldReference, data)
            ResolvedCallableReference -> transformResolvedCallableReferenceChildren(element as FirResolvedCallableReference, data)
            ResolvedTypeRef -> transformResolvedTypeRefChildren(element as FirResolvedTypeRef, data)
            ErrorTypeRef -> transformErrorTypeRefChildren(element as FirErrorTypeRef, data)
            UserTypeRef -> transformUserTypeRefChildren(element as FirUserTypeRef, data)
            DynamicTypeRef -> transformDynamicTypeRefChildren(element as FirDynamicTypeRef, data)
            FunctionTypeRef -> transformFunctionTypeRefChildren(element as FirFunctionTypeRef, data)
            IntersectionTypeRef -> transformIntersectionTypeRefChildren(element as FirIntersectionTypeRef, data)
            ImplicitTypeRef -> transformImplicitTypeRefChildren(element as FirImplicitTypeRef, data)
            SmartCastedTypeRef -> transformSmartCastedTypeRefChildren(element as FirSmartCastedTypeRef, data)
            EffectDeclaration -> transformEffectDeclarationChildren(element as FirEffectDeclaration, data)
            ContractDescription -> transformContractDescriptionChildren(element as FirContractDescription, data)
            LegacyRawContractDescription -> transformLegacyRawContractDescriptionChildren(element as FirLegacyRawContractDescription, data)
            RawContractDescription -> transformRawContractDescriptionChildren(element as FirRawContractDescription, data)
            ResolvedContractDescription -> transformResolvedContractDescriptionChildren(element as FirResolvedContractDescription, data)
        }
    }
}
