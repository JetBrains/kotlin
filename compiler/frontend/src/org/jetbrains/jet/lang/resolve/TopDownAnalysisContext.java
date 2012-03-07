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
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadingConflictResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.defaults.DefaultPicoContainer;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;
import org.picocontainer.defaults.SetterInjectionComponentAdapterFactory;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class TopDownAnalysisContext {

    //private final MutablePicoContainer picoContainer;

    private final ObservableBindingTrace trace;
    private final Configuration configuration;

    @NotNull
    private final DescriptorResolver descriptorResolver;
    @NotNull
    private final ImportsResolver importsResolver;
    @NotNull
    private final BodyResolver bodyResolver;
    @NotNull
    private final DeclarationResolver declarationResolver;
    @NotNull
    private final CallResolver.Context callResolverContext;
    @NotNull
    private final TypeResolver typeResolver;
    @NotNull
    private final ExpressionTypingServices expressionTypingServices;

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
    private boolean analyzingBootstrapLibrary = false;
    private boolean declaredLocally;

    public TopDownAnalysisContext(final JetSemanticServices semanticServices, final BindingTrace trace, Predicate<PsiFile> analyzeCompletely, @NotNull Configuration configuration, boolean declaredLocally) {
        class TdacModule extends AbstractModule {
            @Override
            protected void configure() {
                bind(JetSemanticServices.class).toInstance(semanticServices);
                bind(TopDownAnalysisContext.class).toInstance(TopDownAnalysisContext.this);
            }
        }

        Injector injector = Guice.createInjector(new TdacModule());
        
        this.importsResolver = injector.getInstance(ImportsResolver.class);
        this.bodyResolver = injector.getInstance(BodyResolver.class);
        this.declarationResolver = injector.getInstance(DeclarationResolver.class);
        this.callResolverContext = injector.getInstance(CallResolver.Context.class);
        this.typeResolver = injector.getInstance(TypeResolver.class);
        this.expressionTypingServices = injector.getInstance(ExpressionTypingServices.class);
        this.descriptorResolver = injector.getInstance(DescriptorResolver.class);

        this.trace = new ObservableBindingTrace(trace);
        this.analyzeCompletely = analyzeCompletely;
        this.configuration = configuration;
        this.declaredLocally = declaredLocally;
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

    public void setAnalyzingBootstrapLibrary(boolean analyzingBootstrapLibrary) {
        this.analyzingBootstrapLibrary = analyzingBootstrapLibrary;
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

    public DescriptorResolver getDescriptorResolver() {
        return descriptorResolver;
    }

    public ImportsResolver getImportsResolver() {
        return importsResolver;
    }

    @NotNull
    public BodyResolver getBodyResolver() {
        return bodyResolver;
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

    @NotNull
    public Configuration getConfiguration() {
        return configuration;
    }

    public boolean isDeclaredLocally() {
        return declaredLocally;
    }

    @NotNull
    public DeclarationResolver getDeclarationResolver() {
        return declarationResolver;
    }

    @NotNull
    public CallResolver.Context getCallResolverContext() {
        return callResolverContext;
    }

    @NotNull
    public TypeResolver getTypeResolver() {
        return typeResolver;
    }

    @NotNull
    public ExpressionTypingServices getExpressionTypingServices() {
        return expressionTypingServices;
    }
}
