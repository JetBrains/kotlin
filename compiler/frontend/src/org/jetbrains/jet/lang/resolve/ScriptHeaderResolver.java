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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
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
import java.util.List;
import java.util.Map;

// SCRIPT: Resolve declarations in scripts
public class ScriptHeaderResolver {

    public static final Key<Integer> PRIORITY_KEY = Key.create(JetScript.class.getName() + ".priority");

    @NotNull
    private MutablePackageFragmentProvider packageFragmentProvider;
    @NotNull
    private DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver;
    @NotNull
    private BindingTrace trace;

    @Inject
    public void setPackageFragmentProvider(@NotNull MutablePackageFragmentProvider packageFragmentProvider) {
        this.packageFragmentProvider = packageFragmentProvider;
    }

    @Inject
    public void setDependencyClassByQualifiedNameResolver(@NotNull DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver) {
        this.dependencyClassByQualifiedNameResolver = dependencyClassByQualifiedNameResolver;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
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
        return new ValueParameterDescriptorImpl(script, null, index, Annotations.EMPTY, scriptParameter.getName(), type, false, null);
    }

    public void processScriptHierarchy(@NotNull TopDownAnalysisContext c, @NotNull JetScript script, @NotNull JetScope outerScope) {
        JetFile file = (JetFile) script.getContainingFile();
        FqName fqName = file.getPackageFqName();
        PackageFragmentDescriptor ns = packageFragmentProvider.getOrCreateFragment(fqName);

        Integer priority = script.getUserData(PRIORITY_KEY);
        if (priority == null) {
            priority = 0;
        }

        FqName nameForScript = ScriptNameUtil.classNameForScript((JetFile) script.getContainingFile());
        Name className = nameForScript.shortName();
        ScriptDescriptor scriptDescriptor = new ScriptDescriptorImpl(ns, priority, outerScope, className);

        //WriteThroughScope scriptScope = new WriteThroughScope(
        //        outerScope, ns.getMemberScope(), new TraceBasedRedeclarationHandler(trace));
        WritableScopeImpl scriptScope = new WritableScopeImpl(outerScope, scriptDescriptor, RedeclarationHandler.DO_NOTHING, "script");
        scriptScope.changeLockLevel(WritableScope.LockLevel.BOTH);

        c.getScriptScopes().put(script, scriptScope);
        c.getScripts().put(script, scriptDescriptor);

        trace.record(BindingContext.SCRIPT, script, scriptDescriptor);

        ((WritableScope)outerScope).addClassifierDescriptor(scriptDescriptor.getClassDescriptor());
    }

    public void resolveScriptDeclarations(@NotNull TopDownAnalysisContext c) {
        for (Map.Entry<JetScript, ScriptDescriptor> e : c.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptorImpl descriptor = (ScriptDescriptorImpl) e.getValue();
            WritableScope scope = c.getScriptScopes().get(declaration);

            List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();

            scope.setImplicitReceiver(descriptor.getThisAsReceiverParameter());

            JetFile file = (JetFile) declaration.getContainingFile();
            JetScriptDefinition scriptDefinition = JetScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file);

            int index = 0;
            List<AnalyzerScriptParameter> scriptParameters = !scriptDefinition.getScriptParameters().isEmpty()
                                                       ? scriptDefinition.getScriptParameters()
                                                       : c.getTopDownAnalysisParameters().getScriptParameters();

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
