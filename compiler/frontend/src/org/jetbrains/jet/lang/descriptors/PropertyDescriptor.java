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

package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.jet.lang.types.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor.NO_RECEIVER;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends VariableDescriptorImpl implements CallableMemberDescriptor {

    private final Modality modality;
    private final Visibility visibility;
    private final boolean isVar;
    private final boolean isObject;
    private final Set<PropertyDescriptor> overriddenProperties = Sets.newLinkedHashSet();
    private final PropertyDescriptor original;
    private final Kind kind;

    private ReceiverDescriptor expectedThisObject;
    private ReceiverDescriptor receiver;
    private List<TypeParameterDescriptor> typeParemeters;
    private PropertyGetterDescriptor getter;
    private PropertySetterDescriptor setter;
    
    private PropertyDescriptor() {
        super(ErrorUtils.getErrorClass(), Collections.<AnnotationDescriptor>emptyList(), "dummy");
        this.modality = null;
        this.visibility = null;
        this.isVar = false;
        this.isObject = false;
        this.original = null;
        this.kind = Kind.DECLARATION;
    }

    private PropertyDescriptor(
            @Nullable PropertyDescriptor original,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            boolean isObject,
            @NotNull String name,
            Kind kind) {
        super(containingDeclaration, annotations, name);
        this.isVar = isVar;
        this.isObject = isObject;
        this.modality = modality;
        this.visibility = visibility;
        this.original = original == null ? this : original.getOriginal();
        this.kind = kind;
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            boolean isObject,
            @NotNull String name,
            Kind kind) {
        this(null, containingDeclaration, annotations, modality, visibility, isVar, isObject, name, kind);
    }

    public PropertyDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            boolean isObject,
            @Nullable JetType receiverType,
            @NotNull ReceiverDescriptor expectedThisObject,
            @NotNull String name,
            @NotNull JetType outType,
            Kind kind
        ) {
        this(containingDeclaration, annotations, modality, visibility, isVar, isObject, name, kind);
        setType(outType, Collections.<TypeParameterDescriptor>emptyList(), expectedThisObject, receiverType);
    }

    public void setType(@NotNull JetType outType, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor expectedThisObject, @Nullable JetType receiverType) {
        ReceiverDescriptor receiver = receiverType == null
                ? NO_RECEIVER
                : new ExtensionReceiver(this, receiverType);
        setType(outType, typeParameters, expectedThisObject, receiver);
    }

    public void setType(@NotNull JetType outType, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor expectedThisObject, @NotNull ReceiverDescriptor receiver) {
        setOutType(outType);

        this.typeParemeters = typeParameters;

        this.receiver = receiver;
        this.expectedThisObject = expectedThisObject;
    }

    public void initialize(@Nullable PropertyGetterDescriptor getter, @Nullable PropertySetterDescriptor setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParemeters;
    }

    @NotNull
    public ReceiverDescriptor getReceiverParameter() {
        return receiver;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getExpectedThisObject() {
        return expectedThisObject;
    }

    @Override
    public JetType getReturnType() {
        return getOutType();
    }

    public boolean isVar() {
        return isVar;
    }

    @Override
    public boolean isObjectDeclaration() {
        return isObject;
    }

    @NotNull
    @Override
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Nullable
    public PropertyGetterDescriptor getGetter() {
        return getter;
    }

    @Nullable
    public PropertySetterDescriptor getSetter() {
        return setter;
    }

    @Override
    public PropertyDescriptor substitute(TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        return doSubstitute(originalSubstitutor, getContainingDeclaration(), modality, true, true, getKind());
    }

    private PropertyDescriptor doSubstitute(TypeSubstitutor originalSubstitutor,
            DeclarationDescriptor newOwner, Modality newModality, boolean preserveOriginal, boolean copyOverrides, Kind kind) {
        PropertyDescriptor substitutedDescriptor = new PropertyDescriptor(preserveOriginal ? getOriginal() : this, newOwner,
                getAnnotations(), newModality, getVisibility(), isVar(), isObjectDeclaration(), getName(), kind);

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType originalOutType = getOutType();
        JetType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }

        ReceiverDescriptor substitutedExpectedThisObject;
        if (expectedThisObject.exists()) {
            JetType substitutedExpectedThisObjectType = substitutor.substitute(getExpectedThisObject().getType(), Variance.INVARIANT);
            substitutedExpectedThisObject = new TransientReceiver(substitutedExpectedThisObjectType);
        }
        else {
            substitutedExpectedThisObject = NO_RECEIVER;
        }

        JetType substitutedReceiverType;
        if (receiver.exists()) {
            substitutedReceiverType = substitutor.substitute(receiver.getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverType == null) return null;
        }
        else {
            substitutedReceiverType = null;
        }

        substitutedDescriptor.setType(outType, substitutedTypeParameters, substitutedExpectedThisObject, substitutedReceiverType);

        PropertyGetterDescriptor newGetter = getter == null ? null : new PropertyGetterDescriptor(
                substitutedDescriptor, Lists.newArrayList(getter.getAnnotations()),
                DescriptorUtils.convertModality(getter.getModality(), false), getter.getVisibility(),
                getter.hasBody(), getter.isDefault(), kind);
        if (newGetter != null) {
            JetType returnType = getter.getReturnType();
            newGetter.initialize(returnType != null ? substitutor.substitute(returnType, Variance.OUT_VARIANCE) : null);
        }
        PropertySetterDescriptor newSetter = setter == null ? null : new PropertySetterDescriptor(
                substitutedDescriptor, Lists.newArrayList(setter.getAnnotations()), DescriptorUtils.convertModality(setter.getModality(), false), setter.getVisibility(),
                setter.hasBody(), setter.isDefault(), kind);
        if (newSetter != null) {
            List<ValueParameterDescriptor> substitutedValueParameters = FunctionDescriptorUtil.getSubstitutedValueParameters(newSetter, setter, substitutor);
            if (substitutedValueParameters == null) {
                return null;
            }
            if (substitutedValueParameters.size() != 1) {
                throw new IllegalStateException();
            }
            newSetter.initialize(substitutedValueParameters.get(0));
        }

        substitutedDescriptor.initialize(newGetter, newSetter);

        if (copyOverrides) {
            for (PropertyDescriptor propertyDescriptor : overriddenProperties) {
                substitutedDescriptor.addOverriddenDescriptor(propertyDescriptor.substitute(substitutor));
            }
        }

        return substitutedDescriptor;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }

    @NotNull
    @Override
    public PropertyDescriptor getOriginal() {
        return original;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public void addOverriddenDescriptor(PropertyDescriptor overridden) {
        overriddenProperties.add(overridden);
    }

    @NotNull
    @Override
    public Set<? extends PropertyDescriptor> getOverriddenDescriptors() {
        return overriddenProperties;
    }

    @NotNull
    @Override
    public PropertyDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        return doSubstitute(TypeSubstitutor.EMPTY, newOwner, DescriptorUtils.convertModality(modality, makeNonAbstract), false, copyOverrides, kind);
    }
    
    public static PropertyDescriptor createDummy() {
        return new PropertyDescriptor();
    }
}
