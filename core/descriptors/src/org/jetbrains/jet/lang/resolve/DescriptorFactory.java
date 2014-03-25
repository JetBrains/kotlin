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
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collections;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getDefaultConstructorVisibility;

public class DescriptorFactory {
    public static final Name VALUE_OF_METHOD_NAME = Name.identifier("valueOf");
    public static final Name VALUES_METHOD_NAME = Name.identifier("values");

    private static class DefaultConstructorDescriptor extends ConstructorDescriptorImpl {
        public DefaultConstructorDescriptor(@NotNull ClassDescriptor containingClass) {
            super(containingClass, null, Annotations.EMPTY, true, Kind.DECLARATION);
            initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList(),
                       getDefaultConstructorVisibility(containingClass), true);
        }
    }

    private DescriptorFactory() {
    }

    @NotNull
    public static PropertySetterDescriptorImpl createDefaultSetter(@NotNull PropertyDescriptor propertyDescriptor) {
        return createSetter(propertyDescriptor, true);
    }

    @NotNull
    public static PropertySetterDescriptorImpl createSetter(@NotNull PropertyDescriptor propertyDescriptor, boolean isDefault) {
        PropertySetterDescriptorImpl setterDescriptor = new PropertySetterDescriptorImpl(
                propertyDescriptor, Annotations.EMPTY, propertyDescriptor.getModality(),
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
                propertyDescriptor, Annotations.EMPTY, propertyDescriptor.getModality(),
                propertyDescriptor.getVisibility(),
                !isDefault, isDefault, CallableMemberDescriptor.Kind.DECLARATION);
    }

    @NotNull
    public static ConstructorDescriptorImpl createPrimaryConstructorForObject(@NotNull ClassDescriptor containingClass) {
        return new DefaultConstructorDescriptor(containingClass);
    }

    public static boolean isDefaultPrimaryConstructor(@NotNull ConstructorDescriptor constructor) {
        return constructor instanceof DefaultConstructorDescriptor;
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValuesMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull JetType returnType
    ) {
        SimpleFunctionDescriptorImpl values =
                SimpleFunctionDescriptorImpl.create(classObject, Annotations.EMPTY, VALUES_METHOD_NAME,
                                                    CallableMemberDescriptor.Kind.SYNTHESIZED);
        return values.initialize(null, classObject.getThisAsReceiverParameter(), Collections.<TypeParameterDescriptor>emptyList(),
                                 Collections.<ValueParameterDescriptor>emptyList(),
                                 returnType, Modality.FINAL,
                                 Visibilities.PUBLIC);
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumClassObjectValueOfMethod(
            @NotNull ClassDescriptor classObject,
            @NotNull JetType returnType
    ) {
        SimpleFunctionDescriptorImpl values =
                SimpleFunctionDescriptorImpl.create(classObject, Annotations.EMPTY, VALUE_OF_METHOD_NAME,
                                                    CallableMemberDescriptor.Kind.SYNTHESIZED);
        ValueParameterDescriptor parameterDescriptor = new ValueParameterDescriptorImpl(
                values,
                0,
                Annotations.EMPTY,
                Name.identifier("value"),
                KotlinBuiltIns.getInstance().getStringType(),
                false,
                null);
        return values.initialize(null, classObject.getThisAsReceiverParameter(),
                                 Collections.<TypeParameterDescriptor>emptyList(),
                                 Collections.singletonList(parameterDescriptor),
                                 returnType, Modality.FINAL,
                                 Visibilities.PUBLIC);
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
}
