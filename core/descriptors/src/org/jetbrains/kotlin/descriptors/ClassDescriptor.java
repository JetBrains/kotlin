/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors;

import kotlin.annotations.jvm.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.kotlin.types.TypeProjection;
import org.jetbrains.kotlin.types.TypeSubstitution;

import java.util.Collection;
import java.util.List;

public interface ClassDescriptor extends ClassifierDescriptorWithTypeParameters, ClassOrPackageFragmentDescriptor {
    @NotNull
    MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments);
    @NotNull
    MemberScope getMemberScope(@NotNull List<? extends TypeProjection> typeArguments, @NotNull ModuleDescriptor moduleDescriptor);

    @NotNull
    MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution);
    @NotNull
    MemberScope getMemberScope(@NotNull TypeSubstitution typeSubstitution, @NotNull ModuleDescriptor moduleDescriptor);

    @NotNull
    MemberScope getUnsubstitutedMemberScope();

    @NotNull
    MemberScope getUnsubstitutedMemberScope(@NotNull ModuleDescriptor moduleDescriptor);

    @NotNull
    MemberScope getUnsubstitutedInnerClassesScope();

    @NotNull
    MemberScope getStaticScope();

    @NotNull
    @ReadOnly
    Collection<ClassConstructorDescriptor> getConstructors();

    @Override
    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    /**
     * @return type A&lt;T&gt; for the class A&lt;T&gt;
     */
    @NotNull
    @Override
    SimpleType getDefaultType();

    /**
     * @return nested object declared as 'companion' if one is present.
     */
    @Nullable
    ClassDescriptor getCompanionObjectDescriptor();

    @NotNull
    ClassKind getKind();

    @Override
    @NotNull
    Modality getModality();

    @Override
    @NotNull
    Visibility getVisibility();

    boolean isCompanionObject();

    boolean isData();

    boolean isInline();

    @NotNull
    ReceiverParameterDescriptor getThisAsReceiverParameter();

    @Nullable
    ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor();

    /**
     * It may differ from 'typeConstructor.parameters' in current class is inner, 'typeConstructor.parameters' contains
     * captured parameters from outer declaration.
     * @return list of type parameters actually declared type parameters in current class
     */
    @Override
    @ReadOnly
    @NotNull
    List<TypeParameterDescriptor> getDeclaredTypeParameters();

    /**
     * @return direct subclasses of this class if it's a sealed class, empty list otherwise
     */
    @ReadOnly
    @NotNull
    Collection<ClassDescriptor> getSealedSubclasses();

    @NotNull
    @Override
    ClassDescriptor getOriginal();
}
