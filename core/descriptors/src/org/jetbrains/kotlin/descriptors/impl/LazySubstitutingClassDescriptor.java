/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope;
import org.jetbrains.kotlin.types.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LazySubstitutingClassDescriptor implements ClassDescriptor {
    private final ClassDescriptor original;
    private final TypeSubstitutor originalSubstitutor;
    private TypeSubstitutor newSubstitutor;
    private List<TypeParameterDescriptor> typeConstructorParameters;
    private List<TypeParameterDescriptor> declaredTypeParameters;
    private TypeConstructor typeConstructor;

    public LazySubstitutingClassDescriptor(ClassDescriptor descriptor, TypeSubstitutor substitutor) {
        this.original = descriptor;
        this.originalSubstitutor = substitutor;
    }

    private TypeSubstitutor getSubstitutor() {
        if (newSubstitutor == null) {
            if (originalSubstitutor.isEmpty()) {
                newSubstitutor = originalSubstitutor;
            }
            else {
                List<TypeParameterDescriptor> originalTypeParameters = original.getTypeConstructor().getParameters();
                typeConstructorParameters = new ArrayList<TypeParameterDescriptor>(originalTypeParameters.size());
                newSubstitutor = DescriptorSubstitutor.substituteTypeParameters(
                        originalTypeParameters, originalSubstitutor.getSubstitution(), this, typeConstructorParameters
                );

                declaredTypeParameters = CollectionsKt.filter(typeConstructorParameters, new Function1<TypeParameterDescriptor, Boolean>() {
                    @Override
                    public Boolean invoke(TypeParameterDescriptor descriptor) {
                        return !descriptor.isCapturedFromOuterDeclaration();
                    }
                });
            }
        }
        return newSubstitutor;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        TypeConstructor originalTypeConstructor = original.getTypeConstructor();
        if (originalSubstitutor.isEmpty()) {
            return originalTypeConstructor;
        }

        if (typeConstructor == null) {
            TypeSubstitutor substitutor = getSubstitutor();

            Collection<KotlinType> originalSupertypes = originalTypeConstructor.getSupertypes();
            Collection<KotlinType> supertypes = new ArrayList<KotlinType>(originalSupertypes.size());
            for (KotlinType supertype : originalSupertypes) {
                supertypes.add(substitutor.substitute(supertype, Variance.INVARIANT));
            }

            typeConstructor = new ClassTypeConstructorImpl(this, originalTypeConstructor.isFinal(), typeConstructorParameters, supertypes);
        }

        return typeConstructor;
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        MemberScope memberScope = original.getMemberScope(typeArguments);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
        MemberScope memberScope = original.getMemberScope(typeSubstitution);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope() {
        MemberScope memberScope = original.getUnsubstitutedMemberScope();
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public MemberScope getStaticScope() {
        return original.getStaticScope();
    }

    @NotNull
    @Override
    public SimpleType getDefaultType() {
        List<TypeProjection> typeProjections = TypeUtils.getDefaultTypeProjections(getTypeConstructor().getParameters());
        return KotlinTypeFactory.simpleNotNullType(getAnnotations(), this, typeProjections);
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Collection<ClassConstructorDescriptor> getConstructors() {
        Collection<ClassConstructorDescriptor> originalConstructors = original.getConstructors();
        Collection<ClassConstructorDescriptor> result = new ArrayList<ClassConstructorDescriptor>(originalConstructors.size());
        for (ClassConstructorDescriptor constructor : originalConstructors) {
            ClassConstructorDescriptor copy =
                    constructor.copy(this, constructor.getModality(), constructor.getVisibility(), constructor.getKind(), false);
            result.add(copy.substitute(getSubstitutor()));
        }
        return result;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return original.getAnnotations();
    }

    @NotNull
    @Override
    public Name getName() {
        return original.getName();
    }

    @NotNull
    @Override
    public ClassDescriptor getOriginal() {
        return original.getOriginal();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return original.getContainingDeclaration();
    }

    @NotNull
    @Override
    public ClassDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;
        return new LazySubstitutingClassDescriptor(this, TypeSubstitutor.createChainedSubstitutor(substitutor.getSubstitution(), getSubstitutor().getSubstitution()));
    }

    @Override
    public ClassDescriptor getCompanionObjectDescriptor() {
        return original.getCompanionObjectDescriptor();
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return original.getKind();
    }

    @Override
    @NotNull
    public Modality getModality() {
        return original.getModality();
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return original.getVisibility();
    }

    @Override
    public boolean isInner() {
        return original.isInner();
    }

    @Override
    public boolean isData() {
        return original.isData();
    }

    @Override
    public boolean isExternal() {
        return original.isExternal();
    }

    @Override
    public boolean isCompanionObject() {
        return original.isCompanionObject();
    }

    @Override
    public boolean isHeader() {
        return original.isHeader();
    }

    @Override
    public boolean isImpl() {
        return original.isImpl();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitClassDescriptor(this, data);
    }

    @Override
    public void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedInnerClassesScope() {
        return original.getUnsubstitutedInnerClassesScope();
    }

    @Nullable
    @Override
    public ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return original.getUnsubstitutedPrimaryConstructor();
    }

    @NotNull
    @Override
    public SourceElement getSource() {
        return SourceElement.NO_SOURCE;
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        getSubstitutor();
        return declaredTypeParameters;
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getSealedSubclasses() {
        return original.getSealedSubclasses();
    }
}
