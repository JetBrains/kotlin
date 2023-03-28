/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element.Kind.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
object FirTreeBuilder : AbstractFirTreeBuilder() {
    val annotationContainer by element(Other)
    val typeRef by sealedElement(TypeRef, annotationContainer)
    val reference by element(Reference)
    val label by element(Other)

    val resolvable by sealedElement(Expression)

    val targetElement by element(Other)

    val declarationStatus by element(Declaration)
    val resolvedDeclarationStatus by element(Declaration, declarationStatus)

    val controlFlowGraphOwner by element(Declaration)

    val statement by element(Expression, annotationContainer)
    val expression by element(Expression, statement)
    val lazyExpression by element(Expression, expression)

    val contextReceiver by element(Declaration)

    val elementWithResolveState by element(Other)
    val fileAnnotationsContainer by element(Other, elementWithResolveState, annotationContainer)
    val declaration by sealedElement(Declaration, elementWithResolveState, annotationContainer)
    val typeParameterRefsOwner by sealedElement(Declaration)
    val typeParametersOwner by sealedElement(Declaration, typeParameterRefsOwner)
    val memberDeclaration by sealedElement(Declaration, declaration, typeParameterRefsOwner)
    val anonymousInitializer by element(Declaration, declaration, controlFlowGraphOwner)
    val callableDeclaration by sealedElement(Declaration, memberDeclaration)
    val typeParameterRef by element(Declaration)
    val typeParameter by element(Declaration, typeParameterRef, declaration)

    val variable by sealedElement(Declaration, callableDeclaration, statement)
    val valueParameter by element(Declaration, variable, controlFlowGraphOwner)
    val receiverParameter by element(Declaration, annotationContainer)
    val property by element(Declaration, variable, typeParametersOwner, controlFlowGraphOwner)
    val field by element(Declaration, variable, controlFlowGraphOwner)
    val enumEntry by element(Declaration, variable)

    val functionTypeParameter by element(Other, baseFirElement)

    val classLikeDeclaration by sealedElement(Declaration, memberDeclaration, statement)
    val klass by sealedElement("Class", Declaration, classLikeDeclaration, statement, typeParameterRefsOwner, controlFlowGraphOwner)
    val regularClass by element(Declaration, klass)
    val typeAlias by element(Declaration, classLikeDeclaration, typeParametersOwner)

    val function by sealedElement(Declaration, callableDeclaration, targetElement, controlFlowGraphOwner, statement)

    val contractDescriptionOwner by sealedElement(Declaration)
    val simpleFunction by element(Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val propertyAccessor by element(Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val backingField by element(Declaration, variable, typeParametersOwner, statement)
    val constructor by element(Declaration, function, typeParameterRefsOwner, contractDescriptionOwner)
    val file by element(Declaration, declaration)
    val script by element(Declaration, declaration)
    val packageDirective by element(Other)

    val anonymousFunction by element(Declaration, function, typeParametersOwner, contractDescriptionOwner)
    val anonymousFunctionExpression by element(Expression, expression)

    val anonymousObject by element(Declaration, klass)
    val anonymousObjectExpression by element(Expression, expression)

    val diagnosticHolder by element(Diagnostics)

    val import by element(Declaration)
    val resolvedImport by element(Declaration, import)
    val errorImport by element(Declaration, import, diagnosticHolder)

    val loop by sealedElement(Expression, statement, targetElement)
    val errorLoop by element(Expression, loop, diagnosticHolder)
    val doWhileLoop by element(Expression, loop)
    val whileLoop by element(Expression, loop)

    val block by element(Expression, expression)
    val lazyBlock by element(Expression, block)

    val binaryLogicExpression by element(Expression, expression)
    val jump by sealedElement(Expression, expression)
    val loopJump by element(Expression, jump)
    val breakExpression by element(Expression, loopJump)
    val continueExpression by element(Expression, loopJump)
    val catchClause by element("Catch", Expression)
    val tryExpression by element(Expression, expression, resolvable)
    val constExpression by element(Expression, expression)
    val typeProjection by element(TypeRef)
    val starProjection by element(TypeRef, typeProjection)
    val placeholderProjection by element(TypeRef, typeProjection)
    val typeProjectionWithVariance by element(TypeRef, typeProjection)
    val argumentList by element(Expression)
    val call by sealedElement(Expression, statement) // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val annotation by element(Expression, expression)
    val annotationCall by element(Expression, annotation, call, resolvable)
    val annotationArgumentMapping by element(Expression)
    val errorAnnotationCall by element(Expression, annotationCall, diagnosticHolder)
    val comparisonExpression by element(Expression, expression)
    val typeOperatorCall by element(Expression, expression, call)
    val assignmentOperatorStatement by element(Expression, statement)
    val incrementDecrementExpression by element(Expression, expression)
    val equalityOperatorCall by element(Expression, expression, call)
    val whenExpression by element(Expression, expression, resolvable)
    val whenBranch by element(Expression)
    val contextReceiverArgumentListOwner by element(Expression)
    val checkNotNullCall by element(Expression, expression, call, resolvable)
    val elvisExpression by element(Expression, expression, resolvable)

    val arrayOfCall by element(Expression, expression, call)
    val augmentedArraySetCall by element(Expression, statement)
    val classReferenceExpression by element(Expression, expression)
    val errorExpression by element(Expression, expression, diagnosticHolder)
    val errorFunction by element(Declaration, function, diagnosticHolder)
    val errorProperty by element(Declaration, variable, diagnosticHolder)
    val danglingModifierList by element(Declaration, declaration, diagnosticHolder)
    val qualifiedAccessExpression by element(Expression, expression, resolvable, contextReceiverArgumentListOwner)
    val qualifiedErrorAccessExpression by element(Expression, expression, diagnosticHolder)
    val propertyAccessExpression by element(Expression, qualifiedAccessExpression)
    val functionCall by element(Expression, qualifiedAccessExpression, call)
    val integerLiteralOperatorCall by element(Expression, functionCall)
    val implicitInvokeCall by element(Expression, functionCall)
    val delegatedConstructorCall by element(Expression, resolvable, call, contextReceiverArgumentListOwner)
    val componentCall by element(Expression, functionCall)
    val callableReferenceAccess by element(Expression, qualifiedAccessExpression)
    val thisReceiverExpression by element(Expression, qualifiedAccessExpression)

    val smartCastExpression by element(Expression, expression)
    val safeCallExpression by element(Expression, expression)
    val checkedSafeCallSubject by element(Expression, expression)
    val getClassCall by element(Expression, expression, call)
    val wrappedExpression by element(Expression, expression)
    val wrappedArgumentExpression by element(Expression, wrappedExpression)
    val lambdaArgumentExpression by element(Expression, wrappedArgumentExpression)
    val spreadArgumentExpression by element(Expression, wrappedArgumentExpression)
    val namedArgumentExpression by element(Expression, wrappedArgumentExpression)
    val varargArgumentsExpression by element(Expression, expression)

    val resolvedQualifier by element(Expression, expression)
    val errorResolvedQualifier by element(Expression, resolvedQualifier, diagnosticHolder)
    val resolvedReifiedParameterReference by element(Expression, expression)
    val returnExpression by element(Expression, jump)
    val stringConcatenationCall by element(Expression, call, expression)
    val throwExpression by element(Expression, expression)
    val variableAssignment by element(Expression, statement)
    val whenSubjectExpression by element(Expression, expression)
    val desugaredAssignmentValueReferenceExpression by element(Expression, expression)

    val wrappedDelegateExpression by element(Expression, wrappedExpression)

    val namedReference by element(Reference, reference)
    val namedReferenceWithCandidateBase by element(Reference, namedReference)
    val errorNamedReference by element(Reference, namedReference, diagnosticHolder)
    val fromMissingDependenciesNamedReference by element(Reference, namedReference)
    val superReference by element(Reference, reference)
    val thisReference by element(Reference, reference)
    val controlFlowGraphReference by element(Reference, reference)

    val resolvedNamedReference by element(Reference, namedReference)
    val resolvedErrorReference by element(Reference, resolvedNamedReference, diagnosticHolder)
    val delegateFieldReference by element(Reference, resolvedNamedReference)
    val backingFieldReference by element(Reference, resolvedNamedReference)

    val resolvedCallableReference by element(Reference, resolvedNamedReference)

    val resolvedTypeRef by element(TypeRef, typeRef)
    val errorTypeRef by element(TypeRef, resolvedTypeRef, diagnosticHolder)
    val typeRefWithNullability by element(TypeRef, typeRef)
    val userTypeRef by element(TypeRef, typeRefWithNullability)
    val dynamicTypeRef by element(TypeRef, typeRefWithNullability)
    val functionTypeRef by element(TypeRef, typeRefWithNullability)
    val intersectionTypeRef by element(TypeRef, typeRefWithNullability)
    val implicitTypeRef by element(TypeRef, typeRef)

    val effectDeclaration by element(Contracts)

    val contractDescription by element(Contracts)
    val legacyRawContractDescription by element(Contracts, contractDescription)
    val rawContractDescription by element(Contracts, contractDescription)
    val resolvedContractDescription by element(Contracts, contractDescription)
}
