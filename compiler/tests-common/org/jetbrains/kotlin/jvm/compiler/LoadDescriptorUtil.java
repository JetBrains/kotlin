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
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.test.KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;

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
            @NotNull ConfigurationKind configurationKind,
            boolean useTypeTableInSerializer
    ) {
        KtFilesAndAnalysisResult filesAndResult =
                KtFilesAndAnalysisResult.createJetFilesAndAnalyze(kotlinFiles, disposable, configurationKind);
        AnalysisResult result = filesAndResult.getAnalysisResult();
        GenerationState state = GenerationUtils.compileFiles(
                result, filesAndResult.getKtFiles(), useTypeTableInSerializer, CompilerConfiguration.EMPTY
        );
        OutputUtilsKt.writeAllTo(state.getFactory(), outDir);
        return result;
    }

    @NotNull
    public static Pair<PackageViewDescriptor, BindingContext> loadTestPackageAndBindingContextFromJavaRoot(
            @NotNull File javaRoot,
            @NotNull Disposable disposable,
            @NotNull TestJdkKind testJdkKind,
            @NotNull ConfigurationKind configurationKind,
            boolean isBinaryRoot
    ) {
        List<File> javaBinaryRoots = new ArrayList<File>();
        javaBinaryRoots.add(KotlinTestUtils.getAnnotationsJar());

        List<File> javaSourceRoots = new ArrayList<File>();
        javaSourceRoots.add(new File("compiler/testData/loadJava/include"));
        if (isBinaryRoot) {
            javaBinaryRoots.add(javaRoot);
        }
        else {
            javaSourceRoots.add(javaRoot);
        }
        CompilerConfiguration configuration = KotlinTestUtils.compilerConfigurationForTests(
                configurationKind,
                testJdkKind,
                javaBinaryRoots,
                javaSourceRoots
        );
        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        AnalysisResult analysisResult = JvmResolveUtil.analyze(environment);

        PackageViewDescriptor packageView = analysisResult.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        return Pair.create(packageView, analysisResult.getBindingContext());
    }

    public static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        String classPath = ForTestCompileRuntime.runtimeJarForTests() + File.pathSeparator +
                           KotlinTestUtils.getAnnotationsJar().getPath();
        KotlinTestUtils.compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", classPath,
                "-sourcepath", "compiler/testData/loadJava/include",
                "-d", outDir.getPath()
        ));
    }

    private static class KtFilesAndAnalysisResult {
        @NotNull
        public static KtFilesAndAnalysisResult createJetFilesAndAnalyze(
                @NotNull List<File> kotlinFiles,
                @NotNull Disposable disposable,
                @NotNull ConfigurationKind configurationKind
        ) {
            final KotlinCoreEnvironment environment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, configurationKind);
            List<KtFile> ktFiles = ContainerUtil.map(kotlinFiles, new Function<File, KtFile>() {
                @Override
                public KtFile fun(File kotlinFile) {
                    try {
                        return KotlinTestUtils.createFile(
                                kotlinFile.getName(), FileUtil.loadFile(kotlinFile, true), environment.getProject()
                        );
                    }
                    catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }
            });
            AnalysisResult result = JvmResolveUtil.analyzeAndCheckForErrors(ktFiles, environment);
            return new KtFilesAndAnalysisResult(ktFiles, result);
        }

        private final List<KtFile> ktFiles;
        private final AnalysisResult result;

        private KtFilesAndAnalysisResult(@NotNull List<KtFile> ktFiles, @NotNull AnalysisResult result) {
            this.ktFiles = ktFiles;
            this.result = result;
        }

        @NotNull
        public List<KtFile> getKtFiles() {
            return ktFiles;
        }

        @NotNull
        public AnalysisResult getAnalysisResult() {
            return result;
        }
    }
}
