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

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzer;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class TopDownAnalysisContext {

    private final ObservableBindingTrace trace;

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetFile, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetFile, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Map<JetNamedFunction, SimpleFunctionDescriptor> functions = Maps.newLinkedHashMap();
    private final Map<JetSecondaryConstructor, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();

    private final Predicate<PsiFile> analyzeCompletely;

    private StringBuilder debugOutput;
    private final boolean analyzingBootstrapLibrary;
    private boolean declaredLocally;

    private final InjectorForTopDownAnalyzer injector;

    public TopDownAnalysisContext(
            final Project project,
            final BindingTrace trace,
            Predicate<PsiFile> analyzeCompletely,
            @NotNull final ModuleConfiguration configuration,
            @NotNull final ModuleDescriptor moduleDescriptor,
            boolean declaredLocally,
            boolean analyzingBootstrapLibrary,
            @Nullable final JetControlFlowDataTraceFactory jetControlFlowDataTraceFactory) {

        if (analyzingBootstrapLibrary == (jetControlFlowDataTraceFactory != null)) {
            throw new IllegalStateException(
                    "jetControlFlowDataTraceFactory must not be passed when analyzingBootstrapLibrary and vice versa");
        }

        this.injector = new InjectorForTopDownAnalyzer(project, this, configuration, moduleDescriptor, jetControlFlowDataTraceFactory);

        this.trace = new ObservableBindingTrace(trace);
        this.analyzeCompletely = analyzeCompletely;
        this.declaredLocally = declaredLocally;
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
    }

    public InjectorForTopDownAnalyzer getInjector() {
        return injector;
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

    public boolean analyzingBootstrapLibrary() {
        return analyzingBootstrapLibrary;
    }

    public boolean completeAnalysisNeeded(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        boolean result = containingFile != null && analyzeCompletely.apply(containingFile);
        if (!result) {
            debug(containingFile);
        }
        return result;
    }

    @NotNull
    public ObservableBindingTrace getTrace() {
        return trace;
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

    public Map<JetFile, NamespaceDescriptorImpl> getNamespaceDescriptors() {
        return namespaceDescriptors;
    }

    public Set<PropertyDescriptor> getPrimaryConstructorParameterProperties() {
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

    public boolean isDeclaredLocally() {
        return declaredLocally;
    }

}
