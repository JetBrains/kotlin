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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getDefaultConstructorVisibility;

public class DescriptorFactory {
    public static final Name VALUE_OF_METHOD_NAME = Name.identifier("valueOf");
    public static final Name VALUES_METHOD_NAME = Name.identifier("values");

    private DescriptorFactory() {
    }

    @NotNull
    public static PropertySetterDescriptorImpl createDefaultSetter(@NotNull PropertyDescriptor propertyDescriptor) {
        return createSetter(propertyDescriptor, true);
    }

    @NotNull
    public static PropertySetterDescriptorImpl createSetter(@NotNull PropertyDescriptor propertyDescriptor, boolean isDefault) {
        PropertySetterDescriptorImpl setterDescriptor = new PropertySetterDescriptorImpl(
                propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), propertyDescriptor.getModality(),
                propertyDescriptor.getVisibility(),
                !isDefault, isDefault, CallableMemberDescriptor.Kind.DECLARATION);
        setterDescriptor.initializeDefault();
        return setterDescriptor;
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createDefaultGetter(@NotNull PropertyDescriptor propertyDescriptor) {
        return createGetter(propertyDescriptor, true);
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createGetter(@NotNull PropertyDescriptor propertyDescriptor, boolean isDefault) {
        return new PropertyGetterDescriptorImpl(
                propertyDescriptor, Collections.<AnnotationDescriptor>emptyList(), propertyDescriptor.getModality(),
                propertyDescriptor.getVisibility(),
                !isDefault, isDefault, CallableMemberDescriptor.Kind.DECLARATION);
    }

    @NotNull
    public static ConstructorDescriptorImpl createPrimaryConstructorForObject(@NotNull ClassDescriptor containingClass) {
        ConstructorDescriptorImpl constructorDescriptor =
                new ConstructorDescriptorImpl(containingClass, Collections.<AnnotationDescriptor>emptyList(), true);
        constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(),
                                         Collections.<ValueParameterDescriptor>emptyList(),
                                         getDefaultConstructorVisibility(containingClass));
        return constructorDescriptor;
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValuesMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull JetType returnType
    ) {
        SimpleFunctionDescriptorImpl values =
                new SimpleFunctionDescriptorImpl(classObject, Collections.<AnnotationDescriptor>emptyList(), VALUES_METHOD_NAME,
                                                 CallableMemberDescriptor.Kind.SYNTHESIZED);
        return values.initialize(null, classObject.getThisAsReceiverParameter(), Collections.<TypeParameterDescriptor>emptyList(),
                                 Collections.<ValueParameterDescriptor>emptyList(),
                                 returnType, Modality.FINAL,
                                 Visibilities.PUBLIC, false);
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValueOfMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull JetType returnType
    ) {
        SimpleFunctionDescriptorImpl values =
                new SimpleFunctionDescriptorImpl(classObject, Collections.<AnnotationDescriptor>emptyList(), VALUE_OF_METHOD_NAME,
                                                 CallableMemberDescriptor.Kind.SYNTHESIZED);
        ValueParameterDescriptor parameterDescriptor = new ValueParameterDescriptorImpl(
                values,
                0,
                Collections.<AnnotationDescriptor>emptyList(),
                Name.identifier("value"),
                KotlinBuiltIns.getInstance().getStringType(),
                false,
                null);
        return values.initialize(null, classObject.getThisAsReceiverParameter(),
                                 Collections.<TypeParameterDescriptor>emptyList(),
                                 Collections.singletonList(parameterDescriptor),
                                 returnType, Modality.FINAL,
                                 Visibilities.PUBLIC, false);
    }

    @Nullable
    public static ReceiverParameterDescriptor createReceiverParameterForCallable(
            @NotNull CallableDescriptor owner,
            @Nullable JetType receiverParameterType
    ) {
        return receiverParameterType == null
               ? NO_RECEIVER_PARAMETER
               : new ReceiverParameterDescriptorImpl(owner, receiverParameterType, new ExtensionReceiver(owner, receiverParameterType));
    }

    @NotNull
    public static ReceiverParameterDescriptor createLazyReceiverParameterDescriptor(@NotNull final ClassDescriptor classDescriptor) {
        return new AbstractReceiverParameterDescriptor() {
            private ClassReceiver value;

            @NotNull
            @Override
            public JetType getType() {
                // This must be lazy, thus the inner class
                return classDescriptor.getDefaultType();
            }

            @NotNull
            @Override
            public ReceiverValue getValue() {
                if (value == null) {
                    value = new ClassReceiver(classDescriptor);
                }
                return value;
            }

            @NotNull
            @Override
            public DeclarationDescriptor getContainingDeclaration() {
                return classDescriptor;
            }

            @Override
            public String toString() {
                return "class " + classDescriptor.getName() + "::this";
            }
        };
    }
}
