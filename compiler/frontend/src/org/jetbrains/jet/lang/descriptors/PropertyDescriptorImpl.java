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

package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverridingUtil;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.DescriptorSubstitutor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.Variance;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class PropertyDescriptorImpl extends VariableDescriptorImpl implements PropertyDescriptor {

    private final Modality modality;
    private Visibility visibility;
    private final boolean isVar;
    private final Set<PropertyDescriptor> overriddenProperties = Sets.newLinkedHashSet(); // LinkedHashSet is essential here
    private final PropertyDescriptor original;
    private final Kind kind;

    private ReceiverParameterDescriptor expectedThisObject;
    private ReceiverParameterDescriptor receiverParameter;
    private List<TypeParameterDescriptor> typeParameters;
    private PropertyGetterDescriptorImpl getter;
    private PropertySetterDescriptor setter;

    private PropertyDescriptorImpl(
            @Nullable PropertyDescriptor original,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull Kind kind
    ) {
        super(containingDeclaration, annotations, name);
        this.isVar = isVar;
        this.modality = modality;
        this.visibility = visibility;
        this.original = original == null ? this : original.getOriginal();
        this.kind = kind;
    }

    public PropertyDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @NotNull Name name,
            @NotNull Kind kind
    ) {
        this(null, containingDeclaration, annotations, modality, visibility, isVar, name, kind);
    }

    public PropertyDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Visibility visibility,
            boolean isVar,
            @Nullable JetType receiverType,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @NotNull Name name,
            @NotNull JetType outType,
            @NotNull Kind kind
    ) {
        this(containingDeclaration, annotations, modality, visibility, isVar, name, kind);
        setType(outType, Collections.<TypeParameterDescriptor>emptyList(), expectedThisObject, receiverType);
    }

    public void setType(
            @NotNull JetType outType,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @Nullable JetType receiverType
    ) {
        ReceiverParameterDescriptor receiverParameter = DescriptorResolver.resolveReceiverParameterFor(this, receiverType);
        setType(outType, typeParameters, expectedThisObject, receiverParameter);
    }

    public void setType(
            @NotNull JetType outType,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @Nullable ReceiverParameterDescriptor expectedThisObject,
            @Nullable ReceiverParameterDescriptor receiverParameter
    ) {
        setOutType(outType);

        this.typeParameters = Lists.newArrayList(typeParameters);

        this.receiverParameter = receiverParameter;
        this.expectedThisObject = expectedThisObject;
    }

    public void initialize(@Nullable PropertyGetterDescriptorImpl getter, @Nullable PropertySetterDescriptor setter) {
        this.getter = getter;
        this.setter = setter;
    }

    public void setVisibility(@NotNull Visibility visibility) {
        this.visibility = visibility;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getTypeParameters() {
        return typeParameters;
    }

    @Override
    @Nullable
    public ReceiverParameterDescriptor getReceiverParameter() {
        return receiverParameter;
    }

    @Nullable
    @Override
    public ReceiverParameterDescriptor getExpectedThisObject() {
        return expectedThisObject;
    }

    @NotNull
    @Override
    public JetType getReturnType() {
        return getType();
    }

    @Override
    public boolean isVar() {
        return isVar;
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

    @Override
    @Nullable
    public PropertyGetterDescriptorImpl getGetter() {
        return getter;
    }

    @Override
    @Nullable
    public PropertySetterDescriptor getSetter() {
        return setter;
    }

    @Override
    @NotNull
    public List<PropertyAccessorDescriptor> getAccessors() {
        List<PropertyAccessorDescriptor> r = Lists.newArrayListWithCapacity(2);
        if (getter != null) {
            r.add(getter);
        }
        if (setter != null) {
            r.add(setter);
        }
        return r;
    }

    @Override
    public PropertyDescriptor substitute(@NotNull TypeSubstitutor originalSubstitutor) {
        if (originalSubstitutor.isEmpty()) {
            return this;
        }
        return doSubstitute(originalSubstitutor, getContainingDeclaration(), modality, visibility, true, true, getKind());
    }

    private PropertyDescriptor doSubstitute(TypeSubstitutor originalSubstitutor,
            DeclarationDescriptor newOwner, Modality newModality, Visibility newVisibility, boolean preserveOriginal, boolean copyOverrides, Kind kind) {
        PropertyDescriptorImpl substitutedDescriptor = new PropertyDescriptorImpl(preserveOriginal ? getOriginal() : this, newOwner,
                getAnnotations(), newModality, newVisibility, isVar(), getName(), kind);

        List<TypeParameterDescriptor> substitutedTypeParameters = Lists.newArrayList();
        TypeSubstitutor substitutor = DescriptorSubstitutor.substituteTypeParameters(getTypeParameters(), originalSubstitutor, substitutedDescriptor, substitutedTypeParameters);

        JetType originalOutType = getType();
        JetType outType = substitutor.substitute(originalOutType, Variance.OUT_VARIANCE);
        if (outType == null) {
            return null; // TODO : tell the user that the property was projected out
        }


        ReceiverParameterDescriptor substitutedExpectedThisObject;
        ReceiverParameterDescriptor expectedThisObject = getExpectedThisObject();
        if (expectedThisObject != null) {
            substitutedExpectedThisObject = expectedThisObject.substitute(substitutor);
            if (substitutedExpectedThisObject == null) return null;
        }
        else {
            substitutedExpectedThisObject = null;
        }

        JetType substitutedReceiverType;
        if (receiverParameter != null) {
            substitutedReceiverType = substitutor.substitute(receiverParameter.getType(), Variance.IN_VARIANCE);
            if (substitutedReceiverType == null) return null;
        }
        else {
            substitutedReceiverType = null;
        }

        substitutedDescriptor.setType(outType, substitutedTypeParameters, substitutedExpectedThisObject, substitutedReceiverType);

        PropertyGetterDescriptorImpl newGetter = getter == null ? null : new PropertyGetterDescriptorImpl(
                substitutedDescriptor, Lists.newArrayList(getter.getAnnotations()),
                DescriptorUtils.convertModality(getter.getModality(), false), convertVisibility(getter.getVisibility(), newVisibility),
                getter.hasBody(), getter.isDefault(), kind, getter.getOriginal());
        if (newGetter != null) {
            JetType returnType = getter.getReturnType();
            newGetter.initialize(returnType != null ? substitutor.substitute(returnType, Variance.OUT_VARIANCE) : null);
        }
        PropertySetterDescriptorImpl newSetter = setter == null ? null : new PropertySetterDescriptorImpl(
                substitutedDescriptor, Lists.newArrayList(setter.getAnnotations()), DescriptorUtils.convertModality(setter.getModality(), false),
                convertVisibility(setter.getVisibility(), newVisibility), setter.hasBody(), setter.isDefault(), kind, setter.getOriginal());
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
                OverridingUtil.bindOverride(substitutedDescriptor, propertyDescriptor.substitute(substitutor));
            }
        }

        return substitutedDescriptor;
    }

    @NotNull
    private static Visibility convertVisibility(Visibility orig, Visibility candidate) {
        if (candidate == Visibilities.INHERITED) {
            return candidate;
        }

        Integer result = Visibilities.compare(orig, candidate);
        return result != null && result < 0 ? candidate : orig;
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

    @Override
    public void addOverriddenDescriptor(@NotNull CallableMemberDescriptor overridden) {
        overriddenProperties.add((PropertyDescriptorImpl) overridden);
    }

    @NotNull
    @Override
    public Set<? extends PropertyDescriptor> getOverriddenDescriptors() {
        return overriddenProperties;
    }

    @NotNull
    @Override
    public PropertyDescriptor copy(DeclarationDescriptor newOwner, Modality modality, Visibility visibility, Kind kind, boolean copyOverrides) {
        return doSubstitute(TypeSubstitutor.EMPTY, newOwner, modality, visibility, false, copyOverrides, kind);
    }
}
