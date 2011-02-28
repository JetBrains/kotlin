package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class OverloadResolver {
    public static final OverloadResolver INSTANCE = new OverloadResolver();

    private OverloadResolver() {}

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
                            if (!JetTypeChecker.INSTANCE.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                    } else {
                        // vararg
                        int nonVarargs = parameters.size() - 1;
                        for (int i = 0; i < nonVarargs; i++) {
                            Type argumentType = positionedValueArgumentTypes.get(i);
                            Type parameterType = parameters.get(i).getType();
                            if (!JetTypeChecker.INSTANCE.isConvertibleTo(argumentType, parameterType)) {
                                continue descLoop;
                            }
                        }
                        Type varArgType = parameters.get(nonVarargs).getType();
                        for (int i = nonVarargs, args = positionedValueArgumentTypes.size(); i < args; i++) {
                            Type argumentType = positionedValueArgumentTypes.get(i);
                            if (!JetTypeChecker.INSTANCE.isConvertibleTo(argumentType, varArgType)) {
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
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public FunctionDescriptor getFunctionDescriptorForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
                throw new UnsupportedOperationException(); // TODO
            }
        };
    }

}
