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

package org.jetbrains.jet.codegen.forTestCompile;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.junit.Assert;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Compile stdlib.jar that can be used in tests
 *
 * @see #runtimeJarForTests
 */
public class ForTestCompileRuntime {

    private ForTestCompileRuntime() {
    }

    private static class Runtime extends ForTestCompileSomething {
        private Runtime() {
            super("runtime");
        }

        private static final Runtime runtime = new Runtime();

        @Override
        protected void doCompile(@NotNull File classesDir) throws Exception {
            compileJavaPartOfBuiltins(classesDir);
            compileStdlib(classesDir);
        }
    }

    private static void compileStdlib(File destdir) throws IOException {
        ExitCode exitCode = new K2JVMCompiler().exec(System.err,
                "-output", destdir.getPath(),
                "-src", "./libraries/stdlib/src",
                "-noStdlib",
                "-noJdkAnnotations",
                "-annotations", "./jdk-annotations",
                "-classpath", "out/production/runtime");
        if (exitCode != ExitCode.OK) {
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
            }
            else if (file.getName().endsWith(".java")) {
                r.add(file);
            }
        }
        return r;
    }
    
    private static void compileJavaPartOfBuiltins(File destdir) throws IOException {
        if (true) {
            FileUtil.copyDir(new File("out/production/runtime"), destdir);
        }
        else {
            doCompileJavaPartOfBuiltins(destdir);
        }
    }

    private static void doCompileJavaPartOfBuiltins(File destdir) throws IOException {JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

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


    @NotNull
    public static File runtimeJarForTests() {
        return ForTestCompileSomething.ACTUALLY_COMPILE ? Runtime.runtime.getJarFile() : new File("dist/kotlinc/lib/kotlin-runtime.jar");
    }

    // This method is very convenient when you have trouble compiling runtime in tests
    public static void main(String[] args) throws IOException {
        compileStdlib(JetTestUtils.tmpDir("runtime"));
    }

}
