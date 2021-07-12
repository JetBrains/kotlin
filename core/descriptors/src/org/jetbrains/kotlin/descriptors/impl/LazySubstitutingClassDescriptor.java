/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.impl;

import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.SubstitutingScope;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LazySubstitutingClassDescriptor extends ModuleAwareClassDescriptor {
    private final ModuleAwareClassDescriptor original;
    private final TypeSubstitutor originalSubstitutor;
    private TypeSubstitutor newSubstitutor;
    private List<TypeParameterDescriptor> typeConstructorParameters;
    private List<TypeParameterDescriptor> declaredTypeParameters;
    private TypeConstructor typeConstructor;

    public LazySubstitutingClassDescriptor(ModuleAwareClassDescriptor descriptor, TypeSubstitutor substitutor) {
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

            typeConstructor = new ClassTypeConstructorImpl(this, typeConstructorParameters, supertypes, LockBasedStorageManager.NO_LOCKS);
        }

        return typeConstructor;
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments, @NotNull KotlinTypeRefiner kotlinTypeRefiner) {
        MemberScope memberScope = original.getMemberScope(typeArguments, kotlinTypeRefiner);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution, @NotNull KotlinTypeRefiner kotlinTypeRefiner) {
        MemberScope memberScope = original.getMemberScope(typeSubstitution, kotlinTypeRefiner);
        if (originalSubstitutor.isEmpty()) {
            return memberScope;
        }
        return new SubstitutingScope(memberScope, getSubstitutor());
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments) {
        return getMemberScope(typeArguments, DescriptorUtilsKt.getKotlinTypeRefiner(DescriptorUtils.getContainingModule(this)));
    }

    @NotNull
    @Override
    public MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution) {
        return getMemberScope(typeSubstitution, DescriptorUtilsKt.getKotlinTypeRefiner(DescriptorUtils.getContainingModule(this)));
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope() {
        return getUnsubstitutedMemberScope(DescriptorUtilsKt.getKotlinTypeRefiner(DescriptorUtils.getContainingModule(original)));
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull KotlinTypeRefiner kotlinTypeRefiner) {
        MemberScope memberScope = original.getUnsubstitutedMemberScope(kotlinTypeRefiner);
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
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
                getAnnotations(),
                getTypeConstructor(),
                typeProjections,
                false,
                getUnsubstitutedMemberScope()
        );
    }

    @NotNull
    @Override
    public ReceiverParameterDescriptor getThisAsReceiverParameter() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getContextReceivers() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ClassConstructorDescriptor> getConstructors() {
        Collection<ClassConstructorDescriptor> originalConstructors = original.getConstructors();
        Collection<ClassConstructorDescriptor> result = new ArrayList<ClassConstructorDescriptor>(originalConstructors.size());
        for (ClassConstructorDescriptor constructor : originalConstructors) {
            ClassConstructorDescriptor copy = (ClassConstructorDescriptor) constructor.newCopyBuilder()
                    .setOriginal(constructor.getOriginal())
                    .setModality(constructor.getModality())
                    .setVisibility(constructor.getVisibility())
                    .setKind(constructor.getKind())
                    .setCopyOverrides(false)
                    .build();
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
    public DescriptorVisibility getVisibility() {
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
    public boolean isInline() {
        return original.isInline();
    }

    @Override
    public boolean isFun() {
        return original.isFun();
    }

    @Override
    public boolean isValue() {
        return original.isValue();
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
    public boolean isExpect() {
        return original.isExpect();
    }

    @Override
    public boolean isActual() {
        return original.isActual();
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

    @Nullable
    @Override
    public InlineClassRepresentation<SimpleType> getInlineClassRepresentation() {
        InlineClassRepresentation<SimpleType> representation = original.getInlineClassRepresentation();
        //noinspection ConstantConditions
        return representation == null ? null : new InlineClassRepresentation<SimpleType>(
                representation.getUnderlyingPropertyName(),
                substituteSimpleType(getInlineClassRepresentation().getUnderlyingType())
        );
    }

    @Nullable
    @Override
    public SimpleType getDefaultFunctionTypeForSamInterface() {
        return substituteSimpleType(original.getDefaultFunctionTypeForSamInterface());
    }

    @Nullable
    private SimpleType substituteSimpleType(@Nullable SimpleType type) {
        if (type == null || originalSubstitutor.isEmpty()) return type;

        TypeSubstitutor substitutor = getSubstitutor();
        KotlinType substitutedType = substitutor.substitute(type, Variance.INVARIANT);

        assert substitutedType instanceof SimpleType :
                "Substitution for SimpleType should also be a SimpleType, but it is " + substitutedType + "\n" +
                "Unsubstituted: " + type;

        return (SimpleType) substitutedType;
    }

    @Override
    public boolean isDefinitelyNotSamInterface() {
        return original.isDefinitelyNotSamInterface();
    }
}
