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
import kotlin.Charsets;
import kotlin.io.IoPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.codegen.CodegenTestCase;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.lazy.JvmPackageMappingProvider;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.cli.jvm.config.ConfigPackage.addJavaSourceRoot;
import static org.jetbrains.kotlin.cli.jvm.config.ConfigPackage.addJvmClasspathRoot;
import static org.jetbrains.kotlin.codegen.CodegenTestUtil.compileJava;
import static org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassFqName;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    public void doTest(@NotNull String filename) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_AND_ANNOTATIONS);
        blackBoxFileByFullPath(filename);
    }

    public void doTestAgainstJava(@NotNull String filename) {
        blackBoxFileAgainstJavaByFullPath(filename);
    }

    public void doTestWithJava(@NotNull String filename) {
        try {
            blackBoxFileWithJavaByFullPath(filename);
        }
        catch (Exception e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    public void doTestWithStdlib(@NotNull String filename) {
        myEnvironment = JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                getTestRootDisposable(), getTestConfigurationKind(filename), getTestJdkKind(filename)
        );
        blackBoxFileByFullPath(filename);
    }

    private static ConfigurationKind getTestConfigurationKind(String filename) {
        return InTextDirectivesUtils.isDirectiveDefined(
                IoPackage.readText(new File(filename), Charsets.UTF_8), "NO_KOTLIN_REFLECT"
        ) ? ConfigurationKind.NO_KOTLIN_REFLECT : ConfigurationKind.ALL;
    }

    public void doTestMultiFile(@NotNull String folderName) {
        final List<String> files = new ArrayList<String>(2);
        FileUtil.processFilesRecursively(new File(folderName), new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.getName().endsWith(".kt")) {
                    files.add(relativePath(file));
                }
                return true;
            }
        });

        doTestMultiFile(files);
    }

    protected void doTestMultiFile(@NotNull List<String> files) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);
        Collections.sort(files);
        loadFiles(ArrayUtil.toStringArray(files));
        blackBox();
    }

    // NOTE: tests under fullJdk/ are run with FULL_JDK instead of MOCK_JDK
    @NotNull
    private static TestJdkKind getTestJdkKind(@NotNull String sourcePath) {
        if (sourcePath.contains("compiler/testData/codegen/boxWithStdlib/fullJdk")) {
            return TestJdkKind.FULL_JDK;
        }

        return InTextDirectivesUtils.isDirectiveDefined(
                IoPackage.readText(new File(sourcePath), Charsets.UTF_8), "FULL_JDK"
        ) ? TestJdkKind.FULL_JDK : TestJdkKind.MOCK_JDK;
    }

    private void blackBoxFileAgainstJavaByFullPath(@NotNull String ktFileFullPath) {
        String ktFile = relativePath(new File(ktFileFullPath));
        File javaClassesTempDirectory = compileJava(ktFile.replaceFirst("\\.kt$", ".java"));

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                JetTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.ALL, getTestJdkKind(ktFileFullPath), JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory
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

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar()
        );
        addJavaSourceRoot(configuration, dirFile);
        myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        loadFiles(ArrayUtil.toStringArray(ktFilePaths));
        classFileFactory =
                GenerationUtils.compileManyFilesGetGenerationStateForTest(myEnvironment.getProject(), myFiles.getPsiFiles(),
                                                                          new JvmPackageMappingProvider(myEnvironment)).getFactory();
        File kotlinOut = JetTestUtils.tmpDir(toString());
        OutputUtilsPackage.writeAllTo(classFileFactory, kotlinOut);

        File javaOut = compileJava(javaFilePaths, kotlinOut.getPath());
        // Add javac output to classpath so that the created class loader can find generated Java classes
        addJvmClasspathRoot(configuration, javaOut);

        blackBox();
    }

    private void blackBoxFileByFullPath(@NotNull String filename) {
        loadFileByFullPath(filename);
        blackBox();
    }

    protected void blackBox() {
        // If there are many files, the first of them should contain the 'box(): String' function
        JetFile firstFile = myFiles.getPsiFiles().get(0);
        String fqName = getPackageClassFqName(firstFile.getPackageFqName()).asString();

        Class<?> aClass = generateClass(fqName);
        try {
            Method method = aClass.getMethod("box");
            String r = (String) method.invoke(null);
            assertEquals("OK", r);
        }
        catch (Throwable e) {
            System.out.println(generateToText());
            throw UtilsPackage.rethrow(e);
        }
    }
}
