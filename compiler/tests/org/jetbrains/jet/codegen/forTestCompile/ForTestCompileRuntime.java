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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.utils.Profiler;

import java.io.File;
import java.io.IOException;

/**
 * Compile kotlin-runtime.jar that can be used in tests
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
            compileJvmBuiltins(classesDir);
            compileStdlib(classesDir);
        }
    }

    private static void compileStdlib(@NotNull File destDir) throws IOException {
        ExitCode exitCode = new K2JVMCompiler().exec(System.out,
                "-output", destDir.getPath(),
                "-src", "./libraries/stdlib/src",
                "-noStdlib",
                "-noJdkAnnotations",
                "-suppress", "warnings",
                "-annotations", "./jdk-annotations",
                "-classpath", "out/production/builtins" + File.pathSeparator + "out/production/runtime.jvm");
        if (exitCode != ExitCode.OK) {
            throw new IllegalStateException("stdlib for test compilation failed: " + exitCode);
        }
    }

    private static void compileJvmBuiltins(@NotNull File destDir) throws IOException {
        // Sources of stdlib and built-ins may diverge this way, because the former are compiled with the new compiler and the latter are
        // just copied from the sources built by bootstrap plugin (which has an old compiler)
        // TODO: compile Kotlin+Java built-in sources properly here, maybe reusing KotlinCompilerAdapter ant task
        FileUtil.copyDir(new File("out/production/builtins"), destDir);
        FileUtil.copyDir(new File("out/production/runtime.jvm"), destDir);
    }

    @NotNull
    public static File runtimeJarForTests() {
        return ForTestCompileSomething.ACTUALLY_COMPILE ? Runtime.runtime.getJarFile() : new File("dist/kotlinc/lib/kotlin-runtime.jar");
    }

    // This method is very convenient when you have trouble compiling runtime in tests
    public static void main(String[] args) throws IOException {
        Profiler stdlib = Profiler.create("compileStdlib").start();
        compileStdlib(JetTestUtils.tmpDir("runtime"));
        stdlib.end();
    }

}
