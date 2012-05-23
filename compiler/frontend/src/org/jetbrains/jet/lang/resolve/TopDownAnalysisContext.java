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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.Map;

/**
 * @author abreslav
 */
public class TopDownAnalysisContext {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    private JetScope rootScope;
    protected final Map<JetFile, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<JetSecondaryConstructor, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<JetParameter, PropertyDescriptor> primaryConstructorParameterProperties = Maps.newHashMap();
    private Map<JetDeclaration, CallableMemberDescriptor> members = null;

    // File scopes - package scope extended with imports
    protected final Map<JetFile, WritableScope> namespaceScopes = Maps.newHashMap();

    public final Map<JetDeclarationContainer, WithDeferredResolve> forDeferredResolver = Maps.newHashMap();

    public final Map<JetDeclarationContainer, JetScope> normalScope = Maps.newHashMap();

    private final Map<JetScript, ScriptDescriptor> scripts = Maps.newLinkedHashMap();
    private final Map<JetScript, WritableScope> scriptScopes = Maps.newHashMap();

    private StringBuilder debugOutput;


    private TopDownAnalysisParameters topDownAnalysisParameters;

    @Inject
    public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }




    public void debug(Object message) {
        if (debugOutput != null) {
            debugOutput.append(message).append("\n");
        }
    }

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

    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        boolean result = containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
        if (!result) {
            debug(containingFile);
        }
        return result;
    }

    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    public Map<JetFile, WritableScope> getNamespaceScopes() {
        return namespaceScopes;
    }

    public void setRootScope(@NotNull JetScope scope) {
        assert rootScope == null;
        rootScope = scope;
    }

    @NotNull
    public JetScope getRootScope() {
        return rootScope;
    }

    public Map<JetFile, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
    }

    @NotNull
    public Map<JetScript, ScriptDescriptor> getScripts() {
        return scripts;
    }

    @NotNull
    public Map<JetScript, WritableScope> getScriptScopes() {
        return scriptScopes;
    }

    public Map<JetParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    public Map<JetSecondaryConstructor, ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    public Map<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    public Map<JetDeclaration, CallableMemberDescriptor> getMembers() {
        if (members == null) {
            members = Maps.newHashMap();
            members.putAll(functions);
            members.putAll(properties);
            members.putAll(primaryConstructorParameterProperties);
        }
        return members;
    }
}
