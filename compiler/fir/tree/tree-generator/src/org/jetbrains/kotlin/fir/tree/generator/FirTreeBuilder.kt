/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element.Kind.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
object FirTreeBuilder : AbstractFirTreeBuilder() {
    val annotationContainer = element("AnnotationContainer", Other)
    val typeRef = element("TypeRef", TypeRef, annotationContainer)
    val reference = element("Reference", Reference)
    val label = element("Label", Other)
    val import = element("Import", Declaration)
    val resolvedImport = element("ResolvedImport", Declaration, import)
    val symbolOwner = element("SymbolOwner", Other)
    val resolvable = element("Resolvable", Expression)

    val targetElement = element("TargetElement", Other)

    val declarationStatus = element("DeclarationStatus", Declaration)
    val resolvedDeclarationStatus = element("ResolvedDeclarationStatus", Declaration, declarationStatus)

    val controlFlowGraphOwner = element("ControlFlowGraphOwner", Declaration)

    val statement = element("Statement", Expression, annotationContainer)
    val expression = element("Expression", Expression, statement)
    val declaration = element("Declaration", Declaration)
    val annotatedDeclaration = element("AnnotatedDeclaration", Declaration, declaration, annotationContainer)
    val anonymousInitializer = element("AnonymousInitializer", Declaration, declaration, symbolOwner, controlFlowGraphOwner)
    val typedDeclaration = element("TypedDeclaration", Declaration, annotatedDeclaration)
    val callableDeclaration = element("CallableDeclaration", Declaration, typedDeclaration, symbolOwner)
    val typeParameterRef = element("TypeParameterRef", Declaration)
    val typeParameter = element("TypeParameter", Declaration, typeParameterRef, annotatedDeclaration, symbolOwner)
    val typeParameterRefsOwner = element("TypeParameterRefsOwner", Declaration)
    val typeParametersOwner = element("TypeParametersOwner", Declaration, typeParameterRefsOwner)
    val memberDeclaration = element("MemberDeclaration", Declaration, annotatedDeclaration, typeParameterRefsOwner)
    val callableMemberDeclaration = element("CallableMemberDeclaration", Declaration, callableDeclaration, memberDeclaration)

    val variable = element("Variable", Declaration, callableDeclaration, annotatedDeclaration, statement)
    val valueParameter = element("ValueParameter", Declaration, variable, controlFlowGraphOwner)
    val property = element("Property", Declaration, variable, typeParametersOwner, controlFlowGraphOwner, callableMemberDeclaration)
    val field = element("Field", Declaration, variable, typeParametersOwner, callableMemberDeclaration)
    val enumEntry = element("EnumEntry", Declaration, variable, callableMemberDeclaration)

    val classLikeDeclaration = element("ClassLikeDeclaration", Declaration, annotatedDeclaration, statement, symbolOwner)
    val klass = element("Class", Declaration, classLikeDeclaration, statement, typeParameterRefsOwner)
    val regularClass = element("RegularClass", Declaration, memberDeclaration, typeParameterRefsOwner, controlFlowGraphOwner, klass)
    val typeAlias = element("TypeAlias", Declaration, classLikeDeclaration, memberDeclaration, typeParametersOwner)

    val function = element("Function", Declaration, callableDeclaration, targetElement, typeParameterRefsOwner, controlFlowGraphOwner, statement)

    val contractDescriptionOwner = element("ContractDescriptionOwner", Declaration)
    val simpleFunction = element("SimpleFunction", Declaration, function, callableMemberDeclaration, contractDescriptionOwner, typeParametersOwner)
    val propertyAccessor = element("PropertyAccessor", Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val constructor = element("Constructor", Declaration, function, callableMemberDeclaration, typeParameterRefsOwner)
    val file = element("File", Declaration, annotatedDeclaration)

    val anonymousFunction = element("AnonymousFunction", Declaration, function, expression, typeParametersOwner)
    val anonymousObject = element("AnonymousObject", Declaration, klass, controlFlowGraphOwner, expression)

    val diagnosticHolder = element("DiagnosticHolder", Diagnostics)

    val loop = element("Loop", Expression, statement, targetElement)
    val errorLoop = element("ErrorLoop", Expression, loop, diagnosticHolder)
    val doWhileLoop = element("DoWhileLoop", Expression, loop)
    val whileLoop = element("WhileLoop", Expression, loop)

    val block = element("Block", Expression, expression)
    val binaryLogicExpression = element("BinaryLogicExpression", Expression, expression)
    val jump = element("Jump", Expression, expression)
    val loopJump = element("LoopJump", Expression, jump)
    val breakExpression = element("BreakExpression", Expression, loopJump)
    val continueExpression = element("ContinueExpression", Expression, loopJump)
    val catchClause = element("Catch", Expression)
    val tryExpression = element("TryExpression", Expression, expression, resolvable)
    val constExpression = element("ConstExpression", Expression, expression)
    val typeProjection = element("TypeProjection", TypeRef)
    val starProjection = element("StarProjection", TypeRef, typeProjection)
    val typeProjectionWithVariance = element("TypeProjectionWithVariance", TypeRef, typeProjection)
    val argumentList = element("ArgumentList", Expression)
    val call = element("Call", Expression, statement) // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val annotationCall = element("AnnotationCall", Expression, expression, call, resolvable)
    val comparisonExpression = element("ComparisonExpression", Expression, expression)
    val typeOperatorCall = element("TypeOperatorCall", Expression, expression, call)
    val assignmentOperatorStatement = element("AssignmentOperatorStatement", Expression, statement)
    val equalityOperatorCall = element("EqualityOperatorCall", Expression, expression, call)
    val whenExpression = element("WhenExpression", Expression, expression, resolvable)
    val whenBranch = element("WhenBranch", Expression)
    val qualifiedAccessWithoutCallee = element("QualifiedAccessWithoutCallee", Expression, statement)
    val qualifiedAccess = element("QualifiedAccess", Expression, qualifiedAccessWithoutCallee, resolvable)
    val checkNotNullCall = element("CheckNotNullCall", Expression, expression, call, resolvable)
    val elvisExpression = element("ElvisExpression", Expression, expression, resolvable)

    val arrayOfCall = element("ArrayOfCall", Expression, expression, call)
    val arraySetCall = element("AugmentedArraySetCall", Expression, statement)
    val classReferenceExpression = element("ClassReferenceExpression", Expression, expression)
    val errorExpression = element("ErrorExpression", Expression, expression, diagnosticHolder)
    val errorFunction = element("ErrorFunction", Declaration, function, diagnosticHolder, typeParametersOwner)
    val errorProperty = element("ErrorProperty", Declaration, variable, diagnosticHolder, typeParametersOwner)
    val qualifiedAccessExpression = element("QualifiedAccessExpression", Expression, expression, qualifiedAccess)
    val functionCall = element("FunctionCall", Expression, qualifiedAccessExpression, call)
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, resolvable, call)
    val componentCall = element("ComponentCall", Expression, functionCall)
    val callableReferenceAccess = element("CallableReferenceAccess", Expression, qualifiedAccessExpression)
    val thisReceiverExpression = element("ThisReceiverExpression", Expression, qualifiedAccessExpression)
    val expressionWithSmartcast = element("ExpressionWithSmartcast", Expression, qualifiedAccessExpression)
    val safeCallExpression = element("SafeCallExpression", Expression, expression)
    val checkedSafeCallSubject = element("CheckedSafeCallSubject", Expression, expression)
    val getClassCall = element("GetClassCall", Expression, expression, call)
    val wrappedExpression = element("WrappedExpression", Expression, expression)
    val wrappedArgumentExpression = element("WrappedArgumentExpression", Expression, wrappedExpression)
    val lambdaArgumentExpression = element("LambdaArgumentExpression", Expression, wrappedArgumentExpression)
    val spreadArgumentExpression = element("SpreadArgumentExpression", Expression, wrappedArgumentExpression)
    val namedArgumentExpression = element("NamedArgumentExpression", Expression, wrappedArgumentExpression)
    val varargArgumentsExpression = element("VarargArgumentsExpression", Expression, expression)

    val resolvedQualifier = element("ResolvedQualifier", Expression, expression)
    val errorResolvedQualifier = element("ErrorResolvedQualifier", Expression, resolvedQualifier, diagnosticHolder)
    val resolvedReifiedParameterReference = element("ResolvedReifiedParameterReference", Expression, expression)
    val returnExpression = element("ReturnExpression", Expression, jump)
    val stringConcatenationCall = element("StringConcatenationCall", Expression, call, expression)
    val throwExpression = element("ThrowExpression", Expression, expression)
    val variableAssignment = element("VariableAssignment", Expression, qualifiedAccess)
    val whenSubjectExpression = element("WhenSubjectExpression", Expression, expression)

    val wrappedDelegateExpression = element("WrappedDelegateExpression", Expression, wrappedExpression)

    val namedReference = element("NamedReference", Reference, reference)
    val errorNamedReference = element("ErrorNamedReference", Reference, namedReference, diagnosticHolder)
    val superReference = element("SuperReference", Reference, reference)
    val thisReference = element("ThisReference", Reference, reference)
    val controlFlowGraphReference = element("ControlFlowGraphReference", Reference, reference)

    val resolvedNamedReference = element("ResolvedNamedReference", Reference, namedReference)
    val delegateFieldReference = element("DelegateFieldReference", Reference, resolvedNamedReference)
    val backingFieldReference = element("BackingFieldReference", Reference, resolvedNamedReference)

    val resolvedCallableReference = element("ResolvedCallableReference", Reference, resolvedNamedReference)

    val resolvedTypeRef = element("ResolvedTypeRef", TypeRef, typeRef)
    val errorTypeRef = element("ErrorTypeRef", TypeRef, resolvedTypeRef, diagnosticHolder)
    val delegatedTypeRef = element("DelegatedTypeRef", TypeRef, typeRef)
    val typeRefWithNullability = element("TypeRefWithNullability", TypeRef, typeRef)
    val userTypeRef = element("UserTypeRef", TypeRef, typeRefWithNullability)
    val dynamicTypeRef = element("DynamicTypeRef", TypeRef, typeRefWithNullability)
    val functionTypeRef = element("FunctionTypeRef", TypeRef, typeRefWithNullability)
    val resolvedFunctionTypeRef = element("ResolvedFunctionTypeRef", TypeRef, resolvedTypeRef, functionTypeRef)
    val implicitTypeRef = element("ImplicitTypeRef", TypeRef, typeRef)
    val composedSuperTypeRef = element("ComposedSuperTypeRef", TypeRef, typeRef)

    val effectDeclaration = element("EffectDeclaration", Contracts)

    val contractDescription = element("ContractDescription", Contracts)
    val legacyRawContractDescription = element("LegacyRawContractDescription", Contracts, contractDescription)
    val rawContractDescription = element("RawContractDescription", Contracts, contractDescription)
    val resolvedContractDescription = element("ResolvedContractDescription", Contracts, contractDescription)
}
