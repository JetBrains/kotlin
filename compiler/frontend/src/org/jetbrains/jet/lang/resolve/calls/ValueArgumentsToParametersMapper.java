/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.model.*;
import org.jetbrains.jet.lang.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.*;
import static org.jetbrains.jet.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.ERROR;
import static org.jetbrains.jet.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.STRONG_ERROR;

/*package*/ class ValueArgumentsToParametersMapper {

    public enum Status {
        STRONG_ERROR(false),
        ERROR(false),
        WEAK_ERROR(false),
        OK(true);

        private final boolean success;

        private Status(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public Status compose(Status other) {
            if (this == STRONG_ERROR || other == STRONG_ERROR) return STRONG_ERROR;
            if (this == ERROR || other == ERROR) return ERROR;
            if (this == WEAK_ERROR || other == WEAK_ERROR) return WEAK_ERROR;
            return this;
        }
    }
    public static <D extends CallableDescriptor> Status mapValueArgumentsToParameters(
            @NotNull Call call,
            @NotNull TracingStrategy tracing,
            @NotNull ResolvedCallImpl<D> candidateCall,
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
        private final ResolvedCallImpl<D> candidateCall;

        private final Map<Name,ValueParameterDescriptor> parameterByName;

        private final Set<ValueArgument> unmappedArguments = Sets.newHashSet();
        private final Map<ValueParameterDescriptor, VarargValueArgument> varargs = Maps.newHashMap();
        private final Set<ValueParameterDescriptor> usedParameters = Sets.newHashSet();
        private Status status = OK;

        private Processor(@NotNull Call call, @NotNull ResolvedCallImpl<D> candidateCall, @NotNull TracingStrategy tracing) {
            this.call = call;
            this.tracing = tracing;
            this.candidateCall = candidateCall;

            this.parameterByName = Maps.newHashMap();
            for (ValueParameterDescriptor valueParameter : candidateCall.getCandidateDescriptor().getValueParameters()) {
                parameterByName.put(valueParameter.getName(), valueParameter);
            }
        }

        private final ProcessorState initial = new ProcessorState() {

            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                return namedOnly.processNamedArgument(argument);
            }

            @Override
            public ProcessorState processPositionedArgument(
                    @NotNull ValueArgument argument, int index
            ) {
                return positionedOnly.processPositionedArgument(argument, index);
            }
        };

        private final ProcessorState positionedOnly = new ProcessorState() {
            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                return error.processNamedArgument(argument);
            }

            @Override
            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument, int index) {
                BindingTrace traceForCall = candidateCall.getTrace();
                D candidate = candidateCall.getCandidateDescriptor();

                List<ValueParameterDescriptor> valueParameters = candidate.getValueParameters();

                int parameterCount = valueParameters.size();
                if (index < parameterCount) {
                    ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(index);
                    usedParameters.add(valueParameterDescriptor);
                    putVararg(valueParameterDescriptor, argument);
                }
                else if (!valueParameters.isEmpty()) {
                    ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                    if (valueParameterDescriptor.getVarargElementType() != null) {
                        putVararg(valueParameterDescriptor, argument);
                        usedParameters.add(valueParameterDescriptor);
                    }
                    else {
                        traceForCall.report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidate));
                        unmappedArguments.add(argument);
                        setStatus(WEAK_ERROR);
                    }
                }
                else {
                    traceForCall.report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidate));
                    unmappedArguments.add(argument);
                    setStatus(ERROR);
                }

                return positionedOnly;
            }
        };

        private final ProcessorState namedOnly = new ProcessorState() {
            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                assert argument.isNamed();
                DelegatingBindingTrace traceForCall = candidateCall.getTrace();

                JetSimpleNameExpression nameReference = argument.getArgumentName().getReferenceExpression();
                ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(nameReference.getReferencedNameAsName());
                if (valueParameterDescriptor == null) {
                    traceForCall.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference));
                    unmappedArguments.add(argument);
                    setStatus(WEAK_ERROR);
                }
                else {
                    traceForCall.record(REFERENCE_TARGET, nameReference, valueParameterDescriptor);
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        traceForCall.report(ARGUMENT_PASSED_TWICE.on(nameReference));
                        unmappedArguments.add(argument);
                        setStatus(WEAK_ERROR);
                    }
                    else {
                        putVararg(valueParameterDescriptor, argument);
                    }
                }

                return namedOnly;
            }

            @Override
            public ProcessorState processPositionedArgument(
                    @NotNull ValueArgument argument, int index
            ) {
                return error.processPositionedArgument(argument, index);
            }
        };

        private final ProcessorState error = new ProcessorState() {
            @Override
            public ProcessorState processNamedArgument(@NotNull ValueArgument argument) {
                namedOnly.processNamedArgument(argument);

                candidateCall.getTrace().report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.getArgumentName()));
                setStatus(WEAK_ERROR);
                return error;
            }

            @Override
            public ProcessorState processPositionedArgument(
                    @NotNull ValueArgument argument, int index
            ) {
                candidateCall.getTrace().report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.asElement()));
                setStatus(WEAK_ERROR);
                return error;
            }
        };

        public void process() {
            ProcessorState state = initial;
            List<? extends ValueArgument> arguments = call.getValueArguments();
            for (int i = 0; i < arguments.size(); i++) {
                ValueArgument valueArgument = arguments.get(i);
                if (valueArgument.isNamed()) {
                    state = state.processNamedArgument(valueArgument);
                }
                else {
                    state = state.processPositionedArgument(valueArgument, i);
                }
            }

            processFunctionLiteralArguments();
            reportUnmappedParameters();
            checkReceiverArgument();

            assert (candidateCall.getThisObject().exists() == (candidateCall.getResultingDescriptor().getExpectedThisObject() != null))
                    : "Shouldn't happen because of TaskPrioritizer: " + candidateCall.getCandidateDescriptor();
        }

        private void processFunctionLiteralArguments() {
            D candidate = candidateCall.getCandidateDescriptor();
            DelegatingBindingTrace traceForCall = candidateCall.getTrace();
            List<ValueParameterDescriptor> valueParameters = candidate.getValueParameters();

            List<JetExpression> functionLiteralArguments = call.getFunctionLiteralArguments();
            if (!functionLiteralArguments.isEmpty()) {
                JetExpression possiblyLabeledFunctionLiteral = functionLiteralArguments.get(0);

                if (valueParameters.isEmpty()) {
                    traceForCall.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                    setStatus(ERROR);
                }
                else {
                    JetFunctionLiteralExpression functionLiteral;
                    if (possiblyLabeledFunctionLiteral instanceof JetLabelQualifiedExpression) {
                        JetLabelQualifiedExpression labeledFunctionLiteral = (JetLabelQualifiedExpression) possiblyLabeledFunctionLiteral;
                        functionLiteral = (JetFunctionLiteralExpression) labeledFunctionLiteral.getLabeledExpression();
                    }
                    else {
                        functionLiteral = (JetFunctionLiteralExpression) possiblyLabeledFunctionLiteral;
                    }

                    ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                    if (valueParameterDescriptor.getVarargElementType() != null) {
                        traceForCall.report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
                        setStatus(ERROR);
                    }
                    else {
                        if (!usedParameters.add(valueParameterDescriptor)) {
                            traceForCall.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                            setStatus(WEAK_ERROR);
                        }
                        else {
                            putVararg(valueParameterDescriptor, CallMaker.makeValueArgument(functionLiteral));
                        }
                    }
                }

                for (int i = 1; i < functionLiteralArguments.size(); i++) {
                    JetExpression argument = functionLiteralArguments.get(i);
                    traceForCall.report(MANY_FUNCTION_LITERAL_ARGUMENTS.on(argument));
                    setStatus(WEAK_ERROR);
                }
            }
        }

        private void reportUnmappedParameters() {
            DelegatingBindingTrace traceForCall = candidateCall.getTrace();

            List<ValueParameterDescriptor> valueParameters = candidateCall.getCandidateDescriptor().getValueParameters();
            for (ValueParameterDescriptor valueParameter : valueParameters) {
                if (!usedParameters.contains(valueParameter)) {
                    if (valueParameter.hasDefaultValue()) {
                        candidateCall.recordValueArgument(valueParameter, DefaultValueArgument.DEFAULT);
                    }
                    else if (valueParameter.getVarargElementType() != null) {
                        candidateCall.recordValueArgument(valueParameter, new VarargValueArgument());
                    }
                    else {
                        tracing.noValueForParameter(traceForCall, valueParameter);
                        setStatus(ERROR);
                    }
                }
            }
        }

        private void checkReceiverArgument() {
            DelegatingBindingTrace traceForCall = candidateCall.getTrace();
            D candidate = candidateCall.getCandidateDescriptor();

            ReceiverParameterDescriptor receiverParameter = candidate.getReceiverParameter();
            ReceiverValue receiverArgument = candidateCall.getReceiverArgument();
            if (receiverParameter != null &&!receiverArgument.exists()) {
                tracing.missingReceiver(traceForCall, receiverParameter);
                setStatus(ERROR);
            }
            if (receiverParameter == null && receiverArgument.exists()) {
                tracing.noReceiverAllowed(traceForCall);
                if (call.getCalleeExpression() instanceof JetSimpleNameExpression) {
                    setStatus(STRONG_ERROR);
                }
                else {
                    setStatus(ERROR);
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
                    candidateCall.recordValueArgument(valueParameterDescriptor, vararg);
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

        private interface ProcessorState {
            ProcessorState processNamedArgument(@NotNull ValueArgument argument);
            ProcessorState processPositionedArgument(@NotNull ValueArgument argument, int index);
        }

    }
}
