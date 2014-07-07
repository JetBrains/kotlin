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
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.parsing.JetScriptDefinitionProvider;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;

import java.util.List;

// SCRIPT: Resolve script parameters
public final class ScriptParameterResolver {
    @NotNull
    public static List<ValueParameterDescriptor> resolveScriptParameters(
            @NotNull JetScript declaration,
            @NotNull ScriptDescriptor scriptDescriptor
    ) {
        List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();

        JetFile file = declaration.getContainingJetFile();
        JetScriptDefinition scriptDefinition = JetScriptDefinitionProvider.getInstance(file.getProject()).findScriptDefinition(file);

        int index = 0;
        for (AnalyzerScriptParameter scriptParameter : scriptDefinition.getScriptParameters()) {
            ValueParameterDescriptor parameter = resolveScriptParameter(scriptParameter, index, scriptDescriptor);
            valueParameters.add(parameter);
            ++index;
        }
        return valueParameters;
    }

    @NotNull
    private static ValueParameterDescriptor resolveScriptParameter(
            @NotNull AnalyzerScriptParameter scriptParameter,
            int index,
            @NotNull ScriptDescriptor script
    ) {
        return new ValueParameterDescriptorImpl(script, null, index, Annotations.EMPTY, scriptParameter.getName(),
                                                scriptParameter.getType(), false, null, SourceElement.NO_SOURCE);
    }

    private ScriptParameterResolver() {
    }
}
