package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.types.CallMaker;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
/*package*/ class ValueArgumentsToParametersMapper {
    public static <Descriptor extends CallableDescriptor> boolean mapValueArgumentsToParameters(
            @NotNull ResolutionTask<Descriptor> task,
            @NotNull TracingStrategy tracing,
            @NotNull Descriptor candidate,
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

        boolean error = false;
        boolean someNamed = false;
        boolean somePositioned = false;
        for (int i = 0; i < valueArguments.size(); i++) {
            ValueArgument valueArgument = valueArguments.get(i);
            if (valueArgument.isNamed()) {
                someNamed = true;
                JetReferenceExpression nameReference = valueArgument.getArgumentName().getReferenceExpression();
                ValueParameterDescriptor valueParameterDescriptor = parameterByName.get(valueArgument.getArgumentName().getReferenceExpression().getReferencedName());
                if (valueParameterDescriptor == null) {
//                        temporaryTrace.getErrorHandler().genericError(nameNode, "Cannot find a parameter with this name");
                    temporaryTrace.report(NAMED_PARAMETER_NOT_FOUND.on(nameReference));
                    error = true;
                }
                else {
                    if (!usedParameters.add(valueParameterDescriptor)) {
//                        temporaryTrace.getErrorHandler().genericError(nameNode, "An argument is already passed for this parameter");
                        temporaryTrace.report(ARGUMENT_PASSED_TWICE.on(nameReference));
                    }
                    temporaryTrace.record(REFERENCE_TARGET, nameReference, valueParameterDescriptor);
                    argumentsToParameters.put(valueArgument, valueParameterDescriptor);
                }
                if (somePositioned) {
//                    temporaryTrace.getErrorHandler().genericError(nameNode, "Mixing named and positioned arguments in not allowed");
                    temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(nameReference));
                    error = true;
                }
            }
            else {
                somePositioned = true;
                if (someNamed) {
//                    temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), "Mixing named and positioned arguments in not allowed");
                    temporaryTrace.report(MIXING_NAMED_AND_POSITIONED_ARGUMENTS.on(valueArgument.asElement()));
                    error = true;
                }
                else {
                    int parameterCount = valueParameters.size();
                    if (i < parameterCount) {
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
//                            temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), getTooManyArgumentsMessage(candidate));
                            temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
                            error = true;
                        }
                    }
                    else {
//                        temporaryTrace.getErrorHandler().genericError(valueArgument.asElement().getNode(), getTooManyArgumentsMessage(candidate));
                        temporaryTrace.report(TOO_MANY_ARGUMENTS.on(valueArgument.asElement(), candidate));
                        error = true;
                    }
                }
            }
        }

        List<JetExpression> functionLiteralArguments = task.getFunctionLiteralArguments();
        if (!functionLiteralArguments.isEmpty()) {
            JetExpression possiblyLabeledFunctionLiteral = functionLiteralArguments.get(0);

            if (valueParameters.isEmpty()) {
//                temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), getTooManyArgumentsMessage(candidate));
                temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                error = true;
            } else {
                JetFunctionLiteralExpression functionLiteral;
                if (possiblyLabeledFunctionLiteral instanceof JetLabelQualifiedExpression) {
                    JetLabelQualifiedExpression labeledFunctionLiteral = (JetLabelQualifiedExpression) possiblyLabeledFunctionLiteral;
                    functionLiteral = (JetFunctionLiteralExpression) labeledFunctionLiteral.getLabeledExpression();
                }
                else {
                    functionLiteral = (JetFunctionLiteralExpression) possiblyLabeledFunctionLiteral;
                }

                ValueParameterDescriptor parameterDescriptor = valueParameters.get(valueParameters.size() - 1);
                if (parameterDescriptor.isVararg()) {
//                    temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), "Passing value as a vararg is only allowed inside a parenthesized argument list");
                    temporaryTrace.report(VARARG_OUTSIDE_PARENTHESES.on(possiblyLabeledFunctionLiteral));
                    error = true;
                }
                else {
                    if (!usedParameters.add(parameterDescriptor)) {
//                        temporaryTrace.getErrorHandler().genericError(possiblyLabeledFunctionLiteral.getNode(), getTooManyArgumentsMessage(candidate));
                        temporaryTrace.report(TOO_MANY_ARGUMENTS.on(possiblyLabeledFunctionLiteral, candidate));
                        error = true;
                    }
                    else {
                        argumentsToParameters.put(CallMaker.makeValueArgument(functionLiteral), parameterDescriptor);
                    }
                }
            }

            for (int i = 1; i < functionLiteralArguments.size(); i++) {
                JetExpression argument = functionLiteralArguments.get(i);
//                temporaryTrace.getErrorHandler().genericError(argument.getNode(), "Only one function literal is allowed outside a parenthesized argument list");
                temporaryTrace.report(MANY_FUNCTION_LITERAL_ARGUMENTS.on(argument));
                error = true;
            }
        }


        for (ValueParameterDescriptor valueParameter : valueParameters) {
            if (!usedParameters.contains(valueParameter)) {
                if (!valueParameter.hasDefaultValue()) {
//                    tracing.reportWrongValueArguments(temporaryTrace, "No value passed for parameter " + valueParameter.getName());
                    tracing.noValueForParameter(temporaryTrace, valueParameter);
                    error = true;
                }
            }
        }
        return error;
    }

//    private static <Descriptor extends CallableDescriptor> String getTooManyArgumentsMessage(Descriptor candidate) {
//        return "Too many arguments for " + DescriptorRenderer.TEXT.render(candidate);
//    }
}
