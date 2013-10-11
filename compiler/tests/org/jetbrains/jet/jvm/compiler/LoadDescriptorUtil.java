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

package org.jetbrains.jet.jvm.compiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;

public final class LoadDescriptorUtil {

    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(kotlinFile, disposable, configurationKind);
        GenerationState state = GenerationUtils.compileFilesGetGenerationState(fileAndExhaust.getJetFile().getProject(), fileAndExhaust.getExhaust(), Collections.singletonList(
                fileAndExhaust.getJetFile()));
        OutputFileCollection outputFiles = state.getFactory();
        OutputUtilsPackage.writeAllTo(outputFiles, outDir);
        return fileAndExhaust.getExhaust();
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestPackageAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                configurationKind, TestJdkKind.MOCK_JDK,
                JetTestUtils.getAnnotationsJar(),
                javaRoot,
                new File("compiler/tests") // for @ExpectLoadError annotation
        );
        JetCoreEnvironment jetCoreEnvironment = JetCoreEnvironment.createForTests(disposable, configuration);
        BindingTraceContext trace = new BindingTraceContext();
        InjectorForJavaDescriptorResolver injector = new InjectorForJavaDescriptorResolver(jetCoreEnvironment.getProject(), trace);
        PackageViewDescriptor packageView = injector.getModule().getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null;

        return Pair.create(packageView, trace.getBindingContext());
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> compileJavaAndLoadTestPackageAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return loadTestPackageAndBindingContextFromJavaRoot(outDir, disposable, configurationKind);
    }

    private static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = "out/production/runtime" +
                           File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath();
        JetTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-sourcepath", "compiler/tests", // for @ExpectLoadError annotation
                "-d", outDir.getPath()
        ));
    }

    @NotNull
    public static PackageViewDescriptor analyzeKotlinAndLoadTestPackage(
            @NotNull File ktFile,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws Exception {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(ktFile, disposable, configurationKind);
        PackageViewDescriptor packageView =
                fileAndExhaust.getExhaust().getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null: TEST_PACKAGE_FQNAME + " package not found in " + ktFile.getName();
        return packageView;
    }

    private static class JetFileAndExhaust {

        @NotNull
        public static JetFileAndExhaust createJetFileAndAnalyze(@NotNull File kotlinFile, @NotNull Disposable disposable, @NotNull ConfigurationKind configurationKind)
                throws IOException {
            JetCoreEnvironment jetCoreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, configurationKind);
            JetFile jetFile = JetTestUtils.createFile(kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), jetCoreEnvironment.getProject());
            AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                    jetFile, Collections.<AnalyzerScriptParameter>emptyList());
            return new JetFileAndExhaust(jetFile, exhaust);
        }

        @NotNull
        private final JetFile jetFile;
        @NotNull
        private final AnalyzeExhaust exhaust;

        private JetFileAndExhaust(@NotNull JetFile file, @NotNull AnalyzeExhaust exhaust) {
            jetFile = file;
            this.exhaust = exhaust;
        }

        @NotNull
        public JetFile getJetFile() {
            return jetFile;
        }

        @NotNull
        public AnalyzeExhaust getExhaust() {
            return exhaust;
        }
    }
}
