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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableAsFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.Slices;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 * @author svtk
 */
public class BindingContextUtils {
    private BindingContextUtils() {
    }

    private static final Slices.KeyNormalizer<DeclarationDescriptor> DECLARATION_DESCRIPTOR_NORMALIZER = new Slices.KeyNormalizer<DeclarationDescriptor>() {
        @Override
        public DeclarationDescriptor normalize(DeclarationDescriptor declarationDescriptor) {
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callable = (CallableMemberDescriptor) declarationDescriptor;
                if (callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
                    throw new IllegalStateException("non-declaration descriptors should be filtered out earler: " + callable);
                }
            }
            if (declarationDescriptor instanceof VariableAsFunctionDescriptor) {
                VariableAsFunctionDescriptor descriptor = (VariableAsFunctionDescriptor) declarationDescriptor;
                if (descriptor.getOriginal() != descriptor) {
                    throw new IllegalStateException("original should be resolved earlier: " + descriptor);
                }
            }
            return declarationDescriptor.getOriginal();
        }
    };

    static final ReadOnlySlice<DeclarationDescriptor, PsiElement> DESCRIPTOR_TO_DECLARATION =
            Slices.<DeclarationDescriptor, PsiElement>sliceBuilder().setKeyNormalizer(DECLARATION_DESCRIPTOR_NORMALIZER).build();

    @Nullable
    public static PsiElement resolveToDeclarationPsiElement(@NotNull BindingContext bindingContext, @Nullable JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        if (declarationDescriptor == null) {
            return bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression);
        }

        PsiElement element = descriptorToDeclaration(bindingContext, declarationDescriptor);
        if (element != null) {
            return element;
        }

        // TODO: Need to have a valid stubs for standard classes
        if (referenceExpression != null && JetStandardClasses.getAllStandardClasses().contains(declarationDescriptor)) {
            return referenceExpression.getContainingFile();
        }

        return null;
    }

    @NotNull
    public static List<PsiElement> resolveToDeclarationPsiElements(@NotNull BindingContext bindingContext, @Nullable JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        if (declarationDescriptor == null) {
            return Lists.newArrayList(bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression));
        }

        List<PsiElement> elements = descriptorToDeclarations(bindingContext, declarationDescriptor);
        if (elements.size() > 0) {
            return elements;
        }

        // TODO: Need to have a valid stubs for standard classes
        if (referenceExpression != null && JetStandardClasses.getAllStandardClasses().contains(declarationDescriptor)) {
            return Lists.<PsiElement>newArrayList(referenceExpression.getContainingFile());
        }

        return Lists.newArrayList();
    }


    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(@NotNull BindingContext bindingContext, @Nullable JetElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof JetProperty || element instanceof JetParameter)) {
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
        if (descriptor instanceof VariableAsFunctionDescriptor) {
            return ((VariableAsFunctionDescriptor) descriptor).getVariableDescriptor();
        }
        return null;
    }

    // TODO these helper methods are added as a workaround to some compiler bugs in Kotlin...

    // NOTE this is used by KDoc
    @Nullable
    public static NamespaceDescriptor namespaceDescriptor(@NotNull BindingContext context, @NotNull JetFile source) {
        return context.get(BindingContext.FILE_TO_NAMESPACE, source);
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
        if (callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
            if (overriddenDescriptors.size() != 1) {
                // TODO evil code
                throw new IllegalStateException(
                        "cannot find declaration: fake descriptor" +
                                " has more then one overriden descriptor: " + callable);
            }

            return callableDescriptorToDeclaration(context, overriddenDescriptors.iterator().next());
        }

        return doGetDescriptorToDeclaration(context, callable.getOriginal());
    }

    private static List<PsiElement> callableDescriptorToDeclarations(@NotNull BindingContext context, @NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            List<PsiElement> r = new ArrayList<PsiElement>();
            Set<? extends CallableMemberDescriptor> overridenDescriptors = callable.getOverriddenDescriptors();
            for (CallableMemberDescriptor overriden : overridenDescriptors) {
                r.addAll(callableDescriptorToDeclarations(context, overriden));
            }
            return r;
        }
        PsiElement psiElement = doGetDescriptorToDeclaration(context, callable);
        return psiElement != null ? Lists.newArrayList(psiElement) : Lists.<PsiElement>newArrayList();
    }

    @Nullable
    public static PsiElement classDescriptorToDeclaration(@NotNull BindingContext context, @NotNull ClassDescriptor clazz) {
        return doGetDescriptorToDeclaration(context, clazz);
    }

    public static void recordFunctionDeclarationToDescriptor(@NotNull BindingTrace trace,
            @NotNull PsiElement psiElement, @NotNull SimpleFunctionDescriptor function) {

        if (function.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            throw new IllegalArgumentException("function of kind " + function.getKind() + " cannot have declaration");
        }

        trace.record(BindingContext.FUNCTION, psiElement, function);
    }
}
