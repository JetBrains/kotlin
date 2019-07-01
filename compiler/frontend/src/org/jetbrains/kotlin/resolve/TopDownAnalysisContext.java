/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.collect.Maps;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.DeclarationScopeProvider;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;

import java.io.PrintStream;
import java.util.*;

public class TopDownAnalysisContext implements BodiesResolveContext {

    private final DataFlowInfo outerDataFlowInfo;

    private final Map<KtClassOrObject, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();
    private final Map<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> anonymousInitializers = Maps.newLinkedHashMap();
    private final Set<KtFile> files = new LinkedHashSet<>();
    private final Map<KtSecondaryConstructor, ClassConstructorDescriptor> secondaryConstructors = Maps.newLinkedHashMap();

    private final Map<KtNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<KtProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<KtParameter, PropertyDescriptor> primaryConstructorParameterProperties = new HashMap<>();
    private final Map<KtTypeAlias, TypeAliasDescriptor> typeAliases = Maps.newLinkedHashMap();
    private final Map<KtDestructuringDeclarationEntry, PropertyDescriptor> destructuringDeclarationEntries = Maps.newLinkedHashMap();
    private Map<KtCallableDeclaration, CallableMemberDescriptor> members = null;

    private final Map<KtScript, ClassDescriptorWithResolutionScopes> scripts = Maps.newLinkedHashMap();

    private final TopDownAnalysisMode topDownAnalysisMode;
    private final DeclarationScopeProvider declarationScopeProvider;

    private StringBuilder debugOutput;

    public TopDownAnalysisContext(
            @NotNull TopDownAnalysisMode topDownAnalysisMode,
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull DeclarationScopeProvider declarationScopeProvider
    ) {
        this.topDownAnalysisMode = topDownAnalysisMode;
        this.outerDataFlowInfo = outerDataFlowInfo;
        this.declarationScopeProvider = declarationScopeProvider;
    }

    @Override
    @NotNull
    public TopDownAnalysisMode getTopDownAnalysisMode() {
        return topDownAnalysisMode;
    }

    public void debug(Object message) {
        if (debugOutput != null) {
            debugOutput.append(message).append("\n");
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    /*package*/ void enableDebugOutput() {
        if (debugOutput == null) {
            debugOutput = new StringBuilder();
        }
    }
    
    /*package*/ void printDebugOutput(PrintStream out) {
        if (debugOutput != null) {
            out.print(debugOutput);
        }
    }

    @Override
    public Map<KtClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
        return classes;
    }

    @Override
    public Map<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers() {
        return anonymousInitializers;
    }

    @Override
    public Map<KtSecondaryConstructor, ClassConstructorDescriptor> getSecondaryConstructors() {
        return secondaryConstructors;
    }

    @Override
    public Collection<KtFile> getFiles() {
        return files;
    }

    public void addFile(@NotNull KtFile file) {
        files.add(file);
    }

    @Override
    @NotNull
    public Map<KtScript, ClassDescriptorWithResolutionScopes> getScripts() {
        return scripts;
    }

    public Map<KtParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    @Override
    public Map<KtProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    @Nullable
    @Override
    public LexicalScope getDeclaringScope(@NotNull KtDeclaration declaration) {
        return declarationScopeProvider.getResolutionScopeForDeclaration(declaration);
    }

    @Override
    public Map<KtNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    @Override
    public Map<KtTypeAlias, TypeAliasDescriptor> getTypeAliases() {
        return typeAliases;
    }

    @Override
    public Map<KtDestructuringDeclarationEntry, PropertyDescriptor> getDestructuringDeclarationEntries() {
        return destructuringDeclarationEntries;
    }

    @NotNull
    public Map<KtCallableDeclaration, CallableMemberDescriptor> getMembers() {
        if (members == null) {
            members = Maps.newLinkedHashMap();
            members.putAll(functions);
            members.putAll(properties);
            members.putAll(primaryConstructorParameterProperties);
        }
        return members;
    }

    @Override
    @NotNull
    public DataFlowInfo getOuterDataFlowInfo() {
        return outerDataFlowInfo;
    }

    @NotNull
    public Collection<ClassDescriptorWithResolutionScopes> getAllClasses() {
        return CollectionsKt.plus(getDeclaredClasses().values(), getScripts().values());
    }
}
