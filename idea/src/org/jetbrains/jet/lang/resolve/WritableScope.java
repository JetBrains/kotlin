package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class WritableScope implements JetScope {
    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptors;
    @Nullable
    private Map<String, WritableFunctionGroup> functionGroups;

    @NotNull
    private Map<String, PropertyDescriptor> getPropertyDescriptors() {
        if (propertyDescriptors == null) {
            propertyDescriptors = new HashMap<String, PropertyDescriptor>();
        }
        return propertyDescriptors;
    }

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        assert !propertyDescriptors.containsKey(propertyDescriptor.getName()) : "Property redeclared";
        propertyDescriptors.put(propertyDescriptor.getName(), propertyDescriptor);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        @NotNull
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        return propertyDescriptors.get(name);
    }

    @NotNull
    private Map<String, WritableFunctionGroup> getFunctionGroups() {
        if (functionGroups == null) {
            functionGroups = new HashMap<String, WritableFunctionGroup>();
        }
        return functionGroups;
    }

    public void addFunctionDescriptor(FunctionDescriptor functionDescriptor) {
        String name = functionDescriptor.getName();
        Map<String, WritableFunctionGroup> functionGroups = getFunctionGroups();

        @Nullable
        WritableFunctionGroup functionGroup = functionGroups.get(name);
        if (functionGroup == null) {
            functionGroup = new WritableFunctionGroup(name);
            functionGroups.put(name, functionGroup);
        }
        functionGroup.addFunction(functionDescriptor);
    }

    @NotNull
    private FunctionGroup getFunctionGroup(String name) {
        WritableFunctionGroup functionGroup = getFunctionGroups().get(name);
        if (functionGroup == null) {
            return FunctionGroup.EMPTY;
        }
        return functionGroup;
    }

    @NotNull
    @Override
    public OverloadDomain getOverloadDomain(Type receiverType, @NotNull String referencedName) {
        final FunctionGroup functionGroup = getFunctionGroups().get(referencedName);
        if (functionGroup == null) {
            return OverloadDomain.EMPTY;
        }

        return new OverloadDomain() {
            @Override
            public Type getReturnTypeForPositionedArguments(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
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
                    return applicable.get(0).getUnsubstitutedReturnType();
                } else {
                    throw new UnsupportedOperationException();
                }
            }

            @Override
            public Type getReturnTypeForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
                throw new UnsupportedOperationException(); // TODO
            }
        };
    }

    @Override
    public ClassDescriptor getClass(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }
}
