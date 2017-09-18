/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.TypeMismatchDueToTypeProjectionsData;
import org.jetbrains.kotlin.diagnostics.rendering.*;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallDataHtmlRenderer;

import java.net.URL;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.rendering.Renderers.*;
import static org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TextElementType;
import static org.jetbrains.kotlin.idea.highlighter.HtmlTabledDescriptorRenderer.tableForTypes;
import static org.jetbrains.kotlin.idea.highlighter.IdeRenderers.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.ACCIDENTAL_OVERRIDE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.CONFLICTING_JVM_DECLARATIONS;


/**
 * @see DefaultErrorMessages
 */
public class IdeErrorMessages {
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("IDE");

    @NotNull
    public static String render(@NotNull Diagnostic diagnostic) {
        DiagnosticRenderer renderer = MAP.get(diagnostic.getFactory());

        if (renderer != null) {
            //noinspection unchecked
            return renderer.render(diagnostic);
        }

        return DefaultErrorMessages.render(diagnostic);
    }

    @TestOnly
    public static boolean hasIdeSpecificMessage(@NotNull Diagnostic diagnostic) {
        return MAP.get(diagnostic.getFactory()) != null;
    }

    static {
        MAP.put(TYPE_MISMATCH, "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table></html>",
                HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
                "<html>Type mismatch.<table><tr><td>Required:</td><td>{0}</td></tr><tr><td>Found:</td><td>{1}</td></tr></table><br />\n" +
                "Projected type {2} restricts use of <br />\n{3}\n</html>",
                new MultiRenderer<TypeMismatchDueToTypeProjectionsData>() {
                    @NotNull
                    @Override
                    public String[] render(@NotNull TypeMismatchDueToTypeProjectionsData object) {
                        RenderingContext context = RenderingContext
                                .of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(), object.getCallableDescriptor());
                        return new String[] {
                                HTML_RENDER_TYPE.render(object.getExpectedType(), context),
                                HTML_RENDER_TYPE.render(object.getExpressionType(), context),
                                HTML_RENDER_TYPE.render(object.getReceiverType(), context),
                                HTML.render(object.getCallableDescriptor(), context)
                        };
                    }
                });

        MAP.put(ASSIGN_OPERATOR_AMBIGUITY, "<html>Assignment operators ambiguity. All these functions match.<ul>{0}</ul></table></html>",
                HTML_AMBIGUOUS_CALLS);

        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
        MAP.put(TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR, "<html>Type inference failed: {0}</html>", HTML_TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER);
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
                                                  "{1}</html>", HTML_RENDER_RETURN_TYPE, HTML);
        MAP.put(RETURN_TYPE_MISMATCH_ON_INHERITANCE, "<html>Return types of inherited members are incompatible:<br/>{0},<br/>{1}</html>",
                HTML, HTML);

        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE, "<html>Property type is ''{0}'', which is not a subtype type of overridden<br/>" +
                                                  "{1}</html>", HTML_RENDER_RETURN_TYPE, HTML);
        MAP.put(VAR_TYPE_MISMATCH_ON_OVERRIDE, "<html>Var-property type is ''{0}'', which is not a type of overridden<br/>" +
                                                    "{1}</html>", HTML_RENDER_RETURN_TYPE, HTML);
        MAP.put(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE, "<html>Types of inherited properties are incompatible:<br/>{0},<br/>{1}</html>",
                HTML, HTML);
        MAP.put(VAR_TYPE_MISMATCH_ON_INHERITANCE, "<html>Types of inherited var-properties do not match:<br/>{0},<br/>{1}</html>",
                HTML, HTML);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, "<html>Val-property cannot override var-property<br />" +
                                       "{1}</html>", HTML, HTML);
        MAP.put(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION, "<html>Val-property cannot override var-property<br />" +
                                       "{1}</html>", HTML, HTML);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED, "<html>{0} is not abstract and does not implement abstract member<br/>" +
                                                 "{1}</html>", RENDER_CLASS_OR_OBJECT, HTML);
        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED, "<html>{0} is not abstract and does not implement abstract base class member<br/>" +
                                                       "{1}</html>", RENDER_CLASS_OR_OBJECT, HTML);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED, "<html>{0} must override {1}<br />because it inherits many implementations of it</html>",
                RENDER_CLASS_OR_OBJECT, HTML);

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
        MAP.put(DELEGATE_PD_METHOD_NONE_APPLICABLE, "<html>''{0}'' method may be missing. None of the following functions will be called: <ul>{1}</ul></html>",
                STRING, HTML_NONE_APPLICABLE_CALLS);

        MAP.put(CONFLICTING_JVM_DECLARATIONS, "<html>Platform declaration clash: {0}</html>", HTML_CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ACCIDENTAL_OVERRIDE, "<html>Accidental override: {0}</html>", HTML_CONFLICTING_JVM_DECLARATIONS_DATA);

        URL errorIconUrl = AllIcons.class.getResource("/general/error.png");
        MAP.put(EXCEPTION_FROM_ANALYZER, "<html>Internal Error occurred while analyzing this expression <br/>" +
                                         "<table cellspacing=\"0\" cellpadding=\"0\">" +
                                         "<tr>" +
                                         "<td>(<strong>Please use the \"</strong></td>" +
                                         "<td><img src=\"" + errorIconUrl + "\"/></td>" +
                                         "<td><strong>\" icon in the bottom-right corner to report this error</strong>):</td>" +
                                         "</tr>" +
                                         "</table>" +
                                         "<br/>" +
                                         "<pre>{0}</pre>" +
                                         "</html>",
                HTML_THROWABLE);

        MAP.put(ErrorsJs.JSCODE_ERROR, "<html>JavaScript: {0}</html>", JsCallDataHtmlRenderer.INSTANCE);
        MAP.put(ErrorsJs.JSCODE_WARNING, "<html>JavaScript: {0}</html>", JsCallDataHtmlRenderer.INSTANCE);
        MAP.put(UNSUPPORTED_FEATURE, "<html>{0}</html>", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.UNSUPPORTED, true));
        MAP.put(EXPERIMENTAL_FEATURE_WARNING, "<html>{0}</html>", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.WARNING, true));
        MAP.put(EXPERIMENTAL_FEATURE_ERROR, "<html>{0}</html>", new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.ERROR, true));

        MAP.put(NO_ACTUAL_FOR_EXPECT, "<html>Expected {0} has no actual in module{1}{2}</html>", DECLARATION_NAME_WITH_KIND,
                PLATFORM, new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));
        MAP.put(ACTUAL_WITHOUT_EXPECT, "<html>Actual {0} has no corresponding expected declaration{1}</html>", DECLARATION_NAME_WITH_KIND,
                new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));

        MAP.put(NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS, "<html>Actual class ''{0}'' has no corresponding members for expected class members:{1}</html>",
                NAME, new IncompatibleExpectedActualClassScopesRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));

        MAP.setImmutable();
    }

    private IdeErrorMessages() {
    }
}
