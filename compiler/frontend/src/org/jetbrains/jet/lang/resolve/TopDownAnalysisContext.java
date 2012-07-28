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
public class TopDownAnalysisContext implements BodiesResolveContext {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetFile, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
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

    @Override
    @Inject
    public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

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
    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        boolean result = containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
        if (!result) {
            debug(containingFile);
        }
        return result;
    }

    @Override
    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    @Override
    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    public Map<JetFile, WritableScope> getNamespaceScopes() {
        return namespaceScopes;
    }

    public Map<JetFile, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
    }

    @Override
    @NotNull
    public Map<JetScript, ScriptDescriptor> getScripts() {
        return scripts;
    }

    @Override
    @NotNull
    public Map<JetScript, WritableScope> getScriptScopes() {
        return scriptScopes;
    }

    public Map<JetParameter, PropertyDescriptor> getPrimaryConstructorParameterProperties() {
        return primaryConstructorParameterProperties;
    }

    @Override
    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public Map<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    @Override
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
