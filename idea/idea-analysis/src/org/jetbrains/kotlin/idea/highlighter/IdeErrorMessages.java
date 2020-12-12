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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic;
import org.jetbrains.kotlin.diagnostics.rendering.*;
import org.jetbrains.kotlin.idea.KotlinIdeaAnalysisBundle;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.js.resolve.diagnostics.JsCallDataHtmlRenderer;

import java.net.URL;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.diagnostics.rendering.Renderers.*;
import static org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TextElementType;
import static org.jetbrains.kotlin.idea.highlighter.HtmlTabledDescriptorRenderer.tableForTypes;
import static org.jetbrains.kotlin.idea.highlighter.IdeRenderers.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.*;


/**
 * @see DefaultErrorMessages
 */
public class IdeErrorMessages {
    private static final DiagnosticFactoryToRendererMap MAP = new DiagnosticFactoryToRendererMap("IDE");

    @NotNull
    public static String render(@NotNull UnboundDiagnostic diagnostic) {
        DiagnosticRenderer renderer = MAP.get(diagnostic.getFactory());

        if (renderer != null) {
            //noinspection unchecked
            return renderer.render(diagnostic);
        }

        return DefaultErrorMessages.render(diagnostic);
    }

    @TestOnly
    public static boolean hasIdeSpecificMessage(@NotNull UnboundDiagnostic diagnostic) {
        return MAP.get(diagnostic.getFactory()) != null;
    }

    static {
        MAP.put(TYPE_MISMATCH,
                KotlinIdeaAnalysisBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS,
                KotlinIdeaAnalysisBundle.htmlMessage("html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"),
                HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(TYPE_MISMATCH_DUE_TO_TYPE_PROJECTIONS,
                KotlinIdeaAnalysisBundle.htmlMessage(
                        "html.type.mismatch.table.tr.td.required.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.br.projected.type.2.restricts.use.of.br.3.html"),
                object -> {
                    RenderingContext context = RenderingContext
                            .of(object.getExpectedType(), object.getExpressionType(), object.getReceiverType(),
                                object.getCallableDescriptor());
                    return new String[] {
                            HTML_RENDER_TYPE.render(object.getExpectedType(), context),
                            HTML_RENDER_TYPE.render(object.getExpressionType(), context),
                            HTML_RENDER_TYPE.render(object.getReceiverType(), context),
                            HTML.render(object.getCallableDescriptor(), context)
                    };
                });

        MAP.put(ASSIGN_OPERATOR_AMBIGUITY,
                KotlinIdeaAnalysisBundle.htmlMessage("html.assignment.operators.ambiguity.all.these.functions.match.ul.0.ul.table.html"),
                HTML_AMBIGUOUS_CALLS);

        MAP.put(TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS, KotlinIdeaAnalysisBundle.htmlMessage("html.type.inference.failed.0.html"),
                HTML_TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS_RENDERER);
        MAP.put(TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER, KotlinIdeaAnalysisBundle.htmlMessage("html.type.inference.failed.0.html"),
                HTML_TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER_RENDERER);
        MAP.put(TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR, KotlinIdeaAnalysisBundle.htmlMessage("html.type.inference.failed.0.html"),
                HTML_TYPE_INFERENCE_PARAMETER_CONSTRAINT_ERROR_RENDERER);
        MAP.put(TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, tableForTypes(
                KotlinIdeaAnalysisBundle.message("type.inference.failed.expected.type.mismatch"),
                KotlinIdeaAnalysisBundle.message("required.space"), TextElementType.STRONG,
                KotlinIdeaAnalysisBundle.message("found.space"), TextElementType.ERROR), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
        MAP.put(TYPE_INFERENCE_UPPER_BOUND_VIOLATED, "<html>{0}</html>", HTML_TYPE_INFERENCE_UPPER_BOUND_VIOLATED_RENDERER);

        MAP.put(WRONG_SETTER_PARAMETER_TYPE, KotlinIdeaAnalysisBundle.htmlMessage(
                "html.setter.parameter.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);
        MAP.put(WRONG_GETTER_RETURN_TYPE, KotlinIdeaAnalysisBundle.htmlMessage(
                "html.getter.return.type.must.be.equal.to.the.type.of.the.property.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(ITERATOR_AMBIGUITY, KotlinIdeaAnalysisBundle.htmlMessage("html.method.iterator.is.ambiguous.for.this.expression.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS);

        MAP.put(UPPER_BOUND_VIOLATED, KotlinIdeaAnalysisBundle.htmlMessage(
                "html.type.argument.is.not.within.its.bounds.table.tr.td.expected.td.td.0.td.tr.tr.td.found.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(TYPE_MISMATCH_IN_FOR_LOOP, KotlinIdeaAnalysisBundle.htmlMessage(
                "html.loop.parameter.type.mismatch.table.tr.td.iterated.values.td.td.0.td.tr.tr.td.parameter.td.td.1.td.tr.table.html"), HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(RETURN_TYPE_MISMATCH_ON_OVERRIDE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.return.type.is.0.which.is.not.a.subtype.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML_WITH_ANNOTATIONS_WHITELIST);
        MAP.put(RETURN_TYPE_MISMATCH_ON_INHERITANCE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.return.types.of.inherited.members.are.incompatible.br.0.br.1.html"),
                HTML, HTML);

        MAP.put(PROPERTY_TYPE_MISMATCH_ON_OVERRIDE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.property.type.is.0.which.is.not.a.subtype.type.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML);
        MAP.put(VAR_TYPE_MISMATCH_ON_OVERRIDE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.var.property.type.is.0.which.is.not.a.type.of.overridden.br.1.html"), HTML_RENDER_RETURN_TYPE, HTML);
        MAP.put(PROPERTY_TYPE_MISMATCH_ON_INHERITANCE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.types.of.inherited.properties.are.incompatible.br.0.br.1.html"),
                HTML, HTML);
        MAP.put(VAR_TYPE_MISMATCH_ON_INHERITANCE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.types.of.inherited.var.properties.do.not.match.br.0.br.1.html"),
                HTML, HTML);

        MAP.put(VAR_OVERRIDDEN_BY_VAL, KotlinIdeaAnalysisBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"), HTML, HTML);
        MAP.put(VAR_OVERRIDDEN_BY_VAL_BY_DELEGATION,
                KotlinIdeaAnalysisBundle.htmlMessage("html.val.property.cannot.override.var.property.br.1.html"), HTML, HTML);

        MAP.put(ABSTRACT_MEMBER_NOT_IMPLEMENTED,
                KotlinIdeaAnalysisBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.member.br.1.html"), RENDER_CLASS_OR_OBJECT, HTML);
        MAP.put(ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED,
                KotlinIdeaAnalysisBundle.htmlMessage("html.0.is.not.abstract.and.does.not.implement.abstract.base.class.member.br.1.html"), RENDER_CLASS_OR_OBJECT, HTML);

        MAP.put(MANY_IMPL_MEMBER_NOT_IMPLEMENTED,
                KotlinIdeaAnalysisBundle.htmlMessage("html.0.must.override.1.br.because.it.inherits.many.implementations.of.it.html"),
                RENDER_CLASS_OR_OBJECT, HTML);

        MAP.put(RESULT_TYPE_MISMATCH, KotlinIdeaAnalysisBundle.htmlMessage("html.function.return.type.mismatch.table.tr.td.expected.td.td.1.td.tr.tr.td.found.td.td.2.td.tr.table.html"), STRING, HTML_RENDER_TYPE, HTML_RENDER_TYPE);

        MAP.put(OVERLOAD_RESOLUTION_AMBIGUITY,
                KotlinIdeaAnalysisBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS);
        MAP.put(CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY,
                KotlinIdeaAnalysisBundle.htmlMessage("html.overload.resolution.ambiguity.all.these.functions.match.ul.0.ul.html"),
                HTML_AMBIGUOUS_REFERENCES);
        MAP.put(NONE_APPLICABLE, KotlinIdeaAnalysisBundle.htmlMessage("html.none.of.the.following.functions.can.be.called.with.the.arguments.supplied.ul.0.ul.html"),
                HTML_NONE_APPLICABLE_CALLS);
        MAP.put(CANNOT_COMPLETE_RESOLVE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.cannot.choose.among.the.following.candidates.without.completing.type.inference.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS);
        MAP.put(UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                KotlinIdeaAnalysisBundle.htmlMessage(
                "html.unresolved.reference.br.none.of.the.following.candidates.is.applicable.because.of.receiver.type.mismatch.ul.0.ul.html"),
                HTML_AMBIGUOUS_CALLS);

        MAP.put(DELEGATE_SPECIAL_FUNCTION_AMBIGUITY,
                KotlinIdeaAnalysisBundle.htmlMessage("html.overload.resolution.ambiguity.on.method.0.all.these.functions.match.ul.1.ul.html"), STRING,
                HTML_AMBIGUOUS_CALLS);
        MAP.put(DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.property.delegate.must.have.a.0.method.none.of.the.following.functions.are.suitable.ul.1.ul.html"),
                STRING, HTML_NONE_APPLICABLE_CALLS);
        MAP.put(DELEGATE_PD_METHOD_NONE_APPLICABLE,
                KotlinIdeaAnalysisBundle.htmlMessage("html.0.method.may.be.missing.none.of.the.following.functions.will.be.called.ul.1.ul.html"),
                STRING, HTML_NONE_APPLICABLE_CALLS);

        MAP.put(COMPATIBILITY_WARNING,
                KotlinIdeaAnalysisBundle.htmlMessage("html.candidate.resolution.will.be.changed.soon.please.use.fully.qualified.name.to.invoke.the.following.closer.candidate.explicitly.ul.0.ul.html"),
                HTML_COMPATIBILITY_CANDIDATE);

        MAP.put(CONFLICTING_JVM_DECLARATIONS, KotlinIdeaAnalysisBundle.htmlMessage("html.platform.declaration.clash.0.html"), HTML_CONFLICTING_JVM_DECLARATIONS_DATA);
        MAP.put(ACCIDENTAL_OVERRIDE, KotlinIdeaAnalysisBundle.htmlMessage("html.accidental.override.0.html"), HTML_CONFLICTING_JVM_DECLARATIONS_DATA);

        URL errorIconUrl = AllIcons.class.getResource(ErrorIconUtil.getErrorIconUrl());
        MAP.put(EXCEPTION_FROM_ANALYZER, KotlinIdeaAnalysisBundle.htmlMessage(
                "html.internal.error.occurred.while.analyzing.this.expression.br.table.cellspacing.0.cellpadding.0.tr.td.strong.please.use.the.strong.td.td.img.src.0.td.td.strong.icon.in.the.bottom.right.corner.to.report.this.error.strong.td.tr.table.br.pre.0.pre.html",
                errorIconUrl),
                HTML_THROWABLE);

        MAP.put(ErrorsJs.JSCODE_ERROR, KotlinIdeaAnalysisBundle.htmlMessage("html.javascript.0.html"), JsCallDataHtmlRenderer.INSTANCE);
        MAP.put(ErrorsJs.JSCODE_WARNING, KotlinIdeaAnalysisBundle.htmlMessage("html.javascript.0.html"), JsCallDataHtmlRenderer.INSTANCE);
        MAP.put(UNSUPPORTED_FEATURE, "<html>{0}</html>",
                new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.UNSUPPORTED, true));
        MAP.put(EXPERIMENTAL_FEATURE_WARNING, "<html>{0}</html>",
                new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.WARNING, true));
        MAP.put(EXPERIMENTAL_FEATURE_ERROR, "<html>{0}</html>",
                new LanguageFeatureMessageRenderer(LanguageFeatureMessageRenderer.Type.ERROR, true));

        MAP.put(NO_ACTUAL_FOR_EXPECT, KotlinIdeaAnalysisBundle.htmlMessage("html.expected.0.has.no.actual.declaration.in.module.1.2.html"), DECLARATION_NAME_WITH_KIND,
                MODULE_WITH_PLATFORM, new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));
        MAP.put(ACTUAL_WITHOUT_EXPECT, KotlinIdeaAnalysisBundle.htmlMessage("html.0.has.no.corresponding.expected.declaration.1.html"),
                CAPITALIZED_DECLARATION_NAME_WITH_KIND_AND_PLATFORM,
                new PlatformIncompatibilityDiagnosticRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));

        MAP.put(NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS,
                KotlinIdeaAnalysisBundle.htmlMessage("html.actual.class.0.has.no.corresponding.members.for.expected.class.members.1.html"),
                NAME, new IncompatibleExpectedActualClassScopesRenderer(IdeMultiplatformDiagnosticRenderingMode.INSTANCE));

        String MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS =
                KotlinIdeaAnalysisBundle.htmlMessage(
                "html.method.contains.from.concurrenthashmap.may.have.unexpected.semantics.it.calls.containsvalue.instead.of.containskey.br.use.explicit.form.of.the.call.to.containskey.containsvalue.contains.or.cast.the.value.to.kotlin.collections.map.instead.br.see.https.youtrack.jetbrains.com.issue.kt.18053.for.more.details.html");
        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR, MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS);
        MAP.put(CONCURRENT_HASH_MAP_CONTAINS_OPERATOR_ERROR, MESSAGE_FOR_CONCURRENT_HASH_MAP_CONTAINS);

        MAP.setImmutable();
    }

    private IdeErrorMessages() {
    }
}
