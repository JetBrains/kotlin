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

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.DescriptorPredicate;
import org.jetbrains.jet.lang.resolve.scopes.DescriptorPredicateUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeBase;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.*;

/**
 * @author abreslav
 */
public abstract class AbstractLazyMemberScope<D extends DeclarationDescriptor, DP extends DeclarationProvider> extends JetScopeBase {
    protected final ResolveSession resolveSession;
    protected final DP declarationProvider;
    protected final D thisDescriptor;

    protected boolean allDescriptorsComputed = false;

    private final Map<Name, ClassDescriptor> classDescriptors = Maps.newHashMap();
    private final Map<Name, Set<FunctionDescriptor>> functionDescriptors = Maps.newHashMap();
    private final Map<Name, Set<VariableDescriptor>> propertyDescriptors = Maps.newHashMap();

    protected final List<DeclarationDescriptor> allDescriptors = Lists.newArrayList();

    protected AbstractLazyMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull DP declarationProvider,
            @NotNull D thisDescriptor
    ) {
        this.resolveSession = resolveSession;
        this.declarationProvider = declarationProvider;
        this.thisDescriptor = thisDescriptor;
    }

    @Nullable
    private ClassDescriptor getClassOrObjectDescriptor(@NotNull Name name, boolean object) {
        ClassDescriptor known = classDescriptors.get(name);
        if (known != null) return known;

        if (allDescriptorsComputed) return null;

        JetClassOrObject classOrObjectDeclaration = declarationProvider.getClassOrObjectDeclaration(name);
        if (classOrObjectDeclaration == null) return null;

        if (object != classOrObjectDeclaration instanceof JetObjectDeclaration) return null;

        ClassMemberDeclarationProvider classMemberDeclarationProvider =
                resolveSession.getDeclarationProviderFactory().getClassMemberDeclarationProvider(classOrObjectDeclaration);
        ClassDescriptor classDescriptor = new LazyClassDescriptor(resolveSession, thisDescriptor, name, classMemberDeclarationProvider);

        classDescriptors.put(name, classDescriptor);
        allDescriptors.add(classDescriptor);

        return classDescriptor;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return getClassOrObjectDescriptor(name, false);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        // TODO: We shouldn't really allow objects in classes...
        return getClassOrObjectDescriptor(name, true);
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        Set<FunctionDescriptor> known = functionDescriptors.get(name);
        if (known != null) return known;

        // If all descriptors are already computed, we are
        if (allDescriptorsComputed) return Collections.emptySet();

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();

        List<JetNamedFunction> declarations = declarationProvider.getFunctionDeclarations(name);
        for (JetNamedFunction functionDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration);
            result.add(resolveSession.getInjector().getDescriptorResolver().resolveFunctionDescriptor(thisDescriptor, resolutionScope,
                                                                                                      functionDeclaration,
                                                                                                      resolveSession.getTrace()));
        }

        getNonDeclaredFunctions(name, result);

        if (!result.isEmpty()) {
            functionDescriptors.put(name, result);
            allDescriptors.addAll(result);
        }
        return result;
    }

    @NotNull
    protected abstract JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration);

    protected abstract void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result);

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        Set<VariableDescriptor> known = propertyDescriptors.get(name);
        if (known != null) return known;

        // If all descriptors are already computed, we are
        if (allDescriptorsComputed) return Collections.emptySet();

        Set<VariableDescriptor> result = Sets.newLinkedHashSet();

        List<JetProperty> declarations = declarationProvider.getPropertyDeclarations(name);
        for (JetProperty propertyDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration);
            result.add(resolveSession.getInjector().getDescriptorResolver().resolvePropertyDescriptor(thisDescriptor, resolutionScope,
                                                                                                      propertyDeclaration,
                                                                                                      resolveSession.getTrace()));
        }

        // Objects are also properties
        JetClassOrObject classOrObjectDeclaration = declarationProvider.getClassOrObjectDeclaration(name);
        if (classOrObjectDeclaration instanceof JetObjectDeclaration) {
            JetObjectDeclaration objectDeclaration = (JetObjectDeclaration) classOrObjectDeclaration;
            ClassDescriptor classifier = (ClassDescriptor) getClassifier(name);
            VariableDescriptor propertyDescriptor = resolveSession.getInjector().getDescriptorResolver()
                    .resolveObjectDeclaration(thisDescriptor, objectDeclaration, classifier, resolveSession.getTrace());
            result.add(propertyDescriptor);
        }

        getNonDeclaredProperties(name, result);

        if (!result.isEmpty()) {
            propertyDescriptors.put(name, result);
            allDescriptors.addAll(result);
        }
        return result;
    }

    protected abstract void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result);

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        return null;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return thisDescriptor;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors(@NotNull DescriptorPredicate predicate) {
        // TODO: cache only what matches predicate

        for (JetDeclaration declaration : declarationProvider.getAllDeclarations()) {
            if (declaration instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) declaration;
                Name name = classOrObject.getNameAsName();
                if (name != null) {
                    getClassifier(name);
                }
            }
            else if (declaration instanceof JetFunction) {
                JetFunction function = (JetFunction) declaration;
                getFunctions(function.getNameAsSafeName());
            }
            else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                getProperties(property.getNameAsSafeName());
            }
            else if (declaration instanceof JetParameter) {
                JetParameter parameter = (JetParameter) declaration;
                Name name = parameter.getNameAsName();
                if (name != null) {
                    getProperties(name);
                }
            }
            else {
                throw new IllegalArgumentException("Unsupported declaration kind: " + declaration);
            }
        }
        addExtraDescriptors();
        allDescriptorsComputed = true;

        return DescriptorPredicateUtils.filter(allDescriptors, predicate);
    }

    protected abstract void addExtraDescriptors();

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        ReceiverDescriptor receiver = getImplicitReceiver();
        if (receiver.exists()) {
            result.add(receiver);
        }
    }
}
