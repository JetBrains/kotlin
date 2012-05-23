/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.TemporaryBindingTrace;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;
import static org.jetbrains.jet.lang.resolve.calls.ValueArgumentsToParametersMapper.Status.*;

/**
 * @author abreslav
 */
/*package*/ class ValueArgumentsToParametersMapper {

    public enum Status {
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
            switch (other) {
                case ERROR:
                    return ERROR;
                case WEAK_ERROR:
                    if (this != ERROR) {
                        return WEAK_ERROR;
                    }
                default:
                    return this;
            }
        }
    }

    public static <D extends CallableDescriptor> Status mapValueArgumentsToParameters(
            @NotNull Call call,
            @NotNull TracingStrategy tracing,
            @NotNull ResolvedCallImpl<D> candidateCall,
            @NotNull Set<ValueArgument> unmappedArguments
    ) {
        TemporaryBindingTrace temporaryTrace = candidateCall.getTrace();
        Map<ValueParameterDescriptor, VarargValueArgument> varargs = Maps.newHashMap();
        Set<ValueParameterDescriptor> usedParameters = Sets.newHashSet();

        D candidate = candidateCall.getCandidateDescriptor();

        List<ValueParameterDescriptor> valueParameters = candidate.getValueParameters();

        Map<Name, ValueParameterDescriptor> parameterByName = Maps.newHashMap();
        for (ValueParameterDescriptor valueParameter : valueParameters) {
            parameterByName.put(valueParameter.getName(), valueParameter);
        }

        List<? extends ValueArgument> valueArguments = call.getValueArguments();

        Status status = OK;
        boolean someNamed = false;
        boolean somePositioned = false;
        for (int i = 0; i < valueArguments.size(); i++) {
            ValueArgument valueArgument = valueArguments.get(i);
            if (valueArgument.isNamed()) {
                someNamed = true;
                JetSimpleNameExpression nameReference = valueArgument.getArgumentName().getReferenceExpression();
                ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(nameReference.getReferencedNameAsName());
                if (valueParameterDescriptor == null) {
                    temporaryTrace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference));
                    unmappedArguments.add(valueArgument);
                    status = WEAK_ERROR;
                }
                else {
                    temporaryTrace.record(REFERENCE_TARGET, nameReference, valueParameterDescriptor);
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        temporaryTrace.report(ARGUMENT_PASSED_TWICE.on(nameReference));
                        unmappedArguments.add(valueArgument);
                        status = WEAK_ERROR;
                    }
                    else {
                        status = status.compose(put(candidateCall, valueParameterDescriptor, valueArgument, varargs));
                    }
                }
                if (somePositioned) {
                    temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(nameReference));
                    status = WEAK_ERROR;
                }
            }
            else {
                somePositioned = true;
                if (someNamed) {
                    temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(valueArgument.asElement()));
                    status = WEAK_ERROR;
                }
                else {
                    int parameterCount = valueParameters.size();
                    if (i < parameterCount) {
                        ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(i);
                        usedParameters.add(valueParameterDescriptor);
                        status = status.compose(put(candidateCall, valueParameterDescriptor, valueArgument, varargs));
                    }
                    else if (!valueParameters.isEmpty()) {
                        ValueParameterDescriptor valueParameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                        if (valueParameterDescriptor.getVarargElementType() != null) {
                            status = status.compose(put(candidateCall, valueParameterDescriptor, valueArgument, varargs));
                            usedParameters.add(valueParameterDescriptor);
                        }
                        else {
                            temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
                            unmappedArguments.add(valueArgument);
                            status = WEAK_ERROR;
                        }
                    }
                    else {
                        temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
                        unmappedArguments.add(valueArgument);
                        status = ERROR;
                    }
                }
            }
        }

        List<JetExpression> functionLiteralArguments = call.getFunctionLiteralArguments();
        if (!functionLiteralArguments.isEmpty()) {
            JetExpression possiblyLabeledFunctionLiteral = functionLiteralArguments.get(0);

            if (valueParameters.isEmpty()) {
                temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                status = ERROR;
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
                    temporaryTrace.report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
                    status = ERROR;
                }
                else {
                    if (!usedParameters.add(valueParameterDescriptor)) {
                        temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                        status = WEAK_ERROR;
                    }
                    else {
                        status = status.compose(put(candidateCall, valueParameterDescriptor, CallMaker.makeValueArgument(functionLiteral), varargs));
                    }
                }
            }

            for (int i = 1; i < functionLiteralArguments.size(); i++) {
                JetExpression argument = functionLiteralArguments.get(i);
                temporaryTrace.report(MANY_FUNCTION_LITERAL_ARGUMENTS.on(argument));
                status = WEAK_ERROR;
            }
        }


        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!usedParameters.contains(valueParameter)) {
                if (valueParameter.hasDefaultValue()) {
                    candidateCall.recordValueArgument(valueParameter, DefaultValueArgument.DEFAULT);
                }
                else if (valueParameter.getVarargElementType() != null) {
                    candidateCall.recordValueArgument(valueParameter, new VarargValueArgument());
                }
                else {
                    // tracing.reportWrongValueArguments(temporaryTrace, "No value passed for parameter " + valueParameter.getName());
                    tracing.noValueForParameter(temporaryTrace, valueParameter);
                    status = ERROR;
                }
            }
        }

        ReceiverDescriptor receiverParameter = candidate.getReceiverParameter();
        ReceiverDescriptor receiverArgument = candidateCall.getReceiverArgument();
        if (receiverParameter.exists() &&!receiverArgument.exists()) {
            tracing.missingReceiver(temporaryTrace, receiverParameter);
            status = ERROR;
        }
        if (!receiverParameter.exists() && receiverArgument.exists()) {
            tracing.noReceiverAllowed(temporaryTrace);
            status = ERROR;
        }

        assert (candidateCall.getThisObject().exists() == candidateCall.getResultingDescriptor().getExpectedThisObject().exists()) : "Shouldn't happen because of TaskPrioritizer: " + candidateCall.getCandidateDescriptor();

        return status;
    }

    private static <D extends CallableDescriptor> Status put(ResolvedCallImpl<D> candidateCall, ValueParameterDescriptor valueParameterDescriptor, ValueArgument valueArgument, Map<ValueParameterDescriptor, VarargValueArgument> varargs) {
        Status error = OK;
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
                error = WEAK_ERROR;
            }
            ResolvedValueArgument argument = new ExpressionValueArgument(valueArgument);
            candidateCall.recordValueArgument(valueParameterDescriptor, argument);
        }
        return error;
    }
}
