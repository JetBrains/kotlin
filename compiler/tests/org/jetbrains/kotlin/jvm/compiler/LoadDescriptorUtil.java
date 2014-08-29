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
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.test.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;

public final class LoadDescriptorUtil {

    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static AnalysisResult compileKotlinToDirAndGetAnalysisResult(
            @NotNull List<File> kotlinFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind
    ) {
        JetFilesAndAnalysisResult filesAndResult = JetFilesAndAnalysisResult.createJetFilesAndAnalyze(kotlinFiles, disposable, configurationKind);
        AnalysisResult result = filesAndResult.getAnalysisResult();
        List<JetFile> files = filesAndResult.getJetFiles();
        GenerationState state = GenerationUtils.compileFilesGetGenerationState(files.get(0).getProject(), result, files);
        OutputUtilsPackage.writeAllTo(state.getFactory(), outDir);
        return result;
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestPackageAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull TestJdkKind testJdkKind,
            @NotNull ConfigurationKind configurationKind
    ) {
        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                configurationKind,
                testJdkKind,
                JetTestUtils.getAnnotationsJar(),
                javaRoot,
                new File("compiler/tests") // for @ExpectLoadError annotation
        );
        JetCoreEnvironment environment =
                JetCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        BindingTrace trace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
        ModuleDescriptor module = LazyResolveTestUtil.resolve(environment.getProject(), trace, Collections.<JetFile>emptyList());

        PackageViewDescriptor packageView = module.getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null;

        return Pair.create(packageView, trace.getBindingContext());
    }

    public static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = ForTestCompileRuntime.runtimeJarForTests() + File.pathSeparator +
                           JetTestUtils.getAnnotationsJar().getPath();
        JetTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-sourcepath", "compiler/tests", // for @ExpectLoadError annotation
                "-d", outDir.getPath()
        ));
    }

    private static class JetFilesAndAnalysisResult {
        @NotNull
        public static JetFilesAndAnalysisResult createJetFilesAndAnalyze(
                @NotNull List<File> kotlinFiles,
                @NotNull Disposable disposable,
                @NotNull ConfigurationKind configurationKind
        ) {
            final JetCoreEnvironment jetCoreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, configurationKind);
            List<JetFile> jetFiles = ContainerUtil.map(kotlinFiles, new Function<File, JetFile>() {
                @Override
                public JetFile fun(File kotlinFile) {
                    try {
                        return JetTestUtils.createFile(
                                kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), jetCoreEnvironment.getProject());
                    }
                    catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
            });
            AnalysisResult result = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                    jetCoreEnvironment.getProject(), jetFiles);
            return new JetFilesAndAnalysisResult(jetFiles, result);
        }

        private final List<JetFile> jetFiles;
        private final AnalysisResult result;

        private JetFilesAndAnalysisResult(@NotNull List<JetFile> jetFiles, @NotNull AnalysisResult result) {
            this.jetFiles = jetFiles;
            this.result = result;
        }

        @NotNull
        public List<JetFile> getJetFiles() {
            return jetFiles;
        }

        @NotNull
        public AnalysisResult getAnalysisResult() {
            return result;
        }
    }
}
