/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CompileEnvironmentUtil;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.junit.Assert;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.jetbrains.jet.JetTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations;
import static org.jetbrains.jet.lang.psi.JetPsiFactory.createFile;

/**
 * @author Pavel Talanov
 */
public final class LoadDescriptorUtil {

    @NotNull
    public static final FqName TEST_PACKAGE_FQNAME = FqName.topLevel(Name.identifier("test"));

    private LoadDescriptorUtil() {
    }

    @NotNull
    public static NamespaceDescriptor compileKotlinAndExtractTestNamespaceDescriptorFromBinary(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable
    )
            throws IOException {
        compileKotlinToDirAndGetAnalyzeExhaust(kotlinFile, outDir, disposable);
        return extractTestNamespaceFromBinaries(outDir, disposable);
    }

    @NotNull
    public static AnalyzeExhaust compileKotlinToDirAndGetAnalyzeExhaust(
            @NotNull File kotlinFile,
            @NotNull File outDir,
            @NotNull Disposable disposable
    ) throws IOException {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(kotlinFile, disposable);
        GenerationState state = GenerationUtils.compileFilesGetGenerationState(fileAndExhaust.getExhaust(), Collections.singletonList(
                fileAndExhaust.getJetFile()));
        ClassFileFactory classFileFactory = state.getFactory();
        CompileEnvironmentUtil.writeToOutputDirectory(classFileFactory, outDir);
        return fileAndExhaust.getExhaust();
    }

    @NotNull
    public static NamespaceDescriptor extractTestNamespaceFromBinaries(@NotNull File outDir, @NotNull Disposable disposable) {
        Disposer.dispose(disposable);

        CompilerConfiguration configuration = CompileCompilerDependenciesTest.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), outDir,
                ForTestCompileRuntime.runtimeJarForTests());
        JetCoreEnvironment jetCoreEnvironment = new JetCoreEnvironment(disposable, configuration);
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL,
                                                                                       jetCoreEnvironment.getProject());
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        NamespaceDescriptor namespaceDescriptor =
                javaDescriptorResolver.resolveNamespace(TEST_PACKAGE_FQNAME, DescriptorSearchRule.ERROR_IF_FOUND_IN_KOTLIN);
        assert namespaceDescriptor != null;
        return namespaceDescriptor;
    }

    @NotNull
    public static NamespaceDescriptor compileJavaAndExtractTestNamespaceFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable
    )
            throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return extractTestNamespaceFromBinaries(outDir, disposable);
    }

    private static void compileJavaWithAnnotationsJar(@NotNull Collection<File> javaFiles, @NotNull File outDir) throws IOException {
        compileJavaToDir(javaFiles, Arrays.asList(
                "-classpath", "out/production/runtime" + File.pathSeparator + JetTestUtils.getAnnotationsJar().getPath(),
                "-d", outDir.getPath()
        ));
    }

    public static void compileJavaToDir(@NotNull Collection<File> javaFiles, @NotNull List<String> options) throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(javaFiles);
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        }
        finally {
            fileManager.close();
        }
    }

    @NotNull
    public static NamespaceDescriptor analyzeKotlinAndExtractTestNamespace(@NotNull File ktFile, @NotNull Disposable disposable) throws Exception {
        JetFileAndExhaust fileAndExhaust = JetFileAndExhaust.createJetFileAndAnalyze(ktFile, disposable);
        //noinspection ConstantConditions
        return fileAndExhaust.getExhaust().getBindingContext().get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, TEST_PACKAGE_FQNAME);
    }

    private static class JetFileAndExhaust {

        @NotNull
        public static JetFileAndExhaust createJetFileAndAnalyze(@NotNull File kotlinFile, @NotNull Disposable disposable)
                throws IOException {
            JetCoreEnvironment jetCoreEnvironment = createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, ConfigurationKind.JDK_ONLY);
            JetFile jetFile = createFile(jetCoreEnvironment.getProject(), FileUtil.loadFile(kotlinFile, true));
            AnalyzeExhaust exhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegrationAndCheckForErrors(
                    jetFile, Collections.<AnalyzerScriptParameter>emptyList(), BuiltinsScopeExtensionMode.ALL);
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
