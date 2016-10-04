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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collections;

public class AccessorForPropertyDescriptor extends PropertyDescriptorImpl implements AccessorForCallableDescriptor<PropertyDescriptor> {
    private final PropertyDescriptor calleeDescriptor;
    private final ClassDescriptor superCallTarget;
    private final String nameSuffix;
    private final boolean withSyntheticGetterAccessor;
    private final boolean withSyntheticSetterAccessor;

    public AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor property,
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable ClassDescriptor superCallTarget,
            @NotNull String nameSuffix,
            boolean getterAccessorRequired,
            boolean setterAccessorRequired
    ) {
        this(property, property.getType(), DescriptorUtils.getReceiverParameterType(property.getExtensionReceiverParameter()),
             /* dispatchReceiverParameter = */
             CodegenUtilKt.isJvmStaticInObjectOrClass(property) ? null : property.getDispatchReceiverParameter(),
             containingDeclaration, superCallTarget, nameSuffix,
             getterAccessorRequired, setterAccessorRequired);
    }

    protected AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @NotNull KotlinType propertyType,
            @Nullable KotlinType receiverType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable ClassDescriptor superCallTarget,
            @NotNull String nameSuffix
    ) {
        this(original, propertyType, receiverType, dispatchReceiverParameter, containingDeclaration, superCallTarget, nameSuffix, true, true);
    }

    private AccessorForPropertyDescriptor(
            @NotNull PropertyDescriptor original,
            @NotNull KotlinType propertyType,
            @Nullable KotlinType receiverType,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull DeclarationDescriptor containingDeclaration,
            @Nullable ClassDescriptor superCallTarget,
            @NotNull String nameSuffix,
            boolean getterAccessorRequired,
            boolean setterAccessorRequired
    ) {
        super(containingDeclaration, null, Annotations.Companion.getEMPTY(), Modality.FINAL, Visibilities.LOCAL,
              original.isVar(), Name.identifier("access$" + nameSuffix),
              Kind.DECLARATION, SourceElement.NO_SOURCE, /* lateInit = */ false, /* isConst = */ false);

        this.calleeDescriptor = original;
        this.superCallTarget = superCallTarget;
        this.nameSuffix = nameSuffix;
        setType(propertyType, Collections.<TypeParameterDescriptorImpl>emptyList(), dispatchReceiverParameter, receiverType);

        this.withSyntheticGetterAccessor = getterAccessorRequired;
        this.withSyntheticSetterAccessor = setterAccessorRequired;

        PropertyGetterDescriptorImpl getterDescriptor =
                getterAccessorRequired ? new Getter(this) : (PropertyGetterDescriptorImpl) original.getGetter();
        PropertySetterDescriptor setterDescriptor =
                setterAccessorRequired ? new Setter(this) : original.getSetter();
        initialize(getterDescriptor, setterDescriptor);
    }

    public static class Getter extends PropertyGetterDescriptorImpl implements AccessorForCallableDescriptor<PropertyGetterDescriptor> {
        public Getter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.Companion.getEMPTY(), Modality.FINAL, Visibilities.LOCAL,
                  /* isDefault = */ false, /* isExternal = */ false, /* isInline = */false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initialize(property.getType());
        }

        @NotNull
        @Override
        public PropertyGetterDescriptor getCalleeDescriptor() {
            //noinspection ConstantConditions
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getCalleeDescriptor().getGetter();
        }

        @Override
        @Nullable
        public ClassDescriptor getSuperCallTarget() {
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getSuperCallTarget();
        }

    }

    public static class Setter extends PropertySetterDescriptorImpl implements AccessorForCallableDescriptor<PropertySetterDescriptor>{
        public Setter(AccessorForPropertyDescriptor property) {
            super(property, Annotations.Companion.getEMPTY(), Modality.FINAL, Visibilities.LOCAL,
                  /* isDefault = */ false, /* isExternal = */ false, /* isInline = */ false, Kind.DECLARATION, null, SourceElement.NO_SOURCE);
            initializeDefault();
        }

        @NotNull
        @Override
        public PropertySetterDescriptor getCalleeDescriptor() {
            //noinspection ConstantConditions
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getCalleeDescriptor().getSetter();
        }

        @Override
        @Nullable
        public ClassDescriptor getSuperCallTarget() {
            return ((AccessorForPropertyDescriptor) getCorrespondingProperty()).getSuperCallTarget();
        }
    }

    @NotNull
    @Override
    public PropertyDescriptor getCalleeDescriptor() {
        return calleeDescriptor;
    }

    @Override
    public ClassDescriptor getSuperCallTarget() {
        return superCallTarget;
    }

    @NotNull
    public String getAccessorSuffix() {
        return nameSuffix;
    }

    public boolean isWithSyntheticGetterAccessor() {
        return withSyntheticGetterAccessor;
    }

    public boolean isWithSyntheticSetterAccessor() {
        return withSyntheticSetterAccessor;
    }
}
