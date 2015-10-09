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
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase;
import org.jetbrains.kotlin.descriptors.impl.ConstructorDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeConstructor;
import org.jetbrains.kotlin.types.TypeConstructorImpl;
import org.jetbrains.kotlin.types.TypeUtils;

import java.util.*;

public class MutableClassDescriptor extends ClassDescriptorBase implements ClassDescriptor {
    private final ClassKind kind;
    private final boolean isInner;

    private Modality modality;
    private Visibility visibility;
    private TypeConstructor typeConstructor;
    private List<TypeParameterDescriptor> typeParameters;
    private final Collection<JetType> supertypes = new ArrayList<JetType>();

    public MutableClassDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ClassKind kind,
            boolean isInner,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        super(LockBasedStorageManager.NO_LOCKS, containingDeclaration, name, source);
        assert kind != ClassKind.OBJECT : "Fix isCompanionObject()";

        this.kind = kind;
        this.isInner = isInner;
    }

    @Nullable
    @Override
    public ClassDescriptor getCompanionObjectDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        return Annotations.EMPTY;
    }

    public void setModality(@NotNull Modality modality) {
        this.modality = modality;
    }

    @Override
    @NotNull
    public Modality getModality() {
        return modality;
    }

    @NotNull
    @Override
    public ClassKind getKind() {
        return kind;
    }

    public void setVisibility(@NotNull Visibility visibility) {
        this.visibility = visibility;
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public boolean isInner() {
        return isInner;
    }

    @Override
    public boolean isCompanionObject() {
        return false;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    public void addSupertype(@NotNull JetType supertype) {
        assert !supertype.isError() : "Error types must be filtered out in DescriptorResolver";
        if (TypeUtils.getClassDescriptor(supertype) != null) {
            // See the Errors.SUPERTYPE_NOT_A_CLASS_OR_INTERFACE
            supertypes.add(supertype);
        }
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return Collections.emptySet();
    }

    @Override
    @Nullable
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return null;
    }

    public void setTypeParameterDescriptors(@NotNull List<TypeParameterDescriptor> typeParameters) {
        if (this.typeParameters != null) {
            throw new IllegalStateException("Type parameters are already set for " + getName());
        }
        this.typeParameters = new ArrayList<TypeParameterDescriptor>(typeParameters);
    }

    public void createTypeConstructor() {
        assert typeConstructor == null : typeConstructor;
        this.typeConstructor = TypeConstructorImpl.createForClass(
                this,
                Annotations.EMPTY,
                !getModality().isOverridable(),
                getName().asString(),
                typeParameters,
                supertypes
        );
        for (FunctionDescriptor functionDescriptor : getConstructors()) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
    }

    @Override
    @NotNull
    public JetScope getUnsubstitutedMemberScope() {
        return JetScope.Empty.INSTANCE$; // used for getDefaultType
    }

    @NotNull
    @Override
    public JetScope getStaticScope() {
        return JetScope.Empty.INSTANCE$;
    }

    @Override
    public String toString() {
        return DeclarationDescriptorImpl.toString(this);
    }
}
