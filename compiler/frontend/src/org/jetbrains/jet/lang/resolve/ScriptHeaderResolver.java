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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.ref.JetTypeName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ScriptHeaderResolver {

    public static final Key<Integer> PRIORITY_KEY = Key.create(JetScript.class.getName() + ".priority");

    @NotNull
    private NamespaceFactory namespaceFactory;
    @NotNull
    private DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver;
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private BindingTrace trace;
    @NotNull
    private TopDownAnalysisParameters topDownAnalysisParameters;

    @Inject
    public void setNamespaceFactory(@NotNull NamespaceFactory namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setDependencyClassByQualifiedNameResolver(@NotNull DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver) {
        this.dependencyClassByQualifiedNameResolver = dependencyClassByQualifiedNameResolver;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }


    @NotNull
    private ClassDescriptor resolveClass(@NotNull FqName className) {
        ClassDescriptor classDescriptor = dependencyClassByQualifiedNameResolver.resolveClass(className);
        if (classDescriptor == null) {
            throw new IllegalStateException("dependency class not found by name: " + className);
        }
        return classDescriptor;
    }

    @NotNull
    public JetType resolveTypeName(@NotNull JetTypeName typeName) {
        List<JetType> typeArguments = new ArrayList<JetType>();
        for (JetTypeName typeArgumentName : typeName.getArguments()) {
            typeArguments.add(resolveTypeName(typeArgumentName));
        }
        ClassDescriptor classDescriptor = resolveClass(typeName.getClassName());
        return TypeUtils.substituteParameters(classDescriptor, typeArguments);
    }


    @NotNull
    private ValueParameterDescriptor resolveScriptParameter(
            @NotNull AnalyzerScriptParameter scriptParameter,
            int index,
            @NotNull ScriptDescriptor script) {
        JetType type = resolveTypeName(scriptParameter.getType());
        return new ValueParameterDescriptorImpl(script, index, Collections.<AnnotationDescriptor>emptyList(), scriptParameter.getName(), type, false, null);
    }

    public void processScriptHierarchy(@NotNull JetScript script, @NotNull JetScope outerScope) {
        JetFile file = (JetFile) script.getContainingFile();
        JetNamespaceHeader namespaceHeader = file.getNamespaceHeader();
        FqName fqName = namespaceHeader != null ? new FqName(namespaceHeader.getQualifiedName()) : FqName.ROOT;
        PackageFragmentDescriptor ns = namespaceFactory.createNamespaceDescriptorPathIfNeeded(fqName);

        Integer priority = script.getUserData(PRIORITY_KEY);
        if (priority == null) {
            priority = 0;
        }

        Name className = new FqName(ScriptNameUtil.classNameForScript((JetFile) script.getContainingFile()).replace('/', '.')).shortName();
        ScriptDescriptor scriptDescriptor = new ScriptDescriptor(ns, priority, outerScope, className);

        //WriteThroughScope scriptScope = new WriteThroughScope(
        //        outerScope, ns.getMemberScope(), new TraceBasedRedeclarationHandler(trace));
        WritableScopeImpl scriptScope = new WritableScopeImpl(outerScope, scriptDescriptor, RedeclarationHandler.DO_NOTHING, "script");
        scriptScope.changeLockLevel(WritableScope.LockLevel.BOTH);

        context.getScriptScopes().put(script, scriptScope);
        context.getScripts().put(script, scriptDescriptor);

        trace.record(BindingContext.SCRIPT, script, scriptDescriptor);

        ((WritableScope)outerScope).addClassifierDescriptor(scriptDescriptor.getClassDescriptor());
    }

    public void resolveScriptDeclarations() {
        for (Map.Entry<JetScript, ScriptDescriptor> e : context.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptor descriptor = e.getValue();
            WritableScope scope = context.getScriptScopes().get(declaration);

            List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();

            scope.setImplicitReceiver(descriptor.getThisAsReceiverParameter());

            JetFile file = (JetFile) declaration.getContainingFile();
            JetScriptDefinition scriptDefinition = JetScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file);

            int index = 0;
            List<AnalyzerScriptParameter> scriptParameters = !scriptDefinition.getScriptParameters().isEmpty()
                                                       ? scriptDefinition.getScriptParameters()
                                                       : topDownAnalysisParameters.getScriptParameters();

            for (AnalyzerScriptParameter scriptParameter : scriptParameters) {
                ValueParameterDescriptor parameter = resolveScriptParameter(scriptParameter, index, descriptor);
                valueParameters.add(parameter);
                scope.addVariableDescriptor(parameter);
                ++index;
            }

            descriptor.setValueParameters(valueParameters);
        }
    }
}
