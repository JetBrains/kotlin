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

package org.jetbrains.kotlin.resolve;

import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.TargetedAnnotations;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationDescriptor;
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyAnnotationsContextImpl;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.diagnostics.Errors.ANNOTATION_USED_AS_ANNOTATION_ARGUMENT;
import static org.jetbrains.kotlin.diagnostics.Errors.NOT_AN_ANNOTATION_CLASS;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class AnnotationResolverImpl extends AnnotationResolver {
    @NotNull private final CallResolver callResolver;
    @NotNull private final StorageManager storageManager;
    @NotNull private TypeResolver typeResolver;
    @NotNull private final ConstantExpressionEvaluator constantExpressionEvaluator;

    public AnnotationResolverImpl(
            @NotNull CallResolver callResolver,
            @NotNull ConstantExpressionEvaluator constantExpressionEvaluator,
            @NotNull StorageManager storageManager
    ) {
        this.callResolver = callResolver;
        this.constantExpressionEvaluator = constantExpressionEvaluator;
        this.storageManager = storageManager;
    }


    // component dependency cycle
    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }


    @NotNull
    @Override
    public Annotations resolveAnnotationEntries(
            @NotNull LexicalScope scope,
            @NotNull List<KtAnnotationEntry> annotationEntryElements,
            @NotNull BindingTrace trace,
            boolean shouldResolveArguments
    ) {
        if (annotationEntryElements.isEmpty()) return Annotations.Companion.getEMPTY();

        List<AnnotationDescriptor> standard = new ArrayList<>();
        List<AnnotationWithTarget> targeted = new ArrayList<>();

        for (KtAnnotationEntry entryElement : annotationEntryElements) {
            AnnotationDescriptor descriptor = trace.get(BindingContext.ANNOTATION, entryElement);
            if (descriptor == null) {
                descriptor = new LazyAnnotationDescriptor(new LazyAnnotationsContextImpl(this, storageManager, trace, scope), entryElement);
            }
            if (shouldResolveArguments) {
                ForceResolveUtil.forceResolveAllContents(descriptor);
            }

            KtAnnotationUseSiteTarget target = entryElement.getUseSiteTarget();
            if (target != null) {
                targeted.add(new AnnotationWithTarget(descriptor, target.getAnnotationUseSiteTarget()));
            }
            else {
                standard.add(descriptor);
            }
        }
        return new TargetedAnnotations(CollectionsKt.toList(standard), CollectionsKt.toList(targeted));
    }

    @Override
    @NotNull
    public KotlinType resolveAnnotationType(
            @NotNull LexicalScope scope,
            @NotNull KtAnnotationEntry entryElement,
            @NotNull BindingTrace trace
    ) {
        KtTypeReference typeReference = entryElement.getTypeReference();
        if (typeReference == null) {
            return ErrorUtils.createErrorType(ErrorTypeKind.UNRESOLVED_TYPE, entryElement.getText());
        }

        KotlinType type = typeResolver.resolveType(scope, typeReference, trace, true);
        if (!(type.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return ErrorUtils.createErrorType(ErrorTypeKind.NOT_ANNOTATION_TYPE_IN_ANNOTATION_CONTEXT, type.toString());
        }
        return type;
    }

    public static void checkAnnotationType(
            @NotNull KtAnnotationEntry entryElement,
            @NotNull BindingTrace trace,
            @NotNull OverloadResolutionResults<FunctionDescriptor> results
    ) {
        if (!results.isSingleResult()) return;
        FunctionDescriptor descriptor = results.getResultingDescriptor();
        if (!ErrorUtils.isError(descriptor)) {
            if (descriptor instanceof ConstructorDescriptor) {
                ConstructorDescriptor constructor = (ConstructorDescriptor)descriptor;
                ClassDescriptor classDescriptor = constructor.getConstructedClass();
                if (classDescriptor.getKind() != ClassKind.ANNOTATION_CLASS) {
                    trace.report(NOT_AN_ANNOTATION_CLASS.on(entryElement, classDescriptor));
                }
            }
            else {
                trace.report(NOT_AN_ANNOTATION_CLASS.on(entryElement, descriptor));
            }
        }
    }

    @Override
    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveAnnotationCall(
            @NotNull KtAnnotationEntry annotationEntry,
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace
    ) {
        if (PsiTreeUtil.getParentOfType(annotationEntry, KtAnnotationEntry.class) != null) {
            trace.report(ANNOTATION_USED_AS_ANNOTATION_ARGUMENT.on(annotationEntry));
        }

        return callResolver.resolveFunctionCall(
                trace, scope,
                CallMaker.makeCall(null, null, annotationEntry),
                NO_EXPECTED_TYPE,
                DataFlowInfoFactory.EMPTY,
                true,
                null // specific calls in terms of inference, can't be inside annotation calls
        );
    }

    public static void reportUnsupportedAnnotationForTypeParameter(
            @NotNull KtTypeParameter jetTypeParameter,
            @NotNull BindingTrace trace,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        KtModifierList modifierList = jetTypeParameter.getModifierList();
        if (modifierList == null) return;

        for (KtAnnotationEntry annotationEntry : modifierList.getAnnotationEntries()) {
            trace.report(Errors.UNSUPPORTED_FEATURE.on(
                    annotationEntry, new Pair<>(LanguageFeature.ClassTypeParameterAnnotations, languageVersionSettings)
            ));
        }
    }

    @Override
    @Nullable
    public ConstantValue<?> getAnnotationArgumentValue(
            @NotNull BindingTrace trace,
            @NotNull ValueParameterDescriptor valueParameter,
            @NotNull ResolvedValueArgument resolvedArgument
    ) {
        return constantExpressionEvaluator.getAnnotationArgumentValue(trace, valueParameter, resolvedArgument);
    }
}
