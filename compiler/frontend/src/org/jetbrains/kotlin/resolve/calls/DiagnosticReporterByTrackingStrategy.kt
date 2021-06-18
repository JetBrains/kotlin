/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.*
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isNull
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.reportTrailingLambdaErrorOr
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.BuilderInferenceExpectedTypeConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstantChecker
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils
import org.jetbrains.kotlin.types.model.TypeSystemInferenceExtensionContextDelegate
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class DiagnosticReporterByTrackingStrategy(
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val context: BasicCallResolutionContext,
    val psiKotlinCall: PSIKotlinCall,
    val dataFlowValueFactory: DataFlowValueFactory,
    val allDiagnostics: List<KotlinCallDiagnostic>,
    private val smartCastManager: SmartCastManager,
    private val typeSystemContext: TypeSystemInferenceExtensionContextDelegate
) : DiagnosticReporter {
    private val trace = context.trace as TrackingBindingTrace
    private val tracingStrategy: TracingStrategy get() = psiKotlinCall.tracingStrategy
    private val call: Call get() = psiKotlinCall.psiCall

    override fun onExplicitReceiver(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCall(diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            VisibilityError::class.java -> tracingStrategy.invisibleMember(trace, (diagnostic as VisibilityError).invisibleMember)
            NoValueForParameter::class.java -> tracingStrategy.noValueForParameter(
                trace,
                (diagnostic as NoValueForParameter).parameterDescriptor
            )
            InstantiationOfAbstractClass::class.java -> tracingStrategy.instantiationOfAbstractClass(trace)
            AbstractSuperCall::class.java -> tracingStrategy.abstractSuperCall(trace)
            NonApplicableCallForBuilderInferenceDiagnostic::class.java -> {
                val reportOn = (diagnostic as NonApplicableCallForBuilderInferenceDiagnostic).kotlinCall
                trace.reportDiagnosticOnce(NON_APPLICABLE_CALL_FOR_BUILDER_INFERENCE.on(reportOn.psiKotlinCall.psiCall.callElement))
            }
            CandidateChosenUsingOverloadResolutionByLambdaAnnotation::class.java -> {
                trace.report(CANDIDATE_CHOSEN_USING_OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION.on(psiKotlinCall.psiCall.callElement))
            }
            CompatibilityWarning::class.java -> {
                val callElement = psiKotlinCall.psiCall.callElement
                trace.report(
                    COMPATIBILITY_WARNING.on(
                        callElement.getCalleeExpressionIfAny() ?: callElement,
                        (diagnostic as CompatibilityWarning).candidate
                    )
                )
            }
        }
    }

    override fun onTypeArguments(diagnostic: KotlinCallDiagnostic) {
        val psiCallElement = psiKotlinCall.psiCall.callElement
        val reportElement =
            if (psiCallElement is KtCallExpression)
                psiCallElement.typeArgumentList ?: psiCallElement.calleeExpression ?: psiCallElement
            else
                psiCallElement

        when (diagnostic) {
            is WrongCountOfTypeArguments -> {
                val expectedTypeArgumentsCount = diagnostic.descriptor.typeParameters.size
                trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reportElement, expectedTypeArgumentsCount, diagnostic.descriptor))
            }
        }
    }

    override fun onCallName(diagnostic: KotlinCallDiagnostic) {

    }

    override fun onTypeArgument(typeArgument: TypeArgument, diagnostic: KotlinCallDiagnostic) {

    }

    override fun onCallReceiver(callReceiver: SimpleKotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            UnsafeCallError::class.java -> {
                val unsafeCallErrorDiagnostic = diagnostic.cast<UnsafeCallError>()
                val isForImplicitInvoke = when (callReceiver) {
                    is ReceiverExpressionKotlinCallArgument -> callReceiver.isForImplicitInvoke
                    else -> unsafeCallErrorDiagnostic.isForImplicitInvoke
                            || callReceiver.receiver.receiverValue.type.isExtensionFunctionType
                }

                tracingStrategy.unsafeCall(trace, callReceiver.receiver.receiverValue.type, isForImplicitInvoke)
            }

            SuperAsExtensionReceiver::class.java -> {
                val psiExpression = callReceiver.psiExpression
                if (psiExpression is KtSuperExpression) {
                    trace.report(SUPER_CANT_BE_EXTENSION_RECEIVER.on(psiExpression, psiExpression.text))
                }
            }
        }
    }

    override fun onCallArgument(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            SmartCastDiagnostic::class.java -> reportSmartCast(diagnostic as SmartCastDiagnostic)
            UnstableSmartCastDiagnosticError::class.java,
            UnstableSmartCastResolutionError::class.java -> reportUnstableSmartCast(diagnostic as UnstableSmartCast)
            TooManyArguments::class.java -> {
                trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                    TOO_MANY_ARGUMENTS.on(expr, (diagnostic as TooManyArguments).descriptor)
                }

                trace.markAsReported()
            }
            VarargArgumentOutsideParentheses::class.java -> trace.reportTrailingLambdaErrorOr(callArgument.psiExpression) { expr ->
                VARARG_OUTSIDE_PARENTHESES.on(expr)
            }

            MixingNamedAndPositionArguments::class.java ->
                trace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(callArgument.psiCallArgument.valueArgument.asElement()))

            NoneCallableReferenceCandidates::class.java -> {
                val expression = diagnostic.cast<NoneCallableReferenceCandidates>()
                    .argument.safeAs<CallableReferenceKotlinCallArgumentImpl>()?.ktCallableReferenceExpression
                if (expression != null) {
                    trace.report(UNRESOLVED_REFERENCE.on(expression.callableReference, expression.callableReference))
                }
            }

            CallableReferenceCandidatesAmbiguity::class.java -> {
                val ambiguityDiagnostic = diagnostic as CallableReferenceCandidatesAmbiguity
                val expression = when (val psiExpression = ambiguityDiagnostic.argument.psiExpression) {
                    is KtPsiUtil.KtExpressionWrapper -> psiExpression.baseExpression
                    else -> psiExpression
                }.safeAs<KtCallableReferenceExpression>()

                val candidates = ambiguityDiagnostic.candidates.map { it.candidate }
                if (expression != null) {
                    trace.reportDiagnosticOnce(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY.on(expression.callableReference, candidates))
                    trace.record(BindingContext.AMBIGUOUS_REFERENCE_TARGET, expression.callableReference, candidates)
                }
            }

            ArgumentNullabilityMismatchDiagnostic::class.java -> {
                require(diagnostic is ArgumentNullabilityMismatchDiagnostic)
                val expression = callArgument.safeAs<PSIKotlinCallArgument>()?.valueArgument?.getArgumentExpression()?.let {
                    KtPsiUtil.deparenthesize(it) ?: it
                }
                if (expression != null) {
                    if (expression.isNull() && expression is KtConstantExpression) {
                        trace.reportDiagnosticOnce(NULL_FOR_NONNULL_TYPE.on(expression, diagnostic.expectedType))
                    } else {
                        trace.report(TYPE_MISMATCH.on(expression, diagnostic.expectedType, diagnostic.actualType))
                    }
                }
            }

            CallableReferencesDefaultArgumentUsed::class.java -> {
                require(diagnostic is CallableReferencesDefaultArgumentUsed) {
                    "diagnostic ($diagnostic) should have type CallableReferencesDefaultArgumentUsed"
                }

                diagnostic.argument.psiExpression?.let {
                    trace.report(
                        UNSUPPORTED_FEATURE.on(
                            it, LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType to context.languageVersionSettings
                        )
                    )
                }

            }

            ResolvedToSamWithVarargDiagnostic::class.java -> {
                trace.report(TYPE_INFERENCE_CANDIDATE_WITH_SAM_AND_VARARG.on(callArgument.psiCallArgument.valueArgument.asElement()))
            }

            NotEnoughInformationForLambdaParameter::class.java -> {
                val unknownParameterTypeDiagnostic = diagnostic as NotEnoughInformationForLambdaParameter
                val lambdaArgument = unknownParameterTypeDiagnostic.lambdaArgument
                val parameterIndex = unknownParameterTypeDiagnostic.parameterIndex

                val argumentExpression = KtPsiUtil.deparenthesize(lambdaArgument.psiCallArgument.valueArgument.getArgumentExpression())

                val valueParameters = when (argumentExpression) {
                    is KtLambdaExpression -> argumentExpression.valueParameters
                    is KtNamedFunction -> argumentExpression.valueParameters // for anonymous functions
                    else -> return
                }

                val parameter = valueParameters.getOrNull(parameterIndex)
                if (parameter != null) {
                    trace.report(CANNOT_INFER_PARAMETER_TYPE.on(parameter))
                }
            }

            CompatibilityWarningOnArgument::class.java -> {
                trace.report(
                    COMPATIBILITY_WARNING.on(
                        callArgument.psiCallArgument.valueArgument.asElement(),
                        (diagnostic as CompatibilityWarningOnArgument).candidate
                    )
                )
            }

            AdaptedCallableReferenceIsUsedWithReflection::class.java -> {
                trace.report(
                    ADAPTED_CALLABLE_REFERENCE_AGAINST_REFLECTION_TYPE.on(
                        callArgument.psiCallArgument.valueArgument.asElement()
                    )
                )
            }
        }
    }

    override fun onCallArgumentName(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        val nameReference = callArgument.psiCallArgument.valueArgument.getArgumentName()?.referenceExpression ?: return
        when (diagnostic.javaClass) {
            NamedArgumentReference::class.java -> {
                trace.record(BindingContext.REFERENCE_TARGET, nameReference, (diagnostic as NamedArgumentReference).parameterDescriptor)
                trace.markAsReported()
            }
            NameForAmbiguousParameter::class.java -> trace.report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference))
            NameNotFound::class.java -> trace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference))

            NamedArgumentNotAllowed::class.java -> trace.report(
                NAMED_ARGUMENTS_NOT_ALLOWED.on(
                    nameReference,
                    when ((diagnostic as NamedArgumentNotAllowed).descriptor) {
                        is FunctionInvokeDescriptor -> INVOKE_ON_FUNCTION_TYPE
                        is DeserializedCallableMemberDescriptor -> INTEROP_FUNCTION
                        else -> NON_KOTLIN_FUNCTION
                    }
                )
            )
            ArgumentPassedTwice::class.java -> trace.report(ARGUMENT_PASSED_TWICE.on(nameReference))
        }
    }

    override fun onCallArgumentSpread(callArgument: KotlinCallArgument, diagnostic: KotlinCallDiagnostic) {
        when (diagnostic.javaClass) {
            NonVarargSpread::class.java -> {
                val spreadElement = callArgument.safeAs<ExpressionKotlinCallArgumentImpl>()?.valueArgument?.getSpreadElement()
                if (spreadElement != null) {
                    trace.report(NON_VARARG_SPREAD.on(spreadElement))
                }
            }
        }
    }

    private fun reportSmartCast(smartCastDiagnostic: SmartCastDiagnostic) {
        val expressionArgument = smartCastDiagnostic.argument
        val smartCastResult = when (expressionArgument) {
            is ExpressionKotlinCallArgumentImpl -> {
                trace.markAsReported()
                val context = context.replaceDataFlowInfo(expressionArgument.dataFlowInfoBeforeThisArgument)
                val argumentExpression = KtPsiUtil.getLastElementDeparenthesized(
                    expressionArgument.valueArgument.getArgumentExpression(),
                    context.statementFilter
                )
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(expressionArgument.receiver.receiverValue, context)
                val call = if (call.callElement is KtBinaryExpression) null else call
                if (!expressionArgument.valueArgument.isExternal()) {
                    smartCastManager.checkAndRecordPossibleCast(
                        dataFlowValue, smartCastDiagnostic.smartCastType, argumentExpression, context, call,
                        recordExpressionType = false
                    )
                } else null
            }
            is ReceiverExpressionKotlinCallArgument -> {
                trace.markAsReported()
                val receiverValue = expressionArgument.receiver.receiverValue
                val dataFlowValue = dataFlowValueFactory.createDataFlowValue(receiverValue, context)
                smartCastManager.checkAndRecordPossibleCast(
                    dataFlowValue, smartCastDiagnostic.smartCastType, (receiverValue as? ExpressionReceiver)?.expression, context, call,
                    recordExpressionType = true
                )
            }
            else -> null
        }
        val resolvedCall =
            smartCastDiagnostic.kotlinCall?.psiKotlinCall?.psiCall?.getResolvedCall(trace.bindingContext) as? NewResolvedCallImpl<*>
        if (resolvedCall != null && smartCastResult != null) {
            if (resolvedCall.extensionReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.updateExtensionReceiverWithSmartCastIfNeeded(smartCastResult.resultType)
            }
            if (resolvedCall.dispatchReceiver == expressionArgument.receiver.receiverValue) {
                resolvedCall.setSmartCastDispatchReceiverType(smartCastResult.resultType)
            }
        }
    }

    private fun reportUnstableSmartCast(unstableSmartCast: UnstableSmartCast) {
        val dataFlowValue = dataFlowValueFactory.createDataFlowValue(unstableSmartCast.argument.receiver.receiverValue, context)
        val possibleTypes = unstableSmartCast.argument.receiver.typesFromSmartCasts
        val argumentExpression = unstableSmartCast.argument.psiExpression ?: return

        require(possibleTypes.isNotEmpty()) { "Receiver for unstable smart cast without possible types" }
        trace.report(
            SMARTCAST_IMPOSSIBLE.on(
                argumentExpression,
                intersectWrappedTypes(possibleTypes),
                argumentExpression.text,
                dataFlowValue.kind.description
            )
        )
    }

    private fun reportConstraintErrorByPosition(error: NewConstraintMismatch, position: ConstraintPosition) {
        val argument =
            when (position) {
                is ArgumentConstraintPositionImpl -> position.argument
                is ReceiverConstraintPositionImpl -> position.argument
                is LHSArgumentConstraintPositionImpl -> position.argument
                is LambdaArgumentConstraintPositionImpl -> position.lambda.atom
                else -> null
            }
        val isWarning = error is NewConstraintWarning
        val typeMismatchDiagnostic = if (isWarning) TYPE_MISMATCH_WARNING else TYPE_MISMATCH
        val report = if (isWarning) trace::reportDiagnosticOnce else trace::report
        argument?.let {
            it.safeAs<LambdaKotlinCallArgument>()?.let lambda@{ lambda ->
                val parameterTypes = lambda.parametersTypes?.toList() ?: return@lambda
                val index = parameterTypes.indexOf(error.upperKotlinType.unwrap())
                val lambdaExpression = lambda.psiExpression as? KtLambdaExpression ?: return@lambda
                val parameter = lambdaExpression.valueParameters.getOrNull(index) ?: return@lambda
                val diagnosticFactory =
                    if (isWarning) EXPECTED_PARAMETER_TYPE_MISMATCH_WARNING else EXPECTED_PARAMETER_TYPE_MISMATCH
                report(diagnosticFactory.on(parameter, error.upperKotlinType))
                return
            }

            val expression = it.psiExpression ?: return
            val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)
            if (reportConstantTypeMismatch(error, deparenthesized)) return

            val compileTimeConstant = trace[BindingContext.COMPILE_TIME_VALUE, deparenthesized] as? TypedCompileTimeConstant
            if (compileTimeConstant != null) {
                val expressionType = trace[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type
                if (expressionType != null &&
                    !UnsignedTypes.isUnsignedType(compileTimeConstant.type) && UnsignedTypes.isUnsignedType(expressionType)
                ) {
                    return
                }
            }
            report(typeMismatchDiagnostic.on(deparenthesized, error.upperKotlinType, error.lowerKotlinType))
        }

        (position as? ExpectedTypeConstraintPositionImpl)?.let {
            val call = it.topLevelCall.psiKotlinCall.psiCall.callElement.safeAs<KtExpression>()
            val inferredType =
                if (!error.lowerKotlinType.isNullableNothing()) error.lowerKotlinType
                else error.upperKotlinType.makeNullable()
            if (call != null) {
                report(typeMismatchDiagnostic.on(call, error.upperKotlinType, inferredType))
            }
        }

        (position as? BuilderInferenceExpectedTypeConstraintPosition)?.let {
            val inferredType =
                if (!error.lowerKotlinType.isNullableNothing()) error.lowerKotlinType
                else error.upperKotlinType.makeNullable()
            trace.report(TYPE_MISMATCH.on(it.topLevelCall, error.upperKotlinType, inferredType))
        }

        (position as? BuilderInferenceSubstitutionConstraintPositionImpl)?.let {
            reportConstraintErrorByPosition(error, it.initialConstraint.position)
        }

        (position as? ExplicitTypeParameterConstraintPositionImpl)?.let {
            val typeArgumentReference = (it.typeArgument as SimpleTypeArgumentImpl).typeReference
            val diagnosticFactory = if (isWarning) UPPER_BOUND_VIOLATED_WARNING else UPPER_BOUND_VIOLATED
            report(diagnosticFactory.on(typeArgumentReference, error.upperKotlinType, error.lowerKotlinType))
        }

        (position as? FixVariableConstraintPositionImpl)?.let {
            val morePreciseDiagnosticExists = allDiagnostics.any { other ->
                val otherError = other.constraintSystemError ?: return@any false
                otherError is NewConstraintError && otherError.position.from !is FixVariableConstraintPositionImpl
            }
            if (morePreciseDiagnosticExists) return

            val call = it.resolvedAtom?.atom?.safeAs<PSIKotlinCall>()?.psiCall ?: call
            val expression = call.calleeExpression ?: return

            trace.reportDiagnosticOnce(typeMismatchDiagnostic.on(expression, error.upperKotlinType, error.lowerKotlinType))
        }
    }

    override fun constraintError(error: ConstraintSystemError) {
        when (error.javaClass) {
            NewConstraintError::class.java, NewConstraintWarning::class.java -> {
                reportConstraintErrorByPosition(error as NewConstraintMismatch, error.position.from)
            }

            CapturedTypeFromSubtyping::class.java -> {
                error as CapturedTypeFromSubtyping
                val position = error.position
                val argumentPosition: ArgumentConstraintPositionImpl? =
                    position.safeAs() ?: position.safeAs<IncorporationConstraintPosition>()?.from.safeAs()

                argumentPosition?.let {
                    val expression = it.argument.psiExpression ?: return
                    trace.reportDiagnosticOnce(
                        NEW_INFERENCE_ERROR.on(
                            expression,
                            "Capture type from subtyping ${error.constraintType} for variable ${error.typeVariable}"
                        )
                    )
                }
            }

            NotEnoughInformationForTypeParameterImpl::class.java -> {
                error as NotEnoughInformationForTypeParameterImpl

                val resolvedAtom = error.resolvedAtom
                val isDiagnosticRedundant = !isSpecialFunction(resolvedAtom) && allDiagnostics.any {
                    when (it) {
                        is WrongCountOfTypeArguments -> true
                        is KotlinConstraintSystemDiagnostic -> {
                            val otherError = it.error
                            (otherError is ConstrainingTypeIsError && otherError.typeVariable == error.typeVariable)
                                    || otherError is NewConstraintError
                        }
                        else -> false
                    }
                }

                if (isDiagnosticRedundant) return
                val expression = when (val atom = error.resolvedAtom.atom) {
                    is PSIKotlinCall -> {
                        val psiCall = atom.psiCall
                        if (psiCall is CallTransformer.CallForImplicitInvoke) {
                            psiCall.outerCall.calleeExpression
                        } else {
                            psiCall.calleeExpression
                        }
                    }
                    is PSIKotlinCallArgument -> atom.valueArgument.getArgumentExpression()
                    else -> call.calleeExpression
                } ?: return

                if (isSpecialFunction(resolvedAtom)) {
                    // We locally report errors on some arguments of special calls, on which the error may not be reported directly
                    reportNotEnoughInformationForTypeParameterForSpecialCall(resolvedAtom, error)
                } else {
                    val typeVariableName = when (val typeVariable = error.typeVariable) {
                        is TypeVariableFromCallableDescriptor -> typeVariable.originalTypeParameter.name.asString()
                        is TypeVariableForLambdaReturnType -> "return type of lambda"
                        else -> error("Unsupported type variable: $typeVariable")
                    }
                    val unwrappedExpression = if (expression is KtBlockExpression) {
                        expression.statements.lastOrNull() ?: expression
                    } else expression

                    trace.reportDiagnosticOnce(NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(unwrappedExpression, typeVariableName))
                }
            }

            OnlyInputTypesDiagnostic::class.java -> {
                val typeVariable = (error as OnlyInputTypesDiagnostic).typeVariable as? TypeVariableFromCallableDescriptor ?: return
                psiKotlinCall.psiCall.calleeExpression?.let {
                    val factory = if (context.languageVersionSettings.supportsFeature(LanguageFeature.StrictOnlyInputTypesChecks))
                        TYPE_INFERENCE_ONLY_INPUT_TYPES
                    else TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING
                    trace.report(factory.on(it, typeVariable.originalTypeParameter))
                }
            }
        }
    }

    private fun reportNotEnoughInformationForTypeParameterForSpecialCall(
        resolvedAtom: ResolvedCallAtom,
        error: NotEnoughInformationForTypeParameterImpl
    ) {
        val subResolvedAtomsToReportError =
            getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(resolvedAtom, error.typeVariable)

        if (subResolvedAtomsToReportError.isEmpty()) return

        for (subResolvedAtom in subResolvedAtomsToReportError) {
            val atom = subResolvedAtom.atom as? PSIKotlinCallArgument ?: continue
            val argumentsExpression = getArgumentsExpressionOrLastExpressionInBlock(atom)

            if (argumentsExpression != null) {
                val specialFunctionName = requireNotNull(
                    ControlStructureTypingUtils.ResolveConstruct.values().find { specialFunction ->
                        specialFunction.specialFunctionName == resolvedAtom.candidateDescriptor.name
                    }
                ) { "Unsupported special construct: ${resolvedAtom.candidateDescriptor.name} not found in special construct names" }

                trace.reportDiagnosticOnce(
                    NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER.on(
                        argumentsExpression, " for subcalls of ${specialFunctionName.getName()} expression"
                    )
                )
            }
        }
    }

    private fun getArgumentsExpressionOrLastExpressionInBlock(atom: PSIKotlinCallArgument): KtExpression? {
        val valueArgumentExpression = atom.valueArgument.getArgumentExpression()

        return if (valueArgumentExpression is KtBlockExpression) valueArgumentExpression.statements.lastOrNull() else valueArgumentExpression
    }

    private fun KotlinType.containsUninferredTypeParameter(uninferredTypeVariable: TypeVariableMarker) = contains {
        ErrorUtils.isUninferredParameter(it) || it == TypeUtils.DONT_CARE
                || it.constructor == uninferredTypeVariable.freshTypeConstructor(typeSystemContext)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(
        resolvedAtom: ResolvedAtom,
        uninferredTypeVariable: TypeVariableMarker
    ): Set<ResolvedAtom> =
        buildSet {
            for (subResolvedAtom in resolvedAtom.subResolvedAtoms ?: return@buildSet) {
                val atom = subResolvedAtom.atom
                val typeToCheck = when {
                    subResolvedAtom is PostponedResolvedAtom -> subResolvedAtom.expectedType ?: return@buildSet
                    atom is SimpleKotlinCallArgument -> atom.receiver.receiverValue.type
                    else -> return@buildSet
                }

                if (typeToCheck.containsUninferredTypeParameter(uninferredTypeVariable)) {
                    add(subResolvedAtom)
                }

                if (!subResolvedAtom.subResolvedAtoms.isNullOrEmpty()) {
                    addAll(
                        getSubResolvedAtomsOfSpecialCallToReportUninferredTypeParameter(subResolvedAtom, uninferredTypeVariable)
                    )
                }
            }
        }

    @OptIn(ExperimentalContracts::class)
    private fun isSpecialFunction(atom: ResolvedAtom): Boolean {
        contract {
            returns(true) implies (atom is ResolvedCallAtom)
        }
        if (atom !is ResolvedCallAtom) return false

        return ControlStructureTypingUtils.ResolveConstruct.values().any { specialFunction ->
            specialFunction.specialFunctionName == atom.candidateDescriptor.name
        }
    }

    private fun reportConstantTypeMismatch(constraintError: NewConstraintMismatch, expression: KtExpression): Boolean {
        if (expression is KtConstantExpression) {
            val module = context.scope.ownerDescriptor.module
            val constantValue = constantExpressionEvaluator.evaluateToConstantValue(expression, trace, context.expectedType)
            val hasConstantTypeError = CompileTimeConstantChecker(context, module, true)
                .checkConstantExpressionType(constantValue, expression, constraintError.upperKotlinType)
            if (hasConstantTypeError) return true
        }
        return false
    }

}

val NewConstraintMismatch.upperKotlinType get() = upperType as KotlinType
val NewConstraintMismatch.lowerKotlinType get() = lowerType as KotlinType
