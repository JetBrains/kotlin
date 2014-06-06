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

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticFactoryToRendererMap;
import org.jetbrains.jet.lang.diagnostics.rendering.DiagnosticRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.DispatchingDiagnosticRenderer;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.RENDER_CLASS_OR_OBJECT;
import static org.jetbrains.jet.lang.diagnostics.rendering.Renderers.STRING;
import static org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TextElementType;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.ErrorsJvm.ACCIDENTAL_OVERRIDE;
import static org.jetbrains.jet.lang.resolve.java.diagnostics.ErrorsJvm.CONFLICTING_JVM_DECLARATIONS;
import static org.jetbrains.jet.plugin.highlighter.HtmlTabledDescriptorRenderer.tableForTypes;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.*;


/**
 * @see DefaultErrorMessages
 */
public class IdeErrorMessages {
    public static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap();
    public static final DiagnosticRenderer<Diagnostic> RENDERER = new DispatchingDiagnosticRenderer(ContainerUtil.concat(false, DefaultErrorMessages.MAPS, MAP));

    static {
        MAP.put(TYPE_MISMATCH, "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table></html>",
                HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "<html>Assignment operators ambiguity. All these functions match.<ul>{0}</ul></table></html>",
                HTML_AMBIGUOUS_CALLS);

        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
        MAP.put(TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH_RENDERER);
        MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, tableForTypes("Type inference failed. Expected type mismatch: ",
                                                                     "required: ", TextElementType.STRONG,
                                                                     "found: ", TextElementType.ERROR), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
        MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "<html>{0}</html>", HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);

        MAP.put(WRONG_SETTER_PARAMETER_TYPE, "<html>Setter parameter type must be equal to the type of the property." +
                                             "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                             "<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);
        MAP.put(WRONG_GETTER_RETURN_TYPE, "<html>Getter return type must be equal to the type of the property." +
                                          "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                          "<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(ITERATOR_AMBIGUITY, "<html>Method ''iterator()'' is ambiguous for this expression.<ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);

        MAP.put(UPPER_BOUND_VIOLATED, "<html>Type argument is not within its bounds." +
                                      "<table><tr><td>Expected:</td><td>{0}</td></tr>" +
                                      "<tr><td>Found:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, "<html>Loop parameter type mismatch." +
                                           "<table><tr><td>Iterated values:</td><td>{0}</td></tr>" +
                                           "<tr><td>Parameter:</td><td>{1}</td></tr></table></html>", HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE, "<html>Return type is ''{0}'', which is not a subtype of overridden<br/>" +
                                                  "{1}</html>", HTML_RENDER_RETURN_TYPE, DescriptorRenderer.HTML);

        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, "<html>Var-property type is ''{0}'', which is not a type of overridden<br/>" +
                                                  "{1}</html>", HTML_RENDER_RETURN_TYPE, DescriptorRenderer.HTML);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, "<html>Val-property cannot override var-property<br />" +
                                       "{1}</html>", DescriptorRenderer.HTML, DescriptorRenderer.HTML);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "<html>{0} must be declared abstract or implement abstract member<br/>" +
                                                 "{1}</html>", RENDER_CLASS_OR_OBJECT,
                DescriptorRenderer.HTML);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "<html>{0} must override {1}<br />because it inherits many implementations of it</html>",
                RENDER_CLASS_OR_OBJECT, DescriptorRenderer.HTML);
        MAP.put(CONFLICTING_OVERLOADS, "<html>''{0}''<br />is already defined in {1}</html>",
                DescriptorRenderer.HTML_COMPACT_WITH_MODIFIERS, STRING);

        MAP.put(RESULT_TYPE_MISMATCH, "<html>Function return type mismatch." +
                                      "<table><tr><td>Expected:</td><td>{1}</td></tr>" +
                                      "<tr><td>Found:</td><td>{2}</td></tr></table></html>", STRING, HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY, "<html>Overload resolution ambiguity. All these functions match. <ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);
        MAP.put(NONE_APPLICABLE, "<html>None of the following functions can be called with the arguments supplied. <ul>{0}</ul></html>",
                HTML_NONE_APPLICABLE_CALLS);
        MAP.put(CANNOT_COMPLETE_RESOLVE, "<html>Cannot choose among the following candidates without completing type inference: <ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);
        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER, "<html>Unresolved reference. <br/> None of the following candidates is applicable because of receiver type mismatch: <ul>{0}</ul></html>", HTML_AMBIGUOUS_CALLS);

        MAP.put(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY, "<html>Overload resolution ambiguity on method ''{0}''. All these functions match. <ul>{1}</ul></html>", STRING, HTML_AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, "<html>Property delegate must have a ''{0}'' method. None of the following functions is suitable. <ul>{1}</ul></html>",
                STRING, HTML_NONE_APPLICABLE_CALLS);

        MAP.put(CONFLICTING_JVM_DECLARATIONS, "<html>Platform declaration clash: {0}</html>", HTML_CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ACCIDENTAL_OVERRIDE, "<html>Accidental override: {0}</html>", HTML_CONFLICTING_JVM_DECLARATIONS_DATA);

        MAP.setImmutable();
    }

    private IdeErrorMessages() {
    }
}
