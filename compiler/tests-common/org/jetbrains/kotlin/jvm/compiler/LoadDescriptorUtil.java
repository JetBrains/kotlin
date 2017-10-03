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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.text.StringKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.sequences.SequencesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.jvm.compiler.javac.JavacRegistrarForTests;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class LoadDescriptorUtil {
    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static ModuleDescriptor compileKotlinToDirAndGetModule(
            @NotNull List<File> kotlinFiles, @NotNull File outDir, @NotNull KotlinCoreEnvironment environment
    ) {
        GenerationState state = GenerationUtils.compileFiles(createKtFiles(kotlinFiles, environment), environment);
        OutputUtilsKt.writeAllTo(state.getFactory(), outDir);
        return state.getModule();
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestPackageAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull TestJdkKind testJdkKind,
            @NotNull ConfigurationKind configurationKind,
            boolean isBinaryRoot,
            boolean useFastClassReading,
            boolean useJavacWrapper,
            @Nullable LanguageVersionSettings explicitLanguageVersionSettings
    ) {
        List<File> javaBinaryRoots = new ArrayList<>();
        javaBinaryRoots.add(KotlinTestUtils.getAnnotationsJar());

        List<File> javaSourceRoots = new ArrayList<>();
        javaSourceRoots.add(new File("compiler/testData/loadJava/include"));
        if (isBinaryRoot) {
            javaBinaryRoots.add(javaRoot);
        }
        else {
            javaSourceRoots.add(javaRoot);
        }
        CompilerConfiguration configuration =
                KotlinTestUtils.newConfiguration(configurationKind, testJdkKind, javaBinaryRoots, javaSourceRoots);
        configuration.put(JVMConfigurationKeys.USE_FAST_CLASS_FILES_READING, useFastClassReading);
        configuration.put(JVMConfigurationKeys.USE_JAVAC, useJavacWrapper);
        if (explicitLanguageVersionSettings != null) {
            configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, explicitLanguageVersionSettings);
        }
        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        if (useJavacWrapper) {
            JavacRegistrarForTests.INSTANCE.registerJavac(environment);
        }
        AnalysisResult analysisResult = JvmResolveUtil.analyze(environment);

        PackageViewDescriptor packageView = analysisResult.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        return Pair.create(packageView, analysisResult.getBindingContext());
    }

    public static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        List<File> classpath = new ArrayList<>();

        classpath.add(ForTestCompileRuntime.runtimeJarForTests());
        classpath.add(KotlinTestUtils.getAnnotationsJar());

        for (File test: javaFiles) {
            String content = FilesKt.readText(test, Charsets.UTF_8);

            if (InTextDirectivesUtils.isDirectiveDefined(content, "ANDROID_ANNOTATIONS")) {
                classpath.add(ForTestCompileRuntime.androidAnnotationsForTests());
            }
        }

        KotlinTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classpath.stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator)),
                "-sourcepath", "compiler/testData/loadJava/include",
                "-d", outDir.getPath()
        ));
    }

    @NotNull
    private static List<KtFile> createKtFiles(@NotNull List<File> kotlinFiles, @NotNull KotlinCoreEnvironment environment) {
        return CollectionsKt.map(kotlinFiles, kotlinFile -> {
            try {
                return KotlinTestUtils.createFile(kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), environment.getProject());
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        });
    }
}
