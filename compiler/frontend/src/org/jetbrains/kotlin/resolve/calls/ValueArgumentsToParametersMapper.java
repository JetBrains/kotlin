/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.components.ArgumentsUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.Errors.BadNamedArgumentsTarget.*;
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
            @NotNull MutableResolvedCall<D> candidateCall
    ) {
        //return new ValueArgumentsToParametersMapper().process(call, tracing, candidateCall, unmappedArguments);
        Processor<D> processor = new Processor<>(call, candidateCall, tracing);
        processor.process();
        return processor.status;
    }

    private static class Processor<D extends CallableDescriptor> {
        private final Call call;
        private final TracingStrategy tracing;
        private final MutableResolvedCall<D> candidateCall;
        private final List<ValueParameterDescriptor> parameters;

        private final Map<Name,ValueParameterDescriptor> parameterByName;
        private Map<Name,ValueParameterDescriptor> parameterByNameInOverriddenMethods;

        private final Map<ValueParameterDescriptor, VarargValueArgument> varargs = Maps.newHashMap();
        private final Set<ValueParameterDescriptor> usedParameters = Sets.newHashSet();
        private Status status = OK;

        private Processor(@NotNull Call call, @NotNull MutableResolvedCall<D> candidateCall, @NotNull TracingStrategy tracing) {
            this.call = call;
            this.tracing = tracing;
            this.candidateCall = candidateCall;
            this.parameters = candidateCall.getCandidateDescriptor().getValueParameters();

            this.parameterByName = Maps.newHashMap();
            for (ValueParameterDescriptor valueParameter : parameters) {
                parameterByName.put(valueParameter.getName(), valueParameter);
            }
        }

        @Nullable
        private ValueParameterDescriptor getParameterByNameInOverriddenMethods(Name name) {
            if (parameterByNameInOverriddenMethods == null) {
                parameterByNameInOverriddenMethods = Maps.newHashMap();
                for (ValueParameterDescriptor valueParameter : parameters) {
                    for (ValueParameterDescriptor parameterDescriptor : valueParameter.getOverriddenDescriptors()) {
                        parameterByNameInOverriddenMethods.put(parameterDescriptor.getName(), valueParameter);
                    }
                }
            }

            return parameterByNameInOverriddenMethods.get(name);
        }

        // We saw only positioned arguments so far
        private final ProcessorState positionedOnly = new ProcessorState() {
            private int currentParameter = 0;

            private int numberOfParametersForPositionedArguments() {
                return call.getCallType() == Call.CallType.ARRAY_SET_METHOD ? parameters.size() - 1 : parameters.size();
            }

            @Nullable
            public ValueParameterDescriptor nextValueParameter() {
                if (currentParameter >= numberOfParametersForPositionedArguments()) return null;

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
            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument) {
                processArgument(argument, nextValueParameter());
                return positionedOnly;
            }

            @Override
            public ProcessorState processArraySetRHS(@NotNull ValueArgument argument) {
                processArgument(argument, CollectionsKt.lastOrNull(parameters));
                return positionedOnly;
            }

            private void processArgument(@NotNull ValueArgument argument, @Nullable ValueParameterDescriptor parameter) {
                if (parameter != null) {
                    usedParameters.add(parameter);
                    putVararg(parameter, argument);
                }
                else {
                    report(TOO_MANY_ARGUMENTS.on(argument.asElement(), candidateCall.getCandidateDescriptor()));
                    setStatus(WEAK_ERROR);
                }
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
                KtSimpleNameExpression nameReference = argumentName.getReferenceExpression();

                KtPsiUtilKt.checkReservedYield(nameReference, candidateCall.getTrace());
                if (nameReference != null) {
                    if (candidate instanceof MemberDescriptor && ((MemberDescriptor) candidate).isExpect() &&
                        candidate.getContainingDeclaration() instanceof ClassDescriptor) {
                        // We do not allow named arguments for members of expected classes until we're able to use both
                        // expected and actual definitions when compiling platform code
                        report(NAMED_ARGUMENTS_NOT_ALLOWED.on(nameReference, EXPECTED_CLASS_MEMBER));
                    }
                    else if (!candidate.hasStableParameterNames()) {
                        report(NAMED_ARGUMENTS_NOT_ALLOWED.on(
                                nameReference,
                                candidate instanceof FunctionInvokeDescriptor ? INVOKE_ON_FUNCTION_TYPE : NON_KOTLIN_FUNCTION
                        ));
                    }
                }

                if (candidate.hasStableParameterNames() && nameReference != null  &&
                    candidate instanceof CallableMemberDescriptor && ((CallableMemberDescriptor)candidate).getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                    if (valueParameterDescriptor == null) {
                        valueParameterDescriptor = getParameterByNameInOverriddenMethods(argumentName.getAsName());
                    }

                    if (valueParameterDescriptor != null) {
                        for (ValueParameterDescriptor parameterFromSuperclass : valueParameterDescriptor.getOverriddenDescriptors()) {
                            if (OverrideResolver.Companion.shouldReportParameterNameOverrideWarning(valueParameterDescriptor, parameterFromSuperclass)) {
                                report(NAME_FOR_AMBIGUOUS_PARAMETER.on(nameReference));
                            }
                        }
                    }
                }

                if (valueParameterDescriptor == null) {
                    if (nameReference != null) {
                        report(NAMED_PARAMETER_NOT_FOUND.on(nameReference, nameReference));
                    }
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
                        setStatus(WEAK_ERROR);
                    }
                    else {
                        putVararg(valueParameterDescriptor, argument);
                    }
                }

                return positionedThenNamed;
            }

            @Override
            public ProcessorState processPositionedArgument(@NotNull ValueArgument argument) {
                report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(argument.asElement()));
                setStatus(WEAK_ERROR);

                return positionedThenNamed;
            }

            @Override
            public ProcessorState processArraySetRHS(@NotNull ValueArgument argument) {
                throw new IllegalStateException("Array set RHS cannot appear after a named argument syntactically: " + argument);
            }
        };

        public void process() {
            ProcessorState state = positionedOnly;
            boolean isArraySetMethod = call.getCallType() == Call.CallType.ARRAY_SET_METHOD;
            List<? extends ValueArgument> argumentsInParentheses = CallUtilKt.getValueArgumentsInParentheses(call);
            for (Iterator<? extends ValueArgument> iterator = argumentsInParentheses.iterator(); iterator.hasNext(); ) {
                ValueArgument valueArgument = iterator.next();
                if (valueArgument.isNamed()) {
                    state = state.processNamedArgument(valueArgument);
                }
                else if (isArraySetMethod && !iterator.hasNext()) {
                    state = state.processArraySetRHS(valueArgument);
                }
                else {
                    state = state.processPositionedArgument(valueArgument);
                }
            }

            for (Map.Entry<ValueParameterDescriptor, VarargValueArgument> entry : varargs.entrySet()) {
                candidateCall.recordValueArgument(entry.getKey(), entry.getValue());
            }

            processFunctionLiteralArguments();
            reportUnmappedParameters();
        }

        private void processFunctionLiteralArguments() {
            List<? extends LambdaArgument> functionLiteralArguments = call.getFunctionLiteralArguments();
            if (functionLiteralArguments.isEmpty()) return;

            LambdaArgument lambdaArgument = functionLiteralArguments.get(0);
            KtExpression possiblyLabeledFunctionLiteral = lambdaArgument.getArgumentExpression();

            if (parameters.isEmpty()) {
                report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidateCall.getCandidateDescriptor()));
                setStatus(ERROR);
            }
            else {
                ValueParameterDescriptor lastParameter = CollectionsKt.last(parameters);
                if (lastParameter.getVarargElementType() != null) {
                    report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
                    setStatus(ERROR);
                }
                else if (!usedParameters.add(lastParameter)) {
                    report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidateCall.getCandidateDescriptor()));
                    setStatus(WEAK_ERROR);
                }
                else {
                    putVararg(lastParameter, lambdaArgument);
                }
            }

            for (int i = 1; i < functionLiteralArguments.size(); i++) {
                KtExpression argument = functionLiteralArguments.get(i).getArgumentExpression();
                report(MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(argument));
                setStatus(WEAK_ERROR);
            }
        }

        private void reportUnmappedParameters() {
            for (ValueParameterDescriptor valueParameter : parameters) {
                if (!usedParameters.contains(valueParameter)) {
                    if (ArgumentsUtilsKt.hasDefaultValue(valueParameter)) {
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

        private void putVararg(ValueParameterDescriptor valueParameterDescriptor, ValueArgument valueArgument) {
            if (valueParameterDescriptor.getVarargElementType() != null) {
                VarargValueArgument vararg = varargs.computeIfAbsent(valueParameterDescriptor, k -> new VarargValueArgument());
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

            ProcessorState processPositionedArgument(@NotNull ValueArgument argument);

            ProcessorState processArraySetRHS(@NotNull ValueArgument argument);
        }
    }

    private ValueArgumentsToParametersMapper() {}
}
