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
    val typeRef = sealedElement("TypeRef", TypeRef, annotationContainer)
    val reference = element("Reference", Reference)
    val label = element("Label", Other)

    val resolvable = sealedElement("Resolvable", Expression)

    val targetElement = element("TargetElement", Other)

    val declarationStatus = element("DeclarationStatus", Declaration)
    val resolvedDeclarationStatus = element("ResolvedDeclarationStatus", Declaration, declarationStatus)

    val controlFlowGraphOwner = element("ControlFlowGraphOwner", Declaration)

    val statement = element("Statement", Expression, annotationContainer)
    val expression = element("Expression", Expression, statement)
    val declaration = sealedElement("Declaration", Declaration)
    val annotatedDeclaration = sealedElement("AnnotatedDeclaration", Declaration, declaration, annotationContainer)
    val anonymousInitializer = element("AnonymousInitializer", Declaration, declaration, controlFlowGraphOwner)
    val typedDeclaration = sealedElement("TypedDeclaration", Declaration, annotatedDeclaration)
    val typeParameterRefsOwner = sealedElement("TypeParameterRefsOwner", Declaration)
    val typeParametersOwner = sealedElement("TypeParametersOwner", Declaration, typeParameterRefsOwner)
    val memberDeclaration = sealedElement("MemberDeclaration", Declaration, typeParameterRefsOwner)
    val callableDeclaration = sealedElement("CallableDeclaration", Declaration, typedDeclaration, memberDeclaration)
    val typeParameterRef = element("TypeParameterRef", Declaration)
    val typeParameter = element("TypeParameter", Declaration, typeParameterRef, annotatedDeclaration)

    val variable = sealedElement("Variable", Declaration, callableDeclaration, statement)
    val valueParameter = element("ValueParameter", Declaration, variable, controlFlowGraphOwner)
    val property = element("Property", Declaration, variable, typeParametersOwner, controlFlowGraphOwner)
    val field = element("Field", Declaration, variable)
    val enumEntry = element("EnumEntry", Declaration, variable)

    val classLikeDeclaration = sealedElement("ClassLikeDeclaration", Declaration, annotatedDeclaration, statement)
    val klass = sealedElement("Class", Declaration, classLikeDeclaration, statement, typeParameterRefsOwner)
    val regularClass = element("RegularClass", Declaration, klass, memberDeclaration, controlFlowGraphOwner)
    val typeAlias = element("TypeAlias", Declaration, classLikeDeclaration, memberDeclaration, typeParametersOwner)

    val function = sealedElement("Function", Declaration, callableDeclaration, targetElement, controlFlowGraphOwner, statement)

    val contractDescriptionOwner = sealedElement("ContractDescriptionOwner", Declaration)
    val simpleFunction = element("SimpleFunction", Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val propertyAccessor = element("PropertyAccessor", Declaration, function, contractDescriptionOwner, typeParametersOwner)
    val constructor = element("Constructor", Declaration, function, typeParameterRefsOwner)
    val file = element("File", Declaration, annotatedDeclaration)
    val packageDirective = element("PackageDirective", Other)

    val anonymousFunction = element("AnonymousFunction", Declaration, function, typeParametersOwner)
    val anonymousFunctionExpression = element("AnonymousFunctionExpression", Expression, expression)

    val anonymousObject = element("AnonymousObject", Declaration, klass, controlFlowGraphOwner)
    val anonymousObjectExpression = element("AnonymousObjectExpression", Expression, expression)

    val diagnosticHolder = element("DiagnosticHolder", Diagnostics)

    val import = element("Import", Declaration)
    val resolvedImport = element("ResolvedImport", Declaration, import)
    val errorImport = element("ErrorImport", Declaration, import, diagnosticHolder)

    val loop = sealedElement("Loop", Expression, statement, targetElement)
    val errorLoop = element("ErrorLoop", Expression, loop, diagnosticHolder)
    val doWhileLoop = element("DoWhileLoop", Expression, loop)
    val whileLoop = element("WhileLoop", Expression, loop)

    val block = element("Block", Expression, expression)
    val binaryLogicExpression = element("BinaryLogicExpression", Expression, expression)
    val jump = sealedElement("Jump", Expression, expression)
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
    val call = sealedElement("Call", Expression, statement) // TODO: may smth like `CallWithArguments` or `ElementWithArguments`?
    val annotationCall = element("AnnotationCall", Expression, expression, call, resolvable)
    val comparisonExpression = element("ComparisonExpression", Expression, expression)
    val typeOperatorCall = element("TypeOperatorCall", Expression, expression, call)
    val assignmentOperatorStatement = element("AssignmentOperatorStatement", Expression, statement)
    val equalityOperatorCall = element("EqualityOperatorCall", Expression, expression, call)
    val whenExpression = element("WhenExpression", Expression, expression, resolvable)
    val whenBranch = element("WhenBranch", Expression)
    val qualifiedAccess = element("QualifiedAccess", Expression, resolvable, statement)
    val checkNotNullCall = element("CheckNotNullCall", Expression, expression, call, resolvable)
    val elvisExpression = element("ElvisExpression", Expression, expression, resolvable)

    val arrayOfCall = element("ArrayOfCall", Expression, expression, call)
    val arraySetCall = element("AugmentedArraySetCall", Expression, statement)
    val classReferenceExpression = element("ClassReferenceExpression", Expression, expression)
    val errorExpression = element("ErrorExpression", Expression, expression, diagnosticHolder)
    val errorFunction = element("ErrorFunction", Declaration, function, diagnosticHolder)
    val errorProperty = element("ErrorProperty", Declaration, variable, diagnosticHolder)
    val qualifiedAccessExpression = element("QualifiedAccessExpression", Expression, expression, qualifiedAccess)
    val functionCall = element("FunctionCall", Expression, qualifiedAccessExpression, call)
    val implicitInvokeCall = element("ImplicitInvokeCall", Expression, functionCall)
    val delegatedConstructorCall = element("DelegatedConstructorCall", Expression, resolvable, call)
    val componentCall = element("ComponentCall", Expression, functionCall)
    val callableReferenceAccess = element("CallableReferenceAccess", Expression, qualifiedAccessExpression)
    val thisReceiverExpression = element("ThisReceiverExpression", Expression, qualifiedAccessExpression)
    val expressionWithSmartcast = element("ExpressionWithSmartcast", Expression, qualifiedAccessExpression)
    val expressionWithSmartcastToNull = element("ExpressionWithSmartcastToNull", Expression, expressionWithSmartcast)
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
    val typeRefWithNullability = element("TypeRefWithNullability", TypeRef, typeRef)
    val userTypeRef = element("UserTypeRef", TypeRef, typeRefWithNullability)
    val dynamicTypeRef = element("DynamicTypeRef", TypeRef, typeRefWithNullability)
    val functionTypeRef = element("FunctionTypeRef", TypeRef, typeRefWithNullability)
    val implicitTypeRef = element("ImplicitTypeRef", TypeRef, typeRef)

    val effectDeclaration = element("EffectDeclaration", Contracts)

    val contractDescription = element("ContractDescription", Contracts)
    val legacyRawContractDescription = element("LegacyRawContractDescription", Contracts, contractDescription)
    val rawContractDescription = element("RawContractDescription", Contracts, contractDescription)
    val resolvedContractDescription = element("ResolvedContractDescription", Contracts, contractDescription)
}
