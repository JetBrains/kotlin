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
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtScript;
import org.jetbrains.kotlin.resolve.ScriptNameUtil;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.*;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.registerClassNameForScript;

public class KotlinCodegenFacade {

    public static void prepareForCompilation(@NotNull GenerationState state) {
        for (KtFile file : state.getFiles()) {
            if (file.isScript()) {
                // SCRIPT: register class name for scripting from this file, move outside of this function
                KtScript script = file.getScript();
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

        doGenerateFiles(state.getFiles(), state, errorHandler);
    }

    public static void doGenerateFiles(
            @NotNull Collection<KtFile> files,
            @NotNull GenerationState state,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        MultiMap<FqName, KtFile> filesInPackages = new MultiMap<FqName, KtFile>();
        MultiMap<FqName, KtFile> filesInMultifileClasses = new MultiMap<FqName, KtFile>();

        for (KtFile file : files) {
            if (file == null) throw new IllegalArgumentException("A null file given for compilation");

            JvmFileClassInfo fileClassInfo = state.getFileClassesProvider().getFileClassInfo(file);

            if (fileClassInfo.getWithJvmMultifileClass()) {
                filesInMultifileClasses.putValue(fileClassInfo.getFacadeClassFqName(), file);
            }
            else {
                filesInPackages.putValue(file.getPackageFqName(), file);
            }
        }

        Set<FqName> obsoleteMultifileClasses = new HashSet<FqName>(state.getObsoleteMultifileClasses());
        for (FqName multifileClassFqName : Sets.union(filesInMultifileClasses.keySet(), obsoleteMultifileClasses)) {
            doCheckCancelled(state);
            generateMultifileClass(state, multifileClassFqName, filesInMultifileClasses.get(multifileClassFqName), errorHandler);
        }

        Set<FqName> packagesWithObsoleteParts = new HashSet<FqName>(state.getPackagesWithObsoleteParts());
        for (FqName packageFqName : Sets.union(packagesWithObsoleteParts, filesInPackages.keySet())) {
            doCheckCancelled(state);
            generatePackage(state, packageFqName, filesInPackages.get(packageFqName), errorHandler);
        }

        doCheckCancelled(state);
        state.getFactory().done();
    }

    private static void doCheckCancelled(GenerationState state) {
        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();
        }
    }

    public static void generatePackage(
            @NotNull GenerationState state,
            @NotNull FqName packageFqName,
            @NotNull Collection<KtFile> jetFiles,
            @NotNull CompilationErrorHandler errorHandler
    ) {
        // We do not really generate package class, but use old package fqName to identify package in module-info.
        //FqName packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName);
        PackageCodegen codegen = state.getFactory().forPackage(packageFqName, jetFiles);
        codegen.generate(errorHandler);
    }

    private static void generateMultifileClass(
            @NotNull GenerationState state,
            @NotNull FqName multifileClassFqName,
            @NotNull Collection<KtFile> files,
            @NotNull CompilationErrorHandler handler
    ) {
        MultifileClassCodegen codegen = state.getFactory().forMultifileClass(multifileClassFqName, files);
        codegen.generate(handler);
    }

    private KotlinCodegenFacade() {}
}
