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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.modules.ModuleBuilder;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.ClassFileFactory;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractCompileKotlinAgainstKotlinTest extends TestCaseWithTmpdir {
    private File aDir;
    private File bDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        aDir = new File(tmpdir, "a");
        bDir = new File(tmpdir, "b");
        KotlinTestUtils.mkdirs(aDir);
        KotlinTestUtils.mkdirs(bDir);
    }

    public void doTest(@NotNull String fileName) throws Exception {
        compileA(new File(fileName));
        String fileNameB = fileName.replaceFirst("A\\.kt$", "B.kt");
        compileB(new File(fileNameB));
        invokeMain(fileNameB);
    }


    private void invokeMain(@NotNull String fileName) throws Exception {
        Method main = generatedClass(fileName).getMethod("main", String[].class);
        main.invoke(null, new Object[] {ArrayUtil.EMPTY_STRING_ARRAY});
    }

    private void invokeBox(@NotNull String fileName) throws Exception {
        Method box = generatedClass(fileName).getMethod("box");
        String result = (String) box.invoke(null);
        assertEquals("OK", result);
    }

    @NotNull
    private Class<?> generatedClass(@NotNull String fileName) throws Exception {
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ bDir.toURI().toURL(), aDir.toURI().toURL() },
                ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        );
        String fileLastName = new File(fileName).getName();
        return classLoader.loadClass(PackagePartClassUtils.getFilePartShortName(fileLastName));
    }

    private ClassFileFactory compileA(@NotNull File ktAFile) throws IOException {
        KotlinCoreEnvironment jetCoreEnvironment = KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(getTestRootDisposable(),
                                                                                                                  ConfigurationKind.ALL);
        return compileKotlin(ktAFile, aDir, jetCoreEnvironment, getTestRootDisposable());
    }

    private ClassFileFactory compileB(@NotNull File ktBFile) throws IOException {
        CompilerConfiguration configurationWithADirInClasspath = KotlinTestUtils
                .compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, KotlinTestUtils.getAnnotationsJar(), aDir);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configurationWithADirInClasspath, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        return compileKotlin(ktBFile, bDir, environment, getTestRootDisposable());
    }

    private ClassFileFactory compileKotlin(
            @NotNull File file, @NotNull File outputDir, @NotNull KotlinCoreEnvironment jetCoreEnvironment,
            @NotNull Disposable disposable
    ) throws IOException {

        String text = FileUtil.loadFile(file, true);

        KtFile psiFile = KotlinTestUtils.createFile(file.getName(), text, jetCoreEnvironment.getProject());

        ModuleVisibilityManager.SERVICE.getInstance(jetCoreEnvironment.getProject()).addModule(new ModuleBuilder("module for test", tmpdir.getAbsolutePath(), "test"));

        ClassFileFactory outputFiles = GenerationUtils.compileFileGetClassFileFactoryForTest(psiFile, jetCoreEnvironment);

        OutputUtilsKt.writeAllTo(outputFiles, outputDir);

        Disposer.dispose(disposable);
        return outputFiles;
    }

    @NotNull
    protected Pair<ClassFileFactory, ClassFileFactory> doBoxTest(@NotNull String firstFileName) {
        List<String> files = Arrays.asList(firstFileName, StringsKt.substringBeforeLast(firstFileName, "1.kt", "") + "2.kt");

        ClassFileFactory factory1 = null;
        ClassFileFactory factory2 = null;
        try {
            factory1 = compileA(new File(files.get(1)));
            factory2 = compileB(new File(files.get(0)));
            invokeBox(files.get(0));
        }
        catch (Throwable e) {
            String result = "";
            if (factory1 != null) {
                result += "FIRST: \n\n" + factory1.createText();
            }
            if (factory2 != null) {
                result += "\n\nSECOND: \n\n" + factory2.createText();
            }
            System.out.println(result);
            throw ExceptionUtilsKt.rethrow(e);
        }

        return new Pair<ClassFileFactory, ClassFileFactory>(factory1, factory2);
    }
}
