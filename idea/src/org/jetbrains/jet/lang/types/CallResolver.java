package org.jetbrains.jet.lang.types;

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
import org.jetbrains.jet.lang.types.inference.ConstraintSystem;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.types.JetTypeInferrer.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class CallResolver {

    private static class ResolutionTask {
        private final Collection<FunctionDescriptor> candidates;
        private final JetType receiverType;
        private final List<JetTypeProjection> typeArguments;
        private final List<? extends ValueArgument> valueArguments;
        private final List<JetExpression> functionLiteralArguments;

        protected ResolutionTask(
                @NotNull Collection<FunctionDescriptor> candidates,
                @Nullable JetType receiverType,
                @NotNull List<JetTypeProjection> typeArguments,
                @NotNull List<? extends ValueArgument> valueArguments,
                @NotNull List<JetExpression> functionLiteralArguments) {
            this.candidates = candidates;
            this.receiverType = receiverType;
            this.typeArguments = typeArguments;
            this.valueArguments = valueArguments;
            this.functionLiteralArguments = functionLiteralArguments;
        }

        public ResolutionTask(
                @NotNull Collection<FunctionDescriptor> candidates,
                @Nullable JetType receiverType,
                @NotNull Call call) {
            this(candidates, receiverType, call.getTypeArguments(), call.getValueArguments(), call.getFunctionLiteralArguments());
        }

        @NotNull
        public Collection<FunctionDescriptor> getCandidates() {
            return candidates;
        }

        @Nullable
        public JetType getReceiverType() {
            return receiverType;
        }

        @NotNull
        public List<JetTypeProjection> getTypeArguments() {
            return typeArguments;
        }
        
        @NotNull
        public List<? extends ValueArgument> getValueArguments() {
            return valueArguments;
        }

        @NotNull
        public List<JetExpression> getFunctionLiteralArguments() {
            return functionLiteralArguments;
        }
    }

    private interface TracingStrategy {
        void bindFunctionReference(@NotNull BindingTrace trace, @NotNull FunctionDescriptor functionDescriptor);

        void reportOverallResolutionError(@NotNull BindingTrace trace, @NotNull String message);

        void reportWrongTypeArguments(@NotNull BindingTrace trace, @NotNull String message);

        void reportWrongValueArguments(@NotNull BindingTrace trace, @NotNull String message);

        void reportUnresolvedFunctionReference(@NotNull BindingTrace trace);

        void reportErrorOnFunctionReference(BindingTrace trace, String message);
    }

    private final BindingTrace trace;
    private final TypeResolver typeResolver;
    private final JetTypeInferrer typeInferrer;
    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;

    public CallResolver(JetSemanticServices semanticServices, BindingTrace trace, JetTypeInferrer typeInferrer) {
        this.trace = trace;
        this.typeInferrer = typeInferrer;
        this.typeResolver = new TypeResolver(semanticServices, trace, true);
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(trace);
        this.semanticServices = semanticServices;
    }

    @Nullable
    public JetType resolveCall(
            @NotNull JetScope scope,
            @Nullable JetType receiverType,
            @NotNull JetCallElement call,
            @NotNull JetType expectedType
    ) {
        FunctionDescriptor functionDescriptor = resolveSimpleCallToFunctionDescriptor(scope, receiverType, call, expectedType);
        return functionDescriptor == null ? null : functionDescriptor.getReturnType();
    }
    
    @Nullable
    public FunctionDescriptor resolveCallWithGivenName(
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name,
            @Nullable JetType receiverType,
            @NotNull JetType expectedType) {
        // TODO : autocasts
        // TODO : nullability
        List<ResolutionTask> tasks = computePrioritizedTasks(scope, receiverType, call, name);
        return resolveCallToFunctionDescriptor(scope, call, functionReference.getNode(), expectedType, tasks, functionReference);
    }

    @Nullable
    public FunctionDescriptor resolveSimpleCallToFunctionDescriptor(
            @NotNull JetScope scope,
            @Nullable JetType receiverType,
            @NotNull final JetCallElement call,
            @NotNull JetType expectedType
    ) {
        List<ResolutionTask> prioritizedTasks;
        
        JetExpression calleeExpression = call.getCalleeExpression();
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;

            String name = expression.getReferencedName();
            if (name == null) return null;

            prioritizedTasks = computePrioritizedTasks(scope, receiverType, call, name);
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
            JetType constructedType = typeResolver.resolveType(scope, typeReference);
            DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                Set<FunctionDescriptor> members = classDescriptor.getConstructors().getFunctionDescriptors();
                if (members.isEmpty()) {
                    trace.getErrorHandler().genericError(call.getValueArgumentList().getNode(), "This class does not have a constructor");
                    return null;
                }
                addTask(prioritizedTasks, null, call, members);
            }
            else {
                trace.getErrorHandler().genericError(calleeExpression.getNode(), "Not a class");
                return null;
            }
        }
        else {
            throw new UnsupportedOperationException("Type argument inference not implemented for " + call);
        }

        return resolveCallToFunctionDescriptor(scope, call, call.getNode(), expectedType, prioritizedTasks, functionReference);
    }

    private List<ResolutionTask> computePrioritizedTasks(JetScope scope, JetType receiverType, Call call, String name) {
        List<ResolutionTask> result = Lists.newArrayList();
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
            splitLexicallyLocalDescriptors(extensionFunctions, scope.getContainingDeclaration(), locals, nonlocals);

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
            splitLexicallyLocalDescriptors(functions, scope.getContainingDeclaration(), locals, nonlocals);

            addTask(result, receiverType, call, locals);

            addTask(result, receiverType, call, nonlocals);
        }
        return result;
    }

    private void addTask(List<ResolutionTask> result, JetType receiverType, Call call, Collection<FunctionDescriptor> candidates) {
        if (candidates.isEmpty()) return;
        result.add(new ResolutionTask(candidates, receiverType, call));
    }

    private void addConstrtuctors(JetScope scope, String name, Collection<FunctionDescriptor> functions) {
        ClassifierDescriptor classifier = scope.getClassifier(name);
        if (classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor())) {
            ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
            functions.addAll(classDescriptor.getConstructors().getFunctionDescriptors());
        }
    }

    private FunctionDescriptor resolveCallToFunctionDescriptor(
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final ASTNode callNode,
            @NotNull JetType expectedType,
            @NotNull final List<ResolutionTask> prioritizedTasks, // high to low priority
            @NotNull final JetReferenceExpression functionReference) {
        TemporaryBindingTrace traceForFirstNonemptyCandidateSet = null;
        TracingStrategy tracing = new TracingStrategy() {
            @Override
            public void bindFunctionReference(@NotNull BindingTrace trace, @NotNull FunctionDescriptor functionDescriptor) {
                trace.record(BindingContext.REFERENCE_TARGET, functionReference, functionDescriptor);
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
            public void reportUnresolvedFunctionReference(@NotNull BindingTrace trace) {
                trace.getErrorHandler().unresolvedReference(functionReference);
            }

            @Override
            public void reportErrorOnFunctionReference(BindingTrace trace, String message) {
                trace.getErrorHandler().genericError(functionReference.getNode(), message);
            }

        };
        for (ResolutionTask task : prioritizedTasks) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            FunctionDescriptor functionDescriptor = performResolution(temporaryTrace, scope, expectedType, task, tracing);
            if (functionDescriptor != null) {
                temporaryTrace.commit();
                return functionDescriptor;
            }
            if (traceForFirstNonemptyCandidateSet == null && !task.getCandidates().isEmpty()) {
                traceForFirstNonemptyCandidateSet = temporaryTrace;
            }
        }
        if (traceForFirstNonemptyCandidateSet != null) {
            traceForFirstNonemptyCandidateSet.commit();
        }
        else {
            trace.getErrorHandler().unresolvedReference(functionReference);
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean mapValueArgumentsToParameters(
            @NotNull ResolutionTask task,
            @NotNull TracingStrategy tracing,
            @NotNull FunctionDescriptor candidate,
            @NotNull BindingTrace temporaryTrace,
            @NotNull Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters
    ) {
        Set<ValueParameterDescriptor> usedParameters = Sets.newHashSet();

        List<ValueParameterDescriptor> valueParameters = candidate.getValueParameters();

        Map<String, ValueParameterDescriptor> parameterByName = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            parameterByName.put(valueParameter.getName(), valueParameter);
        }

        List<? extends ValueArgument> valueArguments = task.getValueArguments();

        boolean error = false;
        boolean someNamed = false;
        boolean somePositioned = false;
        for (int i = 0; i < valueArguments.size(); i++) {
            ValueArgument valueArgument = valueArguments.get(i);
            if (valueArgument.isNamed()) {
                someNamed = true;
                ASTNode nameNode = valueArgument.getArgumentName().getNode();
                if (somePositioned) {
                    temporaryTrace.getErrorHandler().genericError(nameNode, "Mixing named and positioned arguments in not allowed");
                    error = true;
                }
                else {
                    ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(valueArgument.getArgumentName().getReferenceExpression().getReferencedName());
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        temporaryTrace.getErrorHandler().genericError(nameNode, "An argument is already passed for this parameter");
                    }
                    if (valueParameterDescriptor == null) {
                        temporaryTrace.getErrorHandler().genericError(nameNode, "Cannot find a parameter with this name");
                        error = true;
                    }
                    else {
                        temporaryTrace.record(REFERENCE_TARGET, valueArgument.getArgumentName().getReferenceExpression(), valueParameterDescriptor);
                        argumentsToParameters.put(valueArgument, valueParameterDescriptor);
                    }
                }
            }
            else {
                somePositioned = true;
                if (someNamed) {
                    temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), "Mixing named and positioned arguments in not allowed");
                    error = true;
                }
                else {
                    int parameterCount = valueParameters.size();
                    if (i < parameterCount) {
                        ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(i);
                        usedParameters.add(valueParameterDescriptor);
                        argumentsToParameters.put(valueArgument, valueParameterDescriptor);
                    }
                    else if (!valueParameters.isEmpty()) {
                        ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                        if (valueParameterDescriptor.isVararg()) {
                            argumentsToParameters.put(valueArgument, valueParameterDescriptor);
                            usedParameters.add(valueParameterDescriptor);
                        }
                        else {
                            temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), "Too many arguments");
                            error = true;
                        }
                    }
                    else {
                        temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), "Too many arguments");
                        error = true;
                    }
                }
            }
        }

        List<JetExpression> functionLiteralArguments = task.getFunctionLiteralArguments();
        if (!functionLiteralArguments.isEmpty()) {
            JetExpression possiblyLabeledFunctionLiteral = functionLiteralArguments.get(0);

            if (valueParameters.isEmpty()) {
                temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), "Too many arguments");
                error = true;
            } else {
                JetFunctionLiteralExpression functionLiteral;
                if (possiblyLabeledFunctionLiteral instanceof JetLabelQualifiedExpression) {
                    JetLabelQualifiedExpression labeledFunctionLiteral = (JetLabelQualifiedExpression) possiblyLabeledFunctionLiteral;
                    functionLiteral = (JetFunctionLiteralExpression) labeledFunctionLiteral.getLabeledExpression();
                }
                else {
                    functionLiteral = (JetFunctionLiteralExpression) possiblyLabeledFunctionLiteral;
                }

                ValueParameterDescriptor parameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                if (parameterDescriptor.isVararg()) {
                    temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), "Passing value as a vararg is only allowed inside a parenthesized argument list");
                    error = true;
                }
                else {
                    if (!usedParameters.add(parameterDescriptor)) {
                        temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), "Too many arguments");
                        error = true;
                    }
                    else {
                        argumentsToParameters.put(CallMaker.makeValueArgument(functionLiteral), parameterDescriptor);
                    }
                }
            }

            for (int i = 1; i < functionLiteralArguments.size(); i++) {
                JetExpression argument = functionLiteralArguments.get(i);
                temporaryTrace.getErrorHandler().genericError(argument.getNode(), "Only one function literal is allowed outside a parenthesized argument list");
                error = true;
            }
        }


        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!usedParameters.contains(valueParameter)) {
                if (!valueParameter.hasDefaultValue()) {
                    tracing.reportWrongValueArguments(temporaryTrace, "No value passed for parameter " + valueParameter.getName());
                    error = true;
                }
            }
        }
        return error;
    }

    @Nullable
    private FunctionDescriptor performResolution(@NotNull BindingTrace trace, @NotNull JetScope scope, @NotNull JetType expectedType, @NotNull ResolutionTask task, @NotNull TracingStrategy tracing) {
        Map<FunctionDescriptor, FunctionDescriptor> successfulCandidates = Maps.newLinkedHashMap();
        Set<FunctionDescriptor> failedCandidates = Sets.newLinkedHashSet();
        Set<FunctionDescriptor> dirtyCandidates = Sets.newLinkedHashSet();
        Map<FunctionDescriptor, ConstraintSystem.Solution> solutions = Maps.newHashMap();
        Map<FunctionDescriptor, TemporaryBindingTrace> traces = Maps.newHashMap();

        for (FunctionDescriptor candidate : task.getCandidates()) {
            TemporaryBindingTrace temporaryTrace = TemporaryBindingTrace.create(trace);
            traces.put(candidate, temporaryTrace);
            JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);

            tracing.bindFunctionReference(temporaryTrace, candidate);
            
            if (ErrorUtils.isError(candidate)) {
                successfulCandidates.put(candidate, candidate);
                continue;
            }

            Flag dirty = new Flag(false);

            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters = Maps.newHashMap();
            boolean error = mapValueArgumentsToParameters(task, tracing, candidate, temporaryTrace, argumentsToParameters);

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
                        JetType type = temporaryServices.getType(scope, expression, false, NO_EXPECTED_TYPE);
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
                    typeInferrer.getServices(trace).getType(scope, argumentExpression, false, NO_EXPECTED_TYPE);
                }
            }

            for (JetExpression expression : task.getFunctionLiteralArguments()) {
                typeInferrer.getServices(trace).getType(scope, expression, false, NO_EXPECTED_TYPE);
            }

            for (JetTypeProjection typeProjection : task.getTypeArguments()) {
                new TypeResolver(semanticServices, trace, true).resolveType(scope, typeProjection.getTypeReference());
            }
        }
        return functionDescriptor;
    }

    private boolean checkReceiver(ResolutionTask task, TracingStrategy tracing, FunctionDescriptor candidate, TemporaryBindingTrace temporaryTrace) {
        if (!checkReceiverAbsence(task, tracing, candidate, temporaryTrace)) return false;
        JetType receiverType = task.getReceiverType();
        JetType candidateReceiverType = candidate.getReceiverType();
        if (receiverType != null
                && candidateReceiverType != null
                && !semanticServices.getTypeChecker().isSubtypeOf(receiverType, candidateReceiverType)) {
            tracing.reportErrorOnFunctionReference(temporaryTrace, "This function requires a receiver of type " + candidateReceiverType);
            return false;
        }
        return true;
    }

    private boolean checkReceiverAbsence(ResolutionTask task, TracingStrategy tracing, FunctionDescriptor candidate, TemporaryBindingTrace temporaryTrace) {
        JetType receiverType = task.getReceiverType();
        JetType candidateReceiverType = candidate.getReceiverType();
        if (receiverType != null) {
            if (candidateReceiverType == null) {
                tracing.reportErrorOnFunctionReference(temporaryTrace, "This function does not admit a receiver");
                return false;
            }
        }
        else if (candidateReceiverType != null) {
            tracing.reportErrorOnFunctionReference(temporaryTrace, "Receiver is missing" + candidateReceiverType);
            return false;
        }
        return true;
    }

    @Nullable
    private FunctionDescriptor computeResultAndReportErrors(BindingTrace trace, TracingStrategy tracing, Map<FunctionDescriptor, FunctionDescriptor> successfulCandidates, Set<FunctionDescriptor> failedCandidates, Set<FunctionDescriptor> dirtyCandidates, Map<FunctionDescriptor, TemporaryBindingTrace> traces) {
        if (successfulCandidates.size() > 0) {
            if (successfulCandidates.size() == 1) {
                Map.Entry<FunctionDescriptor, FunctionDescriptor> entry = successfulCandidates.entrySet().iterator().next();
                FunctionDescriptor functionDescriptor = entry.getKey();
                FunctionDescriptor result = entry.getValue();

                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.commit();
                return result;
            }
            else {
                Map<FunctionDescriptor, FunctionDescriptor> cleanCandidates = Maps.newLinkedHashMap(successfulCandidates);
                cleanCandidates.keySet().removeAll(dirtyCandidates);
                if (cleanCandidates.isEmpty()) {
                    cleanCandidates = successfulCandidates;
                }
                FunctionDescriptor maximallySpecific = findMaximallySpecific(cleanCandidates, traces, false);
                if (maximallySpecific != null) {
                    return maximallySpecific;
                }

                FunctionDescriptor maximallySpecificGenericsDiscriminated = findMaximallySpecific(cleanCandidates, traces, true);
                if (maximallySpecificGenericsDiscriminated != null) {
                    return maximallySpecificGenericsDiscriminated;
                }

                if (dirtyCandidates.isEmpty()) {
                    StringBuilder stringBuilder = new StringBuilder();
                    for (FunctionDescriptor functionDescriptor : successfulCandidates.keySet()) {
                        stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                    }

                    tracing.reportOverallResolutionError(trace, "Overload resolution ambiguity: " + stringBuilder);
                }
            }
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() == 1) {
                FunctionDescriptor functionDescriptor = failedCandidates.iterator().next();
                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.commit();
                return failedCandidates.iterator().next();
            }
            else {
                StringBuilder stringBuilder = new StringBuilder();
                for (FunctionDescriptor functionDescriptor : failedCandidates) {
                    stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                }

                tracing.reportOverallResolutionError(trace, "None of the following functions can be called with the arguments supplied: " + stringBuilder);
            }
        }
        else {
            tracing.reportUnresolvedFunctionReference(trace);
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
                JetType type = temporaryServices.getType(scope, argumentExpression, false, parameterType);
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

    @Nullable
    private FunctionDescriptor findMaximallySpecific(Map<FunctionDescriptor, FunctionDescriptor> candidates, Map<FunctionDescriptor, TemporaryBindingTrace> traces, boolean discriminateGenericFunctions) {
        Map<FunctionDescriptor, TemporaryBindingTrace> maximallySpecific = Maps.newHashMap();
        meLoop:
        for (Map.Entry<FunctionDescriptor, FunctionDescriptor> myEntry : candidates.entrySet()) {
            FunctionDescriptor me = myEntry.getValue();
            TemporaryBindingTrace myTrace = traces.get(myEntry.getKey());
            for (FunctionDescriptor other : candidates.values()) {
                if (other == me) continue;
                if (!moreSpecific(me, other, discriminateGenericFunctions) || moreSpecific(other, me, discriminateGenericFunctions)) continue meLoop;
            }
            maximallySpecific.put(me, myTrace);
        }
        if (maximallySpecific.size() == 1) {
            Map.Entry<FunctionDescriptor, TemporaryBindingTrace> result = maximallySpecific.entrySet().iterator().next();
            result.getValue().commit();
            return result.getKey();
        }
        return null;
    }

    /**
     * Let < mean "more specific"
     * Subtype < supertype
     * Double < Float
     * Int < Long
     * Int < Short < Byte
     */
    private boolean moreSpecific(FunctionDescriptor f, FunctionDescriptor g, boolean discriminateGenericFunctions) {
        if (overrides(f, g)) return true;
        if (overrides(g, f)) return false;

        List<ValueParameterDescriptor> fParams = f.getValueParameters();
        List<ValueParameterDescriptor> gParams = g.getValueParameters();

        int fSize = fParams.size();
        if (fSize != gParams.size()) return false;
        for (int i = 0; i < fSize; i++) {
            JetType fParamType = fParams.get(i).getOutType();
            JetType gParamType = gParams.get(i).getOutType();

            if (!semanticServices.getTypeChecker().isSubtypeOf(fParamType, gParamType)
                    && !numericTypeMoreSpecific(fParamType, gParamType)
                    ) {
                return false;
            }
        }
        
        if (discriminateGenericFunctions && isGenericFunction(f)) {
            if (!isGenericFunction(g)) {
                return false;
            }

            // g is generic, too

            return moreSpecific(FunctionDescriptorUtil.substituteBounds(f), FunctionDescriptorUtil.substituteBounds(g), false);
        }
        
        return true;
    }

    private boolean isGenericFunction(FunctionDescriptor f) {
        return !f.getOriginal().getTypeParameters().isEmpty();
    }

    private boolean numericTypeMoreSpecific(@NotNull JetType specific, @NotNull JetType general) {
        JetStandardLibrary standardLibrary = semanticServices.getStandardLibrary();
        JetType _double = standardLibrary.getDoubleType();
        JetType _float = standardLibrary.getFloatType();
        JetType _long = standardLibrary.getLongType();
        JetType _int = standardLibrary.getIntType();
        JetType _byte = standardLibrary.getByteType();
        JetType _short = standardLibrary.getShortType();

        if (eq(specific, _double) && eq(general, _float)) return true;
        if (eq(specific, _int)) {
            if (eq(general, _long)) return true;
            if (eq(general, _byte)) return true;
            if (eq(general, _short)) return true;
        }
        if (eq(specific, _short) && eq(general, _byte)) return true;
        return false;
    }

    private boolean eq(@NotNull JetType a, @NotNull JetType b) {
        return semanticServices.getTypeChecker().isSubtypeOf(a, b) && semanticServices.getTypeChecker().isSubtypeOf(b, a);
    }

    private boolean overrides(@NotNull FunctionDescriptor f, @NotNull FunctionDescriptor g) {
        Set<? extends FunctionDescriptor> overriddenFunctions = f.getOriginal().getOverriddenFunctions();
        FunctionDescriptor originalG = g.getOriginal();
        for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
            if (originalG.equals(overriddenFunction.getOriginal())) return true;
        }
        return false;
    }

    public void checkGenericBoundsInAFunctionCall(List<JetTypeProjection> jetTypeArguments, List<JetType> typeArguments, FunctionDescriptor functionDescriptor) {
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
            splitLexicallyLocalDescriptors(extensionFunctionDescriptors, scope.getContainingDeclaration(), local, nonlocal);


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

    private <T extends DeclarationDescriptor> void splitLexicallyLocalDescriptors(
            Collection<? extends T> allDescriptors, DeclarationDescriptor containerOfTheCurrentLocality, List<? super T> local, List<? super T> nonlocal) {

        for (T descriptor : allDescriptors) {
            if (isLocal(containerOfTheCurrentLocality, descriptor)) {
                local.add(descriptor);
            }
            else {
                nonlocal.add(descriptor);
            }
        }
    }

    /**
     * The primary case for local extensions is the following:
     *
     * I had a locally declared extension function or a local variable of function type called foo
     * And I called it on my x
     * Now, someone added function foo() to the class of x
     * My code should not change
     *
     * thus
     *
     * local extension prevail over members (and members prevail over all non-local extensions)
     */
    private boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
        DeclarationDescriptor parent = candidate.getContainingDeclaration();
        if (!(parent instanceof FunctionDescriptor)) {
            return false;                                                    
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
        DeclarationDescriptor current = containerOfTheCurrentLocality;
        while (current != null) {
            if (current == functionDescriptor) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    private boolean checkValueParameters(@NotNull FunctionDescriptor functionDescriptor, @NotNull List<JetType> parameterTypes) {
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        if (valueParameters.size() != parameterTypes.size()) return false;
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameter = valueParameters.get(i);
            JetType expectedType = parameterTypes.get(i);
            if (!eq(expectedType, valueParameter.getOutType())) return false;
        }
        return true;
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
