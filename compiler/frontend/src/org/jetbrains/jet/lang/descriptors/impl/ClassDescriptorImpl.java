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

package org.jetbrains.jet.lang.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.SubstitutingScope;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassDescriptorImpl extends DeclarationDescriptorNonRootImpl implements ClassDescriptor {
    private TypeConstructor typeConstructor;

    private JetScope memberDeclarations;
    private Set<ConstructorDescriptor> constructors;
    private ConstructorDescriptor primaryConstructor;
    private ReceiverParameterDescriptor thisAsReceiverParameter;
    private final Modality modality;
    private ClassDescriptor classObjectDescriptor;
    private final ClassKind kind;
    private boolean isInner;

    public ClassDescriptorImpl(
        @NotNull DeclarationDescriptor containingDeclaration,
        @NotNull List<AnnotationDescriptor> annotations,
        @NotNull Modality modality,
        @NotNull Name name
    ) {
        this(containingDeclaration, ClassKind.CLASS, annotations, modality, name);
    }

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ClassKind kind,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull Modality modality,
            @NotNull Name name) {
        super(containingDeclaration, annotations, name);
        this.kind = kind;
        this.modality = modality;
    }


    public final ClassDescriptorImpl initialize(
            boolean sealed,
            @NotNull List<? extends TypeParameterDescriptor> typeParameters,
            @NotNull Collection<JetType> supertypes,
            @NotNull JetScope memberDeclarations,
            @NotNull Set<ConstructorDescriptor> constructors,
            @Nullable ConstructorDescriptor primaryConstructor,
            boolean isInner
    ) {
        this.typeConstructor = new TypeConstructorImpl(this, getAnnotations(), sealed, getName().getName(), typeParameters, supertypes);
        this.memberDeclarations = memberDeclarations;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
        this.isInner = isInner;
        return this;
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor primaryConstructor) {
        this.primaryConstructor = primaryConstructor;
    }

    public void setClassObjectDescriptor(@NotNull ClassDescriptor classObjectDescriptor) {
        this.classObjectDescriptor = classObjectDescriptor;
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    @NotNull
    public JetScope getMemberScope(List<TypeProjection> typeArguments) {
        assert typeArguments.size() == typeConstructor.getParameters().size() : typeArguments;
        if (typeConstructor.getParameters().isEmpty()) {
            return  memberDeclarations;
        }
        Map<TypeConstructor, TypeProjection> substitutionContext = SubstitutionUtils
                .buildSubstitutionContext(typeConstructor.getParameters(), typeArguments);
        return new SubstitutingScope(memberDeclarations, TypeSubstitutor.create(substitutionContext));
    }

    @NotNull
    @Override
    public JetType getDefaultType() {
        return TypeUtils.makeUnsubstitutedType(this, memberDeclarations);
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public JetType getClassObjectType() {
        return getClassObjectDescriptor().getDefaultType();
    }

    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return classObjectDescriptor;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @Override
    public boolean isClassObjectAValue() {
        return true;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @Override
    @NotNull
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return Visibilities.PUBLIC;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        if (thisAsReceiverParameter == null) {
            thisAsReceiverParameter = DescriptorResolver.createLazyReceiverParameterDescriptor(this);
        }
        return thisAsReceiverParameter;
    }

    @NotNull
    @Override
    public JetScope getUnsubstitutedInnerClassesScope() {
        return JetScope.EMPTY;
    }
}
