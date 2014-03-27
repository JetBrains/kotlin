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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.ref.JetTypeName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

// SCRIPT: Resolve script parameters
public class ScriptParameterResolver {
    @NotNull
    private DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver;

    @Inject
    public void setDependencyClassByQualifiedNameResolver(@NotNull DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver) {
        this.dependencyClassByQualifiedNameResolver = dependencyClassByQualifiedNameResolver;
    }

    @NotNull
    public List<ValueParameterDescriptor> resolveScriptParameters(
            @NotNull TopDownAnalysisContext c,
            @NotNull JetScript declaration,
            @NotNull ScriptDescriptor scriptDescriptor
    ) {
        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();

        JetFile file = (JetFile) declaration.getContainingFile();
        JetScriptDefinition scriptDefinition = JetScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file);

        int index = 0;
        List<AnalyzerScriptParameter> scriptParameters = !scriptDefinition.getScriptParameters().isEmpty()
                                                   ? scriptDefinition.getScriptParameters()
                                                   : c.getTopDownAnalysisParameters().getScriptParameters();

        for (AnalyzerScriptParameter scriptParameter : scriptParameters) {
            ValueParameterDescriptor parameter = resolveScriptParameter(scriptParameter, index, scriptDescriptor);
            valueParameters.add(parameter);
            ++index;
        }
        return valueParameters;
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
}
