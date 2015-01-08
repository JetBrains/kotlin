/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Sets;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.ScriptNameUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.registerClassNameForScript;

public class KotlinCodegenFacade {

    public static void prepareForCompilation(@NotNull GenerationState state) {
        for (JetFile file : state.getFiles()) {
            if (file.isScript()) {
                // SCRIPT: register class name for scripting from this file, move outside of this function
                JetScript script = file.getScript();
                assert script != null;

                FqName name = ScriptNameUtil.classNameForScript(script);
                Type type = AsmUtil.asmTypeByFqNameWithoutInnerClasses(name);
                registerClassNameForScript(state.getBindingTrace(), script, type);
            }
        }

        state.beforeCompile();
    }

    public static void compileCorrectFiles(
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        prepareForCompilation(state);

        MultiMap<FqName, JetFile> packageFqNameToFiles = new MultiMap<FqName, JetFile>();
        for (JetFile file : state.getFiles()) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");
            packageFqNameToFiles.putValue(file.getPackageFqName(), file);
        }

        Set<FqName> removedPackageFiles = new HashSet<FqName>(state.getPackagesWithRemovedFiles());
        for (FqName fqName : Sets.union(removedPackageFiles, packageFqNameToFiles.keySet())) {
            generatePackage(state, fqName, packageFqNameToFiles.get(fqName), errorHandler);
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
