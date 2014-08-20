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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.calls.inference.InferenceErrorData;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.diagnostics.ConflictingJvmDeclarationsData;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.Renderer;

import java.util.Collection;

import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.*;
import static org.jetbrains.jet.plugin.highlighter.renderersUtil.RenderersUtilPackage.renderResolvedCall;

public class IdeRenderers {
    private static final String RED_TEMPLATE = "<font color=red><b>%s</b></font>";
    private static final String STRONG_TEMPLATE = "<b>%s</b>";

    @NotNull
    public static String strong(Object o) {
        return String.format(STRONG_TEMPLATE, o);
    }

    @NotNull
    public static String error(Object o) {
        return String.format(RED_TEMPLATE, o);
    }

    @NotNull
    public static String strong(Object o, boolean error) {
        return String.format(error ? RED_TEMPLATE : STRONG_TEMPLATE, o);
    }

    public static final Renderer<Collection<? extends ResolvedCall<?>>> HTML_AMBIGUOUS_CALLS =
            new Renderer<Collection<? extends ResolvedCall<?>>>() {
                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<?>> calls) {
                    StringBuilder stringBuilder = new StringBuilder("");
                    for (ResolvedCall<?> call : calls) {
                        stringBuilder.append("<li>");
                        stringBuilder.append(DescriptorRenderer.HTML.render(call.getResultingDescriptor()));
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
            new Renderer<Collection<? extends ResolvedCall<?>>>() {

                @NotNull
                @Override
                public String render(@NotNull Collection<? extends ResolvedCall<?>> calls) {
                    StringBuilder stringBuilder = new StringBuilder("");
                    for (ResolvedCall<?> resolvedCall : calls) {
                        stringBuilder.append("<li>");
                        stringBuilder.append(renderResolvedCall(resolvedCall));
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

    public static final Renderer<ConflictingJvmDeclarationsData> HTML_CONFLICTING_JVM_DECLARATIONS_DATA = new Renderer<ConflictingJvmDeclarationsData>() {
        @NotNull
        @Override
        public String render(@NotNull ConflictingJvmDeclarationsData data) {
            StringBuilder sb = new StringBuilder("<ul>");
            for (JvmDeclarationOrigin origin : data.getSignatureOrigins()) {
                DeclarationDescriptor descriptor = origin.getDescriptor();
                if (descriptor != null) {
                    sb.append("<li>").append(DescriptorRenderer.HTML_COMPACT_WITH_MODIFIERS.render(descriptor)).append("</li>\n");
                }
            }
            sb.append("</ul>");
            return ("The following declarations have the same JVM signature (<code>" + data.getSignature().getName() + data.getSignature().getDesc() + "</code>):<br/>\n" + sb).trim();
        }
    };
}
