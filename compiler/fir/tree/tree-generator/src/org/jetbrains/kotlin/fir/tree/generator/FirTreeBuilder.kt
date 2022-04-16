/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

    val contextReceiver by element(Declaration)

    val declaration by sealedElement(Declaration, annotationContainer)
    val typeParameterRefsOwner by sealedElement(Declaration)
    val typeParametersOwner by sealedElement(Declaration, typeParameterRefsOwner)
    val memberDeclaration by sealedElement(Declaration, declaration, typeParameterRefsOwner)
    val anonymousInitializer by element(Declaration, declaration, controlFlowGraphOwner)
    val callableDeclaration by sealedElement(Declaration, memberDeclaration)
    val typeParameterRef by element(Declaration)
    val typeParameter by element(Declaration, typeParameterRef, declaration)

    val variable by sealedElement(Declaration, callableDeclaration, statement)
    val valueParameter by element(Declaration, variable, controlFlowGraphOwner)
    val property by element(Declaration, variable, typeParametersOwner, controlFlowGraphOwner)
    val field by element(Declaration, variable, controlFlowGraphOwner)
    val enumEntry by element(Declaration, variable)

    val classLikeDeclaration by sealedElement(Declaration, memberDeclaration, statement)
    val klass by sealedElement("Class", Declaration, classLikeDeclaration, statement, typeParameterRefsOwner)
    val regularClass by element(Declaration, klass, controlFlowGraphOwner)
    val typeAlias by element(Declaration, classLikeDeclaration, typeParametersOwner)

    val function by sealedElement(Declaration, callableDeclaration, targetElement, controlFlowGraphOwner, statement)

    val contractDescriptionOwner by sealedElement(Declaration)
    val simpleFunction by element(Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val propertyAccessor by element(Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val backingField by element(Declaration, variable, typeParametersOwner, statement)
    val constructor by element(Declaration, function, typeParameterRefsOwner)
    val file by element(Declaration, declaration)
    val packageDirective by element(Other)

    val anonymousFunction by element(Declaration, function, typeParametersOwner)
    val anonymousFunctionExpression by element(Expression, expression)

    val anonymousObject by element(Declaration, klass, controlFlowGraphOwner)
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
    val comparisonExpression by element(Expression, expression)
    val typeOperatorCall by element(Expression, expression, call)
    val assignmentOperatorStatement by element(Expression, statement)
    val equalityOperatorCall by element(Expression, expression, call)
    val whenExpression by element(Expression, expression, resolvable)
    val whenBranch by element(Expression)
    val qualifiedAccess by element(Expression, resolvable, statement)
    val checkNotNullCall by element(Expression, expression, call, resolvable)
    val elvisExpression by element(Expression, expression, resolvable)

    val arrayOfCall by element(Expression, expression, call)
    val augmentedArraySetCall by element(Expression, statement)
    val classReferenceExpression by element(Expression, expression)
    val errorExpression by element(Expression, expression, diagnosticHolder)
    val errorFunction by element(Declaration, function, diagnosticHolder)
    val errorProperty by element(Declaration, variable, diagnosticHolder)
    val qualifiedAccessExpression by element(Expression, expression, qualifiedAccess)
    val propertyAccessExpression by element(Expression, qualifiedAccessExpression)
    val functionCall by element(Expression, qualifiedAccessExpression, call)
    val integerLiteralOperatorCall by element(Expression, functionCall)
    val implicitInvokeCall by element(Expression, functionCall)
    val delegatedConstructorCall by element(Expression, resolvable, call)
    val componentCall by element(Expression, functionCall)
    val callableReferenceAccess by element(Expression, qualifiedAccessExpression)
    val thisReceiverExpression by element(Expression, qualifiedAccessExpression)
    val wrappedExpressionWithSmartcast by element(Expression)
    val wrappedExpressionWithSmartcastToNull by element(Expression, wrappedExpressionWithSmartcast)
    val expressionWithSmartcast by element(Expression, qualifiedAccessExpression, wrappedExpressionWithSmartcast)
    val expressionWithSmartcastToNull by element(Expression, expressionWithSmartcast, wrappedExpressionWithSmartcastToNull)
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
    val variableAssignment by element(Expression, qualifiedAccess)
    val whenSubjectExpression by element(Expression, expression)
    val whenSubjectExpressionWithSmartcast by element(Expression, whenSubjectExpression, wrappedExpressionWithSmartcast)
    val whenSubjectExpressionWithSmartcastToNull by element(Expression, whenSubjectExpression, wrappedExpressionWithSmartcastToNull)

    val wrappedDelegateExpression by element(Expression, wrappedExpression)

    val namedReference by element(Reference, reference)
    val errorNamedReference by element(Reference, namedReference, diagnosticHolder)
    val superReference by element(Reference, reference)
    val thisReference by element(Reference, reference)
    val controlFlowGraphReference by element(Reference, reference)

    val resolvedNamedReference by element(Reference, namedReference)
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
