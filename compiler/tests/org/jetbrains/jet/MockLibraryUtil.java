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

package org.jetbrains.jet;

import com.intellij.util.io.ZipUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.PathUtil;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;

public class MockLibraryUtil {

    private static Class<?> compilerClass = null;

    public static File compileLibraryToJar(String sourcesPath) {
        try {
            File contentDir = JetTestUtils.tmpDir("lib-content");

            File classesDir = new File(contentDir, "classes");
            compile(sourcesPath, classesDir);

            File jarFile = new File(contentDir, "library.jar");

            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jarFile));
            ZipUtil.addDirToZipRecursively(zip, jarFile, classesDir, "", null, null);
            ZipUtil.addDirToZipRecursively(zip, jarFile, new File(sourcesPath), "src", null, null);
            zip.close();

            return jarFile;
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    // Runs compiler in custom class loader to avoid effects caused by replacing Application with another one created in compiler.
    private static void compile(String sourcesPath, File outDir) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            Class<?> compilerClass = getCompilerClass();
            Object compilerObject = compilerClass.newInstance();
            Method execMethod = compilerClass.getMethod("exec", PrintStream.class, String[].class);

            //noinspection IOResourceOpenedButNotSafelyClosed
            Enum<?> invocationResult = (Enum<?>) execMethod
                    .invoke(compilerObject, new PrintStream(outStream),
                            new String[] {"-src", sourcesPath, "-output", outDir.getAbsolutePath()});

            assertEquals(new String(outStream.toByteArray()), ExitCode.OK.name(), invocationResult.name());
        }
        catch (Throwable e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private static Class<?> getCompilerClass() throws MalformedURLException, ClassNotFoundException {
        if (compilerClass == null) {
            File kotlinCompilerJar = new File(PathUtil.getKotlinPathsForDistDirectory().getLibPath(), "kotlin-compiler.jar");
            File kotlinRuntimeJar = new File(PathUtil.getKotlinPathsForDistDirectory().getLibPath(), "kotlin-runtime.jar");
            URLClassLoader classLoader = new URLClassLoader(new URL[] {kotlinCompilerJar.toURI().toURL(), kotlinRuntimeJar.toURI().toURL()},
                                                            Object.class.getClassLoader());

            compilerClass = classLoader.loadClass(K2JVMCompiler.class.getName());
        }
        return compilerClass;
    }

    private MockLibraryUtil() {
    }
}
