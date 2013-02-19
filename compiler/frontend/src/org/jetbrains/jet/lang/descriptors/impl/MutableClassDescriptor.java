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

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MutableClassDescriptor extends MutableClassDescriptorLite {
    private final Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
    private ConstructorDescriptor primaryConstructor;

    private final Set<CallableMemberDescriptor> declaredCallableMembers = Sets.newLinkedHashSet();
    private final Set<CallableMemberDescriptor> allCallableMembers = Sets.newLinkedHashSet(); // includes fake overrides
    private final Set<PropertyDescriptor> properties = Sets.newLinkedHashSet();
    private final Set<SimpleFunctionDescriptor> functions = Sets.newLinkedHashSet();

    private final WritableScope scopeForMemberResolution;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private WritableScope scopeForInitializers = null; //contains members + primary constructor value parameters + map for backing fields

    public MutableClassDescriptor(@NotNull DeclarationDescriptor containingDeclaration,
                                  @NotNull JetScope outerScope, ClassKind kind, boolean isInner, Name name) {
        super(containingDeclaration, kind, isInner);

        RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

        setScopeForMemberLookup(new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler, "MemberLookup")
                                        .changeLockLevel(WritableScope.LockLevel.BOTH));
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler, "SupertypeResolution")
                .changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler, "MemberResolution")
                .changeLockLevel(WritableScope.LockLevel.BOTH);
        if (getKind() == ClassKind.TRAIT) {
            setUpScopeForInitializers(this);
        }

        setName(name);
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace trace) {
        if (constructorDescriptor.getContainingDeclaration() != this) {
            throw new IllegalStateException("invalid containing declaration of constructor");
        }
        constructors.add(constructorDescriptor);
        if (defaultType != null) {
            ((ConstructorDescriptorImpl) constructorDescriptor).setReturnType(getDefaultType());
        }

        boolean primary = constructorDescriptor.isPrimary();
        if (primary) {
            setUpScopeForInitializers(constructorDescriptor);
            for (ValueParameterDescriptor valueParameterDescriptor : constructorDescriptor.getValueParameters()) {
                JetParameter parameter = (JetParameter) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(), valueParameterDescriptor);
                assert parameter != null;
                if (parameter.getValOrVarNode() == null) {
                    getWritableScopeForInitializers().addVariableDescriptor(valueParameterDescriptor);
                }
            }
        }
    }

    public void setPrimaryConstructor(@NotNull ConstructorDescriptor constructorDescriptor, BindingTrace trace) {
        assert this.primaryConstructor == null : "Primary constructor assigned twice " + this;
        this.primaryConstructor = constructorDescriptor;
        addConstructor(constructorDescriptor, trace);
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

    @NotNull
    public Set<CallableMemberDescriptor> getDeclaredCallableMembers() {
        return declaredCallableMembers;
    }

    @NotNull
    public Set<CallableMemberDescriptor> getAllCallableMembers() {
        return allCallableMembers;
    }

    @Override
    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        super.setTypeParameterDescriptors(typeParameters);
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setName(@NotNull Name name) {
        super.setName(name);
        scopeForMemberResolution.addLabeledDeclaration(this);
    }

    @Override
    public void createTypeConstructor() {
        super.createTypeConstructor();
        for (FunctionDescriptor functionDescriptor : getConstructors()) {
            ((ConstructorDescriptorImpl) functionDescriptor).setReturnType(getDefaultType());
        }
        scopeForMemberResolution.setImplicitReceiver(getThisAsReceiverParameter());
    }

    @NotNull
    public JetScope getScopeForSupertypeResolution() {
        return scopeForSupertypeResolution;
    }

    @NotNull
    public JetScope getScopeForMemberResolution() {
        return scopeForMemberResolution;
    }

    private WritableScope getWritableScopeForInitializers() {
        if (scopeForInitializers == null) {
            throw new IllegalStateException("Scope for initializers queried before the primary constructor is set");
        }
        return scopeForInitializers;
    }

    @NotNull
    public JetScope getScopeForInitializers() {
        return getWritableScopeForInitializers();
    }

    private void setUpScopeForInitializers(@NotNull DeclarationDescriptor containingDeclaration) {
        this.scopeForInitializers = new WritableScopeImpl(
                scopeForMemberResolution, containingDeclaration, RedeclarationHandler.DO_NOTHING, "Initializers")
                    .changeLockLevel(WritableScope.LockLevel.BOTH);
    }

    @Override
    public void lockScopes() {
        super.lockScopes();
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
        getWritableScopeForInitializers().changeLockLevel(WritableScope.LockLevel.READING);
    }

    private NamespaceLikeBuilder builder = null;

    @Override
    public NamespaceLikeBuilder getBuilder() {
        if (builder == null) {
            final NamespaceLikeBuilder superBuilder = super.getBuilder();
            builder = new NamespaceLikeBuilderDummy() {
                @NotNull
                @Override
                public DeclarationDescriptor getOwnerForChildren() {
                    return superBuilder.getOwnerForChildren();
                }

                @Override
                public void addObjectDescriptor(@NotNull MutableClassDescriptorLite objectDescriptor) {
                    superBuilder.addObjectDescriptor(objectDescriptor);
                }

                @Override
                public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
                    superBuilder.addClassifierDescriptor(classDescriptor);
                    scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
                }

                @Override
                public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
                    superBuilder.addFunctionDescriptor(functionDescriptor);
                    functions.add(functionDescriptor);
                    if (functionDescriptor.getKind().isReal()) {
                        declaredCallableMembers.add(functionDescriptor);
                    }
                    allCallableMembers.add(functionDescriptor);
                    scopeForMemberResolution.addFunctionDescriptor(functionDescriptor);
                }

                @Override
                public ClassObjectStatus setClassObjectDescriptor(@NotNull final MutableClassDescriptorLite classObjectDescriptor) {
                    ClassObjectStatus r = superBuilder.setClassObjectDescriptor(classObjectDescriptor);
                    if (r != ClassObjectStatus.OK) {
                        return r;
                    }

                    // Members of the class object are accessible from the class
                    // The scope must be lazy, because classObjectDescriptor may not by fully built yet
                    scopeForMemberResolution.importScope(new AbstractScopeAdapter() {
                        @NotNull
                        @Override
                        protected JetScope getWorkerScope() {
                            return classObjectDescriptor.getDefaultType().getMemberScope();
                        }

                        @NotNull
                        @Override
                        public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
                            return Collections.singletonList(classObjectDescriptor.getThisAsReceiverParameter());
                        }
                    });

                    return ClassObjectStatus.OK;
                }

                @Override
                public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
                    superBuilder.addPropertyDescriptor(propertyDescriptor);
                    properties.add(propertyDescriptor);
                    if (propertyDescriptor.getKind().isReal()) {
                        declaredCallableMembers.add(propertyDescriptor);
                    }
                    allCallableMembers.add(propertyDescriptor);
                    scopeForMemberResolution.addPropertyDescriptor(propertyDescriptor);
                }
            };
        }

        return builder;
    }
}
