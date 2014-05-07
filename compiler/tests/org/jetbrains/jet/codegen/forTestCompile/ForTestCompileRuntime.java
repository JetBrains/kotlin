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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.utils.Profiler;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Compile kotlin-runtime.jar that can be used in tests
 *
 * @see #runtimeJarForTests
 */
public class ForTestCompileRuntime {
    private static final String BUILT_INS_SRC_PATH = "core/builtins/src";
    private static final String RUNTIME_JVM_SRC_PATH = "core/runtime.jvm/src";
    private static final String REFLECTION_SRC_PATH = "core/reflection/src";

    private ForTestCompileRuntime() {
    }

    private static class Runtime extends ForTestCompileSomething {
        private Runtime() {
            super("runtime");
        }

        private static final Runtime runtime = new Runtime();

        @Override
        protected void doCompile(@NotNull File classesDir) throws Exception {
            compileBuiltIns(classesDir);
            compileStdlib(classesDir);
        }
    }

    private static void compileBuiltIns(@NotNull File destDir) throws IOException {
        String src = BUILT_INS_SRC_PATH + File.pathSeparator + RUNTIME_JVM_SRC_PATH + File.pathSeparator + REFLECTION_SRC_PATH;
        compileKotlinToJvm("built-ins", destDir, src, src);

        JetTestUtils.compileJavaFiles(
                ContainerUtil.concat(javaFilesUnder(BUILT_INS_SRC_PATH), javaFilesUnder(RUNTIME_JVM_SRC_PATH)),
                Arrays.asList(
                        "-classpath", destDir.getPath(),
                        "-d", destDir.getPath()
                )
        );
    }

    @NotNull
    private static List<File> javaFilesUnder(@NotNull String path) {
        return FileUtil.findFilesByMask(Pattern.compile(".*\\.java"), new File(path));
    }

    private static void compileStdlib(@NotNull File destDir) throws IOException {
        compileKotlinToJvm("stdlib", destDir, "libraries/stdlib/src", destDir.getPath());
    }

    private static void compileKotlinToJvm(
            @NotNull String debugName,
            @NotNull File destDir,
            @NotNull String src,
            @NotNull String classPath
    ) {
        ExitCode exitCode = new K2JVMCompiler().exec(
                System.out,
                "-output", destDir.getPath(),
                "-src", src,
                "-noStdlib",
                "-noJdkAnnotations",
                "-suppress", "warnings",
                "-annotations", JetTestUtils.getJdkAnnotationsJar().getAbsolutePath(),
                "-classpath", classPath
        );
        if (exitCode != ExitCode.OK) {
            throw new IllegalStateException("Compilation of " + debugName + " failed: " + exitCode);
        }
    }

    @NotNull
    public static File runtimeJarForTests() {
        return ForTestCompileSomething.ACTUALLY_COMPILE ? Runtime.runtime.getJarFile() : new File("dist/kotlinc/lib/kotlin-runtime.jar");
    }

    // This method is very convenient when you have trouble compiling runtime in tests
    public static void main(String[] args) throws IOException {
        File destDir = JetTestUtils.tmpDir("runtime");

        Profiler builtIns = Profiler.create("compileBuiltIns").start();
        compileBuiltIns(destDir);
        builtIns.end();

        Profiler stdlib = Profiler.create("compileStdlib").start();
        compileStdlib(destDir);
        stdlib.end();
    }

}
