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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;

import java.io.File;

public class ForTestCompileBuiltins {

    private ForTestCompileBuiltins() {
    }

    private static class Builtins extends ForTestCompileSomething {

        Builtins() {
            super("builtins");
        }

        @Override
        protected void doCompile(@NotNull File classesDir) throws Exception {
            ExitCode exitCode = new K2JVMCompiler().exec(
                    System.err, "-output", classesDir.getPath(), "-src", "./compiler/frontend/src", "-noStdlib", "-builtins");
            if (exitCode != ExitCode.OK) {
                throw new IllegalStateException("builtins compilation failed: " + exitCode);
            }
        }

        private static final Builtins builtins = new Builtins();
    }

    @NotNull
    public static File builtinsJarForTests() {
        return Builtins.builtins.getJarFile();
    }
}
