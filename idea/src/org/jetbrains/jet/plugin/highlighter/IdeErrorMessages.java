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

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.*;
import org.jetbrains.jet.lang.psi.JetValueArgument;
import org.jetbrains.jet.lang.psi.JetValueArgumentList;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCallImpl;
import org.jetbrains.jet.lang.resolve.calls.ResolvedValueArgument;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.*;

/**
 * @see DefaultErrorMessages
 *
 * @author Evgeny Gerashchenko
 * @since 4/13/12
 */
public class IdeErrorMessages {
    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    public static final DiagnosticRenderer<Diagnostic> RENDERER = new DispatchingDiagnosticRenderer(MAP, DefaultErrorMessages.MAP);

    private static final Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> HTML_AMBIGUOUS_CALLS =
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

    static {
        MAP.put(TYPE_MISMATCH, "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table></html>", RENDER_TYPE, RENDER_TYPE);

        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "<html>Assignment operators ambiguity. All these functions match.<ul>{0}</ul></table></html>",
                HTML_AMBIGUOUS_CALLS);

        MAP.put(WRONG_SETTER_PARAMETER_TYPE, "<html>Setter parameter type must be equal to the type of the property." +
                                             "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                             "<tr><td>Found:</td><td>{1}</td></tr></table></html>", RENDER_TYPE, RENDER_TYPE);
        MAP.put(WRONG_GETTER_RETURN_TYPE, "<html>Getter return type must be equal to the type of the property." +
                                          "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                          "<tr><td>Found:</td><td>{1}</td></tr></table></html>", RENDER_TYPE, RENDER_TYPE);

        MAP.put(ITERATOR_AMBIGUITY, "<html>Method ''iterator()'' is ambiguous for this expression.<ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);

        MAP.put(UPPER_BOUND_VIOLATED, "<html>Type argument is not within its bounds." +
                                      "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                      "<tr><td>Found:</td><td>{1}</td></tr></table></html>", RENDER_TYPE, RENDER_TYPE);

        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "<html>Loop parameter type mismatch." +
                                           "<table><tr><td>Iterated values:</td><td>{0}</td></tr>" +
                                           "<tr><td>Parameter:</td><td>{1}</td></tr></table></html>", RENDER_TYPE, RENDER_TYPE);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "<html>Return type is ''{0}'', which is not a subtype of overridden<br/>" +
                                                  "{1}</html>",
                new Renderer<CallableMemberDescriptor>() {
                    @NotNull
                    @Override
                    public String render(@NotNull CallableMemberDescriptor object) {
                        return DescriptorRenderer.TEXT.renderType(object.getReturnType());
                    }
                }, DescriptorRenderer.HTML);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, "<html>Val-property cannot override var-property<br />" +
                                       "{1}</html>", DescriptorRenderer.HTML, DescriptorRenderer.HTML);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "<html>{0} must be declared abstract or implement abstract member<br/>" +
                                                 "{1}</html>", RENDER_CLASS_OR_OBJECT,
                DescriptorRenderer.HTML);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "<html>{0} must override {1}<br />because it inherits many implementations of it</html>",
                RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);
        MAP.put(CONFLICTING_OVERLOADS, "<html>{1}<br />is already defined in ''{0}''</html>", DescriptorRenderer.HTML, TO_STRING);

        MAP.put(RESULT_TYPE_MISMATCH, "<html>Function return type mismatch." +
                                      "<table><tr><td>Expected:</td><td>{1}</td></tr>" +
                                      "<tr><td>Found:</td><td>{2}</td></tr></table></html>", TO_STRING, RENDER_TYPE, RENDER_TYPE);

        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "<html>Overload resolution ambiguity. All these functions match. <ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);
        MAP.put(NONE_APPLICABLE, "<html>None of the following functions can be called with the arguments supplied. <ul>{0}</ul></html>",
                new NoneApplicableCallsRenderer());

        MAP.setImmutable();
    }

    private IdeErrorMessages() {
    }

    private static class NoneApplicableCallsRenderer implements Renderer<Collection<? extends ResolvedCall<? extends CallableDescriptor>>> {
        @Nullable
        private static ValueParameterDescriptor findParameterByArgumentExpression(
                ResolvedCall<? extends CallableDescriptor> call,
                JetValueArgument argument) {
            for (Map.Entry<ValueParameterDescriptor, ResolvedValueArgument> entry : call.getValueArguments()
                    .entrySet()) {
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
                    //stringBuilder.append(DefaultErrorMessages.RENDERER.render(diagnostic));
                    PsiElement element = diagnostic.getPsiElement();
                    JetValueArgumentList argumentList = PsiTreeUtil.getParentOfType(element, JetValueArgumentList.class, false);
                    assert argumentList != null;
                    JetValueArgument argument = PsiTreeUtil.getParentOfType(element, JetValueArgument.class, false);
                    if (diagnostic.getFactory() == Errors.TOO_MANY_ARGUMENTS) {
                        parameters.add(null);
                    } else if (diagnostic.getFactory() == Errors.NO_VALUE_FOR_PARAMETER) {
                        ValueParameterDescriptor parameter =
                                ((DiagnosticWithParameters1<PsiElement, ValueParameterDescriptor>)diagnostic).getA();
                        parameters.add(parameter);
                    } else {
                        if (argument != null) {
                            assert argument.getParent() == argumentList; // TODO check that this really can't happen
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
            String RED_TEMPLATE = "<font color=red><b>%s</b></font>";

            StringBuilder stringBuilder = new StringBuilder("");
            for (ResolvedCall<? extends CallableDescriptor> call : calls) {
                stringBuilder.append("<li>");
                CallableDescriptor funDescriptor = call.getResultingDescriptor();
                Set<ValueParameterDescriptor> parametersToHighlight = getParametersToHighlight(call);

                stringBuilder.append(funDescriptor.getName()).append("(");
                boolean first = true;
                DescriptorRenderer htmlRend = DescriptorRenderer.HTML;
                for (ValueParameterDescriptor parameter : funDescriptor.getValueParameters()) {
                    if (!first) {
                        stringBuilder.append(", ");
                    }
                    JetType type = parameter.getType();
                    JetType varargElementType = parameter.getVarargElementType();
                    if (varargElementType != null) {
                        type = varargElementType;
                    }
                    String paramString = (varargElementType != null ? "<b>vararg</b> " : "") + htmlRend.renderType(type);
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
                stringBuilder.append(" ").append(htmlRend.renderMessage("defined in")).append(" ");
                DeclarationDescriptor containingDeclaration = funDescriptor.getContainingDeclaration();
                if (containingDeclaration != null) {
                    FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
                    stringBuilder.append(FqName.ROOT.toUnsafe().equals(fqName) ? "root package" : fqName.getFqName());
                }
                stringBuilder.append("</li>");
            }
            return stringBuilder.toString();
        }
    }
}
