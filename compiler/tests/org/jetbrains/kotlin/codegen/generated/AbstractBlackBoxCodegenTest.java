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

package org.jetbrains.kotlin.codegen.generated;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.CodegenTestCase;
import org.jetbrains.kotlin.codegen.CodegenTestFiles;
import org.jetbrains.kotlin.codegen.GeneratedClassLoader;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.codegen.CodegenTestUtil.compileJava;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    @Override
    protected void doMultiFileTest(File file, Map<String, ModuleAndDependencies> modules, List<TestFile> files) throws Exception {
        if (files.size() == 1) {
            createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
            blackBoxFileByFullPath(file.getPath());
        }
        else {
            doTestMultiFile(files);
        }
    }

    public void doTestAgainstJava(@NotNull String filename) {
        blackBoxFileAgainstJavaByFullPath(filename);
    }

    public void doTestWithJava(@NotNull String filename) {
        try {
            blackBoxFileWithJavaByFullPath(filename);
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public void doTestWithStdlib(@NotNull String filename) {
        configurationKind = InTextDirectivesUtils.isDirectiveDefined(
                FilesKt.readText(new File(filename), Charsets.UTF_8), "NO_KOTLIN_REFLECT"
        ) ? ConfigurationKind.NO_KOTLIN_REFLECT : ConfigurationKind.ALL;

        myEnvironment = KotlinTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                getTestRootDisposable(), configurationKind, getTestJdkKind(filename)
        );

        blackBoxFileByFullPath(filename);
    }

    private void doTestMultiFile(@NotNull List<TestFile> files) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
        Collections.sort(files);

        List<KtFile> ktFiles = new ArrayList<KtFile>(files.size());
        for (TestFile file : files) {
            ktFiles.add(KotlinTestUtils.createFile(file.name, file.content, myEnvironment.getProject()));
        }

        myFiles = CodegenTestFiles.create(ktFiles);

        blackBox();
    }

    // NOTE: tests under fullJdk/ are run with FULL_JDK instead of MOCK_JDK
    @NotNull
    private static TestJdkKind getTestJdkKind(@NotNull String sourcePath) {
        if (sourcePath.contains("compiler/testData/codegen/boxWithStdlib/fullJdk")) {
            return TestJdkKind.FULL_JDK;
        }

        return InTextDirectivesUtils.isDirectiveDefined(
                FilesKt.readText(new File(sourcePath), Charsets.UTF_8), "FULL_JDK"
        ) ? TestJdkKind.FULL_JDK : TestJdkKind.MOCK_JDK;
    }

    private void blackBoxFileAgainstJavaByFullPath(@NotNull String ktFileFullPath) {
        String ktFile = relativePath(new File(ktFileFullPath));
        File javaClassesTempDirectory = compileJava(ktFile.replaceFirst("\\.kt$", ".java"));

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                KotlinTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.ALL, getTestJdkKind(ktFileFullPath), KotlinTestUtils.getAnnotationsJar(), javaClassesTempDirectory
                ),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        loadFile(ktFile);
        blackBox();
    }

    private void blackBoxFileWithJavaByFullPath(@NotNull String directory) throws Exception {
        File dirFile = new File(directory);

        final List<String> javaFilePaths = new ArrayList<String>();
        final List<String> ktFilePaths = new ArrayList<String>();
        FileUtil.processFilesRecursively(dirFile, new Processor<File>() {
            @Override
            public boolean process(File file) {
                String path = relativePath(file);
                if (path.endsWith(".kt")) {
                    ktFilePaths.add(path);
                }
                else if (path.endsWith(".java")) {
                    javaFilePaths.add(path);
                }
                return true;
            }
        });

        CompilerConfiguration configuration = KotlinTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, KotlinTestUtils.getAnnotationsJar()
        );
        JvmContentRootsKt.addJavaSourceRoot(configuration, dirFile);
        myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        loadFiles(ArrayUtil.toStringArray(ktFilePaths));
        classFileFactory =
                GenerationUtils.compileManyFilesGetGenerationStateForTest(myEnvironment.getProject(), myFiles.getPsiFiles(),
                                                                          new JvmPackagePartProvider(myEnvironment)).getFactory();
        File kotlinOut = KotlinTestUtils.tmpDir(toString());
        OutputUtilsKt.writeAllTo(classFileFactory, kotlinOut);

        List<String> javacOptions = new ArrayList<String>(0);
        for (KtFile jetFile : myFiles.getPsiFiles()) {
            javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(jetFile.getText(), "// JAVAC_OPTIONS:"));
        }

        File javaOut = compileJava(javaFilePaths, Collections.singletonList(kotlinOut.getPath()), javacOptions);
        // Add javac output to classpath so that the created class loader can find generated Java classes
        JvmContentRootsKt.addJvmClasspathRoot(configuration, javaOut);

        blackBox();
    }

    private void blackBoxFileByFullPath(@NotNull String filename) {
        loadFileByFullPath(filename);
        blackBox();
    }

    protected void blackBox() {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader();
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = JvmFileClassUtil.getFileClassInfoNoResolve(firstFile).getFacadeClassFqName().asString();
            Class<?> aClass = getGeneratedClass(generatedClassLoader, className);
            try {
                Method method = getBoxMethodOrNull(aClass);
                if (method != null) {
                    String r = (String) method.invoke(null);
                    assertEquals("OK", r);
                    return;
                }
            }
            catch (Throwable e) {
                System.out.println(generateToText());
                throw ExceptionUtilsKt.rethrow(e);
            }
        }
    }

    private static Class<?> getGeneratedClass(GeneratedClassLoader generatedClassLoader, String className) {
        try {
            return generatedClassLoader.loadClass(className);
        }
        catch (ClassNotFoundException e) {
            fail("No class file was generated for: " + className);
        }
        return null;
    }

    private static Method getBoxMethodOrNull(Class<?> aClass) {
        try {
            return aClass.getMethod("box");
        }
        catch (NoSuchMethodException e){
            return null;
        }
    }
}
