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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.registerClassNameForScript;

public class KotlinCodegenFacade {
    public static void compileCorrectFiles(
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        for (JetFile file : state.getFiles()) {
            if (file.isScript()) {
                String name = ScriptNameUtil.classNameForScript(file);
                JetScript script = file.getScript();
                assert script != null;
                registerClassNameForScript(state.getBindingTrace(), script, JvmClassName.byInternalName(name));
            }
        }

        state.getScriptCodegen().registerEarlierScripts(Collections.<Pair<ScriptDescriptor, Type>>emptyList());

        state.beforeCompile();

        MultiMap<FqName, JetFile> namespaceGrouping = new MultiMap<FqName, JetFile>();
        for (JetFile file : state.getFiles()) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");
            namespaceGrouping.putValue(JetPsiUtil.getFQName(file), file);
        }

        for (Map.Entry<FqName, Collection<JetFile>> entry : namespaceGrouping.entrySet()) {
            generateNamespace(state, entry.getKey(), entry.getValue(), errorHandler);
        }
    }

    public static void generateNamespace(
            @NotNull GenerationState state,
            @NotNull FqName fqName,
            @NotNull Collection<JetFile> jetFiles,
            @NotNull CompilationErrorHandler errorHandler
    ) {
            NamespaceCodegen codegen = state.getFactory().forNamespace(fqName, jetFiles);
            codegen.generate(errorHandler);
    }

    private KotlinCodegenFacade() {}
}
