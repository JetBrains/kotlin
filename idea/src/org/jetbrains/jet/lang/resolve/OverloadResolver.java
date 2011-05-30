package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionGroup;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class OverloadResolver {

    private final JetTypeChecker typeChecker;

    public OverloadResolver(JetTypeChecker typeChecker) {
        this.typeChecker = typeChecker;
    }

    @NotNull
    public OverloadDomain getOverloadDomain(@Nullable JetType receiverType, @NotNull JetScope outerScope, @NotNull String name) {
        // TODO : extension lookup
        JetScope scope = receiverType == null ? outerScope : new ScopeWithReceiver(outerScope, receiverType, typeChecker);

        final FunctionGroup functionGroup = scope.getFunctionGroup(name);

        return getOverloadDomain(receiverType, functionGroup);
    }

    @NotNull
    public OverloadDomain getOverloadDomain(@Nullable final JetType receiverType, @NotNull final FunctionGroup functionGroup) {
        if (functionGroup.isEmpty()) {
            return OverloadDomain.EMPTY;
        }

        return new OverloadDomain() {
            @NotNull
            @Override
            public OverloadResolutionResult getFunctionDescriptorForPositionedArguments(@NotNull final List<JetType> typeArguments, @NotNull List<JetType> positionedValueArgumentTypes) {
                OverloadResolutionResult resolutionResult = functionGroup.getPossiblyApplicableFunctions(typeArguments, positionedValueArgumentTypes);
                if (!resolutionResult.isAmbiguity() && !resolutionResult.isSuccess()) return resolutionResult;

                Collection<FunctionDescriptor> possiblyApplicableFunctions = resolutionResult.getFunctionDescriptors();

                if (possiblyApplicableFunctions.isEmpty()) {
                    return OverloadResolutionResult.nameNotFound(); // TODO : it may be found, only the number of params did not match
                }

                List<FunctionDescriptor> applicable = new ArrayList<FunctionDescriptor>();

                descLoop:
                for (FunctionDescriptor descriptor : possiblyApplicableFunctions) {
                    // ASSERT: type arguments are figured out and substituted by this time!!!
                    assert descriptor.getTypeParameters().isEmpty();

                    if (receiverType != null) {
                        // ASSERT : either the receiver in not present or we are in a scope with no top-level functions
                        JetType functionReceiverType = descriptor.getReceiverType();
                        if (functionReceiverType != null) {
                            functionReceiverType = TypeUtils.makeNullable(functionReceiverType); // Too look things up in T for T?, and later check receiver's nullability
                            if (!typeChecker.isSubtypeOf(receiverType, functionReceiverType)) {
                                continue;
                            }
                        }
                    }
                    else if (descriptor.getReceiverType() != null) {
                        continue;
                    }

                    List<ValueParameterDescriptor> parameters = descriptor.getUnsubstitutedValueParameters();
                    if (parameters.size() >= positionedValueArgumentTypes.size()) {
                        // possibly, some default values
                        // possibly, nothing passed to a vararg
                        // possibly, a single value passed to a vararg
                        // possibly an array/list/etc passed as a whole vararg
                        for (int i = 0, positionedValueArgumentTypesSize = positionedValueArgumentTypes.size(); i < positionedValueArgumentTypesSize; i++) {
                            JetType argumentType = positionedValueArgumentTypes.get(i);
                            JetType parameterType = parameters.get(i).getOutType();
                            // TODO : handle vararg cases here
                            if (!typeChecker.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                    } else {
                        // vararg
                        int nonVarargs = parameters.size() - 1;
                        for (int i = 0; i < nonVarargs; i++) {
                            JetType argumentType = positionedValueArgumentTypes.get(i);
                            JetType parameterType = parameters.get(i).getOutType();
                            if (!typeChecker.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                        JetType varArgType = parameters.get(nonVarargs).getOutType();
                        for (int i = nonVarargs, args = positionedValueArgumentTypes.size(); i < args; i++) {
                            JetType argumentType = positionedValueArgumentTypes.get(i);
                            if (!typeChecker.isConvertibleTo(argumentType, varArgType)) {
                                continue descLoop;
                            }
                        }
                    }
                    applicable.add(descriptor);
                }

                if (applicable.size() == 0) {
                    if (resolutionResult.singleFunction()) {
                        return OverloadResolutionResult.singleFunctionArgumentMismatch(resolutionResult.getFunctionDescriptor());
                    }
                    return OverloadResolutionResult.nameNotFound();
                } else if (applicable.size() == 1) {
                    return OverloadResolutionResult.success(applicable.get(0));
                } else {
                    // TODO : varargs

                    List<FunctionDescriptor> maximallySpecific = new ArrayList<FunctionDescriptor>();
                    meLoop:
                    for (FunctionDescriptor me : applicable) {
                        for (FunctionDescriptor other : applicable) {
                            if (other == me) continue;
                            if (!moreSpecific(me, other) || moreSpecific(other, me)) continue meLoop;
                        }
                        maximallySpecific.add(me);
                    }
                    if (maximallySpecific.isEmpty()) {
                        return OverloadResolutionResult.ambiguity(applicable);
                    }
                    if (maximallySpecific.size() == 1) {
                        return OverloadResolutionResult.success(maximallySpecific.get(0));
                    }
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public boolean isEmpty() {
                return functionGroup.isEmpty();
            }

            @NotNull
            @Override
            public OverloadResolutionResult getFunctionDescriptorForNamedArguments(@NotNull List<JetType> typeArguments, @NotNull Map<String, JetType> valueArgumentTypes, @Nullable JetType functionLiteralArgumentType) {
                throw new UnsupportedOperationException(); // TODO
            }
        };
    }

    private boolean moreSpecific(FunctionDescriptor f, FunctionDescriptor g) {
        List<ValueParameterDescriptor> fParams = f.getUnsubstitutedValueParameters();
        List<ValueParameterDescriptor> gParams = g.getUnsubstitutedValueParameters();

        int fSize = fParams.size();
        if (fSize != gParams.size()) return false;
        for (int i = 0; i < fSize; i++) {
            JetType fParamType = fParams.get(i).getOutType();
            JetType gParamType = gParams.get(i).getOutType();

            // TODO : maybe isSubtypeOf is sufficient?
            if (!typeChecker.isConvertibleTo(fParamType, gParamType)) {
                return false;
            }
        }
        return true;
    }

}
