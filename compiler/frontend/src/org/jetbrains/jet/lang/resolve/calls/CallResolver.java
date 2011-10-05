package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Ref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.inference.ConstraintSystem;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCall.MAP_TO_CANDIDATE;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCall.MAP_TO_RESULT;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.JetTypeInferrer.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {

    private final JetTypeInferrer typeInferrer;
    private final JetSemanticServices semanticServices;
    private final OverloadingConflictResolver overloadingConflictResolver;
    private final DataFlowInfo dataFlowInfo;

    public CallResolver(JetSemanticServices semanticServices, JetTypeInferrer typeInferrer, DataFlowInfo dataFlowInfo) {
        this.typeInferrer = typeInferrer;
        this.semanticServices = semanticServices;
        this.overloadingConflictResolver = new OverloadingConflictResolver(semanticServices);
        this.dataFlowInfo = dataFlowInfo;
    }

    @Nullable
    public VariableDescriptor resolveSimpleProperty(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull ReceiverDescriptor receiver,
            @NotNull final JetSimpleNameExpression nameExpression,
            @NotNull JetType expectedType) {
        String referencedName = nameExpression.getReferencedName();
        if (referencedName == null) {
            return null;
        }
        Call call = CallMaker.makePropertyCall(nameExpression);
        List<ResolutionTask<VariableDescriptor>> prioritizedTasks = TaskPrioritizers.PROPERTY_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiver, call, referencedName, trace.getBindingContext(), dataFlowInfo);
        return resolveCallToDescriptor(trace, scope, call, nameExpression.getNode(), expectedType, prioritizedTasks, nameExpression);
    }


    @Nullable
    public JetType resolveCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull ReceiverDescriptor receiver,
            @NotNull JetCallElement call,
            @NotNull JetType expectedType
    ) {
        FunctionDescriptor functionDescriptor = resolveSimpleCallToFunctionDescriptor(trace, scope, receiver, call, expectedType);
        return functionDescriptor == null ? null : functionDescriptor.getReturnType();
    }
    
    @Nullable
    public FunctionDescriptor resolveCallWithGivenName(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name,
            @NotNull ReceiverDescriptor receiver,
            @NotNull JetType expectedType) {
        List<ResolutionTask<FunctionDescriptor>> tasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiver, call, name, trace.getBindingContext(), dataFlowInfo);
        return resolveCallToDescriptor(trace, scope, call, functionReference.getNode(), expectedType, tasks, functionReference);
    }

    @Nullable
    public FunctionDescriptor resolveSimpleCallToFunctionDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull ReceiverDescriptor receiver,
            @NotNull final JetCallElement call,
            @NotNull JetType expectedType
    ) {
        List<ResolutionTask<FunctionDescriptor>> prioritizedTasks;
        
        JetExpression calleeExpression = call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            String name = expression.getReferencedName();
            if (name == null) return checkArgumentTypesAndFail(trace, scope, call);

            prioritizedTasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiver, call, name, trace.getBindingContext(), dataFlowInfo);
            ResolutionTask.DescriptorCheckStrategy abstractConstructorCheck = new ResolutionTask.DescriptorCheckStrategy() {
                @Override
                public <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
                    if (descriptor instanceof ConstructorDescriptor) {
                        Modality modality = ((ConstructorDescriptor) descriptor).getContainingDeclaration().getModality();
                        if (modality == Modality.ABSTRACT) {
//                            tracing.reportOverallResolutionError(trace, "Can not create an instance of an abstract class");
                            tracing.instantiationOfAbstractClass(trace);
                            return false;
                        }
                    }
                    return true;
                }
            };
            for (ResolutionTask task : prioritizedTasks) {
                task.setCheckingStrategy(abstractConstructorCheck);
            }
        }
        else {
            JetValueArgumentList valueArgumentList = call.getValueArgumentList();
            ASTNode reportAbsenceOn = valueArgumentList == null ? call.getNode() : valueArgumentList.getNode();
            if (calleeExpression instanceof JetConstructorCalleeExpression) {
                assert !receiver.exists();

                prioritizedTasks = Lists.newArrayList();

                JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
                functionReference = expression.getConstructorReferenceExpression();
                if (functionReference == null) {
                    return checkArgumentTypesAndFail(trace, scope, call); // No type there
                }
                JetTypeReference typeReference = expression.getTypeReference();
                assert typeReference != null;
                JetType constructedType = new TypeResolver(semanticServices, trace, true).resolveType(scope, typeReference);
                DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                    Set<FunctionDescriptor> constructors = classDescriptor.getConstructors();
                    if (constructors.isEmpty()) {
//                        trace.getErrorHandler().genericError(reportAbsenceOn, "This class does not have a constructor");
                        trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                        return checkArgumentTypesAndFail(trace, scope, call);
                    }
                    prioritizedTasks.add(new ResolutionTask<FunctionDescriptor>(ResolvedCall.convertCollection(constructors), NO_RECEIVER, call));
                }
                else {
//                    trace.getErrorHandler().genericError(calleeExpression.getNode(), "Not a class");
                    trace.report(NOT_A_CLASS.on(calleeExpression));
                    return checkArgumentTypesAndFail(trace, scope, call);
                }
            }
            else if (calleeExpression instanceof JetThisReferenceExpression) {
                functionReference = (JetThisReferenceExpression) calleeExpression;
                DeclarationDescriptor containingDeclaration = scope.getContainingDeclaration();
                assert containingDeclaration instanceof ClassDescriptor;
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

                Set<FunctionDescriptor> constructors = classDescriptor.getConstructors();
                if (constructors.isEmpty()) {
//                    trace.getErrorHandler().genericError(reportAbsenceOn, "This class does not have a constructor");
                    trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                    return checkArgumentTypesAndFail(trace, scope, call);
                }
                prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(ResolvedCall.convertCollection(constructors), NO_RECEIVER, call));
            }
            else {
                throw new UnsupportedOperationException("Type argument inference not implemented for " + call.getText());
            }
        }

        return resolveCallToDescriptor(trace, scope, call, call.getNode(), expectedType, prioritizedTasks, functionReference);
    }

    private FunctionDescriptor checkArgumentTypesAndFail(BindingTrace trace, JetScope scope, JetCallElement call) {
        checkTypesWithNoCallee(trace, scope, call.getTypeArguments(), call.getValueArguments(), call.getFunctionLiteralArguments());
        return null;
    }

    @Nullable
    private <D extends CallableDescriptor> D resolveCallToDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final ASTNode callNode,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask<D>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference) {
        ResolvedCall<D> resolvedCall = doResolveCall(trace, scope, call, callNode, expectedType, prioritizedTasks, reference);
        return resolvedCall == null ? null : resolvedCall.getResultingDescriptor();
    }

    @Nullable
    private <D extends CallableDescriptor> ResolvedCall<D> doResolveCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final ASTNode callNode,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask<D>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference) {
        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResults<D> resultsForFirstNonemptyCandidateSet = null;
        TracingStrategy tracing = new TracingStrategy() {
            @Override
            public void bindReference(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiver, @NotNull CallableDescriptor descriptor) {
                trace.record(REFERENCE_TARGET, reference, descriptor);
                trace.record(RECEIVER, reference, receiver);
                if (receiver instanceof AutocastReceiver) {
                    AutocastReceiver autoCastReceiver = (AutocastReceiver) receiver;
                    ReceiverDescriptor original = autoCastReceiver.getOriginal();
                    if (original instanceof ExpressionReceiver) {
                        ExpressionReceiver expressionReceiver = (ExpressionReceiver) original;
                        trace.record(AUTOCAST, expressionReceiver.getExpression(), autoCastReceiver.getType());
                    }
                }
            }

            @Override
            public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCall<D>> candidates) {
                trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, candidates);
            }

            @Override
            public void unresolvedReference(@NotNull BindingTrace trace) {
                trace.report(UNRESOLVED_REFERENCE.on(reference));
            }

            @Override
            public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
//                ASTNode node;
//
//                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
//                if (valueArgumentList != null) {
//                    node = valueArgumentList.getNode();
//                }
//                else if (!call.getFunctionLiteralArguments().isEmpty()) {
//                    node = call.getFunctionLiteralArguments().get(0).getNode();
//                }
//                else {
//                    node = callNode;
//                }
//                trace.report(NO_VALUE_FOR_PARAMETER.on(node, valueParameter));
                trace.report(NO_VALUE_FOR_PARAMETER.on(reference, valueParameter));
            }

            @Override
            public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver) {
                trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
            }

            @Override
            public void noReceiverAllowed(@NotNull BindingTrace trace) {
                trace.report(NO_RECEIVER_ADMITTED.on(reference));
            }

            @Override
            public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {
                JetTypeArgumentList typeArgumentList = call.getTypeArgumentList();
                if (typeArgumentList != null) {
//                    trace.getErrorHandler().genericError(typeArgumentList.getNode(), message);
                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(typeArgumentList, expectedTypeArgumentCount));
                }
                else {
//                    reportOverallResolutionError(trace, message);
                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reference, expectedTypeArgumentCount));
                }
            }

            @Override
            public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors) {
                trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(callNode, descriptors));
            }

            @Override
            public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Set<ResolvedCall<D>> descriptors) {
                trace.report(NONE_APPLICABLE.on(callNode, descriptors));
            }

            @Override
            public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
                trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(callNode));
            }

            @Override
            public void typeInferenceFailed(@NotNull BindingTrace trace) {
                trace.report(TYPE_INFERENCE_FAILED.on(callNode));
            }

        };
        for (ResolutionTask<D> task : prioritizedTasks) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            OverloadResolutionResults<D> results = performResolution(temporaryTrace, scope, expectedType, task, tracing);
            if (results.isSuccess()) {
                temporaryTrace.commit();
                return results.getResult();
            }
            if (traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty()) {
                traceForFirstNonemptyCandidateSet = temporaryTrace;
                resultsForFirstNonemptyCandidateSet = results;
            }
        }
        if (traceForFirstNonemptyCandidateSet != null) {
            traceForFirstNonemptyCandidateSet.commit();
            if (resultsForFirstNonemptyCandidateSet.singleDescriptor()) {
                return resultsForFirstNonemptyCandidateSet.getResult();
            }
        }
        else {
            trace.report(UNRESOLVED_REFERENCE.on(reference));
            checkTypesWithNoCallee(trace, scope, call.getTypeArguments(), call.getValueArguments(), call.getFunctionLiteralArguments());
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> performResolution(@NotNull BindingTrace trace, @NotNull JetScope scope, @NotNull JetType expectedType, @NotNull ResolutionTask<D> task, @NotNull TracingStrategy tracing) {
        Set<ResolvedCall<D>> successfulCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCall<D>> failedCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCall<D>> dirtyCandidates = Sets.newLinkedHashSet();

        for (ResolvedCall<D> candidateCall : task.getCandidates()) {
            D candidate = candidateCall.getCandidateDescriptor();
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            candidateCall.setTrace(temporaryTrace);

            tracing.bindReference(temporaryTrace, task.getReceiver(), candidate);
            
            if (ErrorUtils.isError(candidate)) {
                successfulCandidates.add(candidateCall.setResultingDescriptor(candidate));
                checkTypesWithNoCallee(temporaryTrace, scope, task.getTypeArguments(), task.getValueArguments(), task.getFunctionLiteralArguments());
                continue;
            }


            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters = Maps.newHashMap();
            boolean error = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(task, tracing, candidate, temporaryTrace, argumentsToParameters);

            if (error) {
                failedCandidates.add(candidateCall);
                checkTypesWithNoCallee(temporaryTrace, scope, task.getTypeArguments(), task.getValueArguments(), task.getFunctionLiteralArguments());
                continue;
            }

            Ref<Boolean> dirty = new Ref<Boolean>(false);
            if (task.getTypeArguments().isEmpty()) {
                if (candidate.getTypeParameters().isEmpty()) {
                    if (checkValueArgumentTypes(scope, temporaryTrace, argumentsToParameters, dirty, Functions.<ValueParameterDescriptor>identity())
                            && checkReceiver(task, tracing, candidate, temporaryTrace)) {
                        successfulCandidates.add(candidateCall.setResultingDescriptor(candidate));
                    }
                    else {
                        failedCandidates.add(candidateCall);
                    }
                }
                else {
                    // Type argument inference

                    ConstraintSystem constraintSystem = new ConstraintSystem();
                    for (TypeParameterDescriptor typeParameterDescriptor : candidate.getTypeParameters()) {
                        constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT); // TODO
                    }

                    for (Map.Entry<ValueArgument, ValueParameterDescriptor> entry : argumentsToParameters.entrySet()) {
                        ValueArgument valueArgument = entry.getKey();
                        ValueParameterDescriptor valueParameterDescriptor = entry.getValue();

                        JetExpression expression = valueArgument.getArgumentExpression();
                        // TODO : more attempts, with different expected types
                        JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);
                        JetType type = temporaryServices.getType(scope, expression, NO_EXPECTED_TYPE);
                        if (type != null) {
                            constraintSystem.addSubtypingConstraint(type, valueParameterDescriptor.getOutType());
                        }
                        else {
                            dirty.set(true);
                        }
                    }

                    checkReceiverAbsence(task, tracing, candidate, temporaryTrace);
                    // Error is already reported if something is missing
                    ReceiverDescriptor receiver = task.getReceiver();
                    ReceiverDescriptor candidateReceiver = candidate.getReceiver();
                    if (receiver.exists() && candidateReceiver.exists()) {
                        constraintSystem.addSubtypingConstraint(receiver.getType(), candidateReceiver.getType());
                    }

                    if (expectedType != NO_EXPECTED_TYPE) {
                        constraintSystem.addSubtypingConstraint(candidate.getReturnType(), expectedType);
                    }

                    ConstraintSystem.Solution solution = constraintSystem.solve();
//                    solutions.put(candidate, solution);
                    if (solution.isSuccessful()) {
                        D substitute = (D) candidate.substitute(solution.getSubstitutor());
                        assert substitute != null;
                        successfulCandidates.add(candidateCall.setResultingDescriptor(substitute));
                    }
                    else {
                        tracing.typeInferenceFailed(temporaryTrace);
                        failedCandidates.add(candidateCall);
                    }
                }
            }
            else {
                // Explicit type arguments passed

                final List<JetTypeProjection> jetTypeArguments = task.getTypeArguments();

                for (JetTypeProjection typeArgument : jetTypeArguments) {
                    if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
//                        temporaryTrace.getErrorHandler().genericError(typeArgument.getNode(), "Projections are not allowed on type parameters for methods"); // TODO : better positioning
                        temporaryTrace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeArgument));
                    }
                }

                int expectedTypeArgumentCount = candidate.getTypeParameters().size();
                if (expectedTypeArgumentCount == jetTypeArguments.size()) {
                    List<JetType> typeArguments = new ArrayList<JetType>();
                    for (JetTypeProjection projection : jetTypeArguments) {
                        // TODO : check that there's no projection
                        JetTypeReference typeReference = projection.getTypeReference();
                        if (typeReference != null) {
                            typeArguments.add(new TypeResolver(semanticServices, temporaryTrace, true).resolveType(scope, typeReference));
                        }
                    }

                    checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, temporaryTrace);

                    Map<TypeConstructor, TypeProjection> substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
                    D substitutedFunctionDescriptor = (D) candidate.substitute(TypeSubstitutor.create(substitutionContext));

                    Function<ValueParameterDescriptor, ValueParameterDescriptor> mapFunction = createMapFunction(substitutedFunctionDescriptor);
                    if (checkValueArgumentTypes(scope, temporaryTrace, argumentsToParameters, dirty, mapFunction)
                            && checkReceiver(task, tracing, substitutedFunctionDescriptor, temporaryTrace)) {
                        successfulCandidates.add(candidateCall.setResultingDescriptor(substitutedFunctionDescriptor));
                    }
                    else {
                        failedCandidates.add(candidateCall);
                    }
                }
                else {
                    failedCandidates.add(candidateCall);
//                    tracing.reportWrongTypeArguments(temporaryTrace, "Number of type arguments does not match " + DescriptorRenderer.TEXT.render(candidate));
                    tracing.wrongNumberOfTypeArguments(temporaryTrace, expectedTypeArgumentCount);
                }
            }
            
            if (dirty.get()) {
                dirtyCandidates.add(candidateCall);
            }
            task.performAdvancedChecks(candidate, temporaryTrace, tracing);
        }

        OverloadResolutionResults<D> results = computeResultAndReportErrors(trace, tracing, successfulCandidates, failedCandidates, dirtyCandidates);
        if (!results.singleDescriptor()) {
            checkTypesWithNoCallee(trace, scope, task.getTypeArguments(), task.getValueArguments(), task.getFunctionLiteralArguments());
        }
        return results;
    }

    private void checkTypesWithNoCallee(BindingTrace trace, JetScope scope, List<JetTypeProjection> typeArguments, List<? extends ValueArgument> valueArguments, List<JetExpression> functionLiteralArguments) {
        for (ValueArgument valueArgument : valueArguments) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                typeInferrer.getServices(trace).getType(scope, argumentExpression, NO_EXPECTED_TYPE);
            }
        }

        for (JetExpression expression : functionLiteralArguments) {
            typeInferrer.getServices(trace).getType(scope, expression, NO_EXPECTED_TYPE);
        }

        for (JetTypeProjection typeProjection : typeArguments) {
            new TypeResolver(semanticServices, trace, true).resolveType(scope, typeProjection.getTypeReference());
        }
    }

    private <D extends CallableDescriptor> Function<ValueParameterDescriptor, ValueParameterDescriptor> createMapFunction(D substitutedFunctionDescriptor) {
        assert substitutedFunctionDescriptor != null;
        final Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameterDescriptor : substitutedFunctionDescriptor.getValueParameters()) {
            parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
        }

        return new Function<ValueParameterDescriptor, ValueParameterDescriptor>() {
            @Override
            public ValueParameterDescriptor apply(ValueParameterDescriptor input) {
                return parameterMap.get(input.getOriginal());
            }
        };
    }

    private <D extends CallableDescriptor> boolean checkReceiver(ResolutionTask<D> task, TracingStrategy tracing, D candidate, TemporaryBindingTrace temporaryTrace) {
        if (!checkReceiverAbsence(task, tracing, candidate, temporaryTrace)) return false;
        ReceiverDescriptor receiver = task.getReceiver();
        ReceiverDescriptor candidateReceiver = candidate.getReceiver();
        if (receiver.exists()
                && candidateReceiver.exists()
                && !semanticServices.getTypeChecker().isSubtypeOf(receiver.getType(), candidateReceiver.getType())) {
            tracing.missingReceiver(temporaryTrace, candidateReceiver);
            return false;
        }
        return true;
    }

    private <D extends CallableDescriptor> boolean checkReceiverAbsence(ResolutionTask<D> task, TracingStrategy tracing, D candidate, TemporaryBindingTrace temporaryTrace) {
        ReceiverDescriptor receiver = task.getReceiver();
        ReceiverDescriptor candidateReceiver = candidate.getReceiver();
        if (receiver.exists()) {
            if (!candidateReceiver.exists()) {
                tracing.noReceiverAllowed(temporaryTrace);
                return false;
            }
        }
        else if (candidateReceiver.exists()) {
            tracing.missingReceiver(temporaryTrace, candidateReceiver);
            return false;
        }
        return true;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> computeResultAndReportErrors(
            BindingTrace trace,
            TracingStrategy tracing,
            Set<ResolvedCall<D>> successfulCandidates, // original -> substituted
            Set<ResolvedCall<D>> failedCandidates,
            Set<ResolvedCall<D>> dirtyCandidates) {
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific
        if (successfulCandidates.size() > 0) {
            if (successfulCandidates.size() != 1) {
                Set<ResolvedCall<D>> cleanCandidates = Sets.newLinkedHashSet(successfulCandidates);
                cleanCandidates.removeAll(dirtyCandidates);
                if (cleanCandidates.isEmpty()) {
                    cleanCandidates = successfulCandidates;
                }
                ResolvedCall<D> maximallySpecific = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, false);
                if (maximallySpecific != null) {
                    return OverloadResolutionResults.success(maximallySpecific);
                }

                ResolvedCall<D> maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return OverloadResolutionResults.success(maximallySpecificGenericsDiscriminated);
                }

                Set<ResolvedCall<D>> noOverrides = OverridingUtil.filterOverrides(successfulCandidates, MAP_TO_RESULT);
                if (dirtyCandidates.isEmpty()) {
//                    tracing.reportOverallResolutionError(trace, "Overload resolution ambiguity: "
//                                                                + makeErrorMessageForMultipleDescriptors(noOverrides));
                    tracing.ambiguity(trace, noOverrides);
                }

                tracing.recordAmbiguity(trace, noOverrides);

                return OverloadResolutionResults.ambiguity(noOverrides);
            }
            else {
                ResolvedCall<D> result = successfulCandidates.iterator().next();

                TemporaryBindingTrace temporaryTrace = result.getTrace();
                temporaryTrace.commit();
                return OverloadResolutionResults.success(result);
            }
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() != 1) {
                Set<ResolvedCall<D>> noOverrides = OverridingUtil.filterOverrides(failedCandidates, MAP_TO_CANDIDATE);
                if (noOverrides.size() != 1) {
//                    tracing.reportOverallResolutionError(trace, "None of the following functions can be called with the arguments supplied: "
//                                                                + makeErrorMessageForMultipleDescriptors(noOverrides));
                    tracing.noneApplicable(trace, noOverrides);
                    tracing.recordAmbiguity(trace, noOverrides);
                    return OverloadResolutionResults.manyFailedCandidates(noOverrides);
                }
                failedCandidates = noOverrides;
            }
            ResolvedCall<D> failed = failedCandidates.iterator().next();
            failed.getTrace().commit();
            return OverloadResolutionResults.singleFailedCandidate(failed);
        }
        else {
            tracing.unresolvedReference(trace);
            return OverloadResolutionResults.nameNotFound();
        }
    }

    private boolean checkValueArgumentTypes(
            JetScope scope,
            BindingTrace temporaryTrace,
            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters,
            Ref<Boolean> dirty,
            Function<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap) {
        boolean result = true;
        for (Map.Entry<ValueArgument, ValueParameterDescriptor> entry : argumentsToParameters.entrySet()) {
            ValueArgument valueArgument = entry.getKey();
            ValueParameterDescriptor valueParameterDescriptor = entry.getValue();

            ValueParameterDescriptor substitutedParameter = parameterMap.apply(valueParameterDescriptor);

            assert substitutedParameter != null;

            JetType parameterType = substitutedParameter.getOutType();
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);
                JetType type = temporaryServices.getType(scope, argumentExpression, parameterType);
                if (type == null) {
                    dirty.set(true);
                }
                else if (!semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
//                    VariableDescriptor variableDescriptor = AutoCastUtils.getVariableDescriptorFromSimpleName(temporaryTrace.getBindingContext(), argumentExpression);
//                    if (variableDescriptor != null) {
//                        JetType autoCastType = null;
//                        for (JetType possibleType : dataFlowInfo.getPossibleTypesForVariable(variableDescriptor)) {
//                            if (semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
//                                autoCastType = possibleType;
//                                break;
//                            }
//                        }
//                        if (autoCastType != null) {
//                            if (AutoCastUtils.isAutocastable(variableDescriptor)) {
//                                temporaryTrace.record(AUTOCAST, argumentExpression, autoCastType);
//                            }
//                            else {
//                                temporaryTrace.report(AUTOCAST_IMPOSSIBLE.on(argumentExpression, autoCastType, variableDescriptor));
//                                result = false;
//                            }
//                        }
//                    }
//                    else {
                        result = false;
//                    }
                }
            }
        }
        return result;
    }

    public void checkGenericBoundsInAFunctionCall(List<JetTypeProjection> jetTypeArguments, List<JetType> typeArguments, CallableDescriptor functionDescriptor, BindingTrace trace) {
        Map<TypeConstructor, TypeProjection> context = Maps.newHashMap();

        List<TypeParameterDescriptor> typeParameters = functionDescriptor.getOriginal().getTypeParameters();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor typeParameter = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            context.put(typeParameter.getTypeConstructor(), new TypeProjection(typeArgument));
        }
        TypeSubstitutor substitutor = TypeSubstitutor.create(context);
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
            JetType typeArgument = typeArguments.get(i);
            JetTypeReference typeReference = jetTypeArguments.get(i).getTypeReference();
            assert typeReference != null;
            semanticServices.getClassDescriptorResolver(trace).checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor);
        }
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver, @NotNull String name, @NotNull List<JetType> parameterTypes) {
        List<FunctionDescriptor> result = findCandidatesByExactSignature(scope, receiver, name, parameterTypes);

        BindingTraceContext trace = new BindingTraceContext();
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(trace);
        Set<ResolvedCall<FunctionDescriptor>> candidates = Sets.newLinkedHashSet();
        for (FunctionDescriptor functionDescriptor : result) {
            ResolvedCall<FunctionDescriptor> resolvedCall = ResolvedCall.create(functionDescriptor);
            candidates.add(resolvedCall.setResultingDescriptor(functionDescriptor));
            resolvedCall.setTrace(temporaryBindingTrace);
        }
        return computeResultAndReportErrors(trace, TracingStrategy.EMPTY, candidates, Collections.<ResolvedCall<FunctionDescriptor>>emptySet(), Collections.<ResolvedCall<FunctionDescriptor>>emptySet());
    }

    private List<FunctionDescriptor> findCandidatesByExactSignature(JetScope scope, ReceiverDescriptor receiver, String name, List<JetType> parameterTypes) {
        List<FunctionDescriptor> result = Lists.newArrayList();
        if (receiver.exists()) {
            Set<FunctionDescriptor> extensionFunctionDescriptors = scope.getFunctions(name);
            List<FunctionDescriptor> nonlocal = Lists.newArrayList();
            List<FunctionDescriptor> local = Lists.newArrayList();
            TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


            if (findExtensionFunctions(local, receiver, parameterTypes, result)) {
                return result;
            }

            Set<FunctionDescriptor> functionDescriptors = receiver.getType().getMemberScope().getFunctions(name);
            if (lookupExactSignature(functionDescriptors, parameterTypes, result)) {
                return result;

            }
            findExtensionFunctions(nonlocal, receiver, parameterTypes, result);
            return result;
        }
        else {
            lookupExactSignature(scope.getFunctions(name), parameterTypes, result);
            return result;
        }
    }

    private boolean lookupExactSignature(Set<FunctionDescriptor> candidates, List<JetType> parameterTypes, List<FunctionDescriptor> result) {
        boolean found = false;
        for (FunctionDescriptor functionDescriptor : candidates) {
            if (functionDescriptor.getReceiver().exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes)) continue;
            result.add(functionDescriptor);
            found = true;
        }
        return found;
    }

    private boolean findExtensionFunctions(Collection<FunctionDescriptor> candidates, ReceiverDescriptor receiver, List<JetType> parameterTypes, List<FunctionDescriptor> result) {
        boolean found = false;
        for (FunctionDescriptor functionDescriptor : candidates) {
            ReceiverDescriptor functionReceiver = functionDescriptor.getReceiver();
            if (!functionReceiver.exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!semanticServices.getTypeChecker().isSubtypeOf(receiver.getType(), functionReceiver.getType())) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes))continue;
            result.add(functionDescriptor);
            found = true;
        }
        return found;
    }

    private boolean checkValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull List<JetType> parameterTypes) {
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        if (valueParameters.size() != parameterTypes.size()) return false;
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameter = valueParameters.get(i);
            JetType expectedType = parameterTypes.get(i);
            if (!semanticServices.getTypeChecker().equalTypes(expectedType, valueParameter.getOutType())) return false;
        }
        return true;
    }
}
