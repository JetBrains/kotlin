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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.Renderer;
import org.jetbrains.jet.lang.psi.JetValueArgument;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.inference.TypeBounds;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.*;

/**
 * @author svtk
 */
public class IdeRenderers {
    private static final String RED_TEMPLATE = "<font color=red><b>%s</b></font>";
    private static final String STRONG_TEMPLATE = "<b>%s</b>";

    public static String strong(Object o) {
        return String.format(STRONG_TEMPLATE, o);
    }

    public static String error(Object o) {
        return String.format(RED_TEMPLATE, o);
    }

    public static String strong(Object o, boolean error) {
        return String.format(error ? RED_TEMPLATE : STRONG_TEMPLATE, o);
    }

    public static final Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> HTML_AMBIGUOUS_CALLS =
            new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> calls) {
                    StringBuilder stringBuilder = new StringBuilder("");
                    for (ResolvedCall<? extends CallableDescriptor> call : calls) {
                        stringBuilder.append("<li>");
                        stringBuilder.append(DescriptorRenderer.HTML.render(call.getResultingDescriptor())).append("\n");
                        stringBuilder.append("</li>");
                    }
                    return stringBuilder.toString();
                }
            };

    public static final Renderer<JetType> HTML_RENDER_TYPE = new Renderer<JetType>() {
        @NotNull
        @Override
        public String render(@NotNull JetType type) {
            return DescriptorRenderer.HTML.renderType(type);
        }
    };


    public static class NoneApplicableCallsRenderer implements Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> {
        @Nullable
        private static ValueParameterDescriptor findParameterByArgumentExpression(
                ResolvedCall<? extends CallableDescriptor> call,
                JetValueArgument argument) {
            for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments().entrySet()) {
                for (ValueArgument va : entry.getValue().getArguments()) {
                    if (va == argument) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }

        private static Set<ValueParameterDescriptor> getParametersToHighlight(ResolvedCall<? extends CallableDescriptor> call) {
            Set<ValueParameterDescriptor> parameters = new HashSet<ValueParameterDescriptor>();
            if (call instanceof ResolvedCallImpl) {
                Collection<Diagnostic> diagnostics = ((ResolvedCallImpl)call).getTrace().getBindingContext().getDiagnostics();
                for (Diagnostic diagnostic : diagnostics) {
                    if (diagnostic.getFactory() == Errors.TOO_MANY_ARGUMENTS) {
                        parameters.add(null);
                    } else if (diagnostic.getFactory() == Errors.NO_VALUE_FOR_PARAMETER) {
                        ValueParameterDescriptor parameter =
                                ((DiagnosticWithParameters1<PsiElement, ValueParameterDescriptor>)diagnostic).getA();
                        parameters.add(parameter);
                    } else {
                        JetValueArgument argument = PsiTreeUtil.getParentOfType(diagnostic.getPsiElement(), JetValueArgument.class, false);
                        if (argument != null) {
                            ValueParameterDescriptor parameter = findParameterByArgumentExpression(call, argument);
                            if (parameter != null) {
                                parameters.add(parameter);
                            }
                        }
                    }
                }
            }
            return parameters;
        }

        @NotNull
        @Override
        public String render(@NotNull Collection<? extends ResolvedCall<? extends CallableDescriptor>> calls) {
            StringBuilder stringBuilder = new StringBuilder("");
            for (ResolvedCall<? extends CallableDescriptor> call : calls) {
                stringBuilder.append("<li>");
                CallableDescriptor funDescriptor = call.getResultingDescriptor();
                Set<ValueParameterDescriptor> parametersToHighlight = getParametersToHighlight(call);

                DescriptorRenderer htmlRenderer = DescriptorRenderer.HTML;
                if (funDescriptor.getReceiverParameter() != ReceiverDescriptor.NO_RECEIVER) {
                    stringBuilder.append(htmlRenderer.renderType(funDescriptor.getReceiverParameter().getType())).append(".");
                }
                stringBuilder.append(funDescriptor.getName()).append("(");
                boolean first = true;
                for (ValueParameterDescriptor parameter : funDescriptor.getValueParameters()) {
                    if (!first) {
                        stringBuilder.append(", ");
                    }
                    JetType type = parameter.getType();
                    JetType varargElementType = parameter.getVarargElementType();
                    if (varargElementType != null) {
                        type = varargElementType;
                    }
                    String paramString = (varargElementType != null ? "<b>vararg</b> " : "") + htmlRenderer.renderType(type);
                    if (parameter.hasDefaultValue()) {
                        paramString += " = ...";
                    }
                    if (parametersToHighlight.contains(parameter)) {
                        paramString = String.format(RED_TEMPLATE, paramString);
                    }
                    stringBuilder.append(paramString);

                    first = false;
                }
                stringBuilder.append(parametersToHighlight.contains(null) ? String.format(RED_TEMPLATE, ")") : ")");
                stringBuilder.append(" ").append(htmlRenderer.renderMessage("defined in")).append(" ");
                DeclarationDescriptor containingDeclaration = funDescriptor.getContainingDeclaration();
                if (containingDeclaration != null) {
                    FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
                    stringBuilder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : fqName.getFqName());
                }
                stringBuilder.append("</li>");
            }
            return stringBuilder.toString();
        }
    }

    public static final Renderer<InferenceErrorData> HTML_INFERENCE_ERROR_CONFLICTING_SUBSTITUTIONS =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    Collection<CallableDescriptor> substitutedDescriptors = Lists.newArrayList();
                    Collection<TypeSubstitutor> substitutors = inferenceErrorData.constraintSystem.getSubstitutors();
                    for (TypeSubstitutor substitutor : substitutors) {
                        CallableDescriptor substitutedDescriptor = inferenceErrorData.descriptor.substitute(substitutor);
                        substitutedDescriptors.add(substitutedDescriptor);
                    }

                    String type = strong(inferenceErrorData.constraintSystem.getFirstConflictingParameter().getName());
                    TablesRenderer table = TablesRenderer.create();
                    table.text("Cannot infer type parameter %s in", type)
                         .descriptor(inferenceErrorData.descriptor)
                         .text("None of the following substitutions");

                    for (CallableDescriptor substitutedDescriptor : substitutedDescriptors) {
                        JetType receiverType = substitutedDescriptor.getReceiverParameter().exists() ? substitutedDescriptor.getReceiverParameter().getType() : null;

                        Collection<ConstraintPosition> errorPositions = Sets.newHashSet();
                        List<JetType> valueArgumentTypes = Lists.newArrayList();
                        for (ValueParameterDescriptor valueParameterDescriptor : substitutedDescriptor.getValueParameters()) {
                            valueArgumentTypes.add(valueParameterDescriptor.getType());
                            JetType actualType = inferenceErrorData.valueArgumentsTypes.get(valueParameterDescriptor.getIndex());
                            if (!JetTypeChecker.INSTANCE.isSubtypeOf(actualType, valueParameterDescriptor.getType())) {
                                errorPositions.add(ConstraintPosition.valueParameterPosition(valueParameterDescriptor.getIndex()));
                            }
                        }

                        if (receiverType != null && inferenceErrorData.receiverArgumentType != null &&
                                !JetTypeChecker.INSTANCE.isSubtypeOf(inferenceErrorData.receiverArgumentType, receiverType)) {
                            errorPositions.add(ConstraintPosition.RECEIVER_POSITION);
                        }

                        table.functionArgumentTypeList(receiverType, valueArgumentTypes, errorPositions);
                    }

                    table.text("can be applied to")
                            .functionArgumentTypeList(inferenceErrorData.receiverArgumentType, inferenceErrorData.valueArgumentsTypes);
                    return table.toString();
                }
            };


    public static final Renderer<InferenceErrorData> HTML_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return TablesRenderer.create()
                            .descriptor(inferenceErrorData.descriptor)
                            .text("cannot be applied to")
                            .functionArgumentTypeList(inferenceErrorData.receiverArgumentType, inferenceErrorData.valueArgumentsTypes,
                                                      inferenceErrorData.constraintSystem.getErrorConstraintPositions())
                            .toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_INFERENCE_NO_INFORMATION_FOR_PARAMETER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    TypeParameterDescriptor firstUnknownParameter = null;
                    for (Map.Entry<TypeParameterDescriptor, TypeBounds> entry : inferenceErrorData.constraintSystem.getTypeBoundsMap().entrySet()) {
                        if (entry.getValue().isEmpty()) {
                            firstUnknownParameter = entry.getKey();
                            break;
                        }
                    }
                    assert firstUnknownParameter != null;
                    String firstUnknownParameterName = String.format(STRONG_TEMPLATE, firstUnknownParameter.getName());

                    return TablesRenderer.create()
                            .text("Not enough information to infer parameter %s in", firstUnknownParameterName)
                            .descriptor(inferenceErrorData.descriptor)
                            .text("Please specify it explicitly.")
                            .toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_INFERENCE_UPPER_BOUND_VIOLATED =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {

                    TypeParameterDescriptor typeParameterDescriptor = null;
                    for (TypeParameterDescriptor typeParameter : inferenceErrorData.descriptor.getTypeParameters()) {
                        if (!inferenceErrorData.constraintSystem.checkUpperBound(typeParameter)) {
                            typeParameterDescriptor = typeParameter;
                            break;
                        }
                    }
                    assert typeParameterDescriptor != null;

                    TablesRenderer table = TablesRenderer.create();
                    table.text("Type parameter bound for %s in", strong(typeParameterDescriptor.getName())).
                          descriptor(inferenceErrorData.descriptor);

                    JetType type = inferenceErrorData.constraintSystem.getValue(typeParameterDescriptor);
                    JetType upperBound = typeParameterDescriptor.getUpperBoundsAsType();
                    JetType substitute = inferenceErrorData.constraintSystem.getSubstitutor().substitute(upperBound, Variance.INVARIANT);

                    table.newElement()
                            .text("is not satisfied: inferred type %s is not a subtype of %s.", error(type), strong(substitute));
                    return table.toString();
                }
            };
}
