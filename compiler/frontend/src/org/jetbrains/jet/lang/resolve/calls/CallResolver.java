/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastServiceImpl;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.OperatorConventions;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolutionStatus.*;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl.MAP_TO_CANDIDATE;
import static org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl.MAP_TO_RESULT;
import static org.jetbrains.jet.lang.resolve.calls.inference.ConstraintType.*;
import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {
    private static final JetType DONT_CARE = ErrorUtils.createErrorTypeWithCustomDebugName("DONT_CARE");

    private final JetTypeChecker typeChecker = JetTypeChecker.INSTANCE;

    @NotNull
    private OverloadingConflictResolver overloadingConflictResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private TypeResolver typeResolver;
    @NotNull
    private DescriptorResolver descriptorResolver;


    @Inject
    public void setOverloadingConflictResolver(@NotNull OverloadingConflictResolver overloadingConflictResolver) {
        this.overloadingConflictResolver = overloadingConflictResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }



    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo) {
        JetExpression calleeExpression = call.getCalleeExpression();
        assert calleeExpression instanceof JetSimpleNameExpression;
        JetSimpleNameExpression nameExpression = (JetSimpleNameExpression) calleeExpression;
        String referencedName = nameExpression.getReferencedName();
        if (referencedName == null) {
            return OverloadResolutionResultsImpl.nameNotFound();
        }
        TaskPrioritizer<VariableDescriptor> task_prioritizer;
        if (nameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
            referencedName = referencedName.substring(1);
            task_prioritizer = TaskPrioritizers.PROPERTY_TASK_PRIORITIZER;
        }
        else {
            task_prioritizer = TaskPrioritizers.VARIABLE_TASK_PRIORITIZER;
        }
        List<ResolutionTask<VariableDescriptor>> prioritizedTasks = task_prioritizer.computePrioritizedTasks(scope, call, referencedName, trace.getBindingContext(), dataFlowInfo);
        return resolveCallToDescriptor(trace, scope, call, expectedType, prioritizedTasks, nameExpression, dataFlowInfo);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo) {
        return resolveSimpleCallToFunctionDescriptor(trace, scope, call, expectedType, dataFlowInfo);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name,
            @NotNull JetType expectedType, DataFlowInfo dataFlowInfo) {
        List<ResolutionTask<FunctionDescriptor>> tasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(
                scope, call, name, trace.getBindingContext(), dataFlowInfo);
        return doResolveCall(trace, scope, call, expectedType, tasks, functionReference, dataFlowInfo);
    }

    @NotNull
    private OverloadResolutionResults<FunctionDescriptor> resolveSimpleCallToFunctionDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo) {
        List<ResolutionTask<FunctionDescriptor>> prioritizedTasks;
        
        JetExpression calleeExpression = call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            String name = expression.getReferencedName();
            if (name == null) return checkArgumentTypesAndFail(trace, scope, call);

            prioritizedTasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(scope, call, name, trace.getBindingContext(), dataFlowInfo);
            ResolutionTask.DescriptorCheckStrategy abstractConstructorCheck = new ResolutionTask.DescriptorCheckStrategy() {
                @Override
                public <D extends CallableDescriptor> boolean performAdvancedChecks(D descriptor, BindingTrace trace, TracingStrategy tracing) {
                    if (descriptor instanceof ConstructorDescriptor) {
                        Modality modality = ((ConstructorDescriptor) descriptor).getContainingDeclaration().getModality();
                        if (modality == Modality.ABSTRACT) {
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
            PsiElement reportAbsenceOn = valueArgumentList == null ? call.getCallElement() : valueArgumentList;
            if (calleeExpression instanceof JetConstructorCalleeExpression) {
                assert !call.getExplicitReceiver().exists();

                prioritizedTasks = Lists.newArrayList();

                JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
                functionReference = expression.getConstructorReferenceExpression();
                if (functionReference == null) {
                    return checkArgumentTypesAndFail(trace, scope, call); // No type there
                }
                JetTypeReference typeReference = expression.getTypeReference();
                assert typeReference != null;
                JetType constructedType = typeResolver.resolveType(scope, typeReference, trace, true);
                DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                    Set<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                    if (constructors.isEmpty()) {
                        trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                        return checkArgumentTypesAndFail(trace, scope, call);
                    }
                    prioritizedTasks.add(new ResolutionTask<FunctionDescriptor>(TaskPrioritizer.<FunctionDescriptor>convertWithImpliedThis(scope, Collections.<ReceiverDescriptor>singletonList(NO_RECEIVER), constructors), call, DataFlowInfo.EMPTY));
                }
                else {
                    trace.report(NOT_A_CLASS.on(calleeExpression));
                    return checkArgumentTypesAndFail(trace, scope, call);
                }
            }
            else if (calleeExpression instanceof JetThisReferenceExpression) {
                functionReference = (JetThisReferenceExpression) calleeExpression;
                DeclarationDescriptor containingDeclaration = scope.getContainingDeclaration();
                if (containingDeclaration instanceof ConstructorDescriptor) {
                    containingDeclaration = containingDeclaration.getContainingDeclaration();
                }
                assert containingDeclaration instanceof ClassDescriptor;
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

                Set<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                if (constructors.isEmpty()) {
                    trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                    return checkArgumentTypesAndFail(trace, scope, call);
                }
                prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(ResolvedCallImpl.<FunctionDescriptor>convertCollection(constructors), call, DataFlowInfo.EMPTY));
            }
            else if (calleeExpression != null) {
                // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
                JetType calleeType = expressionTypingServices.safeGetType(scope, calleeExpression, NO_EXPECTED_TYPE, trace); // We are actually expecting a function, but there seems to be no easy way of expressing this

                if (!JetStandardClasses.isFunctionType(calleeType)) {
//                    checkTypesWithNoCallee(trace, scope, call);
                    if (!ErrorUtils.isErrorType(calleeType)) {
                        trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
                    }
                    return checkArgumentTypesAndFail(trace, scope, call);
                }
                
                FunctionDescriptorImpl functionDescriptor = new ExpressionAsFunctionDescriptor(scope.getContainingDeclaration(), "[for expression " + calleeExpression.getText() + "]");
                FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, NO_RECEIVER);
                ResolvedCallImpl<FunctionDescriptor> resolvedCall = ResolvedCallImpl.<FunctionDescriptor>create(functionDescriptor);
                resolvedCall.setReceiverArgument(call.getExplicitReceiver());
                prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(
                        Collections.singleton(resolvedCall), call, dataFlowInfo));

                // strictly speaking, this is a hack:
                // we need to pass a reference, but there's no reference in the PSI,
                // so we wrap what we have into a fake reference and pass it on (unwrap on the other end)
                functionReference = new JetFakeReference(calleeExpression);
            }
            else {
//                checkTypesWithNoCallee(trace, scope, call);
                return checkArgumentTypesAndFail(trace, scope, call);
            }
        }

        return resolveCallToDescriptor(trace, scope, call, expectedType, prioritizedTasks, functionReference, dataFlowInfo);
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> checkArgumentTypesAndFail(BindingTrace trace, JetScope scope, Call call) {
        checkTypesWithNoCallee(trace, scope, call);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> resolveCallToDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask<D>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference,
            @NotNull DataFlowInfo dataFlowInfo) {
        return doResolveCall(trace, scope, call, expectedType, prioritizedTasks, reference, dataFlowInfo);
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask<D>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference,
            @NotNull DataFlowInfo dataFlowInfo) {

        ResolutionDebugInfo.Data debugInfo = ResolutionDebugInfo.create();
        trace.record(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, call.getCallElement(), debugInfo);
        trace.record(RESOLUTION_SCOPE, call.getCalleeExpression(), scope);

        debugInfo.set(ResolutionDebugInfo.TASKS, prioritizedTasks);

        TracingStrategy tracing = new TracingStrategy() {
            @Override
            public <D extends CallableDescriptor> void bindReference(@NotNull BindingTrace trace, @NotNull ResolvedCallImpl<D> resolvedCall) {
                D descriptor = resolvedCall.getCandidateDescriptor();
//                if (descriptor instanceof VariableAsFunctionDescriptor) {
//                    VariableAsFunctionDescriptor variableAsFunctionDescriptor = (VariableAsFunctionDescriptor) descriptor;
//                    trace.record(REFERENCE_TARGET, reference, variableAsFunctionDescriptor.getVariableDescriptor());
//                }
//                else {
//                }
                trace.record(RESOLVED_CALL, call.getCalleeExpression(), resolvedCall);
                trace.record(REFERENCE_TARGET, reference, descriptor);
            }

            @Override
            public <D extends CallableDescriptor> void recordAmbiguity(BindingTrace trace, Collection<ResolvedCallImpl<D>> candidates) {
                Collection<D> descriptors = Sets.newHashSet();
                for (ResolvedCallImpl<D> candidate : candidates) {
                    descriptors.add(candidate.getCandidateDescriptor());
                }
                trace.record(AMBIGUOUS_REFERENCE_TARGET, reference, descriptors);
            }

            @Override
            public void unresolvedReference(@NotNull BindingTrace trace) {
                trace.report(UNRESOLVED_REFERENCE.on(reference));
            }

            @Override
            public void noValueForParameter(@NotNull BindingTrace trace, @NotNull ValueParameterDescriptor valueParameter) {
                PsiElement reportOn;
                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                if (valueArgumentList != null) {
                    reportOn = valueArgumentList;
                }
                else {
                    reportOn = reference;
                }
                trace.report(NO_VALUE_FOR_PARAMETER.on(reportOn, valueParameter));
            }

            @Override
            public void missingReceiver(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor expectedReceiver) {
                trace.report(MISSING_RECEIVER.on(reference, expectedReceiver.getType()));
            }

            @Override
            public void wrongReceiverType(@NotNull BindingTrace trace, @NotNull ReceiverDescriptor receiverParameter, @NotNull ReceiverDescriptor receiverArgument) {
                if (receiverArgument instanceof ExpressionReceiver) {
                    ExpressionReceiver expressionReceiver = (ExpressionReceiver) receiverArgument;
                    trace.report(TYPE_MISMATCH.on(expressionReceiver.getExpression(), receiverParameter.getType(), receiverArgument.getType()));
                }
                else {
                    trace.report(TYPE_MISMATCH.on(reference, receiverParameter.getType(), receiverArgument.getType()));
                }
            }

            @Override
            public void noReceiverAllowed(@NotNull BindingTrace trace) {
                trace.report(NO_RECEIVER_ADMITTED.on(reference));
            }

            @Override
            public void wrongNumberOfTypeArguments(@NotNull BindingTrace trace, int expectedTypeArgumentCount) {
                JetTypeArgumentList typeArgumentList = call.getTypeArgumentList();
                if (typeArgumentList != null) {
                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(typeArgumentList, expectedTypeArgumentCount));
                }
                else {
                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(reference, expectedTypeArgumentCount));
                }
            }

            @Override
            public <D extends CallableDescriptor> void ambiguity(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {
                trace.report(OVERLOAD_RESOLUTION_AMBIGUITY.on(call.getCallElement(), descriptors));
            }

            @Override
            public <D extends CallableDescriptor> void noneApplicable(@NotNull BindingTrace trace, @NotNull Collection<ResolvedCallImpl<D>> descriptors) {
                trace.report(NONE_APPLICABLE.on(reference, descriptors));
            }

            @Override
            public void instantiationOfAbstractClass(@NotNull BindingTrace trace) {
                trace.report(CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS.on(call.getCallElement()));
            }

            @Override
            public void typeInferenceFailed(@NotNull BindingTrace trace, SolutionStatus status) {
                assert !status.isSuccessful();
                trace.report(TYPE_INFERENCE_FAILED.on(call.getCallElement(), status));
            }

            @Override
            public void unsafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {
                ASTNode callOperationNode = call.getCallOperationNode();
                if (callOperationNode != null) {
                    trace.report(UNSAFE_CALL.on(callOperationNode.getPsi(), type));
                }
                else {
                    PsiElement callElement = call.getCallElement();
                    if (callElement instanceof JetBinaryExpression) {
                        JetBinaryExpression binaryExpression = (JetBinaryExpression) callElement;
                        JetSimpleNameExpression operationReference = binaryExpression.getOperationReference();

                        String operationString = operationReference.getReferencedNameElementType() == JetTokens.IDENTIFIER ?
                                operationReference.getText() :
                                OperatorConventions.getNameForOperationSymbol((JetToken) operationReference.getReferencedNameElementType());

                        JetExpression right = binaryExpression.getRight();
                        if (right != null) {
                            trace.report(UNSAFE_INFIX_CALL.on(reference, binaryExpression.getLeft().getText(), operationString, right.getText()));
                        }
                    }
                    else {
                        trace.report(UNSAFE_CALL.on(reference, type));
                    }
                }
            }

            @Override
            public void unnecessarySafeCall(@NotNull BindingTrace trace, @NotNull JetType type) {
                ASTNode callOperationNode = call.getCallOperationNode();
                assert callOperationNode != null;
                trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.getPsi(), type));
            }

            @Override
            public void danglingFunctionLiteralArgumentSuspected(@NotNull BindingTrace trace, @NotNull List<JetExpression> functionLiteralArguments) {
                for (JetExpression functionLiteralArgument : functionLiteralArguments) {
                    trace.report(DANGLING_FUNCTION_LITERAL_ARGUMENT_SUSPECTED.on(functionLiteralArgument));
                }
            }
        };

        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<D> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D> task : prioritizedTasks) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            OverloadResolutionResultsImpl<D> results = performResolutionGuardedForExtraFunctionLiteralArguments(temporaryTrace, scope, expectedType, task, tracing, dataFlowInfo);
            if (results.isSuccess()) {
                temporaryTrace.commit();

                debugInfo.set(ResolutionDebugInfo.RESULT, results.getResultingCall());

                return results;
            }
            if (traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty()) {
                traceForFirstNonemptyCandidateSet = temporaryTrace;
                resultsForFirstNonemptyCandidateSet = results;
            }
        }
        if (traceForFirstNonemptyCandidateSet != null) {
            traceForFirstNonemptyCandidateSet.commit();
            if (resultsForFirstNonemptyCandidateSet.singleResult()) {

                debugInfo.set(ResolutionDebugInfo.RESULT, resultsForFirstNonemptyCandidateSet.getResultingCall());
            }
        }
        else {
            trace.report(UNRESOLVED_REFERENCE.on(reference));
            checkTypesWithNoCallee(trace, scope, call);
        }
        return resultsForFirstNonemptyCandidateSet != null ? resultsForFirstNonemptyCandidateSet : OverloadResolutionResultsImpl.<D>nameNotFound();
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> performResolutionGuardedForExtraFunctionLiteralArguments(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope, @NotNull JetType expectedType,
            @NotNull ResolutionTask<D> task, @NotNull TracingStrategy tracing, @NotNull DataFlowInfo dataFlowInfo
    ) {
        OverloadResolutionResultsImpl<D> results = performResolution(trace, scope, expectedType, task, tracing, dataFlowInfo);

        // If resolution fails, we should check for some of the following situations:
        //   class A {
        //     val foo = Bar() // The following is intended to be an anonymous initializer,
        //                     // but is treated as a function literal argument
        //     {
        //       ...
        //     }
        //  }
        //
        //  fun foo() {
        //    bar {
        //      buzz()
        //      {...} // intended to be a returned from the outer literal
        //    }
        //  }
        EnumSet<OverloadResolutionResults.Code> someFailed = EnumSet.of(OverloadResolutionResults.Code.MANY_FAILED_CANDIDATES,
                                                                        OverloadResolutionResults.Code.SINGLE_CANDIDATE_ARGUMENT_MISMATCH);
        if (someFailed.contains(results.getResultCode()) && !task.getCall().getFunctionLiteralArguments().isEmpty()) {
            // We have some candidates that failed for some reason
            // And we have a suspect: the function literal argument
            // Now, we try to remove this argument and see if it helps
            Collection<ResolvedCallImpl<D>> newCandidates = Lists.newArrayList();
            for (ResolvedCallImpl<D> candidate : task.getCandidates()) {
                newCandidates.add(ResolvedCallImpl.create(candidate.getCandidateDescriptor()));
            }
            ResolutionTask<D> newTask = new ResolutionTask<D>(newCandidates, new DelegatingCall(task.getCall()) {
                @NotNull
                @Override
                public List<JetExpression> getFunctionLiteralArguments() {
                    return Collections.emptyList();
                }
            }, task.getDataFlowInfo());
            OverloadResolutionResultsImpl<D> resultsWithFunctionLiteralsStripped = performResolution(TemporaryBindingTrace.create(trace), scope, expectedType, newTask, tracing, dataFlowInfo);
            if (resultsWithFunctionLiteralsStripped.isSuccess() || resultsWithFunctionLiteralsStripped.isAmbiguity()) {
                tracing.danglingFunctionLiteralArgumentSuspected(trace, task.getCall().getFunctionLiteralArguments());
            }
        }

        return results;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> performResolution(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope, @NotNull JetType expectedType,
            @NotNull ResolutionTask<D> task, @NotNull TracingStrategy tracing, DataFlowInfo dataFlowInfo
    ) {
        for (ResolvedCallImpl<D> candidateCall : task.getCandidates()) {
            D candidate = candidateCall.getCandidateDescriptor();
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            candidateCall.setTrace(temporaryTrace);

            tracing.bindReference(temporaryTrace, candidateCall);
            
            if (ErrorUtils.isError(candidate)) {
                candidateCall.setStatus(SUCCESS);
                checkTypesWithNoCallee(temporaryTrace, scope, task.getCall());
                continue;
            }

            boolean errorInArgumentMapping = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(task, tracing, candidateCall);
            if (errorInArgumentMapping) {
                candidateCall.setStatus(OTHER_ERROR);
                checkTypesWithNoCallee(temporaryTrace, scope, task.getCall());
                continue;
            }

            List<JetTypeProjection> jetTypeArguments = task.getCall().getTypeArguments();
            if (jetTypeArguments.isEmpty()) {
                if (!candidate.getTypeParameters().isEmpty()) {
                    // Type argument inference

                    ResolutionDebugInfo.Data debugInfo = trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, task.getCall().getCallElement());

                    ConstraintSystem constraintSystem = new ConstraintSystemWithPriorities(new DebugConstraintResolutionListener(candidateCall, debugInfo));

                    // If the call is recursive, e.g.
                    //   fun foo<T>(t : T) : T = foo(t)
                    // we can't use same descriptor objects for T's as actual type values and same T's as unknowns,
                    // because constraints become trivial (T :< T), and inference fails
                    //
                    // Thus, we replace the parameters of our descriptor with fresh objects (perform alpha-conversion)
                    CallableDescriptor candidateWithFreshVariables = FunctionDescriptorUtil.alphaConvertTypeParameters(candidate);


                    for (TypeParameterDescriptor typeParameterDescriptor : candidateWithFreshVariables.getTypeParameters()) {
                        constraintSystem.registerTypeVariable(typeParameterDescriptor, Variance.INVARIANT); // TODO: variance of the occurrences
                    }

                    TypeSubstitutor substituteDontCare = ConstraintSystemImpl.makeConstantSubstitutor(candidateWithFreshVariables.getTypeParameters(), DONT_CARE);

                    // Value parameters
                    for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet()) {
                        ResolvedValueArgument valueArgument = entry.getValue();
                        ValueParameterDescriptor valueParameterDescriptor = candidateWithFreshVariables.getValueParameters().get(entry.getKey().getIndex());

                        JetType effectiveExpectedType = getEffectiveExpectedType(valueParameterDescriptor);

                        for (JetExpression expression : valueArgument.getArgumentExpressions()) {
                            // TODO : more attempts, with different expected types

                            // Here we type check expecting an error type (that is a subtype of any type and a supertype of any type
                            // and throw the results away
                            // We'll type check the arguments later, with the inferred types expected
                            TemporaryBindingTrace traceForUnknown = TemporaryBindingTrace.create(temporaryTrace);
                            JetType type = expressionTypingServices.getType(scope, expression, substituteDontCare.substitute(valueParameterDescriptor.getType(), Variance.INVARIANT), traceForUnknown);
                            if (type != null && !ErrorUtils.isErrorType(type)) {
                                constraintSystem.addSubtypingConstraint(VALUE_ARGUMENT.assertSubtyping(type, effectiveExpectedType));
                            }
                            else {
                                candidateCall.argumentHasNoType();
                            }
                        }
                    }

                    // Receiver
                    // Error is already reported if something is missing
                    ReceiverDescriptor receiverArgument = candidateCall.getReceiverArgument();
                    ReceiverDescriptor receiverParameter = candidateWithFreshVariables.getReceiverParameter();
                    if (receiverArgument.exists() && receiverParameter.exists()) {
                        constraintSystem.addSubtypingConstraint(RECEIVER.assertSubtyping(receiverArgument.getType(), receiverParameter.getType()));
                    }

                    // Return type
                    if (expectedType != NO_EXPECTED_TYPE) {
                        constraintSystem.addSubtypingConstraint(EXPECTED_TYPE.assertSubtyping(candidateWithFreshVariables.getReturnType(), expectedType));
                    }

                    // Solution
                    ConstraintSystemSolution solution = constraintSystem.solve();
                    if (solution.getStatus().isSuccessful()) {
                        D substitute = (D) candidateWithFreshVariables.substitute(solution.getSubstitutor());
                        assert substitute != null;
                        replaceValueParametersWithSubstitutedOnes(candidateCall, substitute);
                        candidateCall.setResultingDescriptor(substitute);

                        for (TypeParameterDescriptor typeParameterDescriptor : candidateCall.getCandidateDescriptor().getTypeParameters()) {
                            candidateCall.recordTypeArgument(typeParameterDescriptor, solution.getValue(candidateWithFreshVariables.getTypeParameters().get(typeParameterDescriptor.getIndex())));
                        }

                        // Here we type check the arguments with inferred types expected
                        checkValueArgumentTypes(scope, candidateCall, dataFlowInfo);

                        candidateCall.setStatus(SUCCESS);
                    }
                    else {
                        tracing.typeInferenceFailed(temporaryTrace, solution.getStatus());
                        candidateCall.setStatus(OTHER_ERROR.combine(checkAllValueArguments(scope, tracing, task, candidateCall, dataFlowInfo)));
                    }
                }
                else {
                    candidateCall.setStatus(checkAllValueArguments(scope, tracing, task, candidateCall, dataFlowInfo));
                }
            }
            else {
                // Explicit type arguments passed

                List<JetType> typeArguments = new ArrayList<JetType>();
                for (JetTypeProjection projection : jetTypeArguments) {
                    if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                        temporaryTrace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                    }
                    JetTypeReference typeReference = projection.getTypeReference();
                    if (typeReference != null) {
                        typeArguments.add(typeResolver.resolveType(scope, typeReference, temporaryTrace, true));
                    }
                    else {
                        typeArguments.add(ErrorUtils.createErrorType("Star projection in a call"));
                    }
                }
                int expectedTypeArgumentCount = candidate.getTypeParameters().size();
                if (expectedTypeArgumentCount == jetTypeArguments.size()) {

                    checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, temporaryTrace);
                    
                    Map<TypeConstructor, TypeProjection> substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
                    D substitutedDescriptor = (D) candidate.substitute(TypeSubstitutor.create(substitutionContext));

                    candidateCall.setResultingDescriptor(substitutedDescriptor);
                    replaceValueParametersWithSubstitutedOnes(candidateCall, substitutedDescriptor);

                    List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
                    for (int i = 0; i < typeParameters.size(); i++) {
                        TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                        candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
                    }
                    candidateCall.setStatus(checkAllValueArguments(scope, tracing, task, candidateCall, dataFlowInfo));
                }
                else {
                    candidateCall.setStatus(OTHER_ERROR);
                    tracing.wrongNumberOfTypeArguments(temporaryTrace, expectedTypeArgumentCount);
                }
            }
            
            task.performAdvancedChecks(candidate, temporaryTrace, tracing);

            // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
            // See TaskPrioritizer for more
            JetSuperExpression superExpression = TaskPrioritizer.getReceiverSuper(candidateCall.getReceiverArgument());
            if (superExpression != null) {
                temporaryTrace.report(SUPER_IS_NOT_AN_EXPRESSION.on(superExpression, superExpression.getText()));
                candidateCall.setStatus(OTHER_ERROR);
            }

            recordAutoCastIfNecessary(candidateCall.getReceiverArgument(), candidateCall.getTrace());
            recordAutoCastIfNecessary(candidateCall.getThisObject(), candidateCall.getTrace());
        }

        Set<ResolvedCallImpl<D>> successfulCandidates = Sets.newLinkedHashSet();
        Set<ResolvedCallImpl<D>> failedCandidates = Sets.newLinkedHashSet();
        for (ResolvedCallImpl<D> candidateCall : task.getCandidates()) {
            ResolutionStatus status = candidateCall.getStatus();
            if (status.isSuccess()) {
                successfulCandidates.add(candidateCall);
            }
            else {
                assert status != UNKNOWN_STATUS : "No resolution for " + candidateCall.getCandidateDescriptor();
                failedCandidates.add(candidateCall);
            }
        }
        
        OverloadResolutionResultsImpl<D> results = computeResultAndReportErrors(trace, tracing, successfulCandidates, failedCandidates);
        if (!results.singleResult()) {
            checkTypesWithNoCallee(trace, scope, task.getCall());
        }
        return results;
    }

    private static JetType getEffectiveExpectedType(ValueParameterDescriptor valueParameterDescriptor) {
        JetType effectiveExpectedType = valueParameterDescriptor.getVarargElementType();
        if (effectiveExpectedType == null) {
            effectiveExpectedType = valueParameterDescriptor.getType();
        }
        return effectiveExpectedType;
    }

    private static void recordAutoCastIfNecessary(ReceiverDescriptor receiver, BindingTrace trace) {
        if (receiver instanceof AutoCastReceiver) {
            AutoCastReceiver autoCastReceiver = (AutoCastReceiver) receiver;
            ReceiverDescriptor original = autoCastReceiver.getOriginal();
            if (original instanceof ExpressionReceiver) {
                ExpressionReceiver expressionReceiver = (ExpressionReceiver) original;
                if (autoCastReceiver.canCast()) {
                    trace.record(AUTOCAST, expressionReceiver.getExpression(), autoCastReceiver.getType());
                }
                else {
                    trace.report(AUTOCAST_IMPOSSIBLE.on(expressionReceiver.getExpression(), autoCastReceiver.getType(), expressionReceiver.getExpression().getText()));
                }
            }
            else {
                assert autoCastReceiver.canCast() : "A non-expression receiver must always be autocastabe: " + original;
            }
        }
    }

    private void checkTypesWithNoCallee(BindingTrace trace, JetScope scope, Call call) {
        for (ValueArgument valueArgument : call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(scope, argumentExpression, NO_EXPECTED_TYPE, trace);
            }
        }

        for (JetExpression expression : call.getFunctionLiteralArguments()) {
            expressionTypingServices.getType(scope, expression, NO_EXPECTED_TYPE, trace);
        }

        for (JetTypeProjection typeProjection : call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(scope, typeReference, trace, true);
            }
        }
    }

    private static <D extends CallableDescriptor> void replaceValueParametersWithSubstitutedOnes(
            ResolvedCallImpl<D> candidateCall, @NotNull D substitutedDescriptor) {

        Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameterDescriptor : substitutedDescriptor.getValueParameters()) {
            parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
        }

        Map<ValueParameterDescriptor, ResolvedValueArgument> valueArguments = candidateCall.getValueArguments();
        Map<ValueParameterDescriptor, ResolvedValueArgument> originalValueArguments = Maps.newHashMap(valueArguments);
        valueArguments.clear();
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : originalValueArguments.entrySet()) {
            ValueParameterDescriptor substitutedVersion = parameterMap.get(entry.getKey().getOriginal());
            assert substitutedVersion != null : entry.getKey();
            valueArguments.put(substitutedVersion, entry.getValue());
        }
    }

    private <D extends CallableDescriptor> ResolutionStatus checkAllValueArguments(JetScope scope, TracingStrategy tracing, ResolutionTask<D> task, ResolvedCallImpl<D> candidateCall, DataFlowInfo dataFlowInfo) {
        ResolutionStatus result = checkValueArgumentTypes(scope, candidateCall, dataFlowInfo);
        result = result.combine(checkReceiver(tracing, candidateCall, candidateCall.getResultingDescriptor().getReceiverParameter(), candidateCall.getReceiverArgument(), task));
        result = result.combine(checkReceiver(tracing, candidateCall, candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject(), task));
        return result;
    }

    private <D extends CallableDescriptor> ResolutionStatus checkReceiver(TracingStrategy tracing, ResolvedCallImpl<D> candidateCall,
                                                                          ReceiverDescriptor receiverParameter, ReceiverDescriptor receiverArgument,
                                                                          ResolutionTask<D> task) {
        ResolutionStatus result = SUCCESS;
        if (receiverParameter.exists() && receiverArgument.exists()) {
            ASTNode callOperationNode = task.getCall().getCallOperationNode();
            boolean safeAccess = callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
            JetType receiverArgumentType = receiverArgument.getType();
            AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(task.getDataFlowInfo(), candidateCall.getTrace().getBindingContext());
            if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {
                tracing.unsafeCall(candidateCall.getTrace(), receiverArgumentType);
                result = UNSAFE_CALL_ERROR;
            }
            else {
                JetType effectiveReceiverArgumentType = safeAccess
                                                        ? TypeUtils.makeNotNullable(receiverArgumentType)
                                                        : receiverArgumentType;
                if (!typeChecker.isSubtypeOf(effectiveReceiverArgumentType, receiverParameter.getType())) {
                    tracing.wrongReceiverType(candidateCall.getTrace(), receiverParameter, receiverArgument);
                    result = OTHER_ERROR;
                }
            }

            if (safeAccess && (receiverParameter.getType().isNullable() || !receiverArgumentType.isNullable())) {
                tracing.unnecessarySafeCall(candidateCall.getTrace(), receiverArgumentType);
            }
        }
        return result;
    }

    private <D extends CallableDescriptor> ResolutionStatus checkValueArgumentTypes(JetScope scope, ResolvedCallImpl<D> candidateCall, DataFlowInfo dataFlowInfo) {
        ResolutionStatus result = SUCCESS;
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : candidateCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();

            JetType parameterType = getEffectiveExpectedType(parameterDescriptor);

            List<JetExpression> argumentExpressions = resolvedArgument.getArgumentExpressions();
            for (JetExpression argumentExpression : argumentExpressions) {
                JetType type = expressionTypingServices.getType(scope, argumentExpression, parameterType, dataFlowInfo, candidateCall.getTrace());
                if (type == null || ErrorUtils.isErrorType(type)) {
                    candidateCall.argumentHasNoType();
                }
                else if (!typeChecker.isSubtypeOf(type, parameterType)) {
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
//                            if (AutoCastUtils.isStableVariable(variableDescriptor)) {
//                                temporaryTrace.record(AUTOCAST, argumentExpression, autoCastType);
//                            }
//                            else {
//                                temporaryTrace.report(AUTOCAST_IMPOSSIBLE.on(argumentExpression, autoCastType, variableDescriptor));
//                                result = false;
//                            }
//                        }
//                    }
//                    else {
                    result = OTHER_ERROR;
                }
            }
        }
        return result;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> computeResultAndReportErrors(
            BindingTrace trace,
            TracingStrategy tracing,
            Set<ResolvedCallImpl<D>> successfulCandidates,
            Set<ResolvedCallImpl<D>> failedCandidates) {
        // TODO : maybe it's better to filter overrides out first, and only then look for the maximally specific

        if (successfulCandidates.size() > 0) {
            OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(successfulCandidates, true);
            if (results.isAmbiguity()) {
                // This check is needed for the following case:
                //    x.foo(unresolved) -- if there are multiple foo's, we'd report an ambiguity, and it does not make sense here
                if (allClean(results.getResultingCalls())) {
                    tracing.ambiguity(trace, results.getResultingCalls());
                }
                tracing.recordAmbiguity(trace, results.getResultingCalls());
            }
            return results;
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() != 1) {
                // This is needed when there are several overloads some of which are OK but for nullability of the receiver,
                // and some are not OK at all. In this case we'd like to say "unsafe call" rather than "none applicable"
                // Used to be: weak errors. Generalized for future extensions
                for (EnumSet<ResolutionStatus> severityLevel : SEVERITY_LEVELS) {
                    Set<ResolvedCallImpl<D>> thisLevel = Sets.newLinkedHashSet();
                    for (ResolvedCallImpl<D> candidate : failedCandidates) {
                        if (severityLevel.contains(candidate.getStatus())) {
                            thisLevel.add(candidate);
                        }
                    }
                    if (!thisLevel.isEmpty()) {
                        OverloadResolutionResultsImpl<D> results = chooseAndReportMaximallySpecific(thisLevel, false);
                        if (results.isSuccess()) {
                            results.getResultingCall().getTrace().commit();
                            return OverloadResolutionResultsImpl.singleFailedCandidate(results.getResultingCall());
                        }

                        tracing.noneApplicable(trace, results.getResultingCalls());
                        tracing.recordAmbiguity(trace, results.getResultingCalls());
                        return OverloadResolutionResultsImpl.manyFailedCandidates(results.getResultingCalls());
                    }
                }

                assert false : "Should not be reachable, cause every status must belong to some level";

                Set<ResolvedCallImpl<D>> noOverrides = OverridingUtil.filterOverrides(failedCandidates, MAP_TO_CANDIDATE);
                if (noOverrides.size() != 1) {
                    tracing.noneApplicable(trace, noOverrides);
                    tracing.recordAmbiguity(trace, noOverrides);
                    return OverloadResolutionResultsImpl.manyFailedCandidates(noOverrides);
                }

                failedCandidates = noOverrides;
            }

            ResolvedCallImpl<D> failed = failedCandidates.iterator().next();
            failed.getTrace().commit();
            return OverloadResolutionResultsImpl.singleFailedCandidate(failed);
        }
        else {
            tracing.unresolvedReference(trace);
            return OverloadResolutionResultsImpl.nameNotFound();
        }
    }

    private static <D extends CallableDescriptor> boolean allClean(Collection<ResolvedCallImpl<D>> results) {
        for (ResolvedCallImpl<D> result : results) {
            if (result.isDirty()) return false;
        }
        return true;
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> chooseAndReportMaximallySpecific(Set<ResolvedCallImpl<D>> candidates, boolean discriminateGenerics) {
        if (candidates.size() != 1) {
            Set<ResolvedCallImpl<D>> cleanCandidates = Sets.newLinkedHashSet(candidates);
            for (Iterator<ResolvedCallImpl<D>> iterator = cleanCandidates.iterator(); iterator.hasNext(); ) {
                ResolvedCallImpl<D> candidate = iterator.next();
                if (candidate.isDirty()) {
                    iterator.remove();
                }
            }

            if (cleanCandidates.isEmpty()) {
                cleanCandidates = candidates;
            }
            ResolvedCallImpl<D> maximallySpecific = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, false);
            if (maximallySpecific != null) {
                return OverloadResolutionResultsImpl.success(maximallySpecific);
            }

            if (discriminateGenerics) {
                ResolvedCallImpl<D> maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return OverloadResolutionResultsImpl.success(maximallySpecificGenericsDiscriminated);
                }
            }

            Set<ResolvedCallImpl<D>> noOverrides = OverridingUtil.filterOverrides(candidates, MAP_TO_RESULT);

            return OverloadResolutionResultsImpl.ambiguity(noOverrides);
        }
        else {
            ResolvedCallImpl<D> result = candidates.iterator().next();

            TemporaryBindingTrace temporaryTrace = result.getTrace();
            temporaryTrace.commit();
            return OverloadResolutionResultsImpl.success(result);
        }
    }

    private void checkGenericBoundsInAFunctionCall(List<JetTypeProjection> jetTypeArguments, List<JetType> typeArguments, CallableDescriptor functionDescriptor, BindingTrace trace) {
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
            if (typeReference != null) {
                descriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor, trace);
            }
        }
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveExactSignature(@NotNull JetScope scope, @NotNull ReceiverDescriptor receiver, @NotNull String name, @NotNull List<JetType> parameterTypes) {
        List<ResolvedCallImpl<FunctionDescriptor>> result = findCandidatesByExactSignature(scope, receiver, name, parameterTypes);

        BindingTraceContext trace = new BindingTraceContext();
        TemporaryBindingTrace temporaryBindingTrace = TemporaryBindingTrace.create(trace);
        Set<ResolvedCallImpl<FunctionDescriptor>> candidates = Sets.newLinkedHashSet();
        for (ResolvedCallImpl<FunctionDescriptor> call : result) {
            call.setTrace(temporaryBindingTrace);
            candidates.add(call);
        }
        return computeResultAndReportErrors(trace, TracingStrategy.EMPTY, candidates, Collections.<ResolvedCallImpl<FunctionDescriptor>>emptySet());
    }

    private List<ResolvedCallImpl<FunctionDescriptor>> findCandidatesByExactSignature(JetScope scope, ReceiverDescriptor receiver,
                                                                                      String name, List<JetType> parameterTypes) {
        List<ResolvedCallImpl<FunctionDescriptor>> result = Lists.newArrayList();
        if (receiver.exists()) {
            Collection<ResolvedCallImpl<FunctionDescriptor>> extensionFunctionDescriptors = ResolvedCallImpl.convertCollection(scope.getFunctions(name));
            List<ResolvedCallImpl<FunctionDescriptor>> nonlocal = Lists.newArrayList();
            List<ResolvedCallImpl<FunctionDescriptor>> local = Lists.newArrayList();
            TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


            if (findExtensionFunctions(local, receiver, parameterTypes, result)) {
                return result;
            }

            Collection<ResolvedCallImpl<FunctionDescriptor>> functionDescriptors = ResolvedCallImpl.convertCollection(receiver.getType().getMemberScope().getFunctions(name));
            if (lookupExactSignature(functionDescriptors, parameterTypes, result)) {
                return result;

            }
            findExtensionFunctions(nonlocal, receiver, parameterTypes, result);
            return result;
        }
        else {
            lookupExactSignature(ResolvedCallImpl.convertCollection(scope.getFunctions(name)), parameterTypes, result);
            return result;
        }
    }

    private static boolean lookupExactSignature(Collection<ResolvedCallImpl<FunctionDescriptor>> candidates, List<JetType> parameterTypes,
                                                List<ResolvedCallImpl<FunctionDescriptor>> result) {
        boolean found = false;
        for (ResolvedCallImpl<FunctionDescriptor> resolvedCall : candidates) {
            FunctionDescriptor functionDescriptor = resolvedCall.getResultingDescriptor();
            if (functionDescriptor.getReceiverParameter().exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes)) continue;
            result.add(resolvedCall);
            found = true;
        }
        return found;
    }

    private boolean findExtensionFunctions(Collection<ResolvedCallImpl<FunctionDescriptor>> candidates, ReceiverDescriptor receiver,
                                           List<JetType> parameterTypes, List<ResolvedCallImpl<FunctionDescriptor>> result) {
        boolean found = false;
        for (ResolvedCallImpl<FunctionDescriptor> resolvedCall : candidates) {
            FunctionDescriptor functionDescriptor = resolvedCall.getResultingDescriptor();
            ReceiverDescriptor functionReceiver = functionDescriptor.getReceiverParameter();
            if (!functionReceiver.exists()) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!typeChecker.isSubtypeOf(receiver.getType(), functionReceiver.getType())) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes))continue;
            result.add(resolvedCall);
            found = true;
        }
        return found;
    }

    private static boolean checkValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull List<JetType> parameterTypes) {
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        if (valueParameters.size() != parameterTypes.size()) return false;
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameter = valueParameters.get(i);
            JetType expectedType = parameterTypes.get(i);
            if (!TypeUtils.equalTypes(expectedType, valueParameter.getType())) return false;
        }
        return true;
    }
}
