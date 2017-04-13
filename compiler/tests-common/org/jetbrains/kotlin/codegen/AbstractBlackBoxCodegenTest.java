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
import kotlin.io.FilesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBlackBoxCodegenTest extends CodegenTestCase {

    @Override
    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
        compile(files, javaFilesDir);
        blackBox();
    }


    @NotNull
    protected static List<String> findJavaSourcesInDirectory(@NotNull File directory) {
        List<String> javaFilePaths = new ArrayList<>(1);

        FileUtil.processFilesRecursively(directory, file -> {
            if (file.isFile() && FilesKt.getExtension(file).equals(JavaFileType.DEFAULT_EXTENSION)) {
                javaFilePaths.add(file.getPath());
            }
            return true;
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
