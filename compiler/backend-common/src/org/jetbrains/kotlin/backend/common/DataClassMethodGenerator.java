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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetClass;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.OverrideResolver;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;

import java.util.Collections;
import java.util.List;

/**
 * A platform-independent logic for generating data class synthetic methods.
 * TODO: data class with zero components gets no toString/equals/hashCode methods. This is inconsistent and should be
 *       changed here with the platform backends adopted.
 */
public abstract class DataClassMethodGenerator {
    private final JetClassOrObject declaration;
    private final BindingContext bindingContext;
    private final ClassDescriptor classDescriptor;
    private final KotlinBuiltIns builtIns;

    public DataClassMethodGenerator(JetClassOrObject declaration, BindingContext bindingContext) {
        this.declaration = declaration;
        this.bindingContext = bindingContext;
        this.classDescriptor = BindingContextUtils.getNotNull(bindingContext, BindingContext.CLASS, declaration);
        this.builtIns = DescriptorUtilsKt.getBuiltIns(classDescriptor);
    }

    public void generate() {
        generateComponentFunctionsForDataClasses();

        generateCopyFunctionForDataClasses(getPrimaryConstructorParameters());

        List<PropertyDescriptor> properties = getDataProperties();
        if (!properties.isEmpty()) {
            generateDataClassToStringIfNeeded(properties);
            generateDataClassHashCodeIfNeeded(properties);
            generateDataClassEqualsIfNeeded(properties);
        }
    }

    protected abstract void generateComponentFunction(@NotNull FunctionDescriptor function, @NotNull ValueParameterDescriptor parameter);

    protected abstract void generateCopyFunction(@NotNull FunctionDescriptor function, @NotNull List<JetParameter> constructorParameters);

    protected abstract void generateToStringMethod(@NotNull FunctionDescriptor function, @NotNull List<PropertyDescriptor> properties);

    protected abstract void generateHashCodeMethod(@NotNull FunctionDescriptor function, @NotNull List<PropertyDescriptor> properties);

    protected abstract void generateEqualsMethod(@NotNull FunctionDescriptor function, @NotNull List<PropertyDescriptor> properties);

    @NotNull
    protected ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    private void generateComponentFunctionsForDataClasses() {
        ConstructorDescriptor constructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        // primary constructor should exist for data classes
        // but when generating light-classes still need to check we have one
        if (constructor == null) return;

        for (ValueParameterDescriptor parameter : constructor.getValueParameters()) {
            FunctionDescriptor function = bindingContext.get(BindingContext.DATA_CLASS_COMPONENT_FUNCTION, parameter);
            if (function != null) {
                generateComponentFunction(function, parameter);
            }
        }
    }

    private void generateCopyFunctionForDataClasses(List<JetParameter> constructorParameters) {
        FunctionDescriptor copyFunction = bindingContext.get(BindingContext.DATA_CLASS_COPY_FUNCTION, classDescriptor);
        if (copyFunction != null) {
            generateCopyFunction(copyFunction, constructorParameters);
        }
    }

    private void generateDataClassToStringIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        FunctionDescriptor function = getDeclaredMember("toString", builtIns.getString());
        if (function != null && isTrivial(function)) {
            generateToStringMethod(function, properties);
        }
    }

    private void generateDataClassHashCodeIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        FunctionDescriptor function = getDeclaredMember("hashCode", builtIns.getInt());
        if (function != null && isTrivial(function)) {
            generateHashCodeMethod(function, properties);
        }
    }

    private void generateDataClassEqualsIfNeeded(@NotNull List<PropertyDescriptor> properties) {
        FunctionDescriptor function = getDeclaredMember("equals", builtIns.getBoolean(), builtIns.getAny());
        if (function != null && isTrivial(function)) {
            generateEqualsMethod(function, properties);
        }
    }

    private List<PropertyDescriptor> getDataProperties() {
        List<PropertyDescriptor> result = Lists.newArrayList();
        for (JetParameter parameter : getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVar()) {
                result.add(bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter));
            }
        }
        return result;
    }

    @NotNull
    private List<JetParameter> getPrimaryConstructorParameters() {
        if (declaration instanceof JetClass) {
            return declaration.getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @Nullable
    private FunctionDescriptor getDeclaredMember(
            @NotNull String name,
            @NotNull ClassDescriptor returnedClassifier,
            @NotNull ClassDescriptor... valueParameterClassifiers
    ) {
        return CodegenUtil.getDeclaredFunctionByRawSignature(
                classDescriptor, Name.identifier(name), returnedClassifier, valueParameterClassifiers
        );
    }

    /**
     * @return true if the member is an inherited implementation of a method from Any
     */
    private boolean isTrivial(@NotNull FunctionDescriptor function) {
        if (function.getKind() == CallableMemberDescriptor.Kind.DECLARATION) {
            return false;
        }

        for (CallableDescriptor overridden : OverrideResolver.getOverriddenDeclarations(function)) {
            if (overridden instanceof CallableMemberDescriptor
                && ((CallableMemberDescriptor) overridden).getKind() == CallableMemberDescriptor.Kind.DECLARATION
                && !overridden.getContainingDeclaration().equals(builtIns.getAny())) {
                return false;
            }
        }

        return true;
    }
}
