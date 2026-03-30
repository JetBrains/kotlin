/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtValueArgument;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.psi.ValueArgumentName;
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ParameterSpreadCallAdapter {
    @NotNull
    public static Call adaptCallIfNeeded(@NotNull Call call, @NotNull CallableDescriptor descriptor) {
        List<? extends ValueArgument> originalArguments = call.getValueArguments();
        if (originalArguments.isEmpty()) {
            return call;
        }

        List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();
        if (parameters.isEmpty() || hasVarargParameter(parameters) || !containsParameterSpread(originalArguments)) {
            return call;
        }

        Set<Name> explicitlyNamedParameters = collectExplicitlyNamedParameters(originalArguments);
        Set<Name> consumedParameters = new LinkedHashSet<>();
        List<ValueArgument> adaptedArguments = new ArrayList<>();
        int positionedParameterIndex = 0;

        for (ValueArgument argument : originalArguments) {
            if (isParameterSpread(argument)) {
                KtExpression spreadExpression = argument.getArgumentExpression();
                if (argument instanceof KtValueArgument) {
                    spreadExpression = ((KtValueArgument) argument).getParameterSpreadReceiverExpression();
                }
                if (spreadExpression == null) {
                    adaptedArguments.add(argument);
                    continue;
                }

                Set<Name> excludedParameters = argument instanceof KtValueArgument
                        ? ((KtValueArgument) argument).getParameterSpreadExcludedNames()
                        : java.util.Collections.emptySet();

                for (int i = positionedParameterIndex; i < parameters.size(); i++) {
                    ValueParameterDescriptor parameter = parameters.get(i);
                    Name parameterName = parameter.getName();
                    if (consumedParameters.contains(parameterName)
                            || explicitlyNamedParameters.contains(parameterName)
                            || excludedParameters.contains(parameterName)) {
                        continue;
                    }

                    adaptedArguments.add(
                            new ParameterSpreadProjectionValueArgumentImpl(argument, spreadExpression, parameterName)
                    );
                    consumedParameters.add(parameterName);
                }
                continue;
            }

            adaptedArguments.add(argument);
            ValueArgumentName argumentName = argument.getArgumentName();
            if (argumentName != null) {
                consumedParameters.add(argumentName.getAsName());
                continue;
            }

            if (positionedParameterIndex < parameters.size()) {
                consumedParameters.add(parameters.get(positionedParameterIndex).getName());
                positionedParameterIndex++;
            }
        }

        return new DelegatingCall(call) {
            @Override
            @NotNull
            public List<? extends ValueArgument> getValueArguments() {
                return adaptedArguments;
            }
        };
    }

    private static boolean hasVarargParameter(@NotNull List<ValueParameterDescriptor> parameters) {
        for (ValueParameterDescriptor parameter : parameters) {
            if (parameter.getVarargElementType() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsParameterSpread(@NotNull List<? extends ValueArgument> arguments) {
        for (ValueArgument argument : arguments) {
            if (isParameterSpread(argument)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isParameterSpread(@NotNull ValueArgument argument) {
        return argument instanceof KtValueArgument && ((KtValueArgument) argument).isParameterSpread();
    }

    @NotNull
    private static Set<Name> collectExplicitlyNamedParameters(@NotNull List<? extends ValueArgument> arguments) {
        Set<Name> result = new LinkedHashSet<>();
        for (ValueArgument argument : arguments) {
            if (isParameterSpread(argument)) {
                continue;
            }

            ValueArgumentName argumentName = argument.getArgumentName();
            if (argumentName != null) {
                result.add(argumentName.getAsName());
            }
        }
        return result;
    }
    private ParameterSpreadCallAdapter() {
    }
}
