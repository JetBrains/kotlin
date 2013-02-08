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

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.collect.Sets;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassInfoUtil;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils.safeNameForLazyResolve;
import static org.jetbrains.jet.lang.resolve.lazy.StorageManager.MemoizationMode.STRONG;

public abstract class AbstractLazyMemberScope<D extends DeclarationDescriptor, DP extends DeclarationProvider> implements JetScope {
    protected final ResolveSession resolveSession;
    protected final DP declarationProvider;
    protected final D thisDescriptor;

    private final Function<Name, ClassDescriptor> classDescriptors;
    private final Function<Name, ClassDescriptor> objectDescriptors;

    private final Function<Name, Set<FunctionDescriptor>> functionDescriptors;
    private final Function<Name, Set<VariableDescriptor>> propertyDescriptors;

    private final Collection<DeclarationDescriptor> allDescriptors;
    protected volatile boolean allDescriptorsComputed = false;

    protected AbstractLazyMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull DP declarationProvider,
            @NotNull D thisDescriptor
    ) {
        this.resolveSession = resolveSession;
        this.declarationProvider = declarationProvider;
        this.thisDescriptor = thisDescriptor;

        StorageManager storageManager = resolveSession.getStorageManager();
        this.classDescriptors = storageManager.createMemoizedFunctionWithNullableValues(new Function<Name, ClassDescriptor>() {
            @Override
            public ClassDescriptor fun(Name name) {
                return resolveClassOrObjectDescriptor(name, false);
            }
        }, STRONG);
        this.objectDescriptors = storageManager.createMemoizedFunctionWithNullableValues(new Function<Name, ClassDescriptor>() {
            @Override
            public ClassDescriptor fun(Name name) {
                return resolveClassOrObjectDescriptor(name, true);
            }
        }, STRONG);

        this.functionDescriptors = storageManager.createMemoizedFunction(new Function<Name, Set<FunctionDescriptor>>() {
            @Override
            public Set<FunctionDescriptor> fun(Name name) {
                return doGetFunctions(name);
            }
        }, STRONG);
        this.propertyDescriptors = storageManager.createMemoizedFunction(new Function<Name, Set<VariableDescriptor>>() {
            @Override
            public Set<VariableDescriptor> fun(Name name) {
                return doGetProperties(name);
            }
        }, STRONG);

        this.allDescriptors = storageManager.createConcurrentCollection();
    }

    @Nullable
    private ClassDescriptor resolveClassOrObjectDescriptor(@NotNull Name name, boolean object) {
        Collection<JetClassOrObject> classOrObjectDeclarations = declarationProvider.getClassOrObjectDeclarations(name);

        for (JetClassOrObject classOrObjectDeclaration : classOrObjectDeclarations) {
            if (object != declaresObjectOrEnumConstant(classOrObjectDeclaration)) continue;

            ClassDescriptor classDescriptor = new LazyClassDescriptor(resolveSession, thisDescriptor, name,
                                                                      JetClassInfoUtil.createClassLikeInfo(classOrObjectDeclaration));

            if (!object) {
                registerDescriptor(classDescriptor);
            }

            return classDescriptor;
        }
        return null;
    }

    private static boolean declaresObjectOrEnumConstant(JetClassOrObject declaration) {
        return declaration instanceof JetObjectDeclaration || declaration instanceof JetEnumEntry;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return classDescriptors.fun(name);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        return objectDescriptors.fun(name);
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return functionDescriptors.fun(name);
    }

    @NotNull
    private Set<FunctionDescriptor> doGetFunctions(@NotNull Name name) {
        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();

        Collection<JetNamedFunction> declarations = declarationProvider.getFunctionDeclarations(name);
        for (JetNamedFunction functionDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration);
            result.add(resolveSession.getInjector().getDescriptorResolver().resolveFunctionDescriptor(thisDescriptor, resolutionScope,
                                                                                                      functionDeclaration,
                                                                                                      resolveSession.getTrace()));
        }

        getNonDeclaredFunctions(name, result);

        if (!result.isEmpty()) {
            registerDescriptors(result);
        }
        return result;
    }

    @NotNull
    protected abstract JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration);

    protected abstract void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result);

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        return propertyDescriptors.fun(name);
    }

    @NotNull
    public Set<VariableDescriptor> doGetProperties(@NotNull Name name) {
        Set<VariableDescriptor> result = Sets.newLinkedHashSet();

        Collection<JetProperty> declarations = declarationProvider.getPropertyDeclarations(name);
        for (JetProperty propertyDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration);
            result.add(resolveSession.getInjector().getDescriptorResolver().resolvePropertyDescriptor(thisDescriptor, resolutionScope,
                                                                                                      propertyDeclaration,
                                                                                                      resolveSession.getTrace()));
        }

        // Objects are also properties
        Collection<JetClassOrObject> classOrObjectDeclarations = declarationProvider.getClassOrObjectDeclarations(name);
        for (JetClassOrObject classOrObjectDeclaration : classOrObjectDeclarations) {
            if (declaresObjectOrEnumConstant(classOrObjectDeclaration)) {
                ClassDescriptor classifier = getObjectDescriptor(name);
                if (classifier == null) {
                    throw new IllegalStateException("Object declaration " + name + " found in the DeclarationProvider " + declarationProvider + " but not in the scope " + this);
                }
                VariableDescriptor propertyDescriptor = resolveSession.getInjector().getDescriptorResolver()
                        .resolveObjectDeclaration(thisDescriptor, classOrObjectDeclaration, classifier, resolveSession.getTrace());
                result.add(propertyDescriptor);
            }
        }

        getNonDeclaredProperties(name, result);

        registerDescriptors(result);

        return result;
    }

    protected abstract void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result);

    @NotNull
    @Override
    public Collection<ClassDescriptor> getObjectDescriptors() {
        getAllDescriptors();
        return objectDescriptors.values();
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
        // A member scope has no labels
        return Collections.emptySet();
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        throw new UnsupportedOperationException(); // TODO
    }

    protected void registerDescriptor(@NotNull DeclarationDescriptor descriptor) {
        assert !allDescriptorsComputed : "getAllDescriptors() has been called already";
        allDescriptors.add(descriptor);
    }

    protected void registerDescriptors(@NotNull Collection<? extends DeclarationDescriptor> descriptors) {
        assert !allDescriptorsComputed : "getAllDescriptors() has been called already";
        allDescriptors.addAll(descriptors);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (!allDescriptorsComputed) {
            for (JetDeclaration declaration : declarationProvider.getAllDeclarations()) {
                if (declaration instanceof JetEnumEntry) {
                    JetEnumEntry jetEnumEntry = (JetEnumEntry) declaration;
                    Name name = safeNameForLazyResolve(jetEnumEntry);
                    if (name != null) {
                        getProperties(name);
                        getObjectDescriptor(name);
                    }
                }
                else if (declaration instanceof JetObjectDeclaration) {
                    JetObjectDeclaration objectDeclaration = (JetObjectDeclaration) declaration;
                    Name name = safeNameForLazyResolve(objectDeclaration.getNameAsDeclaration());
                    if (name != null) {
                        getProperties(name);
                        getObjectDescriptor(name);
                    }
                }
                else if (declaration instanceof JetClassOrObject) {
                    JetClassOrObject classOrObject = (JetClassOrObject) declaration;
                    Name name = safeNameForLazyResolve(classOrObject.getNameAsName());
                    if (name != null) {
                        getClassifier(name);
                    }
                }
                else if (declaration instanceof JetFunction) {
                    JetFunction function = (JetFunction) declaration;
                    getFunctions(safeNameForLazyResolve(function));
                }
                else if (declaration instanceof JetProperty) {
                    JetProperty property = (JetProperty) declaration;
                    getProperties(safeNameForLazyResolve(property));
                }
                else if (declaration instanceof JetParameter) {
                    JetParameter parameter = (JetParameter) declaration;
                    Name name = safeNameForLazyResolve(parameter);
                    getProperties(name);
                }
                else if (declaration instanceof JetTypedef || declaration instanceof JetMultiDeclaration) {
                    // Do nothing for typedefs as they are not supported.
                    // MultiDeclarations are not supported on global level too.
                }
                else {
                    throw new IllegalArgumentException("Unsupported declaration kind: " + declaration);
                }
            }
            addExtraDescriptors();
            allDescriptorsComputed = true;
        }
        return allDescriptors;
    }

    protected abstract void addExtraDescriptors();

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        ReceiverParameterDescriptor receiver = getImplicitReceiver();
        if (receiver != null) {
            return Collections.singletonList(receiver);
        }
        return Collections.emptyList();
    }

    @Nullable
    protected abstract ReceiverParameterDescriptor getImplicitReceiver();

    // Do not change this, override in concrete subclasses:
    // it is very easy to compromise laziness of this class, and fail all the debugging
    // a generic implementation can't do this properly
    @Override
    public abstract String toString();

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return getAllDescriptors();
    }
}
