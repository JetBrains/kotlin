/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTTokenNode
import com.intellij.psi.TokenType
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.checkers.projectionKindAsString
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirDynamicUnsupportedChecker
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.originalOrSelf
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeReceiverConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.EmptyIntersectionTypeKind
import org.jetbrains.kotlin.util.getPreviousSibling
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

private fun ConeDiagnostic.toKtDiagnostic(
    source: KtSourceElement?,
    valueParameter: FirValueParameter?,
    callOrAssignmentSource: KtSourceElement?,
    session: FirSession,
): KtDiagnostic? = when (this) {
    is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.createOn(
        source,
        this.name.asString(),
        null,
        session,
    )

    is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.createOn(source, this.classId.asString(), null, session)
    is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.createOn(source, name.asString(), operatorToken, session)
    is ConeUnresolvedTypeQualifierError -> {
        when {
            // this.qualifiers will contain all resolved qualifiers from the left up to (including) the first unresolved qualifier.
            // We want to report UNRESOLVED_REFERENCE exactly on the first unresolved qualifier with its name as argument.
            // Examples: <!UNRESOLVED_REFERENCE!>Unresolved<!>, <!UNRESOLVED_REFERENCE!>Unresolved<!>.Foo,
            // Resolved.<!UNRESOLVED_REFERENCE!>Unresolved<!>, Resolved.<!UNRESOLVED_REFERENCE!>Unresolved<!>.Foo
            source?.kind == KtRealSourceElementKind -> {
                val lastQualifier = this.qualifiers.last()
                FirErrors.UNRESOLVED_REFERENCE.createOn(lastQualifier.source, lastQualifier.name.asString(), null, session)
            }
            else -> {
                FirErrors.UNRESOLVED_REFERENCE.createOn(source, this.qualifier, null, session)
            }
        }
    }
    is ConeFunctionCallExpectedError -> FirErrors.FUNCTION_CALL_EXPECTED.createOn(source, this.name.asString(), this.hasValueParameters, session)
    is ConeFunctionExpectedError -> FirErrors.FUNCTION_EXPECTED.createOn(source, this.expression, this.type, session)
    is ConeNoConstructorError -> FirErrors.NO_CONSTRUCTOR.createOn(callOrAssignmentSource ?: source, session)
    is ConeResolutionToClassifierError -> when (this.candidateSymbol.classKind) {
        ClassKind.INTERFACE -> FirErrors.INTERFACE_AS_FUNCTION.createOn(source, this.candidateSymbol, session)
        ClassKind.CLASS -> when {
            this.candidateSymbol.isInner -> FirErrors.INNER_CLASS_CONSTRUCTOR_NO_RECEIVER.createOn(source, this.candidateSymbol, session)
            this.candidateSymbol.isExpect -> FirErrors.EXPECT_CLASS_AS_FUNCTION.createOn(source, this.candidateSymbol, session)
            else -> FirErrors.RESOLUTION_TO_CLASSIFIER.createOn(source, this.candidateSymbol, session)
        }
        else -> FirErrors.RESOLUTION_TO_CLASSIFIER.createOn(source, this.candidateSymbol, session)
    }
    is ConeHiddenCandidateError -> {
        // Usages of callables with @Deprecated(DeprecationLevel.HIDDEN) should look like unresolved references.
        // See: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/
        FirErrors.UNRESOLVED_REFERENCE.createOn(
            source,
            ((this.candidateSymbol as? FirCallableSymbol)?.name ?: SpecialNames.NO_NAME_PROVIDED).asString(),
            null,
            session,
        )
    }

    is ConeTypeVisibilityError -> symbol.toInvisibleReferenceDiagnostic(smallestUnresolvablePrefix.last().source, session)
    is ConeVisibilityError -> symbol.toInvisibleReferenceDiagnostic(source, session)
    is ConeInapplicableWrongReceiver -> when (val diagnostic = primaryDiagnostic) {
        is DynamicReceiverExpectedButWasNonDynamic ->
            FirErrors.DYNAMIC_RECEIVER_EXPECTED_BUT_WAS_NON_DYNAMIC.createOn(source, diagnostic.actualType, session)
        else -> FirErrors.UNRESOLVED_REFERENCE_WRONG_RECEIVER.createOn(source, this.candidateSymbols, session)
    }
    is ConeNoCompanionObject -> FirErrors.NO_COMPANION_OBJECT.createOn(source, this.candidateSymbol as FirClassLikeSymbol<*>, session)
    is ConeAmbiguityError -> @OptIn(ApplicabilityDetail::class) when {
        // Don't report ambiguity when some non-lambda, non-callable-reference argument has an error type
        candidates.all {
            if (it !is AbstractCallCandidate<*>) return@all false
            // Ambiguous candidates may be not fully processed, so argument mapping may be not initialized
            if (!it.argumentMappingInitialized) return@all false
            it.argumentMapping.keys.any(AbstractConeResolutionAtom::containsErrorTypeForSuppressingAmbiguityError) ||
                    it.contextArguments?.any(AbstractConeResolutionAtom::containsErrorTypeForSuppressingAmbiguityError) == true ||
                    it.chosenExtensionReceiver?.containsErrorTypeForSuppressingAmbiguityError() == true
        } -> null
        applicability.isSuccess -> FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY.createOn(source, this.candidates.map { it.symbol }, session)
        applicability == CandidateApplicability.UNSAFE_CALL -> {
            val diagnosticAndCandidate = candidates.firstNotNullOfOrNull {
                (it as? AbstractCallCandidate<*>)?.diagnostics?.firstIsInstanceOrNull<InapplicableNullableReceiver>()?.to(it)
            }
            if (diagnosticAndCandidate != null) {
                mapInapplicableNullableReceiver(
                    diagnosticAndCandidate.second,
                    diagnosticAndCandidate.first,
                    source,
                    callOrAssignmentSource,
                    session
                )
            } else {
                FirErrors.NONE_APPLICABLE.createOn(source, this.candidates.map { it.symbol }, session)
            }
        }

        applicability == CandidateApplicability.UNSTABLE_SMARTCAST -> {
            val unstableSmartcast = this.candidates.firstNotNullOf {
                (it as? AbstractCallCandidate<*>)?.diagnostics?.firstIsInstanceOrNull<UnstableSmartCast>()
            }
            unstableSmartcast.mapUnstableSmartCast(session)
        }

        else -> FirErrors.NONE_APPLICABLE.createOn(source, this.candidates.map { it.symbol }, session)
    }

    is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.createOn(source, this.candidateSymbols, session)
    is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.createOn(source, session)

    is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.createOn(
        this.source ?: source,
        "for type parameters",
        session
    )
    is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.createOn(source, this.name.asString(), session)
    is ConePlaceholderProjectionInQualifierResolution -> FirErrors.PLACEHOLDER_PROJECTION_IN_QUALIFIER.createOn(source, session)
    is ConeWrongNumberOfTypeArgumentsError ->
        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.createOn(this.source, this.desiredCount, this.symbol, session)
    is ConeTypeArgumentsNotAllowedOnPackageError ->
        FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.createOn(this.source, "for packages", session)
    is ConeTypeArgumentsForOuterClassWhenNestedReferencedError ->
        FirErrors.TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED.createOn(this.source, session)
    is ConeNestedClassAccessedViaInstanceReference ->
        FirErrors.NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.createOn(this.source, this.symbol, session)

    is ConeOuterClassArgumentsRequired ->
        FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED.createOn(callOrAssignmentSource ?: source, this.symbol, session)

    is ConeNoTypeArgumentsOnRhsError ->
        FirErrors.NO_TYPE_ARGUMENTS_ON_RHS.createOn(callOrAssignmentSource ?: source, this.desiredCount, this.symbol, session)

    is ConeSyntaxDiagnostic -> FirSyntaxErrors.SYNTAX.createOn(callOrAssignmentSource ?: source, reason, session)

    is ConeSimpleDiagnostic -> when {
        (source?.kind as? KtFakeSourceElementKind)?.shouldIgnoreSimpleDiagnostic == true -> null
        else -> this.getFactory(source).createOn(callOrAssignmentSource ?: source, session)
    }

    is ConeDestructuringDeclarationsOnTopLevel -> null // TODO Currently a parsing error. Would be better to report here instead KT-58563
    is ConeCannotInferTypeParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE.createOn(source, this.typeParameter, session)
    is ConeCannotInferValueParameterType -> when {
        isTopLevelLambda -> FirErrors.VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE.createOn(source, session)
        source?.elementType == KtNodeTypes.THIS_EXPRESSION -> FirErrors.CANNOT_INFER_RECEIVER_PARAMETER_TYPE.createOn(source, session)
        else -> (this.valueParameter ?: valueParameter?.symbol)?.takeIf { !it.name.isSpecial }?.let { valueParameterSymbol ->
            FirErrors.CANNOT_INFER_VALUE_PARAMETER_TYPE.createOn(
                source, valueParameterSymbol, session
            )
        } ?: FirErrors.CANNOT_INFER_IT_PARAMETER_TYPE.createOn(source, session)
    }
    is ConeCannotInferReceiverParameterType -> FirErrors.CANNOT_INFER_RECEIVER_PARAMETER_TYPE.createOn(source, session)
    is ConeTypeVariableTypeIsNotInferred -> FirErrors.INFERENCE_ERROR.createOn(callOrAssignmentSource ?: source, session)
    is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.createOn(source, this.target, session)
    is ConeUnreportedDuplicateDiagnostic -> null // Unreported because we always report something different
    is ConeIntermediateDiagnostic -> null // At least some usages are accounted in FirMissingDependencyClassChecker
    is ConeContractDescriptionError -> FirErrors.ERROR_IN_CONTRACT_DESCRIPTION.createOn(source, this.reason, session)
    is ConeTypeParameterSupertype -> FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.createOn(source, this.reason, session)
    is ConeTypeParameterInQualifiedAccess -> null // reported in various checkers instead
    is ConeNotAnnotationContainer -> null // Reported in FirAnnotationExpressionChecker.checkAnnotationUsedAsAnnotationArgument
    is ConeImportFromSingleton -> FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.createOn(source, this.name, session)
    is ConeUnsupported -> FirErrors.UNSUPPORTED.createOn(this.source ?: source, this.reason, session)
    is ConeLocalVariableNoTypeOrInitializer -> runIf(variable.isLocalMember) { // Top/Class-level declarations are handled in FirTopLevelPropertiesChecker
        FirErrors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.createOn(source, session)
    }
    is ConeForbiddenIntersection -> null // reported in FirDefinitelyNotNullableChecker

    is ConeUnderscoreIsReserved -> FirErrors.UNDERSCORE_IS_RESERVED.createOn(this.source, session)
    is ConeAmbiguousSuper -> FirErrors.AMBIGUOUS_SUPER.createOn(source, this.candidateTypes, session)
    is ConeUnresolvedParentInImport -> null // reported in FirUnresolvedImportChecker
    is ConeAmbiguousAlteredAssign -> FirErrors.AMBIGUOUS_ALTERED_ASSIGN.createOn(source, this.altererNames, session)
    is ConeAmbiguouslyResolvedAnnotationFromPlugin -> {
        FirErrors.COMPILER_REQUIRED_ANNOTATION_AMBIGUITY.createOn(source, typeFromCompilerPhase, typeFromTypesPhase, session)
    }
    is ConeAmbiguouslyResolvedAnnotationArgument ->
        FirErrors.AMBIGUOUS_ANNOTATION_ARGUMENT.createOn(
            source,
            listOfNotNull(symbolFromCompilerPhase, symbolFromAnnotationArgumentsPhase),
            session
        )
    is ConeAmbiguousFunctionTypeKinds -> FirErrors.AMBIGUOUS_FUNCTION_TYPE_KIND.createOn(source, kinds, session)
    is ConeUnsupportedClassLiteralsWithEmptyLhs -> FirErrors.UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS.createOn(source, session)
    is ConeMultipleLabelsAreForbidden -> FirErrors.MULTIPLE_LABELS_ARE_FORBIDDEN.createOn(this.source, session)
    is ConeNoInferTypeMismatch -> FirErrors.TYPE_MISMATCH.createOn(source, lowerType, upperType, false, session)
    is ConeTypeMismatch -> FirErrors.TYPE_MISMATCH.createOn(source, lowerType, upperType, false, session)
    is ConeDynamicUnsupported -> FirErrors.UNSUPPORTED.createOn(source, FirDynamicUnsupportedChecker.MESSAGE, session)
    is ConeContextParameterWithDefaultValue -> FirErrors.CONTEXT_PARAMETER_WITH_DEFAULT.createOn(source, session)
    is ConeCyclicTypeBound -> null // reported in FirCyclicTypeBoundsChecker
    else -> throw IllegalArgumentException("Unsupported diagnostic type: ${this.javaClass}")
}

private fun AbstractConeResolutionAtom.containsErrorTypeForSuppressingAmbiguityError(): Boolean {
    val arg = expression
    return arg.resolvedType.hasError() && arg !is FirAnonymousFunctionExpression && arg !is FirCallableReferenceAccess
}

/**
 * `DelegatingConstructorCall` because in this case there is either `UNRESOLVED_REFERENCE` or `SYNTAX` already.
 * `ErrorTypeRef` because in this case there is `SYNTAX` already.
 * */
private val KtFakeSourceElementKind.shouldIgnoreSimpleDiagnostic: Boolean
    get() = this == KtFakeSourceElementKind.DelegatingConstructorCall
            || this == KtFakeSourceElementKind.ErrorTypeRef

fun FirBasedSymbol<*>.toInvisibleReferenceDiagnostic(source: KtSourceElement?, session: FirSession): KtDiagnostic? =
    when (val symbol = this) {
        is FirCallableSymbol<*> -> FirErrors.INVISIBLE_REFERENCE.createOn(source, symbol, symbol.visibility, symbol.callableId.classId, session)
        is FirClassLikeSymbol<*> -> FirErrors.INVISIBLE_REFERENCE.createOn(
            source,
            symbol,
            symbol.visibility,
            symbol.classId.outerClassId,
            session
        )
        else -> shouldNotBeCalled("Unexpected receiver $javaClass")
    }

fun ConeDiagnostic.toFirDiagnostics(
    session: FirSession,
    source: KtSourceElement?,
    callOrAssignmentSource: KtSourceElement?,
    valueParameter: FirValueParameter? = null,
): List<KtDiagnostic> {
    return when (this) {
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, this, source, callOrAssignmentSource)
        is ConeConstraintSystemHasContradiction -> mapSystemHasContradictionError(session, this, source, callOrAssignmentSource)
        else -> listOfNotNull(toKtDiagnostic(source, valueParameter, callOrAssignmentSource, session))
    }
}

private fun mapInapplicableNullableReceiver(
    candidate: AbstractCallCandidate<*>,
    rootCause: InapplicableNullableReceiver,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
    session: FirSession,
): KtDiagnostic? {
    if (candidate.callInfo.isImplicitInvoke) {
        return FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL.createOn(source, rootCause.actualType, session)
    }

    val candidateFunctionSymbol = candidate.symbol as? FirNamedFunctionSymbol
    val candidateFunctionName = candidateFunctionSymbol?.name
    val receiverExpression = candidate.callInfo.explicitReceiver
    val singleArgument = candidate.callInfo.argumentList.arguments.singleOrNull()
    if (receiverExpression != null && singleArgument != null &&
        (source?.elementType == KtNodeTypes.OPERATION_REFERENCE || source?.elementType == KtNodeTypes.BINARY_EXPRESSION) &&
        (candidateFunctionSymbol?.isOperator == true || candidateFunctionSymbol?.isInfix == true)
    ) {
        // For augmented assignment operations (e.g., `a += b`), the source is the entire binary expression (BINARY_EXPRESSION).
        // TODO, KT-59809: No need to check for source.elementType == BINARY_EXPRESSION if we use operator as callee reference source
        //  (see FirExpressionsResolveTransformer.transformAssignmentOperatorStatement)
        val operationSource = if (source?.elementType == KtNodeTypes.BINARY_EXPRESSION) {
            source?.getChild(KtNodeTypes.OPERATION_REFERENCE)
        } else {
            source
        }
        return if (operationSource?.getChild(KtTokens.IDENTIFIER) != null) {
            FirErrors.UNSAFE_INFIX_CALL.createOn(
                source,
                rootCause.actualType,
                receiverExpression,
                candidateFunctionName!!.asString(),
                singleArgument.takeIf { it.source != null },
                session,
            )
        } else {
            FirErrors.UNSAFE_OPERATOR_CALL.createOn(
                source,
                rootCause.actualType,
                receiverExpression,
                candidateFunctionName!!.asString(),
                singleArgument.takeIf { it.source != null },
                session,
            )
        }
    }
    return if (source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference) {
        FirErrors.UNSAFE_CALL.createOn(source, rootCause.actualType, receiverExpression, session)
    } else {
        FirErrors.UNSAFE_CALL.createOn(qualifiedAccessSource ?: source, rootCause.actualType, receiverExpression, session)
    }
}

private fun mapInapplicableCandidateError(
    session: FirSession,
    diagnostic: ConeInapplicableCandidateError,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
): List<KtDiagnostic> {
    val typeContext = session.typeContext
    val genericDiagnostic = FirErrors.INAPPLICABLE_CANDIDATE.createOn(source, diagnostic.candidate.symbol, session)

    val diagnostics = diagnostic.candidate.diagnostics.filter { !it.isSuccess }.mapNotNull { rootCause ->
        when (rootCause) {
            is VarargArgumentOutsideParentheses -> FirErrors.VARARG_OUTSIDE_PARENTHESES.createOn(
                rootCause.argument.source ?: qualifiedAccessSource,
                session
            )

            is NamedArgumentNotAllowed -> FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.createOn(
                rootCause.argument.source,
                rootCause.forbiddenNamedArgumentsTarget,
                session
            )

            is MixingNamedAndPositionArguments -> FirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS.createOn(
                rootCause.argument.source,
                session,
            )

            is ArgumentTypeMismatch -> {
                if (!diagnostic.candidate.usedOuterCs && rootCause.systemHadContradiction) return@mapNotNull null
                diagnosticForArgumentTypeMismatch(
                    source = rootCause.argument.source ?: source,
                    expectedType = rootCause.expectedType.substituteTypeVariableTypes(
                        diagnostic.candidate,
                        typeContext,
                    ),
                    // For lambda expressions, use their resolved type because `rootCause.actualType` can contain unresolved types
                    actualType = if (rootCause.argument is FirAnonymousFunctionExpression && !rootCause.argument.resolvedType.hasError()) {
                        rootCause.argument.resolvedType
                    } else {
                        rootCause.actualType.substituteTypeVariableTypes(
                            diagnostic.candidate,
                            typeContext,
                        )
                    },
                    isMismatchDueToNullability = rootCause.isMismatchDueToNullability,
                    candidate = diagnostic.candidate,
                    rootCause.anonymousFunctionIfReturnExpression,
                    session,
                )
            }

            is UnitReturnTypeLambdaContradictsExpectedType -> {
                FirErrors.ARGUMENT_TYPE_MISMATCH.createOn(
                    rootCause.sourceForFunctionExpression ?: rootCause.lambda.source ?: source,
                    rootCause.lambda.typeRef.coneType.substituteTypeVariableTypes(
                        diagnostic.candidate,
                        typeContext,
                    ),
                    rootCause.wholeLambdaExpectedType.substituteTypeVariableTypes(
                        diagnostic.candidate,
                        typeContext,
                    ),
                    false,
                    session // not isMismatchDueToNullability
                )
            }

            // We don't report anything here, because there are already some errors inside the call or declaration
            // And the errors should be reported there
            is ErrorTypeInArguments -> null

            // see EagerResolveOfCallableReferences
            is UnsuccessfulCallableReferenceArgument -> null

            is MultipleContextReceiversApplicableForExtensionReceivers ->
                FirErrors.AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER.createOn(qualifiedAccessSource ?: source, session)

            is NoReceiverAllowed -> FirErrors.NO_RECEIVER_ALLOWED.createOn(qualifiedAccessSource ?: source, session)

            is NoContextArgument ->
                FirErrors.NO_CONTEXT_ARGUMENT.createOn(
                    qualifiedAccessSource ?: source,
                    rootCause.symbol,
                    session
                )

            is UnsupportedContextualDeclarationCall -> FirErrors.UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL.createOn(source, session)

            is AmbiguousContextArgument ->
                FirErrors.AMBIGUOUS_CONTEXT_ARGUMENT.createOn(
                    qualifiedAccessSource ?: source,
                    rootCause.symbol,
                    session
                )

            is TypeVariableAsExplicitReceiver -> {
                val typeParameter = rootCause.typeParameter
                FirErrors.BUILDER_INFERENCE_STUB_RECEIVER.createOn(
                    rootCause.explicitReceiver.source,
                    typeParameter.symbol.name,
                    typeParameter.symbol.containingDeclarationSymbol.memberDeclarationNameOrNull
                        ?: error("containingDeclarationSymbol must have been a member declaration"),
                    session
                )
            }

            is NullForNotNullType -> FirErrors.NULL_FOR_NONNULL_TYPE.createOn(
                rootCause.argument.source ?: source, rootCause.expectedType.substituteTypeVariableTypes(
                    diagnostic.candidate,
                    typeContext,
                ), session
            )

            is NonVarargSpread -> FirErrors.NON_VARARG_SPREAD.createOn(
                rootCause.argument.source?.getChild(KtTokens.MUL, depth = 1)!!,
                session
            )
            is ArgumentPassedTwice -> FirErrors.ARGUMENT_PASSED_TWICE.createOn(rootCause.argument.source, session)
            is TooManyArguments ->
                unexpectedTrailingLambdaOnNewLineOrNull(rootCause.argument, session)
                    ?: FirErrors.TOO_MANY_ARGUMENTS.createOn(
                rootCause.argument.source ?: source,
                rootCause.function.symbol,
                session
            )
            is NoValueForParameter -> FirErrors.NO_VALUE_FOR_PARAMETER.createOn(
                qualifiedAccessSource ?: source,
                rootCause.valueParameter.symbol,
                session
            )

            is NameNotFound -> FirErrors.NAMED_PARAMETER_NOT_FOUND.createOn(
                rootCause.argument.source ?: source,
                rootCause.argument.name.asString(),
                session
            )

            is NameForAmbiguousParameter -> FirErrors.NAME_FOR_AMBIGUOUS_PARAMETER.createOn(
                rootCause.argument.source ?: source,
                session
            )

            is InapplicableNullableReceiver -> mapInapplicableNullableReceiver(
                diagnostic.candidate,
                rootCause,
                source,
                qualifiedAccessSource,
                session
            )
            is ManyLambdaExpressionArguments ->
                unexpectedTrailingLambdaOnNewLineOrNull(rootCause.argument, session)
                    ?: FirErrors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.createOn(
                rootCause.argument.source ?: source,
                session
            )
            is InfixCallOfNonInfixFunction -> FirErrors.INFIX_MODIFIER_REQUIRED.createOn(source, rootCause.function, session)
            is OperatorCallOfNonOperatorFunction ->
                FirErrors.OPERATOR_MODIFIER_REQUIRED.createOn(source, rootCause.function, rootCause.function.name.asString(), session)

            is OperatorCallOfConstructor -> FirErrors.OPERATOR_CALL_ON_CONSTRUCTOR.createOn(
                source,
                rootCause.constructor.name.asString(),
                session
            )
            is UnstableSmartCast -> rootCause.mapUnstableSmartCast(session)

            is DslScopeViolation -> FirErrors.DSL_SCOPE_VIOLATION.createOn(source, rootCause.calleeSymbol, session)
            is InferenceError -> {
                rootCause.constraintError.toDiagnostic(
                    source,
                    qualifiedAccessSource,
                    session,
                    diagnostic.candidate
                )
            }

            is InferredEmptyIntersectionDiagnostic -> reportInferredIntoEmptyIntersection(
                source,
                rootCause.typeVariable,
                rootCause.incompatibleTypes,
                rootCause.causingTypes,
                rootCause.kind,
                isError = rootCause.isError,
                session
            )

            is AdaptedCallableReferenceIsUsedWithReflection -> FirErrors.ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE.createOn(
                qualifiedAccessSource,
                session
            )

            // Reported later
            is TypeParameterAsExpression -> null

            is AmbiguousInterceptedSymbol -> FirErrors.PLUGIN_AMBIGUOUS_INTERCEPTED_SYMBOL.createOn(source, rootCause.pluginNames, session)

            is MissingInnerClassConstructorReceiver -> FirErrors.INNER_CLASS_CONSTRUCTOR_NO_RECEIVER.createOn(
                qualifiedAccessSource ?: source,
                rootCause.candidateSymbol,
                session
            )

            is WrongNumberOfTypeArguments -> FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.createOn(
                qualifiedAccessSource ?: source,
                rootCause.desiredCount, rootCause.symbol, session
            )

            else -> genericDiagnostic
        }
    }.distinct()
    return if (diagnostics.size > 1) {
        // If there are more specific diagnostics, filter out the generic diagnostic.
        diagnostics.filter { it != genericDiagnostic }
    } else {
        diagnostics
    }
}

private fun unexpectedTrailingLambdaOnNewLineOrNull(argument: FirExpression, session: FirSession): KtSimpleDiagnostic? {
    return when (val argumentSource = argument.source) {
        is KtPsiSourceElement if argumentSource.psi.let { it is KtLambdaExpression && it.isTrailingLambdaOnNewLine }
            -> FirErrors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.createOn(argumentSource, session)
        is KtLightSourceElement if argumentSource.isTrailingLambdaOnNewLine()
            -> FirErrors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.createOn(argumentSource, session)
        else
            -> null
    }
}

private fun KtLightSourceElement.isTrailingLambdaOnNewLine(): Boolean {
    val parent = treeStructure.getParent(this.lighterASTNode)
    if (parent?.tokenType == KtStubElementTypes.LAMBDA_ARGUMENT) {
        var prevSibling = parent.getPreviousSibling(treeStructure)
        while (prevSibling != null && prevSibling.tokenType !is KtStubElementType<*, *>) {
            if (prevSibling.tokenType == TokenType.WHITE_SPACE && prevSibling is LighterASTTokenNode && prevSibling.text.contains("\n")) {
                return true
            }
            prevSibling = prevSibling.getPreviousSibling(treeStructure)
        }
    }
    return false
}

private fun diagnosticForArgumentTypeMismatch(
    source: KtSourceElement?,
    expectedType: ConeKotlinType,
    actualType: ConeKotlinType,
    isMismatchDueToNullability: Boolean,
    candidate: AbstractCallCandidate<*>,
    /**
     * See [org.jetbrains.kotlin.fir.resolve.calls.ArgumentTypeMismatch.anonymousFunctionIfReturnExpression]
     */
    anonymousFunctionIfReturnExpression: FirAnonymousFunction?,
    session: FirSession,
): KtDiagnostic? {
    val symbol = candidate.symbol as FirCallableSymbol
    val receiverType = (candidate.chosenExtensionReceiver ?: candidate.dispatchReceiver)?.expression?.resolvedType

    return when {
        anonymousFunctionIfReturnExpression != null ->
            FirErrors.RETURN_TYPE_MISMATCH.createOn(
                source, expectedType, actualType, anonymousFunctionIfReturnExpression, isMismatchDueToNullability, session
            )
        expectedType is ConeCapturedType && expectedType.isBasedOnStarOrOut() && receiverType != null ->
            FirErrors.MEMBER_PROJECTED_OUT.createOn(
                source,
                receiverType,
                expectedType.projectionKindAsString(),
                symbol.originalOrSelf(),
                session,
            )
        else -> FirErrors.ARGUMENT_TYPE_MISMATCH.createOn(
            source,
            actualType,
            expectedType,
            isMismatchDueToNullability,
            session
        )
    }
}

private fun ConeCapturedType.isBasedOnStarOrOut(): Boolean =
    constructor.projection.kind.let { it == ProjectionKind.OUT || it == ProjectionKind.STAR }

private fun UnstableSmartCast.mapUnstableSmartCast(session: FirSession): KtDiagnosticWithParameters4<ConeKotlinType, FirExpression, String, Boolean>? {
    val factory = when {
        isImplicitInvokeReceiver -> FirErrors.SMARTCAST_IMPOSSIBLE_ON_IMPLICIT_INVOKE_RECEIVER
        else -> FirErrors.SMARTCAST_IMPOSSIBLE
    }
    return factory.createOn(
        argument.source,
        targetType,
        argument,
        argument.smartcastStability.description,
        isCastToNotNull,
        session
    )
}

private fun mapSystemHasContradictionError(
    session: FirSession,
    diagnostic: ConeConstraintSystemHasContradiction,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
): List<KtDiagnostic> {
    val candidate = diagnostic.candidate
    val errors = candidate.errors
    return buildList {
        for (error in errors) {
            addIfNotNull(
                error.toDiagnostic(
                    source,
                    qualifiedAccessSource,
                    session,
                    candidate,
                )
            )
        }
    }.ifEmpty {
        // Check if we already have some other reported error
        if (errors.any {
                when (it) {
                    // Error should be reported on the error type itself
                    is ConstrainingTypeIsError -> true
                    // In this case we will have an error type with a reported error either as:
                    // - return type of some synthetic call (if/try/!!/?:)
                    // - type argument of some qualified access
                    // ...or, we have a delegated constructor call with an error reported separately,
                    // see ConstraintSystemError.toDiagnostic, branch isNotEnoughInformationForTypeParameter
                    is NotEnoughInformationForTypeParameter<*> -> it.typeVariable is ConeTypeParameterBasedTypeVariable ||
                            // ... or, we will report a diagnostic on this type inside ErrorNodeDiagnosticCollectorComponent
                            (it.resolvedAtom as? FirAnonymousFunction)?.containsErrorType() == true
                    else -> false
                }
            }
        ) return emptyList()
        listOfNotNull(
            errors.firstNotNullOfOrNull {
                if (it is NewConstraintError) {
                    if (it.position.from is FixVariableConstraintPosition<*>) {
                        val morePreciseDiagnosticExists = errors.any { other ->
                            other is NewConstraintError && other.position.from !is FixVariableConstraintPosition<*>
                        }
                        if (morePreciseDiagnosticExists) return@firstNotNullOfOrNull null
                    }
                    val typeContext = session.typeContext
                    FirErrors.TYPE_MISMATCH.createOn(
                        qualifiedAccessSource ?: source,
                        it.upperConeType.substituteTypeVariableTypes(candidate, typeContext),
                        it.lowerConeType.substituteTypeVariableTypes(candidate, typeContext),
                        false,
                        session
                    )
                } else {
                    // This branch is probably dead, at least we don't have any coverage in test data
                    val message = "Inference error: ${it::class.simpleName}"
                    FirErrors.NEW_INFERENCE_ERROR.createOn(qualifiedAccessSource ?: source, message, session)
                }
            }
        )
    }
}

private fun FirAnonymousFunction.containsErrorType(): Boolean =
    returnTypeRef is FirErrorTypeRef || receiverType is ConeErrorType ||
            valueParameters.any { it.returnTypeRef is FirErrorTypeRef } ||
            contextParameters.any { it.returnTypeRef is FirErrorTypeRef }

private fun ConstraintSystemError.toDiagnostic(
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
    session: FirSession,
    candidate: AbstractCallCandidate<*>,
): KtDiagnostic? {
    val typeContext = session.typeContext
    return when (this) {
        is NewConstraintError -> {
            val position = position.from
            val (argument, reportOn) =
                when (position) {
                    is ConeArgumentConstraintPosition -> position.argument to null
                    is ConeLambdaArgumentConstraintPosition -> position.lambda to null
                    is ConeReceiverConstraintPosition -> position.argument to (position.argument.source.takeIf { it?.kind == KtRealSourceElementKind }
                        ?: position.source)
                    // ConeExpectedTypeConstraintPosition is processed below,
                    // all others are reported as NEW_INFERENCE_ERROR instead (see mapSystemHasContradictionError);
                    // About calls from mapInapplicableCandidateError:
                    // - ConeExplicitTypeParameterConstraintPosition is reported as UPPER_BOUND_VIOLATED
                    // (see e.g. testCheckEnhancedUpperBounds)
                    // - ConeFixVariableConstraintPosition is occurred in delegates only,
                    // and reported as DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE (see testSuccessfulProvideDelegateLeadsToRedGetValue)
                    // Finally, ConeDeclaredUpperBoundConstraintPosition never occurs here
                    else -> null to null
                }

            val typeMismatchDueToNullability = typeContext.isTypeMismatchDueToNullability(lowerConeType, upperConeType)
            argument?.let {
                return diagnosticForArgumentTypeMismatch(
                    source = reportOn ?: it.source ?: source,
                    expectedType = upperConeType.substituteTypeVariableTypes(candidate, typeContext),
                    actualType = lowerConeType.substituteTypeVariableTypes(candidate, typeContext),
                    isMismatchDueToNullability = typeMismatchDueToNullability,
                    candidate = candidate,
                    anonymousFunctionIfReturnExpression = (position as? ConeLambdaArgumentConstraintPosition)?.lambda,
                    session = session,
                )
            }

            when (position) {
                is ConeExpectedTypeConstraintPosition -> {
                    val inferredType =
                        if (!lowerConeType.isNullableNothing)
                            lowerConeType
                        else
                            upperConeType.withNullability(nullable = true, typeContext)

                    FirErrors.TYPE_MISMATCH.createOn(
                        qualifiedAccessSource ?: source,
                        upperConeType.substituteTypeVariableTypes(candidate, typeContext),
                        inferredType.substituteTypeVariableTypes(candidate, typeContext),
                        typeMismatchDueToNullability,
                        session
                    )
                }

                else -> null
            }
        }

        // Always reported as CANNOT_INFER_PARAMETER_TYPE except (!) delegated constructor calls
        is NotEnoughInformationForTypeParameter<*> -> if (candidate.symbol is FirConstructorSymbol &&
            candidate.callInfo.callSite is FirDelegatedConstructorCall
        ) {
            FirErrors.CANNOT_INFER_PARAMETER_TYPE.createOn(
                source,
                ((this.typeVariable as ConeTypeVariable).typeConstructor.originalTypeParameter as ConeTypeParameterLookupTag).typeParameterSymbol,
                session
            )
        } else null

        is InferredEmptyIntersection -> {
            val typeVariable = typeVariable as ConeTypeVariable
            val narrowedSource = candidate.sourceOfCallToSymbolWith(typeVariable)

            @Suppress("UNCHECKED_CAST")
            reportInferredIntoEmptyIntersection(
                narrowedSource ?: source,
                typeVariable,
                incompatibleTypes as Collection<ConeKotlinType>,
                causingTypes as Collection<ConeKotlinType>,
                kind,
                this is InferredEmptyIntersectionError,
                session,
            )
        }

        is OnlyInputTypesDiagnostic -> {
            FirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR.createOn(
                source,
                (typeVariable as ConeTypeParameterBasedTypeVariable).typeParameterSymbol,
                session
            )
        }

        is AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction -> {
            val typeParameterSymbol = (typeParameter as ConeTypeParameterLookupTag).typeParameterSymbol
            FirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.createOn(
                anonymous.source ?: source,
                typeParameterSymbol.name,
                typeParameterSymbol.containingDeclarationSymbol.memberDeclarationNameOrNull
                    ?: error("containingDeclarationSymbol must have been a member declaration"),
                session
            )
        }

        is MultiLambdaBuilderInferenceRestriction<*> -> shouldNotBeCalled()

        else -> null
    }
}

private fun ConeKotlinType.substituteTypeVariableTypes(
    candidate: AbstractCallCandidate<*>,
    typeContext: ConeTypeContext,
): ConeKotlinType {
    val nonErrorSubstitutionMap = candidate.system.asReadOnlyStorage().fixedTypeVariables.filterValues { it !is ConeErrorType }
    val substitutor = typeContext.typeSubstitutorByTypeConstructor(nonErrorSubstitutionMap) as ConeSubstitutor

    return substitutor.substituteOrSelf(this).removeTypeVariableTypes(typeContext, TypeVariableReplacement.ErrorType)
}

private fun AbstractCallCandidate<*>.sourceOfCallToSymbolWith(
    typeVariable: ConeTypeVariable,
): KtSourceElement? {
    if (typeVariable !is ConeTypeParameterBasedTypeVariable) return null

    var narrowedSource: KtSourceElement? = null

    callInfo.callSite.accept(object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            if (narrowedSource != null) return

            if (element is FirQualifiedAccessExpression) {
                val symbol = element.calleeReference.toResolvedCallableSymbol()
                if (symbol != null && symbol.typeParameterSymbols.contains(typeVariable.typeParameterSymbol)) {
                    narrowedSource = element.calleeReference.source
                    return
                }
            }

            element.acceptChildren(this)
        }
    }, null)

    return narrowedSource
}

private fun reportInferredIntoEmptyIntersection(
    source: KtSourceElement?,
    typeVariable: ConeTypeVariable,
    incompatibleTypes: Collection<ConeKotlinType>,
    causingTypes: Collection<ConeKotlinType>,
    kind: EmptyIntersectionTypeKind,
    isError: Boolean,
    session: FirSession
): KtDiagnostic? {
    val typeVariableText =
        (typeVariable.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag)?.name?.asString()
            ?: typeVariable.toString()
    val causingTypesText = if (incompatibleTypes == causingTypes) "" else ": ${causingTypes.joinToString()}"
    val factory =
        when {
            !kind.isDefinitelyEmpty -> FirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION
            isError -> FirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.errorFactory
            else -> FirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.warningFactory
        }

    return factory.createOn(source, typeVariableText, incompatibleTypes, kind.description, causingTypesText, session)
}

private val NewConstraintError.lowerConeType: ConeKotlinType get() = lowerType as ConeKotlinType
private val NewConstraintError.upperConeType: ConeKotlinType get() = upperType as ConeKotlinType

private fun ConeSimpleDiagnostic.getFactory(source: KtSourceElement?): KtDiagnosticFactory0 {
    return when (kind) {
        DiagnosticKind.ReturnNotAllowed -> FirErrors.RETURN_NOT_ALLOWED
        DiagnosticKind.NotAFunctionLabel -> FirErrors.NOT_A_FUNCTION_LABEL
        DiagnosticKind.UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
        DiagnosticKind.AmbiguousLabel -> FirErrors.AMBIGUOUS_LABEL
        DiagnosticKind.LabelNameClash -> FirErrors.LABEL_NAME_CLASH
        DiagnosticKind.NoThis -> FirErrors.NO_THIS
        DiagnosticKind.IllegalConstExpression -> FirErrors.ILLEGAL_CONST_EXPRESSION
        DiagnosticKind.IllegalUnderscore -> FirErrors.ILLEGAL_UNDERSCORE
        DiagnosticKind.DeserializationError -> FirErrors.DESERIALIZATION_ERROR
        DiagnosticKind.InferenceError -> FirErrors.INFERENCE_ERROR
        DiagnosticKind.RecursionInImplicitTypes -> FirErrors.RECURSION_IN_IMPLICIT_TYPES
        DiagnosticKind.Java -> FirErrors.ERROR_FROM_JAVA_RESOLUTION
        DiagnosticKind.SuperNotAllowed -> FirErrors.SUPER_IS_NOT_AN_EXPRESSION
        DiagnosticKind.ExpressionExpected -> when (source?.elementType) {
            KtNodeTypes.BINARY_EXPRESSION -> FirErrors.ASSIGNMENT_IN_EXPRESSION_CONTEXT
            KtNodeTypes.FUN -> FirErrors.ANONYMOUS_FUNCTION_WITH_NAME
            KtNodeTypes.WHEN_CONDITION_IN_RANGE, KtNodeTypes.WHEN_CONDITION_IS_PATTERN, KtNodeTypes.WHEN_CONDITION_EXPRESSION ->
                FirErrors.EXPECTED_CONDITION
            else -> FirErrors.EXPRESSION_EXPECTED
        }

        DiagnosticKind.JumpOutsideLoop -> FirErrors.BREAK_OR_CONTINUE_OUTSIDE_A_LOOP
        DiagnosticKind.NotLoopLabel -> FirErrors.NOT_A_LOOP_LABEL
        DiagnosticKind.VariableExpected -> FirErrors.VARIABLE_EXPECTED
        DiagnosticKind.ValueParameterWithNoTypeAnnotation -> FirErrors.VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE
        DiagnosticKind.IllegalProjectionUsage -> FirErrors.ILLEGAL_PROJECTION_USAGE
        DiagnosticKind.MissingStdlibClass -> FirErrors.MISSING_STDLIB_CLASS
        DiagnosticKind.IntLiteralOutOfRange -> FirErrors.INT_LITERAL_OUT_OF_RANGE
        DiagnosticKind.FloatLiteralOutOfRange -> FirErrors.FLOAT_LITERAL_OUT_OF_RANGE
        DiagnosticKind.WrongLongSuffix -> FirErrors.WRONG_LONG_SUFFIX
        DiagnosticKind.UnsignedNumbersAreNotPresent -> FirErrors.UNSIGNED_LITERAL_WITHOUT_DECLARATIONS_ON_CLASSPATH
        DiagnosticKind.IncorrectCharacterLiteral -> FirErrors.INCORRECT_CHARACTER_LITERAL
        DiagnosticKind.EmptyCharacterLiteral -> FirErrors.EMPTY_CHARACTER_LITERAL
        DiagnosticKind.TooManyCharactersInCharacterLiteral -> FirErrors.TOO_MANY_CHARACTERS_IN_CHARACTER_LITERAL
        DiagnosticKind.IllegalEscape -> FirErrors.ILLEGAL_ESCAPE
        DiagnosticKind.RecursiveTypealiasExpansion -> FirErrors.RECURSIVE_TYPEALIAS_EXPANSION
        DiagnosticKind.LoopInSupertype -> FirErrors.CYCLIC_INHERITANCE_HIERARCHY
        DiagnosticKind.IllegalSelector -> FirErrors.ILLEGAL_SELECTOR
        DiagnosticKind.NoReceiverAllowed -> FirErrors.NO_RECEIVER_ALLOWED
        DiagnosticKind.IsEnumEntry -> FirErrors.IS_ENUM_ENTRY
        DiagnosticKind.EnumEntryAsType -> FirErrors.ENUM_ENTRY_AS_TYPE
        DiagnosticKind.NotASupertype -> FirErrors.NOT_A_SUPERTYPE
        DiagnosticKind.SuperNotAvailable -> FirErrors.SUPER_NOT_AVAILABLE
        DiagnosticKind.AnnotationInWhereClause -> FirErrors.ANNOTATION_IN_WHERE_CLAUSE_ERROR
        DiagnosticKind.MultipleAnnotationWithAllTarget -> FirErrors.INAPPLICABLE_ALL_TARGET_IN_MULTI_ANNOTATION
        DiagnosticKind.UnresolvedSupertype,
        DiagnosticKind.UnresolvedExpandedType,
        DiagnosticKind.Other -> FirErrors.OTHER_ERROR
    }
}


@OptIn(InternalDiagnosticFactoryMethod::class)
private fun KtDiagnosticFactory0.createOn(
    element: KtSourceElement?,
    session: FirSession,
): KtSimpleDiagnostic? {
    return on(element.requireNotNull(), positioningStrategy = null, session.languageVersionSettings)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> KtDiagnosticFactory1<A>.createOn(
    element: KtSourceElement?,
    a: A,
    session: FirSession,
): KtDiagnosticWithParameters1<A>? {
    return on(element.requireNotNull(), a, positioningStrategy = null, session.languageVersionSettings)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B> KtDiagnosticFactory2<A, B>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B,
    session: FirSession,
): KtDiagnosticWithParameters2<A, B>? {
    return on(element.requireNotNull(), a, b, positioningStrategy = null, session.languageVersionSettings)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C> KtDiagnosticFactory3<A, B, C>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B,
    c: C,
    session: FirSession,
): KtDiagnosticWithParameters3<A, B, C>? {
    return on(element.requireNotNull(), a, b, c, positioningStrategy = null, session.languageVersionSettings)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C, D> KtDiagnosticFactory4<A, B, C, D>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B,
    c: C,
    d: D,
    session: FirSession,
): KtDiagnosticWithParameters4<A, B, C, D>? {
    return on(element.requireNotNull(), a, b, c, d, positioningStrategy = null, session.languageVersionSettings)
}
