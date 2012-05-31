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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Set;

/**
* @author abreslav
*/
public class LazyClassMemberScope extends AbstractLazyMemberScope<LazyClassDescriptor, ClassMemberDeclarationProvider> {

    public LazyClassMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull ClassMemberDeclarationProvider declarationProvider,
            @NotNull LazyClassDescriptor thisClass
    ) {
        super(resolveSession, declarationProvider, thisClass);
    }

    @NotNull
    @Override
    protected JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration) {
        return thisDescriptor.getScopeForMemberDeclarationResolution();
    }

    @Override
    protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result) {
        JetClassOrObject owner = declarationProvider.getOwnerClassOrObject();
        //throw new UnsupportedOperationException(); // TODO
        System.err.println("getNonDeclaredFunctions() should generate fake overrides for a class");
    }

    @Override
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        System.err.println("getNonDeclaredProperties() should generate fake overrides for a class");
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        return thisDescriptor.getImplicitReceiver();
    }

    @NotNull
    public Set<ConstructorDescriptor> getConstructors() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Nullable
    public ConstructorDescriptor getPrimaryConstructor() {
        throw new UnsupportedOperationException(); // TODO
    }
}
