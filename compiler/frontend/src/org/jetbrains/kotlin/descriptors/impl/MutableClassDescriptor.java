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
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.types.*;

import java.util.*;

public class MutableClassDescriptor extends ClassDescriptorBase implements ClassDescriptorWithResolutionScopes {
    private final ClassKind kind;
    private final boolean isInner;

    private Modality modality;
    private Visibility visibility;
    private TypeConstructor typeConstructor;
    private List<TypeParameterDescriptor> typeParameters;
    private Collection<JetType> supertypes = new ArrayList<JetType>();

    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
    private ConstructorDescriptor primaryConstructor;

    private final Set<CallableMemberDescriptor> declaredCallableMembers = Sets.newLinkedHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newLinkedHashSet();
    private final Set<SimpleFunctionDescriptor> functions = Sets.newLinkedHashSet();

    private final MutableScopeForMemberResolution mutableScopeForMemberResolution;
    private final JetScope scopeForMemberResolution;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private JetScope scopeForInitializers; //contains members + primary constructor value parameters + map for backing fields
    private final JetScope unsubstitutedMemberScope;
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
        assert kind != ClassKind.OBJECT : "Fix isCompanionObject()";

        this.kind = kind;
        this.isInner = isInner;

        RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

        this.unsubstitutedMemberScope = new WritableScopeImpl(JetScope.Empty.INSTANCE$, this, redeclarationHandler, "MemberLookup", null, this)
                                        .changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler, "SupertypeResolution")
                .changeLockLevel(WritableScope.LockLevel.BOTH);
        this.mutableScopeForMemberResolution = new MutableScopeForMemberResolution();

        if (kind == ClassKind.INTERFACE) {
            setUpScopeForInitializers(this);
        }

        this.scopeForMemberResolution = new ChainedScope(this, "MemberResolutionWithStatic", mutableScopeForMemberResolution, staticScope);
    }

    @Nullable
    @Override
    public MutableClassDescriptor getCompanionObjectDescriptor() {
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

    public void setTypeParameterDescriptors(@NotNull List<TypeParameterDescriptor> typeParameters) {
        if (this.typeParameters != null) {
            throw new IllegalStateException("Type parameters are already set for " + getName());
        }
        this.typeParameters = new ArrayList<TypeParameterDescriptor>(typeParameters);
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            scopeForSupertypeResolution.addClassifierDescriptor(typeParameterDescriptor);
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
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
        mutableScopeForMemberResolution.setImplicitReceiver(getThisAsReceiverParameter());
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

    @Override
    @NotNull
    public JetScope getScopeForInitializerResolution() {
        if (scopeForInitializers == null) {
            throw new IllegalStateException("Scope for initializers queried before the primary constructor is set");
        }
        return scopeForInitializers;
    }

    private void setUpScopeForInitializers(@NotNull DeclarationDescriptor containingDeclaration) {
        this.scopeForInitializers = new WritableScopeImpl(
                scopeForMemberResolution, containingDeclaration, RedeclarationHandler.DO_NOTHING, "Initializers")
                    .changeLockLevel(WritableScope.LockLevel.BOTH);
    }

    @Override
    @NotNull
    public JetScope getUnsubstitutedMemberScope() {
        return unsubstitutedMemberScope;
    }

    @NotNull
    @Override
    public JetScope getStaticScope() {
        return staticScope;
    }

    @Override
    public String toString() {
        return DeclarationDescriptorImpl.toString(this);
    }

    private class MutableScopeForMemberResolution extends AbstractScopeAdapter {
        private ReceiverParameterDescriptor implicitReceiver = null;
        private List<ReceiverParameterDescriptor> implicitReceiversHierarchy = null;

        @NotNull
        @Override
        protected JetScope getWorkerScope() {
            return scopeForSupertypeResolution;
        }

        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            return MutableClassDescriptor.this;
        }

        public void setImplicitReceiver(@NotNull ReceiverParameterDescriptor implicitReceiver) {
            if (this.implicitReceiver != null) {
                throw new UnsupportedOperationException("Receiver redeclared");
            }
            if (this.implicitReceiversHierarchy != null) {
                throw new UnsupportedOperationException("Receiver hierarchy already computed");
            }
            this.implicitReceiver = implicitReceiver;
            this.implicitReceiversHierarchy = KotlinPackage.plus(Collections.singletonList(implicitReceiver), super.getImplicitReceiversHierarchy());
        }

        @NotNull
        @Override
        public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
            if (implicitReceiversHierarchy != null)
                return implicitReceiversHierarchy;
            else
                return super.getImplicitReceiversHierarchy();
        }
    }
}
