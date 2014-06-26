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

package org.jetbrains.jet.codegen.generated;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.CodegenTestCase;
import org.jetbrains.jet.codegen.GenerationUtils;
import org.jetbrains.jet.codegen.InlineTestUtil;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.utils.UtilsPackage;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;
import static org.jetbrains.jet.lang.resolve.java.PackageClassUtils.getPackageClassFqName;

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
        myEnvironment = JetTestUtils.createEnvironmentWithFullJdk(getTestRootDisposable());
        blackBoxFileByFullPath(filename);
    }

    public void doTestMultiFile(@NotNull String folderName) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.ALL);

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

        Collections.sort(files);

        loadFiles(ArrayUtil.toStringArray(files));
        blackBox();
    }

    public void doTestMultiFileWithInlineCheck(@NotNull String folderName) {
        doTestMultiFile(folderName);
        InlineTestUtil.checkNoCallsToInline(initializedClassLoader.getAllGeneratedFiles());
    }

    private void blackBoxFileAgainstJavaByFullPath(@NotNull String ktFileFullPath) {
        String ktFile = relativePath(new File(ktFileFullPath));
        File javaClassesTempDirectory = compileJava(ktFile.replaceFirst("\\.kt$", ".java"));

        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.ALL, TestJdkKind.MOCK_JDK, JetTestUtils.getAnnotationsJar(), javaClassesTempDirectory));

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
                ConfigurationKind.ALL, TestJdkKind.FULL_JDK, JetTestUtils.getAnnotationsJar()
        );
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, dirFile);
        myEnvironment = JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration);
        loadFiles(ArrayUtil.toStringArray(ktFilePaths));
        classFileFactory =
                GenerationUtils.compileManyFilesGetGenerationStateForTest(myEnvironment.getProject(), myFiles.getPsiFiles()).getFactory();
        File kotlinOut = JetTestUtils.tmpDir(toString());
        OutputUtilsPackage.writeAllTo(classFileFactory, kotlinOut);

        // TODO: support several Java sources
        File javaOut = compileJava(KotlinPackage.single(javaFilePaths), kotlinOut.getPath());
        // Add javac output to classpath so that the created class loader can find generated Java classes
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, javaOut);

        blackBox();
    }

    private void blackBoxFileByFullPath(@NotNull String filename) {
        loadFileByFullPath(filename);
        blackBox();
    }

    private void blackBox() {
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
