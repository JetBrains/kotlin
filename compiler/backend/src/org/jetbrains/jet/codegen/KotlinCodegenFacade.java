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

import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Map;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.registerClassNameForScript;

public class KotlinCodegenFacade {
    public static void compileCorrectFiles(
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        for (JetFile file : state.getFiles()) {
            if (file.isScript()) {
                // SCRIPT: register class name for scripting from this file, move outside of this function
                String name = ScriptNameUtil.classNameForScript(file);
                JetScript script = file.getScript();
                assert script != null;
                registerClassNameForScript(state.getBindingTrace(), script, Type.getObjectType(name));
            }
        }

        state.beforeCompile();

        MultiMap<FqName, JetFile> packageFqNameToFiles = new MultiMap<FqName, JetFile>();
        for (JetFile file : state.getFiles()) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");
            packageFqNameToFiles.putValue(file.getPackageFqName(), file);
        }

        for (Map.Entry<FqName, Collection<JetFile>> entry : packageFqNameToFiles.entrySet()) {
            generatePackage(state, entry.getKey(), entry.getValue(), errorHandler);
        }

        state.getFactory().done();
    }

    public static void generatePackage(
            @NotNull GenerationState state,
            @NotNull FqName fqName,
            @NotNull Collection<JetFile> jetFiles,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        PackageCodegen codegen = state.getFactory().forPackage(fqName, jetFiles);
        codegen.generate(errorHandler);
    }

    private KotlinCodegenFacade() {}
}
