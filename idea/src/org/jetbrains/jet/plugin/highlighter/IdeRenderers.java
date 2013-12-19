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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetValueArgument;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.Renderer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.*;

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

    public static final Renderer<Collection<? extends ResolvedCall<?>>> HTML_AMBIGUOUS_CALLS =
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

    public static final Renderer<Collection<? extends ResolvedCall<?>>> HTML_NONE_APPLICABLE_CALLS =
            new Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>>() {
                @Nullable
                private ValueParameterDescriptor findParameterByArgumentExpression(
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

                private Set<ValueParameterDescriptor> getParametersToHighlight(ResolvedCall<? extends CallableDescriptor> call) {
                    Set<ValueParameterDescriptor> parameters = new HashSet<ValueParameterDescriptor>();
                    if (call instanceof ResolvedCallImpl) {
                        Iterable<Diagnostic> diagnostics = ((ResolvedCallImpl)call).getTrace().getBindingContext().getDiagnostics();
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
                        ReceiverParameterDescriptor receiverParameter = funDescriptor.getReceiverParameter();
                        if (receiverParameter != null) {
                            stringBuilder.append(htmlRenderer.renderType(receiverParameter.getType())).append(".");
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
                        stringBuilder.append(" <i>defined in</i> ");
                        DeclarationDescriptor containingDeclaration = funDescriptor.getContainingDeclaration();
                        FqNameUnsafe fqName = DescriptorUtils.getFqName(containingDeclaration);
                        stringBuilder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : fqName.asString());
                        stringBuilder.append("</li>");
                    }
                    return stringBuilder.toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderConflictingSubstitutionsInferenceError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderTypeConstructorMismatchError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderNoInformationForParameterError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<InferenceErrorData> HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER =
            new Renderer<InferenceErrorData>() {
                @NotNull
                @Override
                public String render(@NotNull InferenceErrorData inferenceErrorData) {
                    return renderUpperBoundViolatedInferenceError(inferenceErrorData, HtmlTabledDescriptorRenderer.create()).toString();
                }
            };

    public static final Renderer<CallableMemberDescriptor> HTML_RENDER_RETURN_TYPE = new Renderer<CallableMemberDescriptor>() {
        @NotNull
        @Override
        public String render(@NotNull CallableMemberDescriptor object) {
            JetType returnType = object.getReturnType();
            assert returnType != null;
            return DescriptorRenderer.HTML.renderType(returnType);
        }
    };
}
