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
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;
import org.jetbrains.jet.lang.types.TypeConstructorImpl;
import org.jetbrains.jet.storage.LockBasedStorageManager;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ClassDescriptorImpl extends ClassDescriptorBase {
    private final Modality modality;
    private final TypeConstructor typeConstructor;

    private JetScope scopeForMemberLookup;
    private Set<ConstructorDescriptor> constructors;
    private ConstructorDescriptor primaryConstructor;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Modality modality,
            @NotNull Collection<JetType> supertypes
    ) {
        super(LockBasedStorageManager.NO_LOCKS, containingDeclaration, name);
        this.modality = modality;

        this.typeConstructor = new TypeConstructorImpl(this, Collections.<AnnotationDescriptor>emptyList(), false, getName().asString(),
                                                       Collections.<TypeParameterDescriptor>emptyList(), supertypes);
    }

    public final void initialize(
            @NotNull JetScope scopeForMemberLookup,
            @NotNull Set<ConstructorDescriptor> constructors,
            @Nullable ConstructorDescriptor primaryConstructor
    ) {
        this.scopeForMemberLookup = scopeForMemberLookup;
        this.constructors = constructors;
        this.primaryConstructor = primaryConstructor;
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor primaryConstructor) {
        this.primaryConstructor = primaryConstructor;
    }

    @NotNull
    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @NotNull
    @Override
    public Collection<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberLookup() {
        return scopeForMemberLookup;
    }

    @Nullable
    @Override
    public ClassDescriptor getClassObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return ClassKind.CLASS;
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
        return false;
    }

    @Override
    public String toString() {
        return "class " + getName();
    }
}
