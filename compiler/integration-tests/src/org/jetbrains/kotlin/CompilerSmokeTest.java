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

package org.jetbrains.kotlin;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.*;

public class CompilerSmokeTest extends KotlinIntegrationTestBase {
    @Test
    public void help() throws Exception {
        runCompiler("help", "--help");
    }

    @Test
    public void compileAndRunHelloApp() throws Exception {
        final String jar = tempDir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-src", "hello.kt", "-jar", jar));
        runJava("hello.run", "-cp", jar, "Hello.namespace");
    }

    @Test
    public void compileAndRunModule() throws Exception {
        final String jar = tempDir.getAbsolutePath() + File.separator + "smoke.jar";

        assertEquals("compilation failed", 0, runCompiler("Smoke.compile", "-module", "Smoke.kts", "-jar", jar));
        runJava("Smoke.run", "-cp", jar + File.pathSeparator + getKotlinRuntimePath(), "Smoke.namespace", "1", "2", "3");
    }

    @Test
    public void compilationFailed() throws Exception {
        final String jar = tempDir.getAbsolutePath() + File.separator + "smoke.jar";

        runCompiler("hello.compile", "-src", "hello.kt", "-jar", jar);
    }

    @Test
    public void script() throws Exception {
        runCompiler("script", "-script", "hello.ktscript");
    }
}
