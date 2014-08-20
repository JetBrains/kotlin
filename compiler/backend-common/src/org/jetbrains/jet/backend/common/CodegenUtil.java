/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.backend.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDelegationSpecifier;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

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
        Collection<FunctionDescriptor> functions = owner.getDefaultType().getMemberScope().getFunctions(name);
        for (FunctionDescriptor function : functions) {
            if (!CallResolverUtil.isOrOverridesSynthesized(function)
                && function.getTypeParameters().isEmpty()
                && valueParameterClassesMatch(function.getValueParameters(), Arrays.asList(valueParameterClassifiers))
                && rawTypeMatches(function.getReturnType(), returnedClassifier)) {
                return function;
            }
        }
        return null;
    }

    public static FunctionDescriptor getAnyEqualsMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                getDeclaredFunctionByRawSignature(anyClass, Name.identifier(EQUALS_METHOD_NAME),
                                                  KotlinBuiltIns.getInstance().getBoolean(),
                                                  anyClass);
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyToStringMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                getDeclaredFunctionByRawSignature(anyClass, Name.identifier(TO_STRING_METHOD_NAME),
                                                  KotlinBuiltIns.getInstance().getString());
        assert function != null;
        return function;
    }

    public static FunctionDescriptor getAnyHashCodeMethod() {
        ClassDescriptor anyClass = KotlinBuiltIns.getInstance().getAny();
        FunctionDescriptor function =
                getDeclaredFunctionByRawSignature(anyClass, Name.identifier(HASH_CODE_METHOD_NAME),
                                                  KotlinBuiltIns.getInstance().getInt());
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

    public static Map<CallableMemberDescriptor, CallableMemberDescriptor> getDelegates(ClassDescriptor descriptor, ClassDescriptor toClass) {
        Map<CallableMemberDescriptor, CallableMemberDescriptor> result = new LinkedHashMap<CallableMemberDescriptor, CallableMemberDescriptor>();
        for (DeclarationDescriptor declaration : descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (declaration instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor callableMemberDescriptor = (CallableMemberDescriptor) declaration;
                if (callableMemberDescriptor.getKind() == CallableMemberDescriptor.Kind.DELEGATION) {
                    Set<? extends CallableMemberDescriptor> overriddenDescriptors = callableMemberDescriptor.getOverriddenDescriptors();
                    for (CallableMemberDescriptor overriddenDescriptor : overriddenDescriptors) {
                        if (overriddenDescriptor.getContainingDeclaration() == toClass) {
                            assert !result.containsKey(callableMemberDescriptor) :
                                    "overridden is already set for " + callableMemberDescriptor;
                            result.put(callableMemberDescriptor, overriddenDescriptor);
                        }
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
        return type.getConstructor().getDeclarationDescriptor().getOriginal() == classifier.getOriginal();
    }
}
