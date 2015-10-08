/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.INVOKE_ON_FUNCTION_TYPE;
import static org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.NON_KOTLIN_FUNCTION;
import static org.jetbrains.kotlin.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.kotlin.resolve.calls.ValueArgumentsToParametersMapper.Status.*;

public class ValueArgumentsToParametersMapper {

    public enum Status {
        ERROR(false),
        WEAK_ERROR(false),
        OK(true);

        private final boolean success;

        Status(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public Status compose(Status other) {
            if (this == ERROR || other == ERROR) return ERROR;
            if (this == WEAK_ERROR || other == WEAK_ERROR) return WEAK_ERROR;
            return this;
        }
    }
    public static <D extends CallableDescriptor> Status mapValueArgumentsToParameters(
            @NotNull Call call,
            @NotNull TracingStrategy tracing,
            @NotNull MutableResolvedCall<D> candidateCall,
            @NotNull Set<ValueArgument> unmappedArguments
    ) {
        //return new ValueArgumentsToParametersMapper().process(call, tracing, candidateCall, unmappedArguments);
        Processor<D> processor = new Processor<D>(call, candidateCall, tracing);
        processor.process();
        unmappedArguments.addAll(processor.unmappedArguments);
        return processor.status;
    }

    private static class Processor<D extends CallableDescriptor> {
        private final Call call;
        private final TracingStrategy tracing;
        private final MutableResolvedCall<D> candidateCall;

        private final Map<Name,ValueParameterDescriptor> parameterByName;

        private final Set<ValueArgument> unmappedArguments = Sets.newHashSet();
        private final Map<ValueParameterDescriptor, VarargValueArgument> varargs = Maps.newHashMap();
        private final Set<ValueParameterDescriptor> usedParameters = Sets.newHashSet();
        private Status status = OK;

        private Processor(@NotNull Call call, @NotNull MutableResolvedCall<D> candidateCall, @NotNull TracingStrategy tracing) {
            this.call = call;
            this.tracing = tracing;
            this.candidateCall = candidateCall;

            this.parameterByName = Maps.newHashMap();
            for (ValueParameterDescriptor valueParameter : candidateCall.getCandidateDescriptor().getValueParameters()) {
                parameterByName.put(valueParameter.getName(), valueParameter);
            }
        }

        // We saw only positioned arguments so far
        private final ProcessorState positionedOnly = new ProcessorState() {

            private int currentParameter = 0;

            @Nullable
            public ValueParameterDescriptor nextValueParameter() {
                List<ValueParameterDescriptor> parameters = candidateCall.getCandidateDescriptor().getValueParameters();
                if (currentParameter >= parameters.size()) return null;

                ValueParameterDescriptor head = parameters.get(currentParameter);

                // If we found a vararg parameter, we are stuck with it forever
                if (head.getVarargElementType() == null) {
                    currentParameter++;
                }

                return head;
            }

            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                return positionedThenNamed.processNamedArgument(argument);
            }

            @Override
            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument, int index) {
                ValueParameterDescriptor valueParameterDescriptor = nextValueParameter();

                if (valueParameterDescriptor != null) {
                    usedParameters.add(valueParameterDescriptor);
                    putVararg(valueParameterDescriptor, argument);
                }
                else {
                    report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidateCall.getCandidateDescriptor()));
                    unmappedArguments.add(argument);
                    setStatus(WEAK_ERROR);
                }

                return positionedOnly;
            }
        };

        // We saw zero or more positioned arguments and then a named one
        private final ProcessorState positionedThenNamed = new ProcessorState() {
            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                assert argument.isNamed();

                D candidate = candidateCall.getCandidateDescriptor();

                ValueArgumentName argumentName = argument.getArgumentName();
                assert argumentName != null;
                ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(argumentName.getAsName());
                JetReferenceExpression nameReference = argumentName.getReferenceExpression();
                if (!candidate.hasStableParameterNames() && nameReference != null) {
                    report(NAMED_ARGUMENTS_NOT_ALLOWED.on(
                            nameReference,
                            candidate instanceof FunctionInvokeDescriptor ? INVOKE_ON_FUNCTION_TYPE : NON_KOTLIN_FUNCTION
                    ));
                }

                if (candidate.hasStableParameterNames() && nameReference != null && valueParameterDescriptor != null) {
                    for (ValueParameterDescriptor parameterFromSuperclass : valueParameterDescriptor.getOverriddenDescriptors()) {
                        if (OverrideResolver.shouldReportParameterNameOverrideWarning(valueParameterDescriptor, parameterFromSuperclass)) {
                                report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference));
                        }
                    }
                }

                if (valueParameterDescriptor == null) {
                    if (nameReference != null) {
                        report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference));
                    }
                    unmappedArguments.add(argument);
                    setStatus(WEAK_ERROR);
                }
                else {
                    if (nameReference != null) {
                        candidateCall.getTrace().record(REFERENCE_TARGET, nameReference, valueParameterDescriptor);
                    }
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        if (nameReference != null) {
                            report(ARGUMENT_PASSED_TWICE.on(nameReference));
                        }
                        unmappedArguments.add(argument);
                        setStatus(WEAK_ERROR);
                    }
                    else {
                        putVararg(valueParameterDescriptor, argument);
                    }
                }

                return positionedThenNamed;
            }

            @Override
            public ProcessorState processPositionedArgument(
                    @NotNull ValueArgument argument, int index
            ) {
                report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.asElement()));
                setStatus(WEAK_ERROR);
                unmappedArguments.add(argument);

                return positionedThenNamed;
            }
        };

        public void process() {
            ProcessorState state = positionedOnly;
            List<? extends ValueArgument> argumentsInParentheses = CallUtilPackage.getValueArgumentsInParentheses(call);
            for (int i = 0; i < argumentsInParentheses.size(); i++) {
                ValueArgument valueArgument = argumentsInParentheses.get(i);
                if (valueArgument.isNamed()) {
                    state = state.processNamedArgument(valueArgument);
                }
                else {
                    state = state.processPositionedArgument(valueArgument, i);
                }
            }

            for (Map.Entry<ValueParameterDescriptor, VarargValueArgument> entry : varargs.entrySet()) {
                candidateCall.recordValueArgument(entry.getKey(), entry.getValue());
            }

            processFunctionLiteralArguments();
            reportUnmappedParameters();
        }

        private void processFunctionLiteralArguments() {
            D candidate = candidateCall.getCandidateDescriptor();
            List<ValueParameterDescriptor> valueParameters = candidate.getValueParameters();

            List<? extends FunctionLiteralArgument> functionLiteralArguments = call.getFunctionLiteralArguments();
            if (!functionLiteralArguments.isEmpty()) {
                FunctionLiteralArgument functionLiteralArgument = functionLiteralArguments.get(0);
                JetExpression possiblyLabeledFunctionLiteral = functionLiteralArgument.getArgumentExpression();

                if (valueParameters.isEmpty()) {
                    report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                    setStatus(ERROR);
                }
                else {
                    ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                    if (valueParameterDescriptor.getVarargElementType() != null) {
                        report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
                        setStatus(ERROR);
                    }
                    else {
                        if (!usedParameters.add(valueParameterDescriptor)) {
                            report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                            setStatus(WEAK_ERROR);
                        }
                        else {
                            putVararg(valueParameterDescriptor, functionLiteralArgument);
                        }
                    }
                }

                for (int i = 1; i < functionLiteralArguments.size(); i++) {
                    JetExpression argument = functionLiteralArguments.get(i).getArgumentExpression();
                    report(MANY_FUNCTION_LITERAL_ARGUMENTS.on(argument));
                    setStatus(WEAK_ERROR);
                }
            }
        }

        private void reportUnmappedParameters() {
            List<ValueParameterDescriptor> valueParameters = candidateCall.getCandidateDescriptor().getValueParameters();
            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (!usedParameters.contains(valueParameter)) {
                    if (DescriptorUtilPackage.hasDefaultValue(valueParameter)) {
                        candidateCall.recordValueArgument(valueParameter, DefaultValueArgument.DEFAULT);
                    }
                    else if (valueParameter.getVarargElementType() != null) {
                        candidateCall.recordValueArgument(valueParameter, new VarargValueArgument());
                    }
                    else {
                        tracing.noValueForParameter(candidateCall.getTrace(), valueParameter);
                        setStatus(ERROR);
                    }
                }
            }
        }

        private void putVararg(
                ValueParameterDescriptor valueParameterDescriptor,
                ValueArgument valueArgument
        ) {
            if (valueParameterDescriptor.getVarargElementType() != null) {
                VarargValueArgument vararg = varargs.get(valueParameterDescriptor);
                if (vararg == null) {
                    vararg = new VarargValueArgument();
                    varargs.put(valueParameterDescriptor, vararg);
                }
                vararg.addArgument(valueArgument);
            }
            else {
                LeafPsiElement spread = valueArgument.getSpreadElement();
                if (spread != null) {
                    candidateCall.getTrace().report(NON_VARARG_SPREAD.on(spread));
                    setStatus(WEAK_ERROR);
                }
                ResolvedValueArgument argument = new ExpressionValueArgument(valueArgument);
                candidateCall.recordValueArgument(valueParameterDescriptor, argument);
            }
        }

        private void setStatus(@NotNull Status newStatus) {
            status = status.compose(newStatus);
        }

        private void report(Diagnostic diagnostic) {
            candidateCall.getTrace().report(diagnostic);
        }

        private interface ProcessorState {
            ProcessorState processNamedArgument(@NotNull ValueArgument argument);
            ProcessorState processPositionedArgument(@NotNull ValueArgument argument, int index);
        }

    }

    private ValueArgumentsToParametersMapper() {}
}
