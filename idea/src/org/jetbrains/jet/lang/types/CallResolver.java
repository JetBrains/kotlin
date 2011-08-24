package org.jetbrains.jet.lang.types;

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

    private static abstract class ResolutionTask {
        private final Collection<FunctionDescriptor> candidates;
        private final JetExpression receiver;
        private final List<JetTypeProjection> typeArguments;
        private final List<? extends ValueArgument> valueArguments;

        protected ResolutionTask(
                @NotNull Collection<FunctionDescriptor> candidates,
                @Nullable JetExpression receiver,
                @NotNull List<JetTypeProjection> typeArguments,
                @NotNull List<? extends ValueArgument> valueArguments) {
            this.candidates = candidates;
            this.receiver = receiver;
            this.typeArguments = typeArguments;
            this.valueArguments = valueArguments;
        }

        @NotNull
        public Collection<FunctionDescriptor> getCandidates() {
            return candidates;
        }

        @Nullable
        public JetExpression getReceiver() {
            return receiver;
        }

        @NotNull
        public List<JetTypeProjection> getTypeArguments() {
            return typeArguments;
        }
        
        @NotNull
        public List<? extends ValueArgument> getValueArguments() {
            return valueArguments;
        }

        public abstract void bindFunctionReference(@NotNull BindingTrace trace, @NotNull FunctionDescriptor functionDescriptor);

        public abstract void reportOverallResolutionError(@NotNull BindingTrace trace, @NotNull String message);

        public abstract void reportWrongTypeArguments(@NotNull BindingTrace trace, @NotNull String message);

        public abstract void reportWrongValueArguments(@NotNull BindingTrace trace, @NotNull String message);

        public abstract void reportUnresolvedFunctionReference(@NotNull BindingTrace trace);
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
            @NotNull JetCallElement call,
            @NotNull JetType expectedType
    ) {
        FunctionDescriptor functionDescriptor = resolveSimpleCallToFunctionDescriptor(scope, null, call, expectedType);
        return functionDescriptor == null ? null : functionDescriptor.getReturnType();
    }
    
    @Nullable
    public FunctionDescriptor resolveCallWithGivenName(
            @NotNull JetScope scope,
            @NotNull final Call call,
            @NotNull final JetReferenceExpression functionReference,
            @NotNull String name,
            @Nullable JetExpression receiver,
            @NotNull JetType expectedType) {
        Collection<FunctionDescriptor> candidates;
        if (receiver != null) {
            JetType type = typeInferrer.getServices(trace).getType(scope, receiver, false, NO_EXPECTED_TYPE);
            if (type == null) {
                return null; // TODO : check parameter types anyway?
            }
            // TODO : autocasts
            // TODO : nullability
            candidates = type.getMemberScope().getFunctionGroup(name).getFunctionDescriptors();
        }
        else {
            candidates = scope.getFunctionGroup(name).getFunctionDescriptors();
        }
        return resolveCallToFunctionDescriptor(scope, receiver, call, functionReference.getNode(), expectedType, candidates, functionReference);
    }

    @Nullable
    public FunctionDescriptor resolveSimpleCallToFunctionDescriptor(
            @NotNull JetScope scope,
            @Nullable JetExpression receiver,
            @NotNull final JetCallElement call,
            @NotNull JetType expectedType
    ) {
        JetExpression calleeExpression = call.getCalleeExpression();
        Collection<FunctionDescriptor> candidates;
        final JetReferenceExpression functionReference;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            JetSimpleNameExpression expression = (JetSimpleNameExpression) calleeExpression;
            functionReference = expression;
            candidates = scope.getFunctionGroup(expression.getReferencedName()).getFunctionDescriptors();
        }
        else if (calleeExpression instanceof JetConstructorCalleeExpression) {
            JetConstructorCalleeExpression expression = (JetConstructorCalleeExpression) calleeExpression;
            functionReference = expression.getConstructorReferenceExpression();
            JetType constructedType = typeResolver.resolveType(scope, expression.getTypeReference());
            DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
            if (declarationDescriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                candidates = classDescriptor.getConstructors().getFunctionDescriptors();
            }
            else {
                trace.getErrorHandler().genericError(calleeExpression.getNode(), "Not a class");
                return null;
            }
        }
        else {
            throw new UnsupportedOperationException("Type argument inference not implemented");
        }

        return resolveCallToFunctionDescriptor(scope, receiver, call, call.getNode(), expectedType, candidates, functionReference);
    }


    private FunctionDescriptor resolveCallToFunctionDescriptor(
            JetScope scope,
            final JetExpression receiver,
            final Call call,
            final ASTNode callNode,
            JetType expectedType,
            final Collection<FunctionDescriptor> candidates,
            final JetReferenceExpression functionReference) {
        ResolutionTask task = new ResolutionTask(candidates, receiver, call.getTypeArguments(), call.getValueArguments()) {
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
        };
        return performResolution(scope, expectedType, task);
    }

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean mapValueArgumentsToParameters(
            @NotNull ResolutionTask task,
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

        boolean someNamed = false;
        boolean somePositioned = false;
        boolean error = false;
        for (int i = 0; i < valueArguments.size(); i++) {
            ValueArgument valueArgument = valueArguments.get(i);
            if (valueArgument.isNamed()) {
                someNamed = true;
                if (somePositioned) {
                    temporaryTrace.getErrorHandler().genericError(valueArgument.getArgumentName().getNode(), "Mixing named and positioned arguments in not allowed");
                    error = true;
                }
                else {
                    ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(valueArgument.getArgumentName().getName());
                    usedParameters.add(valueParameterDescriptor);
                    if (valueParameterDescriptor == null) {
                        temporaryTrace.getErrorHandler().genericError(valueArgument.getArgumentName().getNode(), "Cannot find a parameter with this name");
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
                    if (i < valueParameters.size()) {
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

        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!usedParameters.contains(valueParameter)) {
                if (!valueParameter.hasDefaultValue()) {
                    task.reportWrongValueArguments(temporaryTrace, "No value passed for parameter " + valueParameter.getName());
                    error = true;
                }
            }
        }
        return error;
    }

    @Nullable
    private FunctionDescriptor performResolution(@NotNull JetScope scope, @NotNull JetType expectedType, @NotNull ResolutionTask task) {
        Map<FunctionDescriptor, FunctionDescriptor> successfulCandidates = Maps.newHashMap();
        Set<FunctionDescriptor> failedCandidates = Sets.newHashSet();
        Map<FunctionDescriptor, ConstraintSystem.Solution> solutions = Maps.newHashMap();
        Map<FunctionDescriptor, TemporaryBindingTrace> traces = Maps.newHashMap();

        for (FunctionDescriptor candidate : task.getCandidates()) {
            TemporaryBindingTrace temporaryTrace = new TemporaryBindingTrace(trace.getBindingContext());
            traces.put(candidate, temporaryTrace);
            JetTypeInferrer.Services temporaryServices = typeInferrer.getServices(temporaryTrace);

            task.bindFunctionReference(temporaryTrace, candidate);

            Map<ValueArgument, ValueParameterDescriptor> argumentsToParameters = Maps.newHashMap();
            boolean error = mapValueArgumentsToParameters(task, candidate, temporaryTrace, argumentsToParameters);

            if (error) continue;

            if (task.getTypeArguments().isEmpty()) {
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
                    task.reportOverallResolutionError(temporaryTrace, "Type inference failed");
                    failedCandidates.add(candidate);
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

                List<JetType> typeArguments = new ArrayList<JetType>();
                for (JetTypeProjection projection : jetTypeArguments) {
                    // TODO : check that there's no projection
                    JetTypeReference typeReference = projection.getTypeReference();
                    if (typeReference != null) {
                        typeArguments.add(new TypeResolver(semanticServices, temporaryTrace, true).resolveType(scope, typeReference));
                    }
                }

                checkGenericBoundsInAFunctionCall(jetTypeArguments, typeArguments, candidate);

                int typeArgCount = typeArguments.size();
                if (candidate.getTypeParameters().size() == typeArgCount) {
                    FunctionDescriptor substitutedFunctionDescriptor = FunctionDescriptorUtil.substituteFunctionDescriptor(typeArguments, candidate);

                    assert substitutedFunctionDescriptor != null;
                    Map<ValueParameterDescriptor, ValueParameterDescriptor> parameterMap = Maps.newHashMap();
                    for (ValueParameterDescriptor valueParameterDescriptor : substitutedFunctionDescriptor.getValueParameters()) {
                        parameterMap.put(valueParameterDescriptor.getOriginal(), valueParameterDescriptor);
                    }

                    boolean localError = false;
                    for (Map.Entry<ValueArgument, ValueParameterDescriptor> entry : argumentsToParameters.entrySet()) {
                        ValueArgument valueArgument = entry.getKey();
                        ValueParameterDescriptor valueParameterDescriptor = entry.getValue();

                        ValueParameterDescriptor substitutedParameter = parameterMap.get(valueParameterDescriptor.getOriginal());

                        assert substitutedParameter != null;

                        JetType parameterType = substitutedParameter.getOutType();
                        JetType type = temporaryServices.getType(scope, valueArgument.getArgumentExpression(), false, parameterType);
                        if (type == null) {
                            localError = true;
                        }
                    }
                    if (localError) {
                        failedCandidates.add(candidate);
                    }
                    else {
                        successfulCandidates.put(candidate, substitutedFunctionDescriptor);
                    }

                }
                else {
                    failedCandidates.add(candidate);
                    task.reportWrongTypeArguments(temporaryTrace, "Number of type arguments does not match " + DescriptorRenderer.TEXT.render(candidate));
                }
            }
        }

        if (successfulCandidates.size() > 0) {
            if (successfulCandidates.size() == 1) {
                Map.Entry<FunctionDescriptor, FunctionDescriptor> entry = successfulCandidates.entrySet().iterator().next();
                FunctionDescriptor functionDescriptor = entry.getKey();
                FunctionDescriptor result = entry.getValue();

                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.addAllMyDataTo(trace);
                return result;
            }
            else {
                Map<FunctionDescriptor, TemporaryBindingTrace> maximallySpecific = Maps.newHashMap();
                meLoop:
                for (Map.Entry<FunctionDescriptor, FunctionDescriptor> myEntry : successfulCandidates.entrySet()) {
                    FunctionDescriptor me = myEntry.getValue();
                    TemporaryBindingTrace myTrace = traces.get(myEntry.getKey());
                    for (FunctionDescriptor other : successfulCandidates.values()) {
                        if (other == me) continue;
                        if (!moreSpecific(me, other) || moreSpecific(other, me)) continue meLoop;
                    }
                    maximallySpecific.put(me, myTrace);
                }
                if (maximallySpecific.size() == 1) {
                    Map.Entry<FunctionDescriptor, TemporaryBindingTrace> result = maximallySpecific.entrySet().iterator().next();
                    result.getValue().addAllMyDataTo(trace);
                    return result.getKey();
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (FunctionDescriptor functionDescriptor : successfulCandidates.keySet()) {
                    stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                }

                task.reportOverallResolutionError(trace, "Overload resolution ambiguity: " + stringBuilder);
            }
        }
        else if (!failedCandidates.isEmpty()) {
            if (failedCandidates.size() == 1) {
                FunctionDescriptor functionDescriptor = failedCandidates.iterator().next();
                TemporaryBindingTrace temporaryTrace = traces.get(functionDescriptor);
                temporaryTrace.addAllMyDataTo(trace);
            }
            else {
                StringBuilder stringBuilder = new StringBuilder();
                for (FunctionDescriptor functionDescriptor : failedCandidates) {
                    stringBuilder.append(DescriptorRenderer.TEXT.render(functionDescriptor)).append(" ");
                }

                task.reportOverallResolutionError(trace, "None of the following functions can be called with the arguments supplied: " + stringBuilder);
            }
        }
        else {
            task.reportUnresolvedFunctionReference(trace);
        }
        return null;
    }

    private boolean moreSpecific(FunctionDescriptor f, FunctionDescriptor g) {
        List<ValueParameterDescriptor> fParams = f.getValueParameters();
        List<ValueParameterDescriptor> gParams = g.getValueParameters();

        int fSize = fParams.size();
        if (fSize != gParams.size()) return false;
        for (int i = 0; i < fSize; i++) {
            JetType fParamType = fParams.get(i).getOutType();
            JetType gParamType = gParams.get(i).getOutType();

            if (!JetTypeChecker.INSTANCE.isSubtypeOf(fParamType, gParamType)) {
                return false;
            }
        }
        return true;
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
    public OverloadResolutionResult resolveExactSignature(@NotNull JetScope scope, @NotNull JetType receiver, @NotNull String name, @NotNull List<JetType> jetTypes) {
        // TODO
        return OverloadResolutionResult.nameNotFound();
    }

}
