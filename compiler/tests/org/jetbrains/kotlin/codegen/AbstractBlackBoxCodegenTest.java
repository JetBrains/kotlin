/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Processor;
import kotlin.collections.ArraysKt;
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {
    private boolean addRuntime = false;
    private boolean addReflect = false;

    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
        TestJdkKind jdkKind = getJdkKind(files);

        List<String> javacOptions = new ArrayList<String>(0);
        for (TestFile file : files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_RUNTIME")) {
                addRuntime = true;
            }
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_REFLECT")) {
                addReflect = true;
            }

            javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(file.content, "// JAVAC_OPTIONS:"));
        }

        configurationKind =
                addReflect ? ConfigurationKind.ALL :
                addRuntime ? ConfigurationKind.NO_KOTLIN_REFLECT :
                ConfigurationKind.JDK_ONLY;

        compileAndRun(files, javaFilesDir, jdkKind, javacOptions);
    }

    @SuppressWarnings("WeakerAccess")
    protected void compileAndRun(
            @NotNull List<TestFile> files,
            @Nullable File javaSourceDir,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<String> javacOptions
    ) {
        CompilerConfiguration configuration = createCompilerConfigurationForTests(
                configurationKind, jdkKind,
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(new File[] {javaSourceDir}),
                files
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        loadMultiFiles(files);

        classFileFactory = GenerationUtils.compileManyFilesGetGenerationStateForTest(
                myEnvironment.getProject(), myFiles.getPsiFiles(), new JvmPackagePartProvider(myEnvironment),
                myEnvironment.getConfiguration()
        ).getFactory();

        if (javaSourceDir != null) {
            // If there are Java files, they should be compiled against the class files produced by Kotlin, so we dump them to the disk
            File kotlinOut;
            try {
                kotlinOut = KotlinTestUtils.tmpDir(toString());
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }

            OutputUtilsKt.writeAllTo(classFileFactory, kotlinOut);

            File output = CodegenTestUtil.compileJava(
                    findJavaSourcesInDirectory(javaSourceDir), Collections.singletonList(kotlinOut.getPath()), javacOptions
            );
            // Add javac output to classpath so that the created class loader can find generated Java classes
            JvmContentRootsKt.addJvmClasspathRoot(configuration, output);
        }

        blackBox();
    }

    @NotNull
    protected static List<String> findJavaSourcesInDirectory(@NotNull File directory) {
        final List<String> javaFilePaths = new ArrayList<String>(1);

        FileUtil.processFilesRecursively(directory, new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile() && FilesKt.getExtension(file).equals(JavaFileType.DEFAULT_EXTENSION)) {
                    javaFilePaths.add(file.getPath());
                }
                return true;
            }
        });

        return javaFilePaths;
    }

    protected void blackBox() {
        // If there are many files, the first 'box(): String' function will be executed.
        GeneratedClassLoader generatedClassLoader = generateAndCreateClassLoader();
        for (KtFile firstFile : myFiles.getPsiFiles()) {
            String className = getFacadeFqName(firstFile);
            if (className == null) continue;
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

    @Nullable
    private static String getFacadeFqName(@NotNull KtFile firstFile) {
        for (KtDeclaration declaration : firstFile.getDeclarations()) {
            if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction) {
                return JvmFileClassUtil.getFileClassInfoNoResolve(firstFile).getFacadeClassFqName().asString();
            }
        }
        return null;
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
