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
import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.ScriptNameUtil;
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
                registerClassNameForScript(state.getBindingTrace(), script, type, state.getFileClassesProvider());
            }
        }

        state.beforeCompile();
    }

    public static void compileCorrectFiles(
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        prepareForCompilation(state);

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        MultiMap<FqName, JetFile> filesInPackageClasses = new MultiMap<FqName, JetFile>();
        MultiMap<FqName, JetFile> filesInMultifileClasses = new MultiMap<FqName, JetFile>();

        for (JetFile file : state.getFiles()) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");

            JvmFileClassInfo fileClassInfo = state.getFileClassesProvider().getFileClassInfo(file);

            if (fileClassInfo.getTEMP_isMultifileClass()) {
                filesInMultifileClasses.putValue(fileClassInfo.getFacadeClassFqName(), file);
            }

            if (state.getPackageFacadesAsMultifileClasses()) {
                if (!fileClassInfo.getTEMP_isMultifileClass()) {
                    filesInMultifileClasses.putValue(PackageClassUtils.getPackageClassFqName(file.getPackageFqName()), file);
                }
            }
            else {
                filesInPackageClasses.putValue(file.getPackageFqName(), file);
            }
        }

        Set<FqName> packagesWithObsoleteParts = new HashSet<FqName>(state.getPackagesWithObsoleteParts());
        for (FqName packageFqName : Sets.union(packagesWithObsoleteParts, filesInPackageClasses.keySet())) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            generatePackage(state, packageFqName, filesInPackageClasses.get(packageFqName), errorHandler);
        }

        for (FqName multifileClassFqName : filesInMultifileClasses.keySet()) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
            generateMultifileClass(state, multifileClassFqName, filesInMultifileClasses.get(multifileClassFqName), errorHandler);
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        state.getFactory().done();
    }

    public static void generatePackage(
            @NotNull GenerationState state,
            @NotNull FqName packageFqName,
            @NotNull Collection<JetFile> jetFiles,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        PackageCodegen codegen = state.getFactory().forPackage(packageFqName, jetFiles);
        codegen.generate(errorHandler);
    }

    private static void generateMultifileClass(
            @NotNull GenerationState state,
            @NotNull FqName multifileClassFqName,
            @NotNull Collection<JetFile> files,
            @NotNull CompilationErrorHandler handler
    ) {
        MultifileClassCodegen codegen = state.getFactory().forMultifileClass(multifileClassFqName, files);
        codegen.generate(handler);
    }

    private KotlinCodegenFacade() {}
}
