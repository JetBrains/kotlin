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

package org.jetbrains.jet.codegen.forTestCompile;

import com.google.common.io.Files;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TimeUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

abstract class ForTestCompileSomething {

    public static final boolean ACTUALLY_COMPILE = "true".equals(System.getenv("kotlin.tests.actually.compile"));

    @NotNull
    private final String jarName;
    private Throwable error;
    private File jarFile;

    ForTestCompileSomething(@NotNull String jarName) {
        System.out.println("Compiling " + jarName + "...");
        long start = System.currentTimeMillis();
        this.jarName = jarName;
        try {
            File tmpDir = JetTestUtils.tmpDir("test_jars");

            jarFile = new File(tmpDir, jarName + ".jar");

            File classesDir = new File(tmpDir, "classes");

            FileUtil.createParentDirs(new File(classesDir, "dummy"));

            doCompile(classesDir);

            FileOutputStream stdlibJar = new FileOutputStream(jarFile);
            try {
                JarOutputStream jarOutputStream = new JarOutputStream(new BufferedOutputStream(stdlibJar));
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
            long end = System.currentTimeMillis();
            System.out.println("Compiling " + jarName + " done in " + TimeUtils.millisecondsToSecondsString(end - start) + "s");
        } catch (Throwable e) {
            error = e;
        }
    }

    static void copyToJar(File root, JarOutputStream os) throws IOException {
        Stack<Pair<String, File>> toCopy = new Stack<Pair<String, File>>();
        toCopy.add(new Pair<String, File>("", root));
        while (!toCopy.empty()) {
            Pair<String, File> pop = toCopy.pop();
            File file = pop.getSecond();
            if (file.isFile()) {
                os.putNextEntry(new JarEntry(pop.getFirst()));
                Files.copy(file, os);
            }
            else if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    String path = pop.getFirst().isEmpty() ? child.getName() : pop.getFirst() + "/" + child.getName();
                    toCopy.add(new Pair<String, File>(path, child));
                }
            }
            else {
                throw new IllegalStateException();
            }
        }
    }

    protected abstract void doCompile(@NotNull File classesDir) throws Exception;

    @NotNull
    public File getJarFile() {
        if (error != null) {
            throw new IllegalStateException("compilation of " + jarName + " failed: " + error, error);
        }
        return jarFile;
    }
}
