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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.util.lazy.LazyValue;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
* @author abreslav
*/
public class LazyClassMemberScope extends AbstractLazyMemberScope<LazyClassDescriptor, ClassMemberDeclarationProvider> {

    private ConstructorDescriptor primaryConstructor;
    private boolean primaryConstructorResolved = false;

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
    protected void getNonDeclaredFunctions(@NotNull Name name, @NotNull final Set<FunctionDescriptor> result) {
        Collection<FunctionDescriptor> fromSupertypes = Lists.newArrayList();
        for (JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes()) {
            fromSupertypes.addAll(supertype.getMemberScope().getFunctions(name));
        }
        OverrideResolver.generateOverridesInFunctionGroup(
                name,
                fromSupertypes,
                Lists.newArrayList(result),
                thisDescriptor,
                new OverrideResolver.DescriptorSink() {
                    @Override
                    public void addToScope(@NotNull CallableMemberDescriptor fakeOverride) {
                        assert fakeOverride instanceof FunctionDescriptor : "A non-function overrides a function";
                        FunctionDescriptor functionDescriptor = (FunctionDescriptor) fakeOverride;
                        result.add(functionDescriptor);
                    }

                    @Override
                    public void conflict(@NotNull CallableMemberDescriptor fromSuper, @NotNull CallableMemberDescriptor fromCurrent) {
                        BindingTrace trace = resolveSession.getTrace();
                        JetDeclaration declaration = (JetDeclaration) BindingContextUtils.descriptorToDeclaration(trace.getBindingContext(),
                                                                                                                  fromCurrent);
                        assert declaration != null : "fromCurrent can not be a fake override";
                        trace.report(Errors.CONFLICTING_OVERLOADS.on(declaration, fromCurrent, fromCurrent.getContainingDeclaration().getName().getName()));
                    }
                }
        );
    }

    @Override
    protected void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result) {
        System.err.println("getNonDeclaredProperties() should generate fake overrides for a class");

        // Inherited fake overrides
        // Parameters of the primary constructor

        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected void addExtraDescriptors() {
        for (JetType supertype : thisDescriptor.getTypeConstructor().getSupertypes()) {
            for (DeclarationDescriptor descriptor : supertype.getMemberScope().getAllDescriptors()) {
                if (descriptor instanceof FunctionDescriptor) {
                    getFunctions(descriptor.getName());
                }
                else if (descriptor instanceof PropertyDescriptor) {
                    getProperties(descriptor.getName());
                }
                // Nothing else is inherited
            }
        }
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
        ConstructorDescriptor constructor = getPrimaryConstructor();
        return constructor == null ? Collections.<ConstructorDescriptor>emptySet() : Collections.singleton(constructor);
    }

    @Nullable
    public ConstructorDescriptor getPrimaryConstructor() {
        if (!primaryConstructorResolved) {
            if (EnumSet.of(ClassKind.CLASS, ClassKind.ANNOTATION_CLASS).contains(thisDescriptor.getKind())) {
                JetClassOrObject classOrObject = declarationProvider.getOwnerClassOrObject();
                if (classOrObject instanceof JetClass) {
                    JetClass jetClass = (JetClass) classOrObject;
                    ConstructorDescriptorImpl descriptor = resolveSession.getInjector().getDescriptorResolver()
                            .resolvePrimaryConstructorDescriptor(thisDescriptor.getScopeForClassHeaderResolution(), thisDescriptor,
                                                                 jetClass,
                                                                 resolveSession.getTrace());
                    primaryConstructor = descriptor;
                    descriptor.setReturnType(DeferredType.create(resolveSession.getTrace(), new LazyValue<JetType>() {
                        @Override
                        protected JetType compute() {
                            return thisDescriptor.getDefaultType();
                        }
                    }));
                }
            }
            primaryConstructorResolved = true;
        }
        return primaryConstructor;
    }
}
