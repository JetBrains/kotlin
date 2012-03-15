/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.AbstractScopeAdapter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ClassReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class MutableClassDescriptor extends MutableClassDescriptorLite {
    private final Set<CallableMemberDescriptor> callableMembers = Sets.newHashSet();
    private final Set<PropertyDescriptor> properties = Sets.newHashSet();
    private final Set<SimpleFunctionDescriptor> functions = Sets.newHashSet();

    private final WritableScope scopeForMemberResolution;
    // This scope contains type parameters but does not contain inner classes
    private final WritableScope scopeForSupertypeResolution;
    private WritableScope scopeForInitializers = null; //contains members + primary constructor value parameters + map for backing fields

    public MutableClassDescriptor(@NotNull BindingTrace trace, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope outerScope, ClassKind kind) {
        super(containingDeclaration, kind);

        RedeclarationHandler redeclarationHandler = RedeclarationHandler.DO_NOTHING;

        setScopeForMemberLookup(new WritableScopeImpl(JetScope.EMPTY, this, redeclarationHandler).setDebugName("MemberLookup").changeLockLevel(WritableScope.LockLevel.BOTH));
        this.scopeForSupertypeResolution = new WritableScopeImpl(outerScope, this, redeclarationHandler).setDebugName("SupertypeResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        this.scopeForMemberResolution = new WritableScopeImpl(scopeForSupertypeResolution, this, redeclarationHandler).setDebugName("MemberResolution").changeLockLevel(WritableScope.LockLevel.BOTH);
        if (getKind() == ClassKind.TRAIT) {
            setUpScopeForInitializers(this);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    @Override
    public ClassObjectStatus setClassObjectDescriptor(@NotNull final MutableClassDescriptorLite classObjectDescriptor) {
        ClassObjectStatus r = super.setClassObjectDescriptor(classObjectDescriptor);
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
            public ReceiverDescriptor getImplicitReceiver() {
                return classObjectDescriptor.getImplicitReceiver();
            }
        }
        );

        return ClassObjectStatus.OK;
    }

    @Override
    public void addConstructor(@NotNull ConstructorDescriptor constructorDescriptor, @NotNull BindingTrace trace) {
        super.addConstructor(constructorDescriptor, trace);
        if (constructorDescriptor.isPrimary()) {
            setUpScopeForInitializers(constructorDescriptor);
            for (ValueParameterDescriptor valueParameterDescriptor : constructorDescriptor.getValueParameters()) {
                JetParameter parameter = (JetParameter) trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, valueParameterDescriptor);
                assert parameter != null;
                if (parameter.getValOrVarNode() == null || !constructorDescriptor.isPrimary()) {
                    getWritableScopeForInitializers().addVariableDescriptor(valueParameterDescriptor);
                }
            }
        }
    }

    @Override
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
        super.addPropertyDescriptor(propertyDescriptor);
        properties.add(propertyDescriptor);
        callableMembers.add(propertyDescriptor);
        scopeForMemberResolution.addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull SimpleFunctionDescriptor functionDescriptor) {
        super.addFunctionDescriptor(functionDescriptor);
        functions.add(functionDescriptor);
        callableMembers.add(functionDescriptor);
        scopeForMemberResolution.addFunctionDescriptor(functionDescriptor);
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
    public Set<CallableMemberDescriptor> getCallableMembers() {
        return callableMembers;
    }

    @Override
    public void addClassifierDescriptor(@NotNull MutableClassDescriptorLite classDescriptor) {
        super.addClassifierDescriptor(classDescriptor);
        scopeForMemberResolution.addClassifierDescriptor(classDescriptor);
    }

    public void setTypeParameterDescriptors(List<TypeParameterDescriptor> typeParameters) {
        super.setTypeParameterDescriptors(typeParameters);
        for (TypeParameterDescriptor typeParameterDescriptor : typeParameters) {
            scopeForSupertypeResolution.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void setName(@NotNull String name) {
        super.setName(name);
        scopeForMemberResolution.addLabeledDeclaration(this);
    }

    @Override
    public void createTypeConstructor() {
        super.createTypeConstructor();
        scopeForMemberResolution.setImplicitReceiver(new ClassReceiver(this));
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
        this.scopeForInitializers = new WritableScopeImpl(scopeForMemberResolution, containingDeclaration, RedeclarationHandler.DO_NOTHING).setDebugName("Initializers").changeLockLevel(WritableScope.LockLevel.BOTH);
    }

    public void lockScopes() {
        super.lockScopes();
        scopeForSupertypeResolution.changeLockLevel(WritableScope.LockLevel.READING);
        scopeForMemberResolution.changeLockLevel(WritableScope.LockLevel.READING);
        getWritableScopeForInitializers().changeLockLevel(WritableScope.LockLevel.READING);
    }

}
