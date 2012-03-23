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
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.AUTOCAST;
import static org.jetbrains.jet.lang.resolve.BindingContext.RESOLUTION_SCOPE;
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
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleProperty(@NotNull BasicResolutionContext context) {
        JetExpression calleeExpression = context.call.getCalleeExpression();
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
        List<ResolutionTask<VariableDescriptor>> prioritizedTasks = task_prioritizer.computePrioritizedTasks(context, referencedName, nameExpression);
        return doResolveCall(context, prioritizedTasks, nameExpression);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull BasicResolutionContext context,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name) {
        List<ResolutionTask<FunctionDescriptor>> tasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(
                context, name, functionReference);
        return doResolveCall(context, tasks, functionReference);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo) {
        return resolveFunctionCall(BasicResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicResolutionContext context) {
        List<ResolutionTask<FunctionDescriptor>> prioritizedTasks;
        
        JetExpression calleeExpression = context.call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            String name = expression.getReferencedName();
            if (name == null) return checkArgumentTypesAndFail(context);

            prioritizedTasks = TaskPrioritizers.FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(context, name, functionReference);
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
            JetValueArgumentList valueArgumentList = context.call.getValueArgumentList();
            PsiElement reportAbsenceOn = valueArgumentList == null ? context.call.getCallElement() : valueArgumentList;
            if (calleeExpression instanceof JetConstructorCalleeExpression) {
                assert !context.call.getExplicitReceiver().exists();

                prioritizedTasks = Lists.newArrayList();

                JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
                functionReference = expression.getConstructorReferenceExpression();
                if (functionReference == null) {
                    return checkArgumentTypesAndFail(context); // No type there
                }
                JetTypeReference typeReference = expression.getTypeReference();
                assert typeReference != null;
                JetType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
                DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
                if (declarationDescriptor instanceof ClassDescriptor) {
                    ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                    Set<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                    if (constructors.isEmpty()) {
                        context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                        return checkArgumentTypesAndFail(context);
                    }
                    Collection<ResolvedCallImpl<FunctionDescriptor>> candidates = TaskPrioritizer.<FunctionDescriptor>convertWithImpliedThis(context.scope, Collections.<ReceiverDescriptor>singletonList(NO_RECEIVER), constructors);
                    prioritizedTasks.add(new ResolutionTask<FunctionDescriptor>(candidates, functionReference, context));  // !! DataFlowInfo.EMPTY
                }
                else {
                    context.trace.report(NOT_A_CLASS.on(calleeExpression));
                    return checkArgumentTypesAndFail(context);
                }
            }
            else if (calleeExpression instanceof JetThisReferenceExpression) {
                functionReference = (JetThisReferenceExpression) calleeExpression;
                DeclarationDescriptor containingDeclaration = context.scope.getContainingDeclaration();
                if (containingDeclaration instanceof ConstructorDescriptor) {
                    containingDeclaration = containingDeclaration.getContainingDeclaration();
                }
                assert containingDeclaration instanceof ClassDescriptor;
                ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;

                Set<ConstructorDescriptor> constructors = classDescriptor.getConstructors();
                if (constructors.isEmpty()) {
                    context.trace.report(NO_CONSTRUCTOR.on(reportAbsenceOn));
                    return checkArgumentTypesAndFail(context);
                }
                List<ResolvedCallImpl<FunctionDescriptor>> candidates = ResolvedCallImpl.<FunctionDescriptor>convertCollection(constructors);
                prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(candidates, functionReference, context)); // !! DataFlowInfo.EMPTY
            }
            else if (calleeExpression != null) {
                // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
                JetType calleeType = expressionTypingServices.safeGetType(context.scope, calleeExpression, NO_EXPECTED_TYPE, context.trace); // We are actually expecting a function, but there seems to be no easy way of expressing this

                if (!JetStandardClasses.isFunctionType(calleeType)) {
//                    checkTypesWithNoCallee(trace, scope, call);
                    if (!ErrorUtils.isErrorType(calleeType)) {
                        context.trace.report(CALLEE_NOT_A_FUNCTION.on(calleeExpression, calleeType));
                    }
                    return checkArgumentTypesAndFail(context);
                }
                
                FunctionDescriptorImpl functionDescriptor = new ExpressionAsFunctionDescriptor(context.scope.getContainingDeclaration(), "[for expression " + calleeExpression.getText() + "]");
                FunctionDescriptorUtil.initializeFromFunctionType(functionDescriptor, calleeType, NO_RECEIVER);
                ResolvedCallImpl<FunctionDescriptor> resolvedCall = ResolvedCallImpl.<FunctionDescriptor>create(functionDescriptor);
                resolvedCall.setReceiverArgument(context.call.getExplicitReceiver());

                // strictly speaking, this is a hack:
                // we need to pass a reference, but there's no reference in the PSI,
                // so we wrap what we have into a fake reference and pass it on (unwrap on the other end)
                functionReference = new JetFakeReference(calleeExpression);

                prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(Collections.singleton(resolvedCall), functionReference, context));
            }
            else {
//                checkTypesWithNoCallee(trace, scope, call);
                return checkArgumentTypesAndFail(context);
            }
        }

        return doResolveCall(context, prioritizedTasks, functionReference);
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> checkArgumentTypesAndFail(BasicResolutionContext context) {
        checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCall(
            @NotNull final BasicResolutionContext context,
            @NotNull final List<ResolutionTask<D>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference) {

        ResolutionDebugInfo.Data debugInfo = ResolutionDebugInfo.create();
        context.trace.record(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement(), debugInfo);
        context.trace.record(RESOLUTION_SCOPE, context.call.getCalleeExpression(), context.scope);

        debugInfo.set(ResolutionDebugInfo.TASKS, prioritizedTasks);

        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        OverloadResolutionResultsImpl<D> resultsForFirstNonemptyCandidateSet = null;
        for (ResolutionTask<D> task : prioritizedTasks) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(context.trace);
            OverloadResolutionResultsImpl<D> results = performResolutionGuardedForExtraFunctionLiteralArguments(task.withTrace(temporaryTrace));
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
            context.trace.report(UNRESOLVED_REFERENCE.on(reference));
            checkTypesWithNoCallee(context);
        }
        return resultsForFirstNonemptyCandidateSet != null ? resultsForFirstNonemptyCandidateSet : OverloadResolutionResultsImpl.<D>nameNotFound();
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> performResolutionGuardedForExtraFunctionLiteralArguments(ResolutionTask<D> task) {
        OverloadResolutionResultsImpl<D> results = performResolution(task);

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
        if (someFailed.contains(results.getResultCode()) && !task.call.getFunctionLiteralArguments().isEmpty()) {
            // We have some candidates that failed for some reason
            // And we have a suspect: the function literal argument
            // Now, we try to remove this argument and see if it helps
            Collection<ResolvedCallImpl<D>> newCandidates = Lists.newArrayList();
            for (ResolvedCallImpl<D> candidate : task.getCandidates()) {
                newCandidates.add(ResolvedCallImpl.create(candidate.getCandidateDescriptor()));
            }
            ResolutionTask<D> newContext = new ResolutionTask<D>(newCandidates, task.reference, TemporaryBindingTrace.create(task.trace), task.scope, new DelegatingCall(task.call) {
                            @NotNull
                            @Override
                            public List<JetExpression> getFunctionLiteralArguments() {
                                return Collections.emptyList();
                            }
                        }, task.expectedType, task.dataFlowInfo);
            OverloadResolutionResultsImpl<D> resultsWithFunctionLiteralsStripped = performResolution(newContext);
            if (resultsWithFunctionLiteralsStripped.isSuccess() || resultsWithFunctionLiteralsStripped.isAmbiguity()) {
                task.tracing.danglingFunctionLiteralArgumentSuspected(task.trace, task.call.getFunctionLiteralArguments());
            }
        }

        return results;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> performResolution(ResolutionTask<D> task) {
        for (ResolvedCallImpl<D> candidateCall : task.getCandidates()) {
            D candidate = candidateCall.getCandidateDescriptor();
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(task.trace);
            candidateCall.setTrace(temporaryTrace);

            CallResolutionContext<D> context = new CallResolutionContext<D>(candidateCall, task, temporaryTrace, task.tracing);

            context.tracing.bindReference(context.trace, candidateCall);
            
            if (ErrorUtils.isError(candidate)) {
                candidateCall.setStatus(SUCCESS);
                checkTypesWithNoCallee(context.toBasic());
                continue;
            }

            boolean errorInArgumentMapping = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(context.call, context.tracing, candidateCall);
            if (errorInArgumentMapping) {
                candidateCall.setStatus(OTHER_ERROR);
                checkTypesWithNoCallee(context.toBasic());
                continue;
            }

            List<JetTypeProjection> jetTypeArguments = context.call.getTypeArguments();
            if (jetTypeArguments.isEmpty()) {
                if (!candidate.getTypeParameters().isEmpty()) {
                    candidateCall.setStatus(inferTypeArguments(context));
                }
                else {
                    candidateCall.setStatus(checkAllValueArguments(context));
                }
            }
            else {
                // Explicit type arguments passed

                List<JetType> typeArguments = new ArrayList<JetType>();
                for (JetTypeProjection projection : jetTypeArguments) {
                    if (projection.getProjectionKind() != JetProjectionKind.NONE) {
                        context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                    }
                    JetTypeReference typeReference = projection.getTypeReference();
                    if (typeReference != null) {
                        typeArguments.add(typeResolver.resolveType(context.scope, typeReference, context.trace, true));
                    }
                    else {
                        typeArguments.add(ErrorUtils.createErrorType("Star projection in a call"));
                    }
                }
                int expectedTypeArgumentCount = candidate.getTypeParameters().size();
                if (expectedTypeArgumentCount == jetTypeArguments.size()) {

                    checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate, context.trace);
                    
                    Map<TypeConstructor, TypeProjection> substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
                    D substitutedDescriptor = (D) candidate.substitute(TypeSubstitutor.create(substitutionContext));

                    candidateCall.setResultingDescriptor(substitutedDescriptor);
                    replaceValueParametersWithSubstitutedOnes(candidateCall, substitutedDescriptor);

                    List<TypeParameterDescriptor> typeParameters = candidateCall.getCandidateDescriptor().getTypeParameters();
                    for (int i = 0; i < typeParameters.size(); i++) {
                        TypeParameterDescriptor typeParameterDescriptor = typeParameters.get(i);
                        candidateCall.recordTypeArgument(typeParameterDescriptor, typeArguments.get(i));
                    }
                    candidateCall.setStatus(checkAllValueArguments(context));
                }
                else {
                    candidateCall.setStatus(OTHER_ERROR);
                    context.tracing.wrongNumberOfTypeArguments(context.trace, expectedTypeArgumentCount);
                }
            }
            
            task.performAdvancedChecks(candidate, context.trace, context.tracing);

            // 'super' cannot be passed as an argument, for receiver arguments expression typer does not track this
            // See TaskPrioritizer for more
            JetSuperExpression superExpression = TaskPrioritizer.getReceiverSuper(candidateCall.getReceiverArgument());
            if (superExpression != null) {
                context.trace.report(SUPER_IS_NOT_AN_EXPRESSION.on(superExpression, superExpression.getText()));
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
        
        OverloadResolutionResultsImpl<D> results = computeResultAndReportErrors(task.trace, task.tracing, successfulCandidates, failedCandidates);
        if (!results.singleResult()) {
            checkTypesWithNoCallee(task.toBasic());
        }
        return results;
    }

    private <D extends CallableDescriptor> ResolutionStatus inferTypeArguments(CallResolutionContext<D> context) {
        ResolvedCallImpl<D> candidateCall = context.candidateCall;
        D candidate = candidateCall.getCandidateDescriptor();

        ResolutionDebugInfo.Data debugInfo = context.trace.get(ResolutionDebugInfo.RESOLUTION_DEBUG_INFO, context.call.getCallElement());

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
                TemporaryBindingTrace traceForUnknown = TemporaryBindingTrace.create(context.trace);
                JetType type = expressionTypingServices.getType(context.scope, expression, substituteDontCare.substitute(valueParameterDescriptor.getType(), Variance.INVARIANT), traceForUnknown);
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
        if (context.expectedType != NO_EXPECTED_TYPE) {
            constraintSystem.addSubtypingConstraint(EXPECTED_TYPE.assertSubtyping(candidateWithFreshVariables.getReturnType(), context.expectedType));
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
            checkValueArgumentTypes(context);

            return SUCCESS;
        }
        else {
            context.tracing.typeInferenceFailed(context.trace, solution.getStatus());
            return OTHER_ERROR.combine(checkAllValueArguments(context));
        }
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

    private void checkTypesWithNoCallee(BasicResolutionContext context) {
        for (ValueArgument valueArgument : context.call.getValueArguments()) {
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                expressionTypingServices.getType(context.scope, argumentExpression, NO_EXPECTED_TYPE, context.trace);
            }
        }

        for (JetExpression expression : context.call.getFunctionLiteralArguments()) {
            expressionTypingServices.getType(context.scope, expression, NO_EXPECTED_TYPE, context.trace);
        }

        for (JetTypeProjection typeProjection : context.call.getTypeArguments()) {
            JetTypeReference typeReference = typeProjection.getTypeReference();
            if (typeReference == null) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(typeProjection));
            }
            else {
                typeResolver.resolveType(context.scope, typeReference, context.trace, true);
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

    private <D extends CallableDescriptor> ResolutionStatus checkAllValueArguments(CallResolutionContext<D> context) {
        ResolutionStatus result = checkValueArgumentTypes(context);
        ResolvedCall candidateCall = context.candidateCall;
        result = result.combine(checkReceiver(context, candidateCall.getResultingDescriptor().getReceiverParameter(), candidateCall.getReceiverArgument()));
        result = result.combine(checkReceiver(context, candidateCall.getResultingDescriptor().getExpectedThisObject(), candidateCall.getThisObject()));
        return result;
    }

    private <D extends CallableDescriptor> ResolutionStatus checkReceiver(CallResolutionContext<D> context, ReceiverDescriptor receiverParameter, ReceiverDescriptor receiverArgument) {
        ResolutionStatus result = SUCCESS;
        if (receiverParameter.exists() && receiverArgument.exists()) {
            ASTNode callOperationNode = context.call.getCallOperationNode();
            boolean safeAccess = callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
            JetType receiverArgumentType = receiverArgument.getType();
            AutoCastServiceImpl autoCastService = new AutoCastServiceImpl(context.dataFlowInfo, context.candidateCall.getTrace().getBindingContext());
            if (!safeAccess && !receiverParameter.getType().isNullable() && !autoCastService.isNotNull(receiverArgument)) {
                context.tracing.unsafeCall(context.candidateCall.getTrace(), receiverArgumentType);
                result = UNSAFE_CALL_ERROR;
            }
            else {
                JetType effectiveReceiverArgumentType = safeAccess
                                                        ? TypeUtils.makeNotNullable(receiverArgumentType)
                                                        : receiverArgumentType;
                if (!typeChecker.isSubtypeOf(effectiveReceiverArgumentType, receiverParameter.getType())) {
                    context.tracing.wrongReceiverType(context.candidateCall.getTrace(), receiverParameter, receiverArgument);
                    result = OTHER_ERROR;
                }
            }

            if (safeAccess && (receiverParameter.getType().isNullable() || !receiverArgumentType.isNullable())) {
                context.tracing.unnecessarySafeCall(context.candidateCall.getTrace(), receiverArgumentType);
            }
        }
        return result;
    }

    private <D extends CallableDescriptor> ResolutionStatus checkValueArgumentTypes(CallResolutionContext<D> context) {
        ResolutionStatus result = SUCCESS;
        for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : context.candidateCall.getValueArguments().entrySet()) {
            ValueParameterDescriptor parameterDescriptor = entry.getKey();
            ResolvedValueArgument resolvedArgument = entry.getValue();

            JetType parameterType = getEffectiveExpectedType(parameterDescriptor);

            List<JetExpression> argumentExpressions = resolvedArgument.getArgumentExpressions();
            for (JetExpression argumentExpression : argumentExpressions) {
                if (argumentExpression == null) {
                    throw new IllegalStateException("A null instead of an expression in the arguments for " + entry.getKey());
                }
                JetType type = expressionTypingServices.getType(context.scope, argumentExpression, parameterType, context.dataFlowInfo, context.candidateCall.getTrace());
                if (type == null || ErrorUtils.isErrorType(type)) {
                    context.candidateCall.argumentHasNoType();
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

    private static final class CallResolutionContext<D extends CallableDescriptor> extends ResolutionContext {
        /*package*/ final ResolvedCallImpl<D> candidateCall;
        /*package*/ final TracingStrategy tracing;

        public CallResolutionContext(ResolvedCallImpl<D> candidateCall, ResolutionTask<D> task, BindingTrace trace, TracingStrategy tracing) {
            super(trace, task.scope, task.call, task.expectedType, task.dataFlowInfo);
            this.candidateCall = candidateCall;
            this.tracing = tracing;
        }
    }
}
