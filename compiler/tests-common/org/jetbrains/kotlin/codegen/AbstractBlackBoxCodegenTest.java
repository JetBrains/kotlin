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
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
        compile(files, javaSourceDir, configurationKind, jdkKind, javacOptions);

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
            finally {
                clearReflectionCache(generatedClassLoader);
            }
        }
    }

    private static void clearReflectionCache(@NotNull ClassLoader classLoader) {
        try {
            Class<?> klass = classLoader.loadClass(JvmAbi.REFLECTION_FACTORY_IMPL.asSingleFqName().asString());
            Method method = klass.getDeclaredMethod("clearCaches");
            method.invoke(null);
        }
        catch (ClassNotFoundException e) {
            // This is OK for a test without kotlin-reflect in the dependencies
        }
        catch (Exception e) {
            throw ExceptionUtilsKt.rethrow(e);
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
