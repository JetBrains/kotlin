/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.isLocalMember
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.diagnostics.*
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.*
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaReturnType
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeArgumentConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeLambdaArgumentConstraintPosition
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.types.EmptyIntersectionTypeKind
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

private fun ConeDiagnostic.toKtDiagnostic(
    source: KtSourceElement?,
    callOrAssignmentSource: KtSourceElement?
): KtDiagnostic? = when (this) {
    is ConeUnresolvedReferenceError -> FirErrors.UNRESOLVED_REFERENCE.createOn(
        source,
        this.name.asString()
    )

    is ConeUnresolvedSymbolError -> FirErrors.UNRESOLVED_REFERENCE.createOn(source, this.classId.asString())
    is ConeUnresolvedNameError -> FirErrors.UNRESOLVED_REFERENCE.createOn(source, this.name.asString())
    is ConeUnresolvedTypeQualifierError -> FirErrors.UNRESOLVED_REFERENCE.createOn(source, this.qualifier)
    is ConeFunctionCallExpectedError -> FirErrors.FUNCTION_CALL_EXPECTED.createOn(source, this.name.asString(), this.hasValueParameters)
    is ConeFunctionExpectedError -> FirErrors.FUNCTION_EXPECTED.createOn(source, this.expression, this.type)
    is ConeNoConstructorError -> FirErrors.NO_CONSTRUCTOR.createOn(callOrAssignmentSource ?: source)
    is ConeResolutionToClassifierError -> when (this.candidateSymbol.classKind) {
        ClassKind.INTERFACE -> FirErrors.INTERFACE_AS_FUNCTION.createOn(source, this.candidateSymbol)
        ClassKind.CLASS -> when {
            this.candidateSymbol.isInner -> FirErrors.INNER_CLASS_CONSTRUCTOR_NO_RECEIVER.createOn(source, this.candidateSymbol)
            this.candidateSymbol.isExpect -> FirErrors.EXPECT_CLASS_AS_FUNCTION.createOn(source, this.candidateSymbol)
            else -> FirErrors.RESOLUTION_TO_CLASSIFIER.createOn(source, this.candidateSymbol)
        }
        else -> FirErrors.RESOLUTION_TO_CLASSIFIER.createOn(source, this.candidateSymbol)
    }
    is ConeHiddenCandidateError -> {
        // Usages of callables with @Deprecated(DeprecationLevel.HIDDEN) should look like unresolved references.
        // See: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/
        FirErrors.UNRESOLVED_REFERENCE.createOn(
            source,
            ((this.candidateSymbol as? FirCallableSymbol)?.name ?: SpecialNames.NO_NAME_PROVIDED).asString()
        )
    }

    is ConeVisibilityError -> symbol.toInvisibleReferenceDiagnostic(source)
    is ConeInapplicableWrongReceiver -> FirErrors.UNRESOLVED_REFERENCE_WRONG_RECEIVER.createOn(source, this.candidateSymbols)
    is ConeNoCompanionObject -> FirErrors.NO_COMPANION_OBJECT.createOn(source, this.candidateSymbol as FirClassLikeSymbol<*>)
    is ConeAmbiguityError -> when {
        applicability.isSuccess -> FirErrors.OVERLOAD_RESOLUTION_AMBIGUITY.createOn(source, this.candidates.map { it.symbol })
        applicability == CandidateApplicability.UNSAFE_CALL -> {
            val (unsafeCall, candidate) = candidates.firstNotNullOf { it.diagnostics.firstIsInstanceOrNull<UnsafeCall>()?.to(it) }
            mapUnsafeCallError(candidate, unsafeCall, source, callOrAssignmentSource)
        }

        applicability == CandidateApplicability.UNSTABLE_SMARTCAST -> {
            val unstableSmartcast = this.candidates.firstNotNullOf { it.diagnostics.firstIsInstanceOrNull<UnstableSmartCast>() }
            FirErrors.SMARTCAST_IMPOSSIBLE.createOn(
                unstableSmartcast.argument.source,
                unstableSmartcast.targetType,
                unstableSmartcast.argument,
                unstableSmartcast.argument.smartcastStability.description,
                unstableSmartcast.isCastToNotNull
            )
        }

        else -> FirErrors.NONE_APPLICABLE.createOn(source, this.candidates.map { it.symbol })
    }

    is ConeOperatorAmbiguityError -> FirErrors.ASSIGN_OPERATOR_AMBIGUITY.createOn(source, this.candidateSymbols)
    is ConeVariableExpectedError -> FirErrors.VARIABLE_EXPECTED.createOn(source)

    is ConeUnexpectedTypeArgumentsError -> FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.createOn(this.source ?: source)
    is ConeIllegalAnnotationError -> FirErrors.NOT_AN_ANNOTATION_CLASS.createOn(source, this.name.asString())
    is ConeWrongNumberOfTypeArgumentsError ->
        FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS.createOn(this.source, this.desiredCount, this.symbol)

    is ConeOuterClassArgumentsRequired ->
        FirErrors.OUTER_CLASS_ARGUMENTS_REQUIRED.createOn(callOrAssignmentSource ?: source, this.symbol)

    is ConeNoTypeArgumentsOnRhsError ->
        FirErrors.NO_TYPE_ARGUMENTS_ON_RHS.createOn(callOrAssignmentSource ?: source, this.desiredCount, this.symbol)

    is ConeSyntaxDiagnostic -> FirSyntaxErrors.SYNTAX.createOn(callOrAssignmentSource ?: source, reason)

    is ConeSimpleDiagnostic -> when {
        (source?.kind as? KtFakeSourceElementKind)?.canBeIgnored == true -> null
        else -> this.getFactory(source).createOn(callOrAssignmentSource ?: source)
    }

    is ConeDestructuringDeclarationsOnTopLevel -> null // TODO Currently a parsing error. Would be better to report here instead KT-58563
    is ConeCannotInferTypeParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE.createOn(source)
    is ConeCannotInferValueParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE.createOn(source)
    is ConeCannotInferReceiverParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE.createOn(source)
    is ConeTypeVariableTypeIsNotInferred -> FirErrors.INFERENCE_ERROR.createOn(callOrAssignmentSource ?: source)
    is ConeInstanceAccessBeforeSuperCall -> FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL.createOn(source, this.target)
    is ConeStubDiagnostic -> null
    is ConeIntermediateDiagnostic -> null
    is ConeContractDescriptionError -> FirErrors.ERROR_IN_CONTRACT_DESCRIPTION.createOn(source, this.reason)
    is ConeTypeParameterSupertype -> FirErrors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.createOn(source, this.reason)
    is ConeTypeParameterInQualifiedAccess -> null // reported in various checkers instead
    is ConeNotAnnotationContainer -> null
    is ConeImportFromSingleton -> FirErrors.CANNOT_ALL_UNDER_IMPORT_FROM_SINGLETON.createOn(source, this.name)
    is ConeUnsupported -> FirErrors.UNSUPPORTED.createOn(this.source ?: source, this.reason)
    is ConeLocalVariableNoTypeOrInitializer -> runIf(variable.isLocalMember) {
        FirErrors.VARIABLE_WITH_NO_TYPE_NO_INITIALIZER.createOn(source)
    }
    is ConeForbiddenIntersection -> null // reported in FirDefinitelyNotNullableChecker

    is ConeUnderscoreIsReserved -> FirErrors.UNDERSCORE_IS_RESERVED.createOn(this.source)
    is ConeUnderscoreUsageWithoutBackticks -> FirErrors.UNDERSCORE_USAGE_WITHOUT_BACKTICKS.createOn(this.source)
    is ConeAmbiguousSuper -> FirErrors.AMBIGUOUS_SUPER.createOn(source, this.candidateTypes)
    is ConeUnresolvedParentInImport -> null // reported in FirUnresolvedImportChecker
    is ConeAmbiguousAlteredAssign -> FirErrors.AMBIGUOUS_ALTERED_ASSIGN.createOn(source, this.altererNames)
    is ConeAmbiguouslyResolvedAnnotationFromPlugin -> {
        FirErrors.PLUGIN_ANNOTATION_AMBIGUITY.createOn(source, typeFromCompilerPhase, typeFromTypesPhase)
    }
    is ConeAmbiguouslyResolvedAnnotationArgument ->
        FirErrors.AMBIGUOUS_ANNOTATION_ARGUMENT.createOn(source, listOfNotNull(symbolFromCompilerPhase, symbolFromAnnotationArgumentsPhase))
    is ConeAmbiguousFunctionTypeKinds -> FirErrors.AMBIGUOUS_FUNCTION_TYPE_KIND.createOn(source, kinds)
    is ConeUnsupportedClassLiteralsWithEmptyLhs -> FirErrors.UNSUPPORTED_CLASS_LITERALS_WITH_EMPTY_LHS.createOn(source)
    is ConeMissingConstructorKeyword -> FirErrors.MISSING_CONSTRUCTOR_KEYWORD.createOn(source)
    else -> throw IllegalArgumentException("Unsupported diagnostic type: ${this.javaClass}")
}

private val KtFakeSourceElementKind.canBeIgnored: Boolean
    get() = this == KtFakeSourceElementKind.DelegatingConstructorCall
            || this == KtFakeSourceElementKind.ErrorTypeRef

fun FirBasedSymbol<*>.toInvisibleReferenceDiagnostic(source: KtSourceElement?): KtDiagnostic? = when (val symbol = this) {
    is FirCallableSymbol<*> -> FirErrors.INVISIBLE_REFERENCE.createOn(source, symbol, symbol.visibility, symbol.callableId.classId)
    is FirClassLikeSymbol<*> -> FirErrors.INVISIBLE_REFERENCE.createOn(source, symbol, symbol.visibility, symbol.classId.outerClassId)
    else -> shouldNotBeCalled("Unexpected receiver $javaClass")
}

fun ConeDiagnostic.toFirDiagnostics(
    session: FirSession,
    source: KtSourceElement?,
    callOrAssignmentSource: KtSourceElement?
): List<KtDiagnostic> {
    return when (this) {
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, this, source, callOrAssignmentSource)
        is ConeConstraintSystemHasContradiction -> mapSystemHasContradictionError(session, this, source, callOrAssignmentSource)
        else -> listOfNotNull(toKtDiagnostic(source, callOrAssignmentSource))
    }
}

private fun mapUnsafeCallError(
    candidate: AbstractCandidate,
    rootCause: UnsafeCall,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
): KtDiagnostic {
    if (candidate.callInfo.isImplicitInvoke) {
        return FirErrors.UNSAFE_IMPLICIT_INVOKE_CALL.createOn(source, rootCause.actualType)
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
                singleArgument,
            )
        } else {
            FirErrors.UNSAFE_OPERATOR_CALL.createOn(
                source,
                rootCause.actualType,
                receiverExpression,
                candidateFunctionName!!.asString(),
                singleArgument,
            )
        }
    }
    return if (source?.kind == KtFakeSourceElementKind.ArrayAccessNameReference) {
        FirErrors.UNSAFE_CALL.createOn(source, rootCause.actualType, receiverExpression)
    } else {
        FirErrors.UNSAFE_CALL.createOn(qualifiedAccessSource ?: source, rootCause.actualType, receiverExpression)
    }
}

private fun mapInapplicableCandidateError(
    session: FirSession,
    diagnostic: ConeInapplicableCandidateError,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
): List<KtDiagnostic> {
    val typeContext = session.typeContext
    val genericDiagnostic = FirErrors.INAPPLICABLE_CANDIDATE.createOn(source, diagnostic.candidate.symbol)
    val diagnostics = diagnostic.candidate.diagnostics.filter { it.applicability == diagnostic.applicability }.mapNotNull { rootCause ->
        when (rootCause) {
            is VarargArgumentOutsideParentheses -> FirErrors.VARARG_OUTSIDE_PARENTHESES.createOn(
                rootCause.argument.source ?: qualifiedAccessSource
            )

            is NamedArgumentNotAllowed -> FirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.createOn(
                rootCause.argument.source,
                rootCause.forbiddenNamedArgumentsTarget
            )

            is ArgumentTypeMismatch -> {
                FirErrors.ARGUMENT_TYPE_MISMATCH.createOn(
                    rootCause.argument.source ?: source,
                    rootCause.expectedType.removeTypeVariableTypes(typeContext),
                    rootCause.actualType.removeTypeVariableTypes(typeContext),
                    rootCause.isMismatchDueToNullability
                )
            }

            // We don't report anything here, because there are already some errors inside the call or declaration
            // And the errors should be reported there
            is ErrorTypeInArguments -> null

            // see EagerResolveOfCallableReferences
            is UnsuccessfulCallableReferenceAtom -> null

            is MultipleContextReceiversApplicableForExtensionReceivers ->
                FirErrors.AMBIGUOUS_CALL_WITH_IMPLICIT_CONTEXT_RECEIVER.createOn(qualifiedAccessSource ?: source)

            is NoApplicableValueForContextReceiver ->
                FirErrors.NO_CONTEXT_RECEIVER.createOn(
                    qualifiedAccessSource ?: source,
                    rootCause.expectedContextReceiverType.removeTypeVariableTypes(typeContext)
                )

            is UnsupportedContextualDeclarationCall -> FirErrors.UNSUPPORTED_CONTEXTUAL_DECLARATION_CALL.createOn(source)

            is AmbiguousValuesForContextReceiverParameter ->
                FirErrors.MULTIPLE_ARGUMENTS_APPLICABLE_FOR_CONTEXT_RECEIVER.createOn(
                    qualifiedAccessSource ?: source,
                    rootCause.expectedContextReceiverType.removeTypeVariableTypes(typeContext)
                )

            is NullForNotNullType -> FirErrors.NULL_FOR_NONNULL_TYPE.createOn(
                rootCause.argument.source ?: source, rootCause.expectedType.removeTypeVariableTypes(typeContext)
            )

            is NonVarargSpread -> FirErrors.NON_VARARG_SPREAD.createOn(rootCause.argument.source?.getChild(KtTokens.MUL, depth = 1)!!)
            is ArgumentPassedTwice -> FirErrors.ARGUMENT_PASSED_TWICE.createOn(rootCause.argument.source)
            is TooManyArguments -> FirErrors.TOO_MANY_ARGUMENTS.createOn(rootCause.argument.source ?: source, rootCause.function.symbol)
            is NoValueForParameter -> FirErrors.NO_VALUE_FOR_PARAMETER.createOn(
                qualifiedAccessSource ?: source,
                rootCause.valueParameter.symbol
            )

            is NameNotFound -> FirErrors.NAMED_PARAMETER_NOT_FOUND.createOn(
                rootCause.argument.source ?: source,
                rootCause.argument.name.asString()
            )

            is NameForAmbiguousParameter -> FirErrors.NAME_FOR_AMBIGUOUS_PARAMETER.createOn(
                rootCause.argument.source ?: source
            )

            is UnsafeCall -> mapUnsafeCallError(diagnostic.candidate, rootCause, source, qualifiedAccessSource)
            is ManyLambdaExpressionArguments -> FirErrors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.createOn(rootCause.argument.source ?: source)
            is InfixCallOfNonInfixFunction -> FirErrors.INFIX_MODIFIER_REQUIRED.createOn(source, rootCause.function)
            is OperatorCallOfNonOperatorFunction ->
                FirErrors.OPERATOR_MODIFIER_REQUIRED.createOn(source, rootCause.function, rootCause.function.name.asString())

            is UnstableSmartCast -> FirErrors.SMARTCAST_IMPOSSIBLE.createOn(
                rootCause.argument.source,
                rootCause.targetType,
                rootCause.argument,
                rootCause.argument.smartcastStability.description,
                rootCause.isCastToNotNull
            )

            is DslScopeViolation -> FirErrors.DSL_SCOPE_VIOLATION.createOn(source, rootCause.calleeSymbol)
            is InferenceError -> {
                rootCause.constraintError.toDiagnostic(
                    source,
                    qualifiedAccessSource,
                    session.typeContext,
                    diagnostic.candidate
                )
            }

            is InferredEmptyIntersectionDiagnostic -> reportInferredIntoEmptyIntersection(
                source,
                rootCause.typeVariable,
                rootCause.incompatibleTypes,
                rootCause.causingTypes,
                rootCause.kind,
                isError = rootCause.isError
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

private fun mapSystemHasContradictionError(
    session: FirSession,
    diagnostic: ConeConstraintSystemHasContradiction,
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
): List<KtDiagnostic> {
    return buildList {
        for (error in diagnostic.candidate.errors) {
            addIfNotNull(
                error.toDiagnostic(
                    source,
                    qualifiedAccessSource,
                    session.typeContext,
                    diagnostic.candidate,
                )
            )
        }
    }.ifEmpty {
        listOfNotNull(
            diagnostic.candidate.errors.firstNotNullOfOrNull {
                val message = when (it) {
                    is NewConstraintError -> "NewConstraintError at ${it.position}: ${it.lowerType} <!: ${it.upperType}"
                    // Error should be reported on the error type itself
                    is ConstrainingTypeIsError -> return@firstNotNullOfOrNull null
                    is NotEnoughInformationForTypeParameter<*> -> return@firstNotNullOfOrNull null
                    else -> "Inference error: ${it::class.simpleName}"
                }

                if (it is NewConstraintError && it.position.from is FixVariableConstraintPosition<*>) {
                    val morePreciseDiagnosticExists = diagnostic.candidate.errors.any { other ->
                        other is NewConstraintError && other.position.from !is FixVariableConstraintPosition<*>
                    }
                    if (morePreciseDiagnosticExists) return@firstNotNullOfOrNull null
                }

                FirErrors.NEW_INFERENCE_ERROR.createOn(qualifiedAccessSource ?: source, message)
            }
        )
    }
}

private fun ConstraintSystemError.toDiagnostic(
    source: KtSourceElement?,
    qualifiedAccessSource: KtSourceElement?,
    typeContext: ConeTypeContext,
    candidate: AbstractCandidate,
): KtDiagnostic? {
    return when (this) {
        is NewConstraintError -> {
            val position = position.from
            val argument =
                when (position) {
                    // TODO, KT-59810: Support other ReceiverConstraintPositionImpl, LHSArgumentConstraintPositionImpl
                    is ConeArgumentConstraintPosition -> position.argument
                    is ConeLambdaArgumentConstraintPosition -> position.lambda
                    else -> null
                }

            val typeMismatchDueToNullability = typeContext.isTypeMismatchDueToNullability(lowerConeType, upperConeType)
            argument?.let {
                return FirErrors.ARGUMENT_TYPE_MISMATCH.createOn(
                    it.source ?: source,
                    lowerConeType.removeTypeVariableTypes(typeContext),
                    upperConeType.removeTypeVariableTypes(typeContext),
                    typeMismatchDueToNullability
                )
            }

            when (position) {
                is ConeExpectedTypeConstraintPosition -> {
                    val inferredType =
                        if (!lowerConeType.isNullableNothing)
                            lowerConeType
                        else
                            upperConeType.withNullability(ConeNullability.NULLABLE, typeContext)

                    FirErrors.TYPE_MISMATCH.createOn(
                        qualifiedAccessSource ?: source,
                        upperConeType.removeTypeVariableTypes(typeContext),
                        inferredType.removeTypeVariableTypes(typeContext),
                        typeMismatchDueToNullability
                    )
                }

                else -> null
            }
        }

        is NotEnoughInformationForTypeParameter<*> -> {
            val isDiagnosticRedundant = candidate.errors.any { otherError ->
                (otherError is ConstrainingTypeIsError && otherError.typeVariable == this.typeVariable)
                        || otherError is NewConstraintError
            }

            if (isDiagnosticRedundant) return null

            val typeVariableName = when (val typeVariable = this.typeVariable) {
                is ConeTypeParameterBasedTypeVariable -> typeVariable.typeParameterSymbol.name.asString()
                is ConeTypeVariableForLambdaReturnType -> "return type of lambda"
                else -> error("Unsupported type variable: $typeVariable")
            }

            FirErrors.NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.createOn(
                source,
                typeVariableName,
            )
        }

        is NoSuccessfulFork -> {
            FirErrors.INFERENCE_UNSUCCESSFUL_FORK.createOn(
                source,
                position.initialConstraint.asStringWithoutPosition(),
            )
        }

        is InferredEmptyIntersection -> {
            val typeVariable = typeVariable as ConeTypeVariable
            val narrowedSource = findInferredEmptyIntersectionNarrowedSource(typeVariable, candidate)

            @Suppress("UNCHECKED_CAST")
            reportInferredIntoEmptyIntersection(
                narrowedSource ?: source,
                typeVariable,
                incompatibleTypes as Collection<ConeKotlinType>,
                causingTypes as Collection<ConeKotlinType>,
                kind,
                this is InferredEmptyIntersectionError,
            )
        }

        is OnlyInputTypesDiagnostic -> {
            FirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR.createOn(
                source,
                (typeVariable as ConeTypeParameterBasedTypeVariable).typeParameterSymbol
            )
        }

        else -> null
    }
}

private fun findInferredEmptyIntersectionNarrowedSource(
    typeVariable: ConeTypeVariable,
    candidate: AbstractCandidate,
): KtSourceElement? {
    if (typeVariable !is ConeTypeParameterBasedTypeVariable) return null

    var narrowedSource: KtSourceElement? = null

    candidate.callInfo.callSite.accept(object : FirVisitorVoid() {
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
    isError: Boolean
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

    return factory.createOn(source, typeVariableText, incompatibleTypes, kind.description, causingTypesText)
}

private val NewConstraintError.lowerConeType: ConeKotlinType get() = lowerType as ConeKotlinType
private val NewConstraintError.upperConeType: ConeKotlinType get() = upperType as ConeKotlinType

private fun ConeSimpleDiagnostic.getFactory(source: KtSourceElement?): KtDiagnosticFactory0 {
    return when (kind) {
        DiagnosticKind.ReturnNotAllowed -> FirErrors.RETURN_NOT_ALLOWED
        DiagnosticKind.NotAFunctionLabel -> FirErrors.NOT_A_FUNCTION_LABEL
        DiagnosticKind.UnresolvedLabel -> FirErrors.UNRESOLVED_LABEL
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
        DiagnosticKind.ValueParameterWithNoTypeAnnotation -> FirErrors.VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION
        DiagnosticKind.CannotInferParameterType -> FirErrors.CANNOT_INFER_PARAMETER_TYPE
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
        DiagnosticKind.AnnotationNotAllowed -> FirErrors.ANNOTATION_IN_WHERE_CLAUSE_ERROR
        DiagnosticKind.UnresolvedSupertype,
        DiagnosticKind.UnresolvedExpandedType,
        DiagnosticKind.Other -> FirErrors.OTHER_ERROR
    }
}


@OptIn(InternalDiagnosticFactoryMethod::class)
private fun KtDiagnosticFactory0.createOn(
    element: KtSourceElement?
): KtSimpleDiagnostic {
    return on(element.requireNotNull(), positioningStrategy = null)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> KtDiagnosticFactory1<A>.createOn(
    element: KtSourceElement?,
    a: A
): KtDiagnosticWithParameters1<A> {
    return on(element.requireNotNull(), a, positioningStrategy = null)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B> KtDiagnosticFactory2<A, B>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B
): KtDiagnosticWithParameters2<A, B> {
    return on(element.requireNotNull(), a, b, positioningStrategy = null)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C> KtDiagnosticFactory3<A, B, C>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B,
    c: C
): KtDiagnosticWithParameters3<A, B, C> {
    return on(element.requireNotNull(), a, b, c, positioningStrategy = null)
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C, D> KtDiagnosticFactory4<A, B, C, D>.createOn(
    element: KtSourceElement?,
    a: A,
    b: B,
    c: C,
    d: D
): KtDiagnosticWithParameters4<A, B, C, D> {
    return on(element.requireNotNull(), a, b, c, d, positioningStrategy = null)
}
