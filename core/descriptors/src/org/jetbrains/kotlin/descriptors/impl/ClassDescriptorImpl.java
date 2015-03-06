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

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.resolve.scopes.StaticScopeForKotlinClass;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeConstructorImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class ClassDescriptorImpl extends ClassDescriptorBase {
    private final Modality modality;
    private final TypeConstructor typeConstructor;
    private final JetScope staticScope = new StaticScopeForKotlinClass(this);

    private JetScope scopeForMemberLookup;
    private Set<ConstructorDescriptor> constructors;
    private ConstructorDescriptor primaryConstructor;

    public ClassDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Modality modality,
            @NotNull Collection<JetType> supertypes,
            @NotNull SourceElement source
    ) {
        super(LockBasedStorageManager.NO_LOCKS, containingDeclaration, name, source);
        this.modality = modality;

        this.typeConstructor = TypeConstructorImpl.createForClass(this, Annotations.EMPTY, false, getName().asString(),
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
    public Annotations getAnnotations() {
        return Annotations.EMPTY;
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

    @NotNull
    @Override
    public JetScope getStaticScope() {
        return staticScope;
    }

    @Nullable
    @Override
    public ClassDescriptor getDefaultObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return ClassKind.CLASS;
    }

    @Override
    public boolean isDefaultObject() {
        return false;
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
