/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.types.ClassTypeConstructorImpl;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDescriptorImpl extends ClassDescriptorBase {
    private final Modality modality;
    private final ClassKind kind;
    private final TypeConstructor typeConstructor;

    private MemberScope unsubstitutedMemberScope;
    private Set<ClassConstructorDescriptor> constructors;
    private ClassConstructorDescriptor primaryConstructor;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Modality modality,
            @NotNull ClassKind kind,
            @NotNull Collection<KotlinType> supertypes,
            @NotNull SourceElement source,
            boolean isExternal,
            @NotNull StorageManager storageManager
    ) {
        super(storageManager, containingDeclaration, name, source, isExternal);
        assert modality != Modality.SEALED : "Implement getSealedSubclasses() for this class: " + getClass();
        this.modality = modality;
        this.kind = kind;

        this.typeConstructor = new ClassTypeConstructorImpl(this, Collections.<TypeParameterDescriptor>emptyList(), supertypes, storageManager);
    }

    public final void initialize(
            @NotNull MemberScope unsubstitutedMemberScope,
            @NotNull Set<ClassConstructorDescriptor> constructors,
            @Nullable ClassConstructorDescriptor primaryConstructor
    ) {
        this.unsubstitutedMemberScope = unsubstitutedMemberScope;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return Annotations.Companion.getEMPTY();
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @NotNull
    @Override
    public Collection<ClassConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @NotNull
    @Override
    public MemberScope getUnsubstitutedMemberScope(@NotNull ModuleDescriptor moduleDescriptor) {
        return unsubstitutedMemberScope;
    }

    @NotNull
    @Override
    public MemberScope getStaticScope() {
        return MemberScope.Empty.INSTANCE;
    }

    @Nullable
    @Override
    public ClassDescriptor getCompanionObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    @Override
    public boolean isCompanionObject() {
        return false;
    }

    @Override
    public boolean isExpect() {
        return false;
    }

    @Override
    public boolean isActual() {
        return false;
    }

    @Override
    public ClassConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
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
    public boolean isData() {
        return false;
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public boolean isInner() {
        return false;
    }

    @Override
    public String toString() {
        return "class " + getName();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getDeclaredTypeParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<ClassDescriptor> getSealedSubclasses() {
        return Collections.emptyList();
    }
}
