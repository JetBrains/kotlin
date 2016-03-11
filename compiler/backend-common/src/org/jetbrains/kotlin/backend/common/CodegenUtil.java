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

package org.jetbrains.kotlin.backend.common;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;

import java.util.*;

public class CodegenUtil {

    private CodegenUtil() {
    }

    @Nullable
    public static FunctionDescriptor getDeclaredFunctionByRawSignature(
            @NotNull ClassDescriptor owner,
            @NotNull Name name,
            @NotNull ClassifierDescriptor returnedClassifier,
            @NotNull ClassifierDescriptor... valueParameterClassifiers
    ) {
        Collection<SimpleFunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getContributedFunctions(name, NoLookupLocation.FROM_BACKEND);
        for (FunctionDescriptor function : functions) {
            if (!CallResolverUtilKt.isOrOverridesSynthesized(function)
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClassifiers))
                && rawTypeMatches(function.getReturnType(), returnedClassifier)) {
                return function;
            }
        }
        return null;
    }

    @Nullable
    public static PropertyDescriptor getDelegatePropertyIfAny(KtExpression expression, ClassDescriptor classDescriptor, BindingContext bindingContext) {
        PropertyDescriptor propertyDescriptor = null;
        if (expression instanceof KtSimpleNameExpression) {
            ResolvedCall<?> call = CallUtilKt.getResolvedCall(expression, bindingContext);
            if (call != null) {
                CallableDescriptor callResultingDescriptor = call.getResultingDescriptor();
                if (callResultingDescriptor instanceof ValueParameterDescriptor) {
                    ValueParameterDescriptor valueParameterDescriptor = (ValueParameterDescriptor) callResultingDescriptor;
                    // constructor parameter
                    if (valueParameterDescriptor.getContainingDeclaration() instanceof ConstructorDescriptor) {
                        // constructor of my class
                        if (valueParameterDescriptor.getContainingDeclaration().getContainingDeclaration() == classDescriptor) {
                            propertyDescriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameterDescriptor);
                        }
                    }
                }

                // todo: when and if frontend will allow properties defined not as constructor parameters to be used in delegation specifier
            }
        }
        return propertyDescriptor;
    }

    public static boolean isFinalPropertyWithBackingField(PropertyDescriptor propertyDescriptor, BindingContext bindingContext) {
        return propertyDescriptor != null &&
               !propertyDescriptor.isVar() &&
               Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor));
    }

    @NotNull
    public static Map<FunctionDescriptor, FunctionDescriptor> getNonPrivateTraitMethods(ClassDescriptor descriptor) {
        Map<FunctionDescriptor, FunctionDescriptor> result = new LinkedHashMap<FunctionDescriptor, FunctionDescriptor>();
        for (DeclarationDescriptor declaration : DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().getMemberScope())) {
            if (!(declaration instanceof CallableMemberDescriptor)) continue;

            CallableMemberDescriptor inheritedMember = (CallableMemberDescriptor) declaration;
            CallableMemberDescriptor traitMember = ImplKt.findTraitImplementation(inheritedMember);
            if (traitMember == null || Visibilities.isPrivate(traitMember.getVisibility())) continue;

            assert traitMember.getModality() != Modality.ABSTRACT : "Cannot delegate to abstract trait method: " + inheritedMember;

            // inheritedMember can be abstract here. In order for FunctionCodegen to generate the method body, we're creating a copy here
            // with traitMember's modality
            result.putAll(copyFunctions(inheritedMember, traitMember, inheritedMember.getContainingDeclaration(), traitMember.getModality(), Visibilities.PUBLIC,
                                                     CallableMemberDescriptor.Kind.DECLARATION, true));
        }
        return result;
    }

    @NotNull
    public static Map<FunctionDescriptor, FunctionDescriptor> copyFunctions(
            @NotNull CallableMemberDescriptor inheritedMember,
            @NotNull CallableMemberDescriptor traitMember,
            DeclarationDescriptor newOwner,
            Modality modality,
            Visibility visibility,
            CallableMemberDescriptor.Kind kind,
            boolean copyOverrides
    ) {
        CallableMemberDescriptor copy = inheritedMember.copy(newOwner, modality, visibility, kind, copyOverrides);
        Map<FunctionDescriptor, FunctionDescriptor> result = new LinkedHashMap<FunctionDescriptor, FunctionDescriptor>(0);
        if (traitMember instanceof SimpleFunctionDescriptor) {
            result.put((FunctionDescriptor) traitMember, (FunctionDescriptor) copy);
        }
        else if (traitMember instanceof PropertyDescriptor) {
            for (PropertyAccessorDescriptor traitAccessor : ((PropertyDescriptor) traitMember).getAccessors()) {
                for (PropertyAccessorDescriptor inheritedAccessor : ((PropertyDescriptor) copy).getAccessors()) {
                    if (inheritedAccessor.getClass() == traitAccessor.getClass()) { // same accessor kind
                        result.put(traitAccessor, inheritedAccessor);
                    }
                }
            }
        }
        return result;
    }

    @NotNull
    public static ClassDescriptor getSuperClassBySuperTypeListEntry(@NotNull KtSuperTypeListEntry specifier, @NotNull BindingContext bindingContext) {
        KotlinType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
        assert superType != null : "superType should not be null: " + specifier.getText();

        ClassDescriptor superClassDescriptor = (ClassDescriptor) superType.getConstructor().getDeclarationDescriptor();
        assert superClassDescriptor != null : "superClassDescriptor should not be null: " + specifier.getText();
        return superClassDescriptor;
    }

    private static boolean valueParameterClassesMatch(
            @NotNull List<ValueParameterDescriptor> parameters,
            @NotNull List<ClassifierDescriptor> classifiers
    ) {
        if (parameters.size() != classifiers.size()) return false;
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameterDescriptor = parameters.get(i);
            ClassifierDescriptor classDescriptor = classifiers.get(i);
            if (!rawTypeMatches(parameterDescriptor.getType(), classDescriptor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rawTypeMatches(KotlinType type, ClassifierDescriptor classifier) {
        return type.getConstructor().equals(classifier.getTypeConstructor());
    }

    public static boolean isEnumValueOfMethod(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> methodTypeParameters = functionDescriptor.getValueParameters();
        KotlinType nullableString = TypeUtils.makeNullable(DescriptorUtilsKt.getBuiltIns(functionDescriptor).getStringType());
        return DescriptorUtils.ENUM_VALUE_OF.equals(functionDescriptor.getName())
               && methodTypeParameters.size() == 1
               && KotlinTypeChecker.DEFAULT.isSubtypeOf(methodTypeParameters.get(0).getType(), nullableString);
    }

    @Nullable
    public static Integer getLineNumberForElement(@NotNull PsiElement statement, boolean markEndOffset) {
        PsiFile file = statement.getContainingFile();
        if (file instanceof KtFile) {
            if (KtPsiFactoryKt.getDoNotAnalyze((KtFile) file) != null) {
                return null;
            }
        }

        if (statement instanceof KtConstructorDelegationReferenceExpression && statement.getTextLength() == 0) {
            // PsiElement for constructor delegation reference is always generated, so we shouldn't mark it's line number if it's empty
            return null;
        }

        Document document = file.getViewProvider().getDocument();
        return document != null ? document.getLineNumber(markEndOffset ? statement.getTextRange().getEndOffset() : statement.getTextOffset()) + 1 : null;
    }
}
