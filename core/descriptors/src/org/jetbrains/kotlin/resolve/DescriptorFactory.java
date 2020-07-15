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
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.*;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.Variance;

import java.util.Collections;

import static org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUES;
import static org.jetbrains.kotlin.builtins.StandardNames.ENUM_VALUE_OF;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.getDefaultConstructorVisibility;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.getBuiltIns;

public class DescriptorFactory {
    private static class DefaultClassConstructorDescriptor extends ClassConstructorDescriptorImpl {
        public DefaultClassConstructorDescriptor(
                @NotNull ClassDescriptor containingClass,
                @NotNull SourceElement source,
                boolean freedomForSealedInterfacesSupported
        ) {
            super(containingClass, null, Annotations.Companion.getEMPTY(), true, Kind.DECLARATION, source);
            initialize(Collections.<ValueParameterDescriptor>emptyList(),
                       getDefaultConstructorVisibility(containingClass, freedomForSealedInterfacesSupported));
        }
    }

    private DescriptorFactory() {
    }

    @NotNull
    public static PropertySetterDescriptorImpl createDefaultSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations
    ) {
        return createSetter(propertyDescriptor, annotations, parameterAnnotations, true, false, false, propertyDescriptor.getSource());
    }

    @NotNull
    public static PropertySetterDescriptorImpl createSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations,
            boolean isDefault,
            boolean isExternal,
            boolean isInline,
            @NotNull SourceElement sourceElement
    ) {
        return createSetter(
                propertyDescriptor, annotations, parameterAnnotations, isDefault, isExternal, isInline,
                propertyDescriptor.getVisibility(), sourceElement
        );
    }

    @NotNull
    public static PropertySetterDescriptorImpl createSetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            @NotNull Annotations parameterAnnotations,
            boolean isDefault,
            boolean isExternal,
            boolean isInline,
            @NotNull DescriptorVisibility visibility,
            @NotNull SourceElement sourceElement
    ) {
        PropertySetterDescriptorImpl setterDescriptor = new PropertySetterDescriptorImpl(
                propertyDescriptor, annotations, propertyDescriptor.getModality(), visibility, isDefault, isExternal,
                isInline, CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        );
        ValueParameterDescriptorImpl parameter =
                PropertySetterDescriptorImpl.createSetterParameter(setterDescriptor, propertyDescriptor.getType(), parameterAnnotations);
        setterDescriptor.initialize(parameter);
        return setterDescriptor;
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createDefaultGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations
    ) {
        return createGetter(propertyDescriptor, annotations, true, false, false);
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            boolean isDefault,
            boolean isExternal,
            boolean isInline
    ) {
        return createGetter(propertyDescriptor, annotations, isDefault, isExternal, isInline, propertyDescriptor.getSource());
    }

    @NotNull
    public static PropertyGetterDescriptorImpl createGetter(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations,
            boolean isDefault,
            boolean isExternal,
            boolean isInline,
            @NotNull SourceElement sourceElement
    ) {
        return new PropertyGetterDescriptorImpl(
                propertyDescriptor, annotations, propertyDescriptor.getModality(), propertyDescriptor.getVisibility(),
                isDefault, isExternal, isInline, CallableMemberDescriptor.Kind.DECLARATION, null, sourceElement
        );
    }

    @NotNull
    public static ClassConstructorDescriptorImpl createPrimaryConstructorForObject(
            @NotNull ClassDescriptor containingClass,
            @NotNull SourceElement source
    ) {
        /*
         * Language version settings are needed here only for computing default visibility of constructors of sealed classes
         *   Since object can not be sealed class it's OK to pass default settings here
         */
        return new DefaultClassConstructorDescriptor(containingClass, source, false);
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumValuesMethod(@NotNull ClassDescriptor enumClass) {
        SimpleFunctionDescriptorImpl values =
                SimpleFunctionDescriptorImpl.create(enumClass, Annotations.Companion.getEMPTY(), ENUM_VALUES,
                                                    CallableMemberDescriptor.Kind.SYNTHESIZED, enumClass.getSource());
        return values.initialize(null, null, Collections.<ReceiverParameterDescriptor>emptyList(), Collections.<TypeParameterDescriptor>emptyList(),
                                 Collections.<ValueParameterDescriptor>emptyList(),
                                 getBuiltIns(enumClass).getArrayType(Variance.INVARIANT, enumClass.getDefaultType()),
                                 Modality.FINAL, DescriptorVisibilities.PUBLIC);
    }

    @NotNull
    public static SimpleFunctionDescriptor createEnumValueOfMethod(@NotNull ClassDescriptor enumClass) {
        SimpleFunctionDescriptorImpl valueOf =
                SimpleFunctionDescriptorImpl.create(enumClass, Annotations.Companion.getEMPTY(), ENUM_VALUE_OF,
                                                    CallableMemberDescriptor.Kind.SYNTHESIZED, enumClass.getSource());
        ValueParameterDescriptor parameterDescriptor = new ValueParameterDescriptorImpl(
                valueOf, null, 0, Annotations.Companion.getEMPTY(), Name.identifier("value"), getBuiltIns(enumClass).getStringType(),
                /* declaresDefaultValue = */ false,
                /* isCrossinline = */ false,
                /* isNoinline = */ false,
                null,
                enumClass.getSource()
        );
        return valueOf.initialize(null, null, Collections.<ReceiverParameterDescriptor>emptyList(), Collections.<TypeParameterDescriptor>emptyList(),
                                  Collections.singletonList(parameterDescriptor), enumClass.getDefaultType(),
                                  Modality.FINAL, DescriptorVisibilities.PUBLIC);
    }

    public static boolean isEnumValuesMethod(@NotNull FunctionDescriptor descriptor) {
        return descriptor.getName().equals(ENUM_VALUES) && isEnumSpecialMethod(descriptor);
    }

    public static boolean isEnumValueOfMethod(@NotNull FunctionDescriptor descriptor) {
        return descriptor.getName().equals(ENUM_VALUE_OF) && isEnumSpecialMethod(descriptor);
    }

    private static boolean isEnumSpecialMethod(@NotNull FunctionDescriptor descriptor) {
        return descriptor.getKind() == CallableMemberDescriptor.Kind.SYNTHESIZED &&
               DescriptorUtils.isEnumClass(descriptor.getContainingDeclaration());
    }

    @Nullable
    public static ReceiverParameterDescriptor createExtensionReceiverParameterForCallable(
            @NotNull CallableDescriptor owner,
            @Nullable KotlinType receiverParameterType,
            @NotNull Annotations annotations
    ) {
        return receiverParameterType == null
               ? null
               : new ReceiverParameterDescriptorImpl(owner, new ExtensionReceiver(owner, receiverParameterType, null), annotations);
    }
}
