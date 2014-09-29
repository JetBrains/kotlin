/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy.descriptors;

import com.google.common.collect.Sets;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function0;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.lazy.data.JetScriptInfo;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;
import org.jetbrains.jet.utils.Printer;

import java.util.*;

public abstract class AbstractLazyMemberScope<D extends DeclarationDescriptor, DP extends DeclarationProvider> implements JetScope {
    protected final ResolveSession resolveSession;
    protected final BindingTrace trace;
    protected final DP declarationProvider;
    protected final D thisDescriptor;

    private final MemoizedFunctionToNotNull<Name, List<ClassDescriptor>> classDescriptors;

    private final MemoizedFunctionToNotNull<Name, Set<FunctionDescriptor>> functionDescriptors;
    private final MemoizedFunctionToNotNull<Name, Set<VariableDescriptor>> propertyDescriptors;

    private final NotNullLazyValue<Collection<DeclarationDescriptor>> descriptorsFromDeclaredElements;
    private final NotNullLazyValue<Collection<DeclarationDescriptor>> extraDescriptors;

    protected AbstractLazyMemberScope(
            @NotNull ResolveSession resolveSession,
            @NotNull DP declarationProvider,
            @NotNull D thisDescriptor,
            @NotNull BindingTrace trace
    ) {
        this.resolveSession = resolveSession;
        this.trace = trace;
        this.declarationProvider = declarationProvider;
        this.thisDescriptor = thisDescriptor;

        StorageManager storageManager = resolveSession.getStorageManager();
        this.classDescriptors = storageManager.createMemoizedFunction(new Function1<Name, List<ClassDescriptor>>() {
            @Override
            public List<ClassDescriptor> invoke(Name name) {
                return resolveClassDescriptor(name);
            }
        });

        this.functionDescriptors = storageManager.createMemoizedFunction(new Function1<Name, Set<FunctionDescriptor>>() {
            @Override
            public Set<FunctionDescriptor> invoke(Name name) {
                return doGetFunctions(name);
            }
        });
        this.propertyDescriptors = storageManager.createMemoizedFunction(new Function1<Name, Set<VariableDescriptor>>() {
            @Override
            public Set<VariableDescriptor> invoke(Name name) {
                return doGetProperties(name);
            }
        });

        this.descriptorsFromDeclaredElements = storageManager.createLazyValue(new Function0<Collection<DeclarationDescriptor>>() {
            @Override
            public Collection<DeclarationDescriptor> invoke() {
                return computeDescriptorsFromDeclaredElements();
            }
        });
        this.extraDescriptors = storageManager.createLazyValue(new Function0<Collection<DeclarationDescriptor>>() {
            @Override
            public Collection<DeclarationDescriptor> invoke() {
                return computeExtraDescriptors();
            }
        });
    }

    @Nullable
    private List<ClassDescriptor> resolveClassDescriptor(@NotNull final Name name) {
        Collection<JetClassLikeInfo> classOrObjectDeclarations = declarationProvider.getClassOrObjectDeclarations(name);

        return ContainerUtil.mapNotNull(classOrObjectDeclarations, new Function<JetClassLikeInfo, ClassDescriptor>() {
            @Override
            public ClassDescriptor fun(JetClassLikeInfo classLikeInfo) {
                // SCRIPT: Creating a script class
                if (classLikeInfo instanceof JetScriptInfo) {
                    return new LazyScriptClassDescriptor(resolveSession, thisDescriptor, name, (JetScriptInfo) classLikeInfo);
                }
                return new LazyClassDescriptor(resolveSession, thisDescriptor, name, classLikeInfo);
            }
        });
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        return first(classDescriptors.invoke(name));
    }

    private static <T> T first(@NotNull List<T> list) {
        if (list.isEmpty()) return null;
        return list.get(0);
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        return functionDescriptors.invoke(name);
    }

    @NotNull
    private Set<FunctionDescriptor> doGetFunctions(@NotNull Name name) {
        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();

        Collection<JetNamedFunction> declarations = declarationProvider.getFunctionDeclarations(name);
        for (JetNamedFunction functionDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(functionDeclaration);
            result.add(resolveSession.getDescriptorResolver().resolveFunctionDescriptorWithAnnotationArguments(
                  thisDescriptor, resolutionScope,
                  functionDeclaration,
                  trace,
                  // this relies on the assumption that a lazily resolved declaration is not a local one,
                  // thus doesn't have a surrounding data flow
                  DataFlowInfo.EMPTY)
            );
        }

        getNonDeclaredFunctions(name, result);

        return result;
    }

    @NotNull
    protected abstract JetScope getScopeForMemberDeclarationResolution(JetDeclaration declaration);

    protected abstract void getNonDeclaredFunctions(@NotNull Name name, @NotNull Set<FunctionDescriptor> result);

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        return propertyDescriptors.invoke(name);
    }

    @NotNull
    public Set<VariableDescriptor> doGetProperties(@NotNull Name name) {
        Set<VariableDescriptor> result = Sets.newLinkedHashSet();

        Collection<JetProperty> declarations = declarationProvider.getPropertyDeclarations(name);
        for (JetProperty propertyDeclaration : declarations) {
            JetScope resolutionScope = getScopeForMemberDeclarationResolution(propertyDeclaration);
            PropertyDescriptor propertyDescriptor =
                    resolveSession.getDescriptorResolver().resolvePropertyDescriptor(
                           thisDescriptor, resolutionScope,
                           propertyDeclaration,
                           trace,
                           // this relies on the assumption that a lazily resolved declaration is not a local one,
                           // thus doesn't have a surrounding data flow
                           DataFlowInfo.EMPTY
                    );
            result.add(propertyDescriptor);
            AnnotationResolver.resolveAnnotationsArguments(propertyDescriptor.getAnnotations(), trace);
        }

        getNonDeclaredProperties(name, result);

        return result;
    }

    protected abstract void getNonDeclaredProperties(@NotNull Name name, @NotNull Set<VariableDescriptor> result);

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
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull Name labelName) {
        // A member scope has no labels
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        Collection<DeclarationDescriptor> result = new LinkedHashSet<DeclarationDescriptor>(descriptorsFromDeclaredElements.invoke());
        result.addAll(extraDescriptors.invoke());
        return result;
    }

    @NotNull
    private Collection<DeclarationDescriptor> computeDescriptorsFromDeclaredElements() {
        List<JetDeclaration> declarations = declarationProvider.getAllDeclarations();
        ArrayList<DeclarationDescriptor> result = new ArrayList<DeclarationDescriptor>(declarations.size());
        for (JetDeclaration declaration : declarations) {
            if (declaration instanceof JetClassOrObject) {
                JetClassOrObject classOrObject = (JetClassOrObject) declaration;
                result.addAll(classDescriptors.invoke(classOrObject.getNameAsSafeName()));
            }
            else if (declaration instanceof JetFunction) {
                JetFunction function = (JetFunction) declaration;
                result.addAll(getFunctions(function.getNameAsSafeName()));
            }
            else if (declaration instanceof JetProperty) {
                JetProperty property = (JetProperty) declaration;
                result.addAll(getProperties(property.getNameAsSafeName()));
            }
            else if (declaration instanceof JetParameter) {
                JetParameter parameter = (JetParameter) declaration;
                result.addAll(getProperties(parameter.getNameAsSafeName()));
            }
            else if (declaration instanceof JetScript) {
                result.addAll(classDescriptors.invoke(ScriptNameUtil.classNameForScript((JetScript) declaration).shortName()));
            }
            else if (declaration instanceof JetTypedef || declaration instanceof JetMultiDeclaration) {
                // Do nothing for typedefs as they are not supported.
                // MultiDeclarations are not supported on global level too.
            }
            else {
                throw new IllegalArgumentException("Unsupported declaration kind: " + declaration);
            }
        }
        result.trimToSize();
        return result;
    }

    @NotNull
    protected abstract Collection<DeclarationDescriptor> computeExtraDescriptors();

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        return Collections.emptyList();
    }

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

    @Override
    public void printScopeStructure(@NotNull Printer p) {
        p.println(getClass().getSimpleName(), " {");
        p.pushIndent();

        p.println("thisDescriptor = ", thisDescriptor);

        p.popIndent();
        p.println("}");
    }
}
