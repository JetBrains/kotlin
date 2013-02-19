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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A storage for the part of {@see TopDownAnalysisContext} collected during headers analysis that will be used during resolution of
 * bodies
*/
public class CachedBodiesResolveContext implements BodiesResolveContext {
    private final Collection<JetFile> files;
    private final Map<JetClass, MutableClassDescriptor> classes;
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects;
    private final Map<JetProperty, PropertyDescriptor> properties;
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions;
    private final Function<JetDeclaration, JetScope> declaringScopes;
    private final Map<JetScript, ScriptDescriptor> scripts;
    private final Map<JetScript, WritableScope> scriptScopes;

    private @NotNull TopDownAnalysisParameters topDownAnalysisParameters;

    public CachedBodiesResolveContext(TopDownAnalysisContext context) {
        files = Collections.unmodifiableCollection(context.getFiles());
        classes = Collections.unmodifiableMap(context.getClasses());
        objects = Collections.unmodifiableMap(context.getObjects());
        properties = Collections.unmodifiableMap(context.getProperties());
        functions = Collections.unmodifiableMap(context.getFunctions());
        declaringScopes = context.getDeclaringScopes();
        scripts = Collections.unmodifiableMap(context.getScripts());
        scriptScopes = Collections.unmodifiableMap(context.getScriptScopes());

        topDownAnalysisParameters = context.getTopDownAnalysisParameters();
    }

    @Override
    public Collection<JetFile> getFiles() {
        return files;
    }

    @Override
    public Map<JetClass, MutableClassDescriptor> getClasses() {
        return classes;
    }

    @Override
    public Map<JetObjectDeclaration, MutableClassDescriptor> getObjects() {
        return objects;
    }

    @Override
    public Map<JetProperty, PropertyDescriptor> getProperties() {
        return properties;
    }

    @Override
    public Map<JetNamedFunction, SimpleFunctionDescriptor> getFunctions() {
        return functions;
    }

    @Override
    public Function<JetDeclaration, JetScope> getDeclaringScopes() {
        return declaringScopes;
    }

    @Override
    public Map<JetScript, ScriptDescriptor> getScripts() {
        return scripts;
    }

    @Override
    public Map<JetScript, WritableScope> getScriptScopes() {
        return scriptScopes;
    }

    @Override
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters parameters) {
        topDownAnalysisParameters = parameters;
    }

    @Override
    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        return containingFile != null && topDownAnalysisParameters.getAnalyzeCompletely().apply(containingFile);
    }
}
