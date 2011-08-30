package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.inference.ConstraintSystem;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.types.JetTypeInferrer.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {

//    private final BindingTrace trace;
    private final TypeResolver typeResolver;
    private final JetTypeInferrer typeInferrer;
    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final OverloadingConflictResolver overloadingConflictResolver;

    public CallResolver(JetSemanticServices semanticServices, BindingTrace trace, JetTypeInferrer typeInferrer) {
//        this.trace = trace;
        this.typeInferrer = typeInferrer;
        this.typeResolver = new TypeResolver(semanticServices, trace, true);
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        this.semanticServices = semanticServices;
        this.overloadingConflictResolver = new OverloadingConflictResolver(semanticServices);
    }

    @Nullable
    public VariableDescriptor resolveSimpleProperty(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @Nullable JetType receiverType,
            @NotNull final JetSimpleNameExpression nameExpression,
            @NotNull JetType expectedType) {
        Call call = CallMaker.makePropertyCall(nameExpression);
        List<ResolutionTask<VariableDescriptor>> prioritizedTasks = PROPERTY_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiverType, call, nameExpression.getReferencedName());
        return resolveCallToDescriptor(trace, scope, call, nameExpression.getNode(), expectedType, prioritizedTasks, nameExpression);
    }


    @Nullable
    public JetType resolveCall(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @Nullable JetType receiverType,
            @NotNull JetCallElement call,
            @NotNull JetType expectedType
    ) {
        FunctionDescriptor functionDescriptor = resolveSimpleCallToFunctionDescriptor(trace, scope, receiverType, call, expectedType);
        return functionDescriptor == null ? null : functionDescriptor.getReturnType();
    }
    
    @Nullable
    public FunctionDescriptor resolveCallWithGivenName(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name,
            @Nullable JetType receiverType,
            @NotNull JetType expectedType) {
        // TODO : autocasts
        // TODO : nullability
        List<ResolutionTask<FunctionDescriptor>> tasks = FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiverType, call, name);
        return resolveCallToDescriptor(trace, scope, call, functionReference.getNode(), expectedType, tasks, functionReference);
    }

    @Nullable
    public FunctionDescriptor resolveSimpleCallToFunctionDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @Nullable JetType receiverType,
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
            if (name == null) return null;

            prioritizedTasks = FUNCTION_TASK_PRIORITIZER.computePrioritizedTasks(scope, receiverType, call, name);
        }
        else if (calleeExpression instanceof JetConstructorCalleeExpression) {
            assert receiverType == null;

            prioritizedTasks = Lists.newArrayList();

            JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
            functionReference = expression.getConstructorReferenceExpression();
            if (functionReference == null) {
                return null; // No type there
            }
            JetTypeReference typeReference = expression.getTypeReference();
            assert typeReference != null;
            JetType constructedType = typeResolver.resolveType(scope, typeReference);
            DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                Set<FunctionDescriptor> constructors = classDescriptor.getConstructors().getFunctionDescriptors();
                if (constructors.isEmpty()) {
                    trace.getErrorHandler().genericError(call.getValueArgumentList().getNode(), "This class does not have a constructor");
                    return null;
                }
                prioritizedTasks.add(new ResolutionTask<FunctionDescriptor>(constructors, null, call));
            }
            else {
                trace.getErrorHandler().genericError(calleeExpression.getNode(), "Not a class");
                return null;
            }
        } else if (calleeExpression instanceof JetThisReferenceExpression) {
            functionReference = (JetThisReferenceExpression) calleeExpression;
            DeclarationDescriptor containingDeclaration = scope.getContainingDeclaration();
            assert containingDeclaration instanceof ClassDescriptor;
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;


            Set<FunctionDescriptor> constructors = classDescriptor.getConstructors().getFunctionDescriptors();
            if (constructors.isEmpty()) {
                trace.getErrorHandler().genericError(call.getValueArgumentList().getNode(), "This class does not have a constructor");
                return null;
            }
            prioritizedTasks = Collections.singletonList(new ResolutionTask<FunctionDescriptor>(constructors, null, call));
        }
        else {
            throw new UnsupportedOperationException("Type argument inference not implemented for " + call);
        }

        return resolveCallToDescriptor(trace, scope, call, call.getNode(), expectedType, prioritizedTasks, functionReference);
    }



    private <Descriptor extends CallableDescriptor> Descriptor resolveCallToDescriptor(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final ASTNode callNode,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask<Descriptor>> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression reference) {
        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        TracingStrategy tracing = new TracingStrategy() {
            @Override
            public void bindReference(@NotNull BindingTrace trace, @NotNull CallableDescriptor descriptor) {
                trace.record(REFERENCE_TARGET, reference, descriptor);
            }

            @Override
            public void reportOverallResolutionError(@NotNull BindingTrace trace, @NotNull String message) {
                trace.getErrorHandler().genericError(callNode, message);
            }

            @Override
            public void reportWrongTypeArguments(@NotNull BindingTrace trace, @NotNull String message) {
                JetTypeArgumentList typeArgumentList = call.getTypeArgumentList();
                if (typeArgumentList != null) {
                    trace.getErrorHandler().genericError(typeArgumentList.getNode(), message);
                }
                else {
                    reportOverallResolutionError(trace, message);
                }
            }

            @Override
            public void reportWrongValueArguments(@NotNull BindingTrace trace, @NotNull String message) {
                ASTNode node;

                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                if (valueArgumentList != null) {
                    node = valueArgumentList.getNode();
                }
                else if (!call.getFunctionLiteralArguments().isEmpty()) {
                    node = call.getFunctionLiteralArguments().get(0).getNode();
                }
                else {
                    node = callNode;
                }

                trace.getErrorHandler().genericError(node, message);
            }

            @Override
            public void reportUnresolvedReference(@NotNull BindingTrace trace) {
                trace.getErrorHandler().unresolvedReference(reference);
            }

            @Override
            public void reportErrorOnReference(BindingTrace trace, String message) {
                trace.getErrorHandler().genericError(reference.getNode(), message);
            }

        };
        for (ResolutionTask<Descriptor> task : prioritizedTasks) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            Descriptor descriptor = performResolution(temporaryTrace, scope, expectedType, task, tracing);
            if (descriptor != null) {
                temporaryTrace.commit();
                return descriptor;
            }
            if (traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty()) {
                traceForFirstNonemptyCandidateSet = temporaryTrace;
            }
        }
        if (traceForFirstNonemptyCandidateSet != null) {
            traceForFirstNonemptyCandidateSet.commit();
        }
        else {
            trace.getErrorHandler().unresolvedReference(reference);
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Nullable
    private <Descriptor extends CallableDescriptor, Task extends ResolutionTask<Descriptor>> Descriptor performResolution(@NotNull BindingTrace trace, @NotNull JetScope scope, @NotNull JetType expectedType, @NotNull Task task, @NotNull TracingStrategy tracing) {
        Map<Descriptor, Descriptor> successfulCandidates = Maps.newLinkedHashMap();
        Set<Descriptor> failedCandidates = Sets.newLinkedHashSet();
        Set<Descriptor> dirtyCandidates = Sets.newLinkedHashSet();
        Map<Descriptor, ConstraintSystem.Solution> solutions = Maps.newHashMap();
        Map<Descriptor, TemporaryBindingTrace> traces = Maps.newHashMap();

        for (Descriptor candidate : task.getCandidates()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            traces.put(candidate, temporaryTrace);
            JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);

            tracing.bindReference(temporaryTrace, candidate);
            
            if (ErrorUtils.isError(candidate)) {
                successfulCandidates.put(candidate, candidate);
                continue;
            }

            Flag dirty = new Flag(false);

            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters = Maps.newHashMap();
            boolean error = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(task, tracing, candidate, temporaryTrace, argumentsToParameters);

            if (error) {
                failedCandidates.add(candidate);
                continue;
            }

            if (task.getTypeArguments().isEmpty()) {
                if (candidate.getTypeParameters().isEmpty()) {
                    if (checkValueArgumentTypes(scope, temporaryServices, argumentsToParameters, dirty, Functions.<ValueParameterDescriptor>identity())
                            && checkReceiver(task, tracing, candidate, temporaryTrace)) {
                        successfulCandidates.put(candidate, candidate);
                    }
                    else {
                        failedCandidates.add(candidate);
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
                        JetType type = temporaryServices.getType(scope, expression, NO_EXPECTED_TYPE);
                        if (type != null) {
                            constraintSystem.addSubtypingConstraint(type, valueParameterDescriptor.getOutType());
                        }
                        else {
                            dirty.setValue(true);
                        }
                    }

                    checkReceiverAbsence(task, tracing, candidate, temporaryTrace);
                    // Error is already reported if something is missing
                    JetType receiverType = task.getReceiverType();
                    JetType candidateReceiverType = candidate.getReceiverType();
                    if (receiverType != null && candidateReceiverType != null) {
                        constraintSystem.addSubtypingConstraint(receiverType, candidateReceiverType);
                    }

                    if (expectedType != NO_EXPECTED_TYPE) {
                        constraintSystem.addSubtypingConstraint(candidate.getReturnType(), expectedType);
                    }

                    ConstraintSystem.Solution solution = constraintSystem.solve();
                    solutions.put(candidate, solution);
                    if (solution.isSuccessful()) {
                        Descriptor substitute = (Descriptor) candidate.substitute(solution.getSubstitutor());
                        assert substitute != null;
                        successfulCandidates.put(candidate, substitute);
                    }
                    else {
                        tracing.reportOverallResolutionError(temporaryTrace, "Type inference failed");
                        failedCandidates.add(candidate);
                    }
                }
            }
            else {
                // Explicit type arguments passed

                final List<JetTypeProjection> jetTypeArguments = task.getTypeArguments();

                for (JetTypeProjection typeArgument : jetTypeArguments) {
                    if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
                        temporaryTrace.getErrorHandler().genericError(typeArgument.getNode(), "Projections are not allowed on type parameters for methods"); // TODO : better positioning
                    }
                }

                if (candidate.getTypeParameters().size() == jetTypeArguments.size()) {
                    List<JetType> typeArguments = new ArrayList<JetType>();
                    for (JetTypeProjection projection : jetTypeArguments) {
                        // TODO : check that there's no projection
                        JetTypeReference typeReference = projection.getTypeReference();
                        if (typeReference != null) {
                            typeArguments.add(new TypeResolver(semanticServices, temporaryTrace, true).resolveType(scope, typeReference));
                        }
                    }

                    checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate);

                    Map<TypeConstructor, TypeProjection> substitutionContext = FunctionDescriptorUtil.createSubstitutionContext((FunctionDescriptor) candidate, typeArguments);
                    Descriptor substitutedFunctionDescriptor = (Descriptor) candidate.substitute(TypeSubstitutor.create(substitutionContext));

                    Function<ValueParameterDescriptor, ValueParameterDescriptor> mapFunction = createMapFunction(substitutedFunctionDescriptor);
                    if (checkValueArgumentTypes(scope, temporaryServices, argumentsToParameters, dirty, mapFunction)
                            && checkReceiver(task, tracing, substitutedFunctionDescriptor, temporaryTrace)) {
                        successfulCandidates.put(candidate, substitutedFunctionDescriptor);
                    }
                    else {
                        failedCandidates.add(candidate);
                    }
                }
                else {
                    failedCandidates.add(candidate);
                    tracing.reportWrongTypeArguments(temporaryTrace, "Number of type arguments does not match " + DescriptorRenderer.TEXT.render(candidate));
                }
            }
            
            if (dirty.getValue()) {
                dirtyCandidates.add(candidate);
            }
        }

        Descriptor resultingDescriptor = computeResultAndReportErrors(trace, tracing, successfulCandidates, failedCandidates, dirtyCandidates, traces);
        if (resultingDescriptor == null) {
            for (ValueArgument valueArgument : task.getValueArguments()) {
                JetExpression argumentExpression = valueArgument.getArgumentExpression();
                if (argumentExpression != null) {
                    typeInferrer.getServices(trace).getType(scope, argumentExpression, NO_EXPECTED_TYPE);
                }
            }

            for (JetExpression expression : task.getFunctionLiteralArguments()) {
                typeInferrer.getServices(trace).getType(scope, expression, NO_EXPECTED_TYPE);
            }

            for (JetTypeProjection typeProjection : task.getTypeArguments()) {
                new TypeResolver(semanticServices, trace, true).resolveType(scope, typeProjection.getTypeReference());
            }
        }
        return resultingDescriptor;
    }

    private <Descriptor extends CallableDescriptor> Function<ValueParameterDescriptor, ValueParameterDescriptor> createMapFunction(Descriptor substitutedFunctionDescriptor) {
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

    @Nullable
    private FunctionDescriptor oldPerformResolution(@NotNull BindingTrace trace, @NotNull JetScope scope, @NotNull JetType expectedType, @NotNull ResolutionTask<FunctionDescriptor> task, @NotNull TracingStrategy tracing) {
        Map<FunctionDescriptor, FunctionDescriptor> successfulCandidates = Maps.newLinkedHashMap();
        Set<FunctionDescriptor> failedCandidates = Sets.newLinkedHashSet();
        Set<FunctionDescriptor> dirtyCandidates = Sets.newLinkedHashSet();
        Map<FunctionDescriptor, ConstraintSystem.Solution> solutions = Maps.newHashMap();
        Map<FunctionDescriptor, TemporaryBindingTrace> traces = Maps.newHashMap();

        for (FunctionDescriptor candidate : task.getCandidates()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            traces.put(candidate, temporaryTrace);
            JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);

            tracing.bindReference(temporaryTrace, candidate);

            if (ErrorUtils.isError(candidate)) {
                successfulCandidates.put(candidate, candidate);
                continue;
            }

            Flag dirty = new Flag(false);

            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters = Maps.newHashMap();
            boolean error = ValueArgumentsToParametersMapper.mapValueArgumentsToParameters(task, tracing, candidate, temporaryTrace, argumentsToParameters);

            if (error) {
                failedCandidates.add(candidate);
                continue;
            }

            if (task.getTypeArguments().isEmpty()) {
                if (candidate.getTypeParameters().isEmpty()) {
                    if (checkValueArgumentTypes(scope, temporaryServices, argumentsToParameters, dirty, Functions.<ValueParameterDescriptor>identity())
                            && checkReceiver(task, tracing, candidate, temporaryTrace)) {
                        successfulCandidates.put(candidate, candidate);
                    }
                    else {
                        failedCandidates.add(candidate);
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
                        JetType type = temporaryServices.getType(scope, expression, NO_EXPECTED_TYPE);
                        if (type != null) {
                            constraintSystem.addSubtypingConstraint(type, valueParameterDescriptor.getOutType());
                        }
                        else {
                            dirty.setValue(true);
                        }
                    }

                    checkReceiverAbsence(task, tracing, candidate, temporaryTrace);
                    // Error is already reported if something is missing
                    JetType receiverType = task.getReceiverType();
                    JetType candidateReceiverType = candidate.getReceiverType();
                    if (receiverType != null && candidateReceiverType != null) {
                        constraintSystem.addSubtypingConstraint(receiverType, candidateReceiverType);
                    }

                    if (expectedType != NO_EXPECTED_TYPE) {
                        constraintSystem.addSubtypingConstraint(candidate.getReturnType(), expectedType);
                    }

                    ConstraintSystem.Solution solution = constraintSystem.solve();
                    solutions.put(candidate, solution);
                    if (solution.isSuccessful()) {
                        FunctionDescriptor substitute = candidate.substitute(solution.getSubstitutor());
                        assert substitute != null;
                        successfulCandidates.put(candidate, substitute);
                    }
                    else {
                        tracing.reportOverallResolutionError(temporaryTrace, "Type inference failed");
                        failedCandidates.add(candidate);
                    }
                }
            }
            else {
                // Explicit type arguments passed

                final List<JetTypeProjection> jetTypeArguments = task.getTypeArguments();

                for (JetTypeProjection typeArgument : jetTypeArguments) {
                    if (typeArgument.getProjectionKind() != JetProjectionKind.NONE) {
                        temporaryTrace.getErrorHandler().genericError(typeArgument.getNode(), "Projections are not allowed on type parameters for methods"); // TODO : better positioning
                    }
                }

                if (candidate.getTypeParameters().size() == jetTypeArguments.size()) {
                    List<JetType> typeArguments = new ArrayList<JetType>();
                    for (JetTypeProjection projection : jetTypeArguments) {
                        // TODO : check that there's no projection
                        JetTypeReference typeReference = projection.getTypeReference();
                        if (typeReference != null) {
                            typeArguments.add(new TypeResolver(semanticServices, temporaryTrace, true).resolveType(scope, typeReference));
                        }
                    }

                    checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate);

                    FunctionDescriptor substitutedFunctionDescriptor = FunctionDescriptorUtil.substituteFunctionDescriptor(typeArguments, candidate);

                    assert substitutedFunctionDescriptor != null;
                    final Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
                    for (ValueParameterDescriptor valueParameterDescriptor : substitutedFunctionDescriptor.getValueParameters()) {
                        parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
                    }

                    Function<ValueParameterDescriptor, ValueParameterDescriptor> mapFunction = new Function<ValueParameterDescriptor, ValueParameterDescriptor>() {
                        @Override
                        public ValueParameterDescriptor apply(ValueParameterDescriptor input) {
                            return parameterMap.get(input.getOriginal());
                        }
                    };
                    if (checkValueArgumentTypes(scope, temporaryServices, argumentsToParameters, dirty, mapFunction)
                            && checkReceiver(task, tracing, substitutedFunctionDescriptor, temporaryTrace)) {
                        successfulCandidates.put(candidate, substitutedFunctionDescriptor);
                    }
                    else {
                        failedCandidates.add(candidate);
                    }
                }
                else {
                    failedCandidates.add(candidate);
                    tracing.reportWrongTypeArguments(temporaryTrace, "Number of type arguments does not match " + DescriptorRenderer.TEXT.render(candidate));
                }
            }

            if (dirty.getValue()) {
                dirtyCandidates.add(candidate);
            }
        }

        FunctionDescriptor functionDescriptor = computeResultAndReportErrors(trace, tracing, successfulCandidates, failedCandidates, dirtyCandidates, traces);
        if (functionDescriptor == null) {
            for (ValueArgument valueArgument : task.getValueArguments()) {
                JetExpression argumentExpression = valueArgument.getArgumentExpression();
                if (argumentExpression != null) {
                    typeInferrer.getServices(trace).getType(scope, argumentExpression, NO_EXPECTED_TYPE);
                }
            }

            for (JetExpression expression : task.getFunctionLiteralArguments()) {
                typeInferrer.getServices(trace).getType(scope, expression, NO_EXPECTED_TYPE);
            }

            for (JetTypeProjection typeProjection : task.getTypeArguments()) {
                new TypeResolver(semanticServices, trace, true).resolveType(scope, typeProjection.getTypeReference());
            }
        }
        return functionDescriptor;
    }

    private <Descriptor extends CallableDescriptor> boolean checkReceiver(ResolutionTask<Descriptor> task, TracingStrategy tracing, Descriptor candidate, TemporaryBindingTrace temporaryTrace) {
        if (!checkReceiverAbsence(task, tracing, candidate, temporaryTrace)) return false;
        JetType receiverType = task.getReceiverType();
        JetType candidateReceiverType = candidate.getReceiverType();
        if (receiverType != null
                && candidateReceiverType != null
                && !semanticServices.getTypeChecker().isSubtypeOf(receiverType, candidateReceiverType)) {
            tracing.reportErrorOnReference(temporaryTrace, "This function requires a receiver of type " + candidateReceiverType);
            return false;
        }
        return true;
    }

    private <Descriptor extends CallableDescriptor> boolean checkReceiverAbsence(ResolutionTask<Descriptor> task, TracingStrategy tracing, Descriptor candidate, TemporaryBindingTrace temporaryTrace) {
        JetType receiverType = task.getReceiverType();
        JetType candidateReceiverType = candidate.getReceiverType();
        if (receiverType != null) {
            if (candidateReceiverType == null) {
                tracing.reportErrorOnReference(temporaryTrace, "This function does not admit a receiver");
                return false;
            }
        }
        else if (candidateReceiverType != null) {
            tracing.reportErrorOnReference(temporaryTrace, "Receiver is missing" + candidateReceiverType);
            return false;
        }
        return true;
    }

    @Nullable
    private <Descriptor extends CallableDescriptor> Descriptor computeResultAndReportErrors(BindingTrace trace, TracingStrategy tracing, Map<Descriptor, Descriptor> successfulCandidates, Set<Descriptor> failedCandidates, Set<Descriptor> dirtyCandidates, Map<Descriptor, TemporaryBindingTrace> traces) {
        if (successfulCandidates.size() > 0) {
            if (successfulCandidates.size() == 1) {
                Map.Entry<Descriptor, Descriptor> entry = successfulCandidates.entrySet().iterator().next();
                Descriptor functionDescriptor = entry.getKey();
                Descriptor result = entry.getValue();

                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.commit();
                return result;
            }
            else {
                Map<Descriptor, Descriptor> cleanCandidates = Maps.newLinkedHashMap(successfulCandidates);
                cleanCandidates.keySet().removeAll(dirtyCandidates);
                if (cleanCandidates.isEmpty()) {
                    cleanCandidates = successfulCandidates;
                }
                Descriptor maximallySpecific = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, traces, false);
                if (maximallySpecific != null) {
                    return maximallySpecific;
                }

                Descriptor maximallySpecificGenericsDiscriminated = overloadingConflictResolver.findMaximallySpecific(cleanCandidates, traces, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return maximallySpecificGenericsDiscriminated;
                }

                if (dirtyCandidates.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (Descriptor functionDescriptor : successfulCandidates.keySet()) {
                        stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                    }

                    tracing.reportOverallResolutionError(trace, "Overload resolution ambiguity: " + stringBuilder);
                }
            }
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() == 1) {
                Descriptor functionDescriptor = failedCandidates.iterator().next();
                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.commit();
                return failedCandidates.iterator().next();
            }
            else {
                StringBuilder stringBuilder = new StringBuilder();
                for (Descriptor functionDescriptor : failedCandidates) {
                    stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                }

                tracing.reportOverallResolutionError(trace, "None of the following functions can be called with the arguments supplied: " + stringBuilder);
            }
        }
        else {
            tracing.reportUnresolvedReference(trace);
        }



        return null;
    }

    private boolean checkValueArgumentTypes(JetScope scope, JetTypeInferrer.Services temporaryServices, Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters, Flag dirty, Function<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap) {
        for (Map.Entry<ValueArgument, ValueParameterDescriptor> entry : argumentsToParameters.entrySet()) {
            ValueArgument valueArgument = entry.getKey();
            ValueParameterDescriptor valueParameterDescriptor = entry.getValue();

            ValueParameterDescriptor substitutedParameter = parameterMap.apply(valueParameterDescriptor);

            assert substitutedParameter != null;

            JetType parameterType = substitutedParameter.getOutType();
            JetExpression argumentExpression = valueArgument.getArgumentExpression();
            if (argumentExpression != null) {
                JetType type = temporaryServices.getType(scope, argumentExpression, parameterType);
                if (type == null) {
                    dirty.setValue(true);
                }
                else if (!semanticServices.getTypeChecker().isSubtypeOf(type, parameterType)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void checkGenericBoundsInAFunctionCall(List<JetTypeProjection> jetTypeArguments, List<JetType> typeArguments, CallableDescriptor functionDescriptor) {
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
            classDescriptorResolver.checkBounds(typeReference, typeArgument, typeParameterDescriptor, substitutor);
        }
    }

    @NotNull
    public OverloadResolutionResult resolveExactSignature(@NotNull JetScope scope, @Nullable JetType receiverType, @NotNull String name, @NotNull List<JetType> parameterTypes) {
        List<FunctionDescriptor> result = findCandidatesByExactSignature(scope, receiverType, name, parameterTypes);
        return listToOverloadResolutionResult(result);
    }

    private List<FunctionDescriptor> findCandidatesByExactSignature(JetScope scope, JetType receiverType, String name, List<JetType> parameterTypes) {
        List<FunctionDescriptor> result = Lists.newArrayList();
        if (receiverType != null) {
            Set<FunctionDescriptor> extensionFunctionDescriptors = scope.getFunctionGroup(name).getFunctionDescriptors();
            List<FunctionDescriptor> nonlocal = Lists.newArrayList();
            List<FunctionDescriptor> local = Lists.newArrayList();
            TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


            if (findExtensionFunctions(local, receiverType, parameterTypes, result)) {
                return result;
            }

            Set<FunctionDescriptor> functionDescriptors = receiverType.getMemberScope().getFunctionGroup(name).getFunctionDescriptors();
            if (lookupExactSignature(functionDescriptors, parameterTypes, result)) {
                return result;

            }
            findExtensionFunctions(nonlocal, receiverType, parameterTypes, result);
            return result;
        }
        else {
            lookupExactSignature(scope.getFunctionGroup(name).getFunctionDescriptors(), parameterTypes, result);
            return result;
        }
    }

    private boolean lookupExactSignature(Set<FunctionDescriptor> candidates, List<JetType> parameterTypes, List<FunctionDescriptor> result) {
        boolean found = false;
        for (FunctionDescriptor functionDescriptor : candidates) {
            if (functionDescriptor.getReceiverType() != null) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!checkValueParameters(functionDescriptor, parameterTypes)) continue;
            result.add(functionDescriptor);
            found = true;
        }
        return found;
    }

    private OverloadResolutionResult listToOverloadResolutionResult(List<FunctionDescriptor> result) {
        if (result.isEmpty()) {
            return OverloadResolutionResult.nameNotFound();
        }
        else if (result.size() == 1) {
            return OverloadResolutionResult.success(result.get(0));
        }
        else {
            return OverloadResolutionResult.ambiguity(result);
        }
    }

    private boolean findExtensionFunctions(Collection<FunctionDescriptor> candidates, JetType receiverType, List<JetType> parameterTypes, List<FunctionDescriptor> result) {
        boolean found = false;
        for (FunctionDescriptor functionDescriptor : candidates) {
            JetType functionReceiverType = functionDescriptor.getReceiverType();
            if (functionReceiverType == null) continue;
            if (!functionDescriptor.getTypeParameters().isEmpty()) continue;
            if (!semanticServices.getTypeChecker().isSubtypeOf(receiverType, functionReceiverType)) continue;
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


    private static TaskPrioritizer<FunctionDescriptor> FUNCTION_TASK_PRIORITIZER = new TaskPrioritizer<FunctionDescriptor>() {

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            Set<FunctionDescriptor> functions = Sets.newLinkedHashSet(scope.getFunctionGroup(name).getFunctionDescriptors());
            for (Iterator<FunctionDescriptor> iterator = functions.iterator(); iterator.hasNext(); ) {
                FunctionDescriptor functionDescriptor = iterator.next();
                if (functionDescriptor.getReceiverType() != null) {
                    iterator.remove();
                }
            }
            addConstrtuctors(scope, name, functions);
            return functions;
        }

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getMembersByName(@NotNull JetType receiverType, String name) {
            Set<FunctionDescriptor> members = Sets.newHashSet(receiverType.getMemberScope().getFunctionGroup(name).getFunctionDescriptors());
            addConstrtuctors(receiverType.getMemberScope(), name, members);
            return members;
        }

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getExtensionsByName(JetScope scope, String name) {
            Set<FunctionDescriptor> extensionFunctions = Sets.newHashSet(scope.getFunctionGroup(name).getFunctionDescriptors());
            for (Iterator<FunctionDescriptor> iterator = extensionFunctions.iterator(); iterator.hasNext(); ) {
                FunctionDescriptor descriptor = iterator.next();
                if (descriptor.getReceiverType() == null) {
                    iterator.remove();
                }
            }
            return extensionFunctions;
        }

        @NotNull
        @Override
        protected ResolutionTask<FunctionDescriptor> createTask(JetType receiverType, Call call, Collection<FunctionDescriptor> candidates) {
            return new ResolutionTask<FunctionDescriptor>(candidates, receiverType, call);
        }

        private void addConstrtuctors(JetScope scope, String name, Collection<FunctionDescriptor> functions) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor())) {
                ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
                functions.addAll(classDescriptor.getConstructors().getFunctionDescriptors());
            }
        }

    };

    private static TaskPrioritizer<VariableDescriptor> PROPERTY_TASK_PRIORITIZER = new TaskPrioritizer<VariableDescriptor>() {

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            VariableDescriptor variable = scope.getVariable(name);
            if (variable != null && variable.getReceiverType() == null) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, String name) {
            VariableDescriptor variable = receiverType.getMemberScope().getVariable(name);
            if (variable != null) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getExtensionsByName(JetScope scope, String name) {
            VariableDescriptor variable = scope.getVariable(name);
            if (variable != null && variable.getReceiverType() != null) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        protected ResolutionTask<VariableDescriptor> createTask(JetType receiverType, Call call, Collection<VariableDescriptor> candidates) {
            return new ResolutionTask<VariableDescriptor>(candidates, receiverType, call);
        }
    };


    private static class FunctionTaskPrioritizer1 {

        private List<ResolutionTask<FunctionDescriptor>> computePrioritizedTasks(JetScope scope, JetType receiverType, Call call, String name) {
            List<ResolutionTask<FunctionDescriptor>> result = Lists.newArrayList();
            if (receiverType != null) {
                Collection<FunctionDescriptor> extensionFunctions = Sets.newLinkedHashSet(scope.getFunctionGroup(name).getFunctionDescriptors());
                for (Iterator<FunctionDescriptor> iterator = extensionFunctions.iterator(); iterator.hasNext(); ) {
                    FunctionDescriptor functionDescriptor = iterator.next();
                    if (functionDescriptor.getReceiverType() == null) {
                        iterator.remove();
                    }
                }
                List<FunctionDescriptor> nonlocals = Lists.newArrayList();
                List<FunctionDescriptor> locals = Lists.newArrayList();
                TaskPrioritizer.splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

                Set<FunctionDescriptor> members = Sets.newHashSet(receiverType.getMemberScope().getFunctionGroup(name).getFunctionDescriptors());
                addConstrtuctors(receiverType.getMemberScope(), name, members);

                addTask(result, receiverType, call, locals);
                addTask(result, null, call, members);
                addTask(result, receiverType, call, nonlocals);
            }
            else {
                Collection<FunctionDescriptor> functions = Sets.newLinkedHashSet(scope.getFunctionGroup(name).getFunctionDescriptors());
                for (Iterator<FunctionDescriptor> iterator = functions.iterator(); iterator.hasNext(); ) {
                    FunctionDescriptor functionDescriptor = iterator.next();
                    if (functionDescriptor.getReceiverType() != null) {
                        iterator.remove();
                    }
                }
                addConstrtuctors(scope, name, functions);

                List<FunctionDescriptor> nonlocals = Lists.newArrayList();
                List<FunctionDescriptor> locals = Lists.newArrayList();
                TaskPrioritizer.splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

                addTask(result, receiverType, call, locals);

                addTask(result, receiverType, call, nonlocals);
            }
            return result;
        }

        private void addTask(List<ResolutionTask<FunctionDescriptor>> result, JetType receiverType, Call call, Collection<FunctionDescriptor> candidates) {
            if (candidates.isEmpty()) return;
            result.add(new ResolutionTask<FunctionDescriptor>(candidates, receiverType, call));
        }

        private void addConstrtuctors(JetScope scope, String name, Collection<FunctionDescriptor> functions) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor())) {
                ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
                functions.addAll(classDescriptor.getConstructors().getFunctionDescriptors());
            }
        }

    }

    private static class Flag {
        private boolean flag;

        public Flag(boolean  flag) {
            this.flag = flag;
        }

        public boolean getValue() {
            return flag;
        }

        public void setValue(boolean flag) {
            this.flag = flag;
        }
    }
}
