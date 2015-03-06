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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MutableClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes {
    private final ClassKind kind;
    private final boolean isInner;

    private Annotations annotations;
    private Modality modality;
    private Visibility visibility;
    private TypeConstructor typeConstructor;
    private List<TypeParameterDescriptor> typeParameters;
    private Collection<JetType> supertypes = new ArrayList<JetType>();

    private MutableClassDescriptor defaultObjectDescriptor;

    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
    private ConstructorDescriptor primaryConstructor;

    private final Set<CallableMemberDescriptor> declaredCallableMembers = Sets.newLinkedHashSet();
    private final Set<CallableMemberDescriptor> allCallableMembers = Sets.newLinkedHashSet(); // includes fake overrides
    private final Set<PropertyDescriptor> properties = Sets.newLinkedHashSet();
    private final Set<SimpleFunctionDescriptor> functions = Sets.newLinkedHashSet();

    private final WritableScope scopeForMemberResolution;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private WritableScope scopeForInitializers; //contains members + primary constructor value parameters + map for backing fields
    private JetScope scopeForMemberLookup;
    private final JetScope staticScope = new StaticScopeForKotlinClass(this);

    public MutableClassDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull JetScope outerScope,
            @NotNull ClassKind kind,
            boolean isInner,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        super(LockBasedStorageManager.NO_LOCKS, containingDeclaration, name, source);
        assert kind != ClassKind.OBJECT : "Fix isDefaultObject()";

        this.kind = kind;
        this.isInner = isInner;

        RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

        setScopeForMemberLookup(new WritableScopeImpl(JetScope.Empty.INSTANCE$, this, redeclarationHandler, "MemberLookup")
                                        .changeLockLevel(WritableScope.LockLevel.BOTH));
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler, "SupertypeResolution")
                .changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler, "MemberResolution")
                .changeLockLevel(WritableScope.LockLevel.BOTH);

        if (kind == ClassKind.TRAIT) {
            setUpScopeForInitializers(this);
        }

        scopeForMemberResolution.importScope(staticScope);
        scopeForMemberResolution.addLabeledDeclaration(this);
    }

    @Nullable
    @Override
    public MutableClassDescriptor getDefaultObjectDescriptor() {
        return defaultObjectDescriptor;
    }

    @NotNull
    @Override
    public Annotations getAnnotations() {
        if (annotations == null) {
            annotations = new AnnotationsImpl(new ArrayList<AnnotationDescriptor>(0));
        }
        return annotations;
    }

    public void addAnnotations(@NotNull Iterable<AnnotationDescriptor> annotationsToAdd) {
        List<AnnotationDescriptor> annotations = ((AnnotationsImpl) getAnnotations()).getAnnotationDescriptors();
        for (AnnotationDescriptor annotationDescriptor : annotationsToAdd) {
            annotations.add(annotationDescriptor);
        }
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
    public boolean isDefaultObject() {
        //TODO:
        return false;
    }

    @NotNull
    @Override
    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @NotNull
    public Collection<JetType> getSupertypes() {
        return supertypes;
    }

    public void setSupertypes(@NotNull Collection<JetType> supertypes) {
        this.supertypes = supertypes;
    }

    public void addSupertype(@NotNull JetType supertype) {
        assert !supertype.isError() : "Error types must be filtered out in DescriptorResolver";
        if (TypeUtils.getClassDescriptor(supertype) != null) {
            // See the Errors.SUPERTYPE_NOT_A_CLASS_OR_TRAIT
            supertypes.add(supertype);
        }
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor) {
        assert primaryConstructor == null : "Primary constructor assigned twice " + this;
        primaryConstructor = constructorDescriptor;

        constructors.add(constructorDescriptor);

        ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(new DelegatingType() {
            @Override
            protected JetType getDelegate() {
                return getDefaultType();
            }
        });

        if (constructorDescriptor.isPrimary()) {
            setUpScopeForInitializers(constructorDescriptor);
        }
    }

    public void addConstructorParametersToInitializersScope(@NotNull Collection<? extends VariableDescriptor> variables) {
        WritableScope scope = getWritableScopeForInitializers();
        for (VariableDescriptor variable : variables) {
            scope.addVariableDescriptor(variable);
        }
    }

    @NotNull
    @Override
    public Set<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    @Override
    @Nullable
    public ConstructorDescriptor getUnsubstitutedPrimaryConstructor() {
        return primaryConstructor;
    }

    @NotNull
    public Set<SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    @NotNull
    public Set<PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    @NotNull
    public Set<CallableMemberDescriptor> getDeclaredCallableMembers() {
        return declaredCallableMembers;
    }

    @NotNull
    public Set<CallableMemberDescriptor> getAllCallableMembers() {
        return allCallableMembers;
    }

    public void setTypeParameterDescriptors(@NotNull List<TypeParameterDescriptor> typeParameters) {
        if (this.typeParameters != null) {
            throw new IllegalStateException("Type parameters are already set for " + getName());
        }
        this.typeParameters = new ArrayList<TypeParameterDescriptor>(typeParameters);
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
    }

    public void createTypeConstructor() {
        assert typeConstructor == null : typeConstructor;
        this.typeConstructor = TypeConstructorImpl.createForClass(
                this,
                Annotations.EMPTY, // TODO : pass annotations from the class?
                !getModality().isOverridable(),
                getName().asString(),
                typeParameters,
                supertypes
        );
        for (FunctionDescriptor functionDescriptor : getConstructors()) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
        scopeForMemberResolution.setImplicitReceiver(getThisAsReceiverParameter());
    }

    @Override
    @NotNull
    public JetScope getScopeForClassHeaderResolution() {
        return scopeForSupertypeResolution;
    }

    @Override
    @NotNull
    public JetScope getScopeForMemberDeclarationResolution() {
        return scopeForMemberResolution;
    }

    private WritableScope getWritableScopeForInitializers() {
        if (scopeForInitializers == null) {
            throw new IllegalStateException("Scope for initializers queried before the primary constructor is set");
        }
        return scopeForInitializers;
    }

    @Override
    @NotNull
    public JetScope getScopeForInitializerResolution() {
        return getWritableScopeForInitializers();
    }

    private void setUpScopeForInitializers(@NotNull DeclarationDescriptor containingDeclaration) {
        this.scopeForInitializers = new WritableScopeImpl(
                scopeForMemberResolution, containingDeclaration, RedeclarationHandler.DO_NOTHING, "Initializers")
                    .changeLockLevel(WritableScope.LockLevel.BOTH);
    }

    public void setScopeForMemberLookup(@NotNull JetScope scopeForMemberLookup) {
        this.scopeForMemberLookup = scopeForMemberLookup;
    }

    @Override
    @NotNull
    public JetScope getScopeForMemberLookup() {
        return scopeForMemberLookup;
    }

    @NotNull
    private WritableScope getScopeForMemberLookupAsWritableScope() {
        // hack
        return (WritableScope) scopeForMemberLookup;
    }

    @NotNull
    @Override
    public JetScope getStaticScope() {
        return staticScope;
    }

    public void lockScopes() {
        getScopeForMemberLookupAsWritableScope().changeLockLevel(WritableScope.LockLevel.READING);
        if (defaultObjectDescriptor != null) {
            defaultObjectDescriptor.lockScopes();
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
        getWritableScopeForInitializers().changeLockLevel(WritableScope.LockLevel.READING);
    }

    @Override
    public String toString() {
        return DeclarationDescriptorImpl.toString(this);
    }
}
