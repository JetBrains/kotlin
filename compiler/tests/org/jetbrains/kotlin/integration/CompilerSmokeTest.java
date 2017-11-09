/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.integration;

import java.io.File;

public class CompilerSmokeTest extends CompilerSmokeTestBase {

    public void testHelloApp() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt");
    }

    public void testHelloAppFQMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt");
    }

    public void testHelloAppVarargMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt");
    }

    public void testCompilationFailed() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "smoke.jar";

        runCompiler("hello.compile", "hello.kt", "-d", jar);
    }

    public void testSyntaxErrors() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "smoke.jar";

        runCompiler("test.compile", "test.kt", "-d", jar);
    }

    public void testSimpleScript() throws Exception {
        runCompiler("script", "-script", "script.kts", "hi", "there");
    }

    public void testScriptDashedArgs() throws Exception {
        runCompiler("script", "-script", "script.kts", "--", "hi", "-name", "Marty", "--", "there");
    }

    public void testScriptWithClasspath() throws Exception {
        runCompiler("script", "-cp", new File("lib/javax.inject.jar").getAbsolutePath(), "-script", "script.kts");
    }

    public void testScriptException() throws Exception {
        runCompiler("script", "-script", "script.kts");
    }

    public void testScriptFlushBeforeShutdown() throws Exception {
        runCompiler("script", "-script", "script.kts");
    }

    public void testCompileScript() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "script.jar";

        runCompiler("script", "script.kts", "-d", jar);
    }

    public void testInlineOnly() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "inlineOnly.jar";

        assertEquals("compilation failed", 0, runCompiler("inlineOnly.compile", "-include-runtime", "inlineOnly.kt", "-d", jar));
        run("inlineOnly.run", "-cp", jar, "InlineOnly.InlineOnlyKt");
    }

    public void testPrintVersion() throws Exception {
        runCompiler("test.compile", "-version");
    }
}
