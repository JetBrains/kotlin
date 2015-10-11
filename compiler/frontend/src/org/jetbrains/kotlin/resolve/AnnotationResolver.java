/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.*;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.annotations.*;
import org.jetbrains.kotlin.resolve.annotations.AnnotationsPackage;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationsContextImpl;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.NOT_AN_ANNOTATION_CLASS;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class AnnotationResolver {
    @NotNull private final CallResolver callResolver;
    @NotNull private final StorageManager storageManager;
    @NotNull private TypeResolver typeResolver;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;

    @NotNull private final List<AnnotationDescriptor> modifiersAnnotations;

    public AnnotationResolver(
            @NotNull CallResolver callResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull StorageManager storageManager,
            @NotNull KotlinBuiltIns kotlinBuiltIns
    ) {
        this.callResolver = callResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.storageManager = storageManager;

        modifiersAnnotations = AnnotationsPackage.buildMigrationAnnotationDescriptors(kotlinBuiltIns);
    }


    // component dependency cycle
    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    @NotNull
    public Annotations resolveAnnotationsWithoutArguments(
            @NotNull LexicalScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, false);
    }

    @NotNull
    public Annotations resolveAnnotationsWithArguments(
            @NotNull LexicalScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotations(scope, modifierList, trace, true);
    }

    @NotNull
    public Annotations resolveAnnotationsWithoutArguments(
            @NotNull LexicalScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntries,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotationEntries(scope, annotationEntries, trace, false);
    }

    @NotNull
    public Annotations resolveAnnotationsWithArguments(
            @NotNull LexicalScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntries,
            @NotNull BindingTrace trace
    ) {
        return resolveAnnotationEntries(scope, annotationEntries, trace, true);
    }

    private Annotations resolveAnnotations(
            @NotNull LexicalScope scope,
            @Nullable JetModifierList modifierList,
            @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (modifierList == null) {
            return Annotations.Companion.getEMPTY();
        }

        List<JetAnnotationEntry> annotationEntryElements = modifierList.getAnnotationEntries();

        return resolveAndAppendAnnotationsFromModifiers(
                resolveAnnotationEntries(scope, annotationEntryElements, trace, shouldResolveArguments),
                modifierList
        );
    }

    @NotNull
    public Annotations resolveAndAppendAnnotationsFromModifiers(
            @NotNull Annotations annotations,
            @NotNull JetModifierList modifierList
    ) {
        List<AnnotationDescriptor> annotationFromModifiers = new ArrayList<AnnotationDescriptor>();

        for (int i = 0; i < JetTokens.ANNOTATION_MODIFIERS_KEYWORDS_ARRAY.length; i++) {
            JetModifierKeywordToken modifier = JetTokens.ANNOTATION_MODIFIERS_KEYWORDS_ARRAY[i];
            if (modifierList.hasModifier(modifier)) {
                annotationFromModifiers.add(modifiersAnnotations.get(i));
            }
        }

        if (annotationFromModifiers.isEmpty()) return annotations;

        return new CompositeAnnotations(annotations, new AnnotationsImpl(annotationFromModifiers));
    }

    private Annotations resolveAnnotationEntries(
            @NotNull LexicalScope scope,
            @NotNull List<JetAnnotationEntry> annotationEntryElements,
            @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (annotationEntryElements.isEmpty()) return Annotations.Companion.getEMPTY();
        List<AnnotationWithTarget> result = new ArrayList<AnnotationWithTarget>(0);

        for (JetAnnotationEntry entryElement : annotationEntryElements) {
            AnnotationDescriptor descriptor = trace.get(BindingContext.ANNOTATION, entryElement);
            if (descriptor == null) {
                descriptor = new LazyAnnotationDescriptor(new LazyAnnotationsContextImpl(this, storageManager, trace, scope), entryElement);
            }
            if (shouldResolveArguments) {
                ForceResolveUtil.forceResolveAllContents(descriptor);
            }

            JetAnnotationUseSiteTarget target = entryElement.getUseSiteTarget();
            if (target != null) {
                result.add(new AnnotationWithTarget(descriptor, target.getAnnotationUseSiteTarget()));
            }
            else {
                result.add(new AnnotationWithTarget(descriptor, null));
            }
        }
        return AnnotationsImpl.create(result);
    }

    @NotNull
    public JetType resolveAnnotationType(@NotNull LexicalScope scope, @NotNull JetAnnotationEntry entryElement) {
        JetTypeReference typeReference = entryElement.getTypeReference();
        if (typeReference == null) {
            return ErrorUtils.createErrorType("No type reference: " + entryElement.getText());
        }

        JetType type = typeResolver.resolveType(scope, typeReference, new BindingTraceContext(), true);
        if (!(type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return ErrorUtils.createErrorType("Not an annotation: " + type);
        }
        return type;
    }

    public static void checkAnnotationType(
            @NotNull JetAnnotationEntry entryElement,
            @NotNull BindingTrace trace,
            @NotNull OverloadResolutionResults<FunctionDescriptor> results
    ) {
        if (!results.isSingleResult()) return;
        FunctionDescriptor descriptor = results.getResultingDescriptor();
        if (!ErrorUtils.isError(descriptor)) {
            if (descriptor instanceof ConstructorDescriptor) {
                ConstructorDescriptor constructor = (ConstructorDescriptor)descriptor;
                ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
                if (classDescriptor.getKind() != ClassKind.ANNOTATION_CLASS) {
                    trace.report(NOT_AN_ANNOTATION_CLASS.on(entryElement, classDescriptor));
                }
            }
            else {
                trace.report(NOT_AN_ANNOTATION_CLASS.on(entryElement, descriptor));
            }
        }
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveAnnotationCall(
            JetAnnotationEntry annotationEntry,
            LexicalScope scope,
            BindingTrace trace
    ) {
        return callResolver.resolveFunctionCall(
                trace, scope,
                CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, annotationEntry),
                NO_EXPECTED_TYPE,
                DataFlowInfo.EMPTY,
                true
        );
    }

    public static void reportUnsupportedAnnotationForTypeParameter(@NotNull JetTypeParameter jetTypeParameter, @NotNull BindingTrace trace) {
        JetModifierList modifierList = jetTypeParameter.getModifierList();
        if (modifierList == null) return;

        for (JetAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            trace.report(Errors.UNSUPPORTED.on(annotationEntry, "Annotations for type parameters are not supported yet"));
        }
    }

    @Nullable
    public ConstantValue<?> getAnnotationArgumentValue(
            @NotNull BindingTrace trace,
            @NotNull ValueParameterDescriptor valueParameter,
            @NotNull ResolvedValueArgument resolvedArgument
    ) {
        return constantExpressionEvaluator.getAnnotationArgumentValue(trace, valueParameter, resolvedArgument);
    }
}
