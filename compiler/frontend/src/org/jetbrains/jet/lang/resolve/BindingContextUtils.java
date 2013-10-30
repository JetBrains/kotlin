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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInfo;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.Slices;

import java.util.*;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.*;
import static org.jetbrains.jet.lang.diagnostics.Errors.AMBIGUOUS_LABEL;
import static org.jetbrains.jet.lang.resolve.BindingContext.AMBIGUOUS_LABEL_TARGET;
import static org.jetbrains.jet.lang.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR;

public class BindingContextUtils {
    private BindingContextUtils() {
    }

    private static final Slices.KeyNormalizer<DeclarationDescriptor> DECLARATION_DESCRIPTOR_NORMALIZER = new Slices.KeyNormalizer<DeclarationDescriptor>() {
        @Override
        public DeclarationDescriptor normalize(DeclarationDescriptor declarationDescriptor) {
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callable = (CallableMemberDescriptor) declarationDescriptor;
                if (callable.getKind() != DECLARATION) {
                    throw new IllegalStateException("non-declaration descriptors should be filtered out earlier: " + callable);
                }
            }
            //if (declarationDescriptor instanceof VariableAsFunctionDescriptor) {
            //    VariableAsFunctionDescriptor descriptor = (VariableAsFunctionDescriptor) declarationDescriptor;
            //    if (descriptor.getOriginal() != descriptor) {
            //        throw new IllegalStateException("original should be resolved earlier: " + descriptor);
            //    }
            //}
            return declarationDescriptor.getOriginal();
        }
    };

    /*package*/ static final ReadOnlySlice<DeclarationDescriptor, PsiElement> DESCRIPTOR_TO_DECLARATION =
            Slices.<DeclarationDescriptor, PsiElement>sliceBuilder().setKeyNormalizer(DECLARATION_DESCRIPTOR_NORMALIZER).setDebugName("DESCRIPTOR_TO_DECLARATION").build();

    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable JetElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof JetVariableDeclaration || element instanceof JetParameter)) {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        else if (element instanceof JetSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) element);
        }
        else if (element instanceof JetQualifiedExpression) {
            descriptor = extractVariableDescriptorIfAny(bindingContext, ((JetQualifiedExpression) element).getSelectorExpression(), onlyReference);
        }
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
        }
        return null;
    }

    @Nullable
    public static JetFile getContainingFile(@NotNull BindingContext context, @NotNull DeclarationDescriptor declarationDescriptor) {
        // declarationDescriptor may describe a synthesized element which doesn't have PSI
        // To workaround that, we find a top-level parent (which is inside a NamespaceDescriptor), which is guaranteed to have PSI
        DeclarationDescriptor descriptor = DescriptorUtils.findTopLevelParent(declarationDescriptor);
        if (descriptor == null) return null;

        PsiElement declaration = descriptorToDeclaration(context, descriptor);
        if (declaration == null) return null;

        PsiFile containingFile = declaration.getContainingFile();
        if (!(containingFile instanceof JetFile)) return null;
        return (JetFile) containingFile;
    }

    @Nullable
    private static PsiElement doGetDescriptorToDeclaration(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor) {
        return context.get(DESCRIPTOR_TO_DECLARATION, descriptor);
    }

    // NOTE this is also used by KDoc
    @Nullable
    public static PsiElement descriptorToDeclaration(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclaration(context, (CallableMemberDescriptor) descriptor);
        }
        else if (descriptor instanceof ClassDescriptor) {
            return classDescriptorToDeclaration(context, (ClassDescriptor) descriptor);
        }
        else {
            return doGetDescriptorToDeclaration(context, descriptor);
        }
    }

    @NotNull
    public static List<PsiElement> descriptorToDeclarations(@NotNull BindingContext context, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclarations(context, (CallableMemberDescriptor) descriptor);
        }
        else {
            PsiElement psiElement = descriptorToDeclaration(context, descriptor);
            if (psiElement != null) {
                return Lists.newArrayList(psiElement);
            } else {
                return Lists.newArrayList();
            }
        }
    }

    @Nullable
    public static PsiElement callableDescriptorToDeclaration(@NotNull BindingContext context, @NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() == SYNTHESIZED) {
            CallableMemberDescriptor original = callable.getOriginal();
            if (original instanceof SynthesizedCallableMemberDescriptor<?>) {
                DeclarationDescriptor base = ((SynthesizedCallableMemberDescriptor<?>) original).getBaseForSynthesized();
                return descriptorToDeclaration(context, base);
            }
            return null;
        }

        if (callable.getKind() == DECLARATION) {
            return doGetDescriptorToDeclaration(context, callable.getOriginal());
        }

        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        if (overriddenDescriptors.size() != 1) {
            throw new IllegalStateException(
                    "Cannot find declaration: fake descriptor " + callable + " has more than one overridden descriptor:\n" +
                    StringUtil.join(overriddenDescriptors, ",\n"));
        }

        return callableDescriptorToDeclaration(context, overriddenDescriptors.iterator().next());
    }

    @NotNull
    public static List<PsiElement> callableDescriptorToDeclarations(
            @NotNull BindingContext context,
            @NotNull CallableMemberDescriptor callable
    ) {
        if (callable.getKind() == SYNTHESIZED) {
            CallableMemberDescriptor original = callable.getOriginal();
            if (original instanceof SynthesizedCallableMemberDescriptor<?>) {
                DeclarationDescriptor base = ((SynthesizedCallableMemberDescriptor<?>) original).getBaseForSynthesized();
                return descriptorToDeclarations(context, base);
            }
            return Collections.emptyList();
        }

        if (callable.getKind() == DECLARATION) {
            PsiElement psiElement = doGetDescriptorToDeclaration(context, callable);
            return psiElement != null ? Lists.newArrayList(psiElement) : Lists.<PsiElement>newArrayList();
        }

        List<PsiElement> r = new ArrayList<PsiElement>();
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            r.addAll(callableDescriptorToDeclarations(context, overridden));
        }
        return r;
    }

    @Nullable
    public static PsiElement classDescriptorToDeclaration(@NotNull BindingContext context, @NotNull ClassDescriptor clazz) {
        return doGetDescriptorToDeclaration(context, clazz);
    }

    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
            @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {

        if (function.getKind() != DECLARATION) {
            throw new IllegalArgumentException("function of kind " + function.getKind() + " cannot have declaration");
        }

        trace.record(BindingContext.FUNCTION, psiElement, function);
    }

    @NotNull
    public static <K, V> V getNotNull(
        @NotNull BindingContext bindingContext,
        @NotNull ReadOnlySlice<K, V> slice,
        @NotNull K key
    ) {
        return getNotNull(bindingContext, slice, key, "Value at " + slice + " must not be null for " + key);
    }

    @NotNull
    public static <K, V> V getNotNull(
            @NotNull BindingContext bindingContext,
            @NotNull ReadOnlySlice<K, V> slice,
            @NotNull K key,
            @NotNull String messageIfNull
    ) {
        V value = bindingContext.get(slice, key);
        if (value == null) {
            throw new IllegalStateException(messageIfNull);
        }
        return value;
    }

    @NotNull
    public static DeclarationDescriptor getEnclosingDescriptor(@NotNull BindingContext context, @NotNull JetElement element) {
        JetNamedDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetNamedDeclaration.class);
        if (declaration instanceof JetFunctionLiteral) {
            return getEnclosingDescriptor(context, declaration);
        }
        DeclarationDescriptor descriptor = context.get(DECLARATION_TO_DESCRIPTOR, declaration);
        assert descriptor != null : "No descriptor for named declaration: " + declaration.getText() + "\n(of type " + declaration.getClass() + ")";
        return descriptor;
    }

    public static void reportAmbiguousLabel(
            @NotNull BindingTrace trace,
            @NotNull JetSimpleNameExpression targetLabel,
            @NotNull Collection<DeclarationDescriptor> declarationsByLabel
    ) {
        Collection<PsiElement> targets = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : declarationsByLabel) {
            PsiElement element = descriptorToDeclaration(trace.getBindingContext(), descriptor);
            assert element != null : "Label can only point to something in the same lexical scope";
            targets.add(element);
        }
        if (!targets.isEmpty()) {
            trace.record(AMBIGUOUS_LABEL_TARGET, targetLabel, targets);
        }
        trace.report(AMBIGUOUS_LABEL.on(targetLabel));
    }

    @Nullable
    public static JetType updateRecordedType(
            @Nullable JetType type,
            @NotNull JetExpression expression,
            @NotNull BindingTrace trace,
            boolean shouldBeMadeNullable
    ) {
        if (type == null) return null;
        if (shouldBeMadeNullable) {
            type = TypeUtils.makeNullable(type);
        }
        trace.record(BindingContext.EXPRESSION_TYPE, expression, type);
        return type;
    }

    @Nullable
    public static JetTypeInfo getRecordedTypeInfo(@NotNull JetExpression expression, @NotNull BindingContext context) {
        if (!context.get(BindingContext.PROCESSED, expression)) return null;
        DataFlowInfo dataFlowInfo = context.get(BindingContext.EXPRESSION_DATA_FLOW_INFO, expression);
        if (dataFlowInfo == null) {
            dataFlowInfo = DataFlowInfo.EMPTY;
        }
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, expression);
        return JetTypeInfo.create(type, dataFlowInfo);
    }

    public static boolean isExpressionWithValidReference(
            @NotNull JetExpression expression,
            @NotNull BindingContext context
    ) {
        if (expression instanceof JetCallExpression) {
            return isCallExpressionWithValidReference(expression, context);
        }

        return expression instanceof JetReferenceExpression;
    }

    public static boolean isCallExpressionWithValidReference(
            @NotNull JetExpression expression,
            @NotNull BindingContext context
    ) {
        if (expression instanceof JetCallExpression) {
            JetExpression calleeExpression = ((JetCallExpression) expression).getCalleeExpression();
            ResolvedCall<? extends CallableDescriptor> resolvedCall = context.get(BindingContext.RESOLVED_CALL, calleeExpression);
            if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public static Set<CallableMemberDescriptor> getDirectlyOverriddenDeclarations(@NotNull CallableMemberDescriptor descriptor) {
        Set<CallableMemberDescriptor> result = Sets.newHashSet();
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = descriptor.getOverriddenDescriptors();
        for (CallableMemberDescriptor overriddenDescriptor : overriddenDescriptors) {
            CallableMemberDescriptor.Kind kind = overriddenDescriptor.getKind();
            if (kind == DECLARATION) {
                result.add(overriddenDescriptor);
            }
            else if (kind == FAKE_OVERRIDE || kind == DELEGATION) {
                result.addAll(getDirectlyOverriddenDeclarations(overriddenDescriptor));
            }
            else if (kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
        }
        return OverridingUtil.filterOutOverridden(result);
    }

    @NotNull
    public static Set<FunctionDescriptor> getDirectlyOverriddenDeclarations(@NotNull FunctionDescriptor descriptor) {
        //noinspection unchecked
        return (Set) getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
    }

    @NotNull
    public static Set<PropertyDescriptor> getDirectlyOverriddenDeclarations(@NotNull PropertyDescriptor descriptor) {
        //noinspection unchecked
        return (Set) getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
    }

    @NotNull
    public static Set<FunctionDescriptor> getAllOverriddenDeclarations(@NotNull FunctionDescriptor functionDescriptor) {
        Set<FunctionDescriptor> result = Sets.newHashSet();
        for (FunctionDescriptor overriddenDeclaration : functionDescriptor.getOverriddenDescriptors()) {
            CallableMemberDescriptor.Kind kind = overriddenDeclaration.getKind();
            if (kind == DECLARATION) {
                result.add(overriddenDeclaration);
            }
            else if (kind == DELEGATION || kind == FAKE_OVERRIDE || kind == SYNTHESIZED) {
                //do nothing
            }
            else {
                throw new AssertionError("Unexpected callable kind " + kind);
            }
            result.addAll(getAllOverriddenDeclarations(overriddenDeclaration));
        }
        return result;
    }
}
