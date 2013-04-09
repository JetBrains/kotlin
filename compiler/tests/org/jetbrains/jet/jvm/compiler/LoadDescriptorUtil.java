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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.*;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.SubModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.KotlinLightClassResolver;
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
    public static PackageViewDescriptor compileKotlinAndLoadTestNamespaceDescriptorFromBinary(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileKotlinToDirAndGetAnalyzeExhaust(kotlinFile, outDir, disposable, configurationKind);
        return loadTestNamespaceAndBindingContextFromJavaRoot(outDir, disposable, ConfigurationKind.JDK_ONLY).first;
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = compileKotlinToDirAndGetFileAndExhaust(kotlinFile, outDir, disposable, configurationKind);
        return fileAndExhaust.getExhaust();
    }

    @NotNull
    public static SubModuleDescriptor compileKotlinToDirAndGetSubModule(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = compileKotlinToDirAndGetFileAndExhaust(kotlinFile, outDir, disposable, configurationKind);
        return fileAndExhaust.getExhaust().getModuleSourcesManager().getSubModuleForFile(fileAndExhaust.getJetFile());
    }

    @NotNull
    private static JetFileAndExhaust compileKotlinToDirAndGetFileAndExhaust(
            File kotlinFile,
            File outDir,
            Disposable disposable,
            ConfigurationKind configurationKind
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(kotlinFile, disposable, configurationKind);
        GenerationState state = GenerationUtils
                .compileFilesGetGenerationState(fileAndExhaust.getJetFile().getProject(), fileAndExhaust.getExhaust(),
                                                Collections.singletonList(
                                                        fileAndExhaust.getJetFile()));
        ClassFileFactory classFileFactory = state.getFactory();
        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, outDir);
        return fileAndExhaust;
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestNamespaceAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        Disposer.dispose(disposable);

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                configurationKind, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(),
                javaRoot,
                ForTestCompileRuntime.runtimeJarForTests(),
                new File("compiler/tests") // for @ExpectLoadError annotation
        );
        TestCoreEnvironment coreEnvironment = new TestCoreEnvironment(disposable, configuration);
        JavaDescriptorResolver javaDescriptorResolver = coreEnvironment.getJavaDescriptorResolver();
        PackageViewDescriptor packageViewDescriptor =
                javaDescriptorResolver.resolveNamespace(TEST_PACKAGE_FQNAME, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        assert packageViewDescriptor != null;
        return Pair.create(packageViewDescriptor, coreEnvironment.getBindingTrace().getBindingContext());
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    )
            throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return loadTestNamespaceAndBindingContextFromJavaRoot(outDir, disposable, configurationKind);
    }

    private static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath();
        JetTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-sourcepath", "compiler/tests", // for @ExpectLoadError annotation
                "-d", outDir.getPath()
        ));
    }

    @NotNull
    public static PackageViewDescriptor analyzeKotlinAndLoadTestNamespace(@NotNull File ktFile, @NotNull Disposable disposable, @NotNull ConfigurationKind configurationKind) throws Exception {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(ktFile, disposable, configurationKind);
        PackageViewDescriptor packageView = fileAndExhaust.getExhaust().getModuleSourcesManager().getSubModuleForFile(
                fileAndExhaust.getJetFile())
                .getPackageView(TEST_PACKAGE_FQNAME);
        assert packageView != null: TEST_PACKAGE_FQNAME + " package not found in " + ktFile.getName();
        return packageView;
    }

    private static class JetFileAndExhaust {

        @NotNull
        public static JetFileAndExhaust createJetFileAndAnalyze(@NotNull File kotlinFile, @NotNull Disposable disposable, @NotNull ConfigurationKind configurationKind)
                throws IOException {
            TestCoreEnvironment coreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, configurationKind);
            JetFile jetFile = JetTestUtils.createFile(coreEnvironment.getProject(), kotlinFile.getName(), FileUtil.loadFile(kotlinFile,
                                                                                                                               true));
            AnalyzeExhaust exhaust = AnalyzerUtilForTests.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
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
