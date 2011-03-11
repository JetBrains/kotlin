package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class OverloadResolver {

    private final JetTypeChecker typeChecker;

    public OverloadResolver(JetTypeChecker typeChecker) {
        this.typeChecker = typeChecker;
    }

    @NotNull
    public OverloadDomain getOverloadDomain(Type receiverType, @NotNull JetScope outerScope, @NotNull String name) {
        // TODO : extension lookup
        JetScope scope = receiverType == null ? outerScope : receiverType.getMemberScope();

        final FunctionGroup functionGroup = scope.getFunctionGroup(name);

        if (functionGroup.isEmpty()) {
            return OverloadDomain.EMPTY;
        }

        return new OverloadDomain() {
            @Override
            public FunctionDescriptor getFunctionDescriptorForPositionedArguments(@NotNull final List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
                Collection<FunctionDescriptor> possiblyApplicableFunctions = functionGroup.getPossiblyApplicableFunctions(typeArguments, positionedValueArgumentTypes);
                if (possiblyApplicableFunctions.isEmpty()) {
                    return null;
                }

                List<FunctionDescriptor> applicable = new ArrayList<FunctionDescriptor>();

                descLoop:
                for (FunctionDescriptor descriptor : possiblyApplicableFunctions) {
                    // ASSERT: type arguments are figured out and substituted by this time!!!
                    assert descriptor.getTypeParameters().isEmpty();

                    List<ValueParameterDescriptor> parameters = descriptor.getUnsubstitutedValueParameters();
                    if (parameters.size() >= positionedValueArgumentTypes.size()) {
                        // possibly, some default values
                        // possibly, nothing passed to a vararg
                        // possibly, a single value passed to a vararg
                        // possibly an array/list/etc passed as a whole vararg
                        for (int i = 0, positionedValueArgumentTypesSize = positionedValueArgumentTypes.size(); i < positionedValueArgumentTypesSize; i++) {
                            Type argumentType = positionedValueArgumentTypes.get(i);
                            Type parameterType = parameters.get(i).getType();
                            // TODO : handle vararg cases here
                            if (!typeChecker.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                    } else {
                        // vararg
                        int nonVarargs = parameters.size() - 1;
                        for (int i = 0; i < nonVarargs; i++) {
                            Type argumentType = positionedValueArgumentTypes.get(i);
                            Type parameterType = parameters.get(i).getType();
                            if (!typeChecker.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                        Type varArgType = parameters.get(nonVarargs).getType();
                        for (int i = nonVarargs, args = positionedValueArgumentTypes.size(); i < args; i++) {
                            Type argumentType = positionedValueArgumentTypes.get(i);
                            if (!typeChecker.isConvertibleTo(argumentType, varArgType)) {
                                continue descLoop;
                            }
                        }
                    }
                    applicable.add(descriptor);
                }

                if (applicable.size() == 0) {
                    return null;
                } else if (applicable.size() == 1) {
                    return applicable.get(0);
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
                        return null;
                    }
                    if (maximallySpecific.size() == 1) {
                        return maximallySpecific.get(0);
                    }
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
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
            Type fParamType = fParams.get(i).getType();
            Type gParamType = gParams.get(i).getType();

            // TODO : maybe isSubtypeOf is sufficient?
            if (!typeChecker.isConvertibleTo(fParamType, gParamType)) {
                return false;
            }
        }
        return true;
    }

}
