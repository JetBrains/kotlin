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

package org.jetbrains.jet.codegen;

import com.google.common.io.Files;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Compile stdlib.jar that can be used in tests
 *
 * @see #stdlibJarForTests()
 *
 * @author Stepan Koltsov
 */
public class ForTestCompileStdlib {
    private static File stdlibJarFile;

    private ForTestCompileStdlib() {
    }

    private static File doCompile() throws Exception {
        File tmpDir = JetTestUtils.tmpDir("stdlibjar");

        File jarFile = new File(tmpDir, "stdlib.jar");
        
        File classesDir = new File(tmpDir, "classes");

        FileUtil.createParentDirs(new File(classesDir, "dummy"));
        compileJavaPartOfStdlib(classesDir);
        compileKotlinPartOfStdlib(classesDir);

        FileOutputStream stdlibJar = new FileOutputStream(jarFile);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(stdlibJar);
            try {
                copyToJar(classesDir, jarOutputStream);
            }
            finally {
                jarOutputStream.close();
            }
        }
        finally {
            stdlibJar.close();
        }
        
        FileUtil.delete(classesDir);
        return jarFile;

    }
    private static void copyToJar(File root, JarOutputStream os) throws IOException {
        Stack<Pair<String, File>> toCopy = new Stack<Pair<String, File>>();
        toCopy.add(new Pair<String, File>("", root));
        while (!toCopy.empty()) {
            Pair<String, File> pop = toCopy.pop();
            File file = pop.getSecond();
            if (file.isFile()) {
                os.putNextEntry(new JarEntry(pop.getFirst()));
                Files.copy(file, os);
            } else if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    String path = pop.getFirst().isEmpty() ? child.getName() : pop.getFirst() + "/" + child.getName();
                    toCopy.add(new Pair<String, File>(path, child));
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static void compileKotlinPartOfStdlib(File destdir) throws IOException {
        KotlinCompiler.ExitCode exitCode = new KotlinCompiler().exec(System.err, "-output", destdir.getPath(), "-src", "./libraries/stdlib/src", "-stdlib", destdir.getAbsolutePath());
        if (exitCode != KotlinCompiler.ExitCode.OK) {
            throw new IllegalStateException("stdlib for test compilation failed: " + exitCode);
        }
    }
    
    private static List<File> javaFilesInDir(File dir) {
        List<File> r = new ArrayList<File>();
        Stack<File> stack = new Stack<File>();
        stack.push(dir);
        while (!stack.empty()) {
            File file = stack.pop();
            if (file.isDirectory()) {
                stack.addAll(Arrays.asList(file.listFiles()));
            } else if (file.getName().endsWith(".java")) {
                r.add(file);
            }
        }
        return r;
    }
    
    private static void compileJavaPartOfStdlib(File destdir) throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(javaFilesInDir(new File("runtime/src")));
            List<String> options = Arrays.asList(
                    "-d", destdir.getPath()
            );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        }
        finally {
            fileManager.close();
        }
    }


    private static Throwable compilationException;
    
    public static File stdlibJarForTests() {
        synchronized (ForTestCompileStdlib.class) {
            if (compilationException != null) {
                throw new RuntimeException("stdlib compilation failed in previous tests: " + compilationException, compilationException);
            }
            if (stdlibJarFile == null || !stdlibJarFile.exists()) {
                try {
                    stdlibJarFile = doCompile();
                } catch (Throwable e) {
                    compilationException = e;
                    throw new RuntimeException(e);
                }
            }
            return stdlibJarFile;
        }

    }

}
