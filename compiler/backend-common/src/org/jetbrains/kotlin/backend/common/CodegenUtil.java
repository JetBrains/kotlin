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
import org.jetbrains.kotlin.backend.common.bridges.BridgesPackage;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.CallResolverUtilPackage;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.incremental.components.LookupLocation;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.*;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;

/**
 * Backend-independent utility class.
 */
public class CodegenUtil {

    private CodegenUtil() {
    }

    // TODO: consider putting predefined method signatures here too.
    public static final String EQUALS_METHOD_NAME = "equals";
    public static final String TO_STRING_METHOD_NAME = "toString";
    public static final String HASH_CODE_METHOD_NAME = "hashCode";

    @Nullable
    public static FunctionDescriptor getDeclaredFunctionByRawSignature(
            @NotNull ClassDescriptor owner,
            @NotNull Name name,
            @NotNull ClassifierDescriptor returnedClassifier,
            @NotNull ClassifierDescriptor... valueParameterClassifiers
    ) {
        Collection<FunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getFunctions(name, LookupLocation.NO_LOCATION);
        for (FunctionDescriptor function : functions) {
            if (!CallResolverUtilPackage.isOrOverridesSynthesized(function)
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClassifiers))
                && rawTypeMatches(function.getReturnType(), returnedClassifier)) {
                return function;
            }
        }
        return null;
    }

    public static FunctionDescriptor getAnyEqualsMethod(@NotNull KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function = getDeclaredFunctionByRawSignature(
                anyClass, Name.identifier(EQUALS_METHOD_NAME), builtIns.getBoolean(), anyClass
        );
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyToStringMethod(@NotNull KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function = getDeclaredFunctionByRawSignature(anyClass, Name.identifier(TO_STRING_METHOD_NAME), builtIns.getString());
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyHashCodeMethod(@NotNull KotlinBuiltIns builtIns) {
        ClassDescriptor anyClass = builtIns.getAny();
        FunctionDescriptor function = getDeclaredFunctionByRawSignature(anyClass, Name.identifier(HASH_CODE_METHOD_NAME), builtIns.getInt());
        assert function != null;
        return function;
    }

    @Nullable
    public static PropertyDescriptor getDelegatePropertyIfAny(JetExpression expression, ClassDescriptor classDescriptor, BindingContext bindingContext) {
        PropertyDescriptor propertyDescriptor = null;
        if (expression instanceof JetSimpleNameExpression) {
            ResolvedCall<?> call = CallUtilPackage.getResolvedCall(expression, bindingContext);
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
    public static Map<FunctionDescriptor, FunctionDescriptor> getTraitMethods(ClassDescriptor descriptor) {
        Map<FunctionDescriptor, FunctionDescriptor> result = new LinkedHashMap<FunctionDescriptor, FunctionDescriptor>();
        for (DeclarationDescriptor declaration : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (!(declaration instanceof CallableMemberDescriptor)) continue;

            CallableMemberDescriptor inheritedMember = (CallableMemberDescriptor) declaration;
            CallableMemberDescriptor traitMember = BridgesPackage.findTraitImplementation(inheritedMember);
            if (traitMember == null) continue;

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
    public static ClassDescriptor getSuperClassByDelegationSpecifier(@NotNull JetDelegationSpecifier specifier, @NotNull BindingContext bindingContext) {
        JetType superType = bindingContext.get(BindingContext.TYPE, specifier.getTypeReference());
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

    private static boolean rawTypeMatches(JetType type, ClassifierDescriptor classifier) {
        return type.getConstructor().equals(classifier.getTypeConstructor());
    }

    public static boolean isEnumValueOfMethod(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> methodTypeParameters = functionDescriptor.getValueParameters();
        JetType nullableString = TypeUtils.makeNullable(getBuiltIns(functionDescriptor).getStringType());
        return DescriptorUtils.ENUM_VALUE_OF.equals(functionDescriptor.getName())
               && methodTypeParameters.size() == 1
               && JetTypeChecker.DEFAULT.isSubtypeOf(methodTypeParameters.get(0).getType(), nullableString);
    }

    public static boolean isEnumValuesMethod(@NotNull FunctionDescriptor functionDescriptor) {
        List<ValueParameterDescriptor> methodTypeParameters = functionDescriptor.getValueParameters();
        return DescriptorUtils.ENUM_VALUES.equals(functionDescriptor.getName())
               && methodTypeParameters.isEmpty();
    }

    @Nullable
    public static Integer getLineNumberForElement(@NotNull PsiElement statement, boolean markEndOffset) {
        PsiFile file = statement.getContainingFile();
        if (file instanceof JetFile) {
            if (PsiPackage.getDoNotAnalyze((JetFile) file) != null) {
                return null;
            }
        }
        Document document = file.getViewProvider().getDocument();
        return document != null ? document.getLineNumber(markEndOffset ? statement.getTextRange().getEndOffset() : statement.getTextOffset()) + 1 : null;
    }
}
