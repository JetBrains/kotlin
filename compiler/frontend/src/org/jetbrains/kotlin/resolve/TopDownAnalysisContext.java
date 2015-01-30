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

package org.jetbrains.kotlin.resolve;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.StorageManager;

import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TopDownAnalysisContext implements BodiesResolveContext {

    private final DataFlowInfo outerDataFlowInfo;

    private final Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> classes = Maps.newLinkedHashMap();
    private final Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> anonymousInitializers = Maps.newLinkedHashMap();
    private final Set<JetFile> files = new LinkedHashSet<JetFile>();

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<JetParameter, PropertyDescriptor> primaryConstructorParameterProperties = Maps.newHashMap();
    private Map<JetCallableDeclaration, CallableMemberDescriptor> members = null;

    private final Map<JetScript, ScriptDescriptor> scripts = Maps.newLinkedHashMap();

    private final TopDownAnalysisParameters topDownAnalysisParameters;

    private StringBuilder debugOutput;

    public TopDownAnalysisContext(@NotNull TopDownAnalysisParameters topDownAnalysisParameters, @NotNull DataFlowInfo outerDataFlowInfo) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
        this.outerDataFlowInfo = outerDataFlowInfo;
    }

    @Override
    @NotNull
    public TopDownAnalysisParameters getTopDownAnalysisParameters() {
        return topDownAnalysisParameters;
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
    public Map<JetClassOrObject, ClassDescriptorWithResolutionScopes> getDeclaredClasses() {
        return classes;
    }

    @Override
    public Map<JetClassInitializer, ClassDescriptorWithResolutionScopes> getAnonymousInitializers() {
        return anonymousInitializers;
    }

    @NotNull
    @Override
    public StorageManager getStorageManager() {
        return topDownAnalysisParameters.getStorageManager();
    }

    @NotNull
    @Override
    public ExceptionTracker getExceptionTracker() {
        return topDownAnalysisParameters.getExceptionTracker();
    }

    @Override
    public Collection<JetFile> getFiles() {
        return files;
    }

    public void addFile(@NotNull JetFile file) {
        files.add(file);
    }

    @Override
    @NotNull
    public Map<JetScript, ScriptDescriptor> getScripts() {
        return scripts;
    }

    public Map<JetParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    @Override
    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public Function<JetDeclaration, JetScope> getDeclaringScopes() {
        return Functions.forMap(declaringScopes);
    }

    public void registerDeclaringScope(@NotNull JetDeclaration declaration, @NotNull JetScope scope) {
        declaringScopes.put(declaration, scope);
    }

    @Override
    public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    @NotNull
    public Map<JetCallableDeclaration, CallableMemberDescriptor> getMembers() {
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
        // SCRIPT: all classes are declared classes + script classes
        Collection<ClassDescriptorWithResolutionScopes> scriptClasses = KotlinPackage.map(
                getScripts().values(),
                new Function1<ScriptDescriptor, ClassDescriptorWithResolutionScopes>() {
                    @Override
                    public ClassDescriptorWithResolutionScopes invoke(ScriptDescriptor scriptDescriptor) {
                        return (ClassDescriptorWithResolutionScopes) scriptDescriptor.getClassDescriptor();
                    }
                }
        );
        return KotlinPackage.plus(getDeclaredClasses().values(), scriptClasses);
    }
}
