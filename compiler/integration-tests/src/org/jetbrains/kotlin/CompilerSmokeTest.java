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

package org.jetbrains.kotlin;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class CompilerSmokeTest extends KotlinIntegrationTestBase {
    @Rule
    public final TestName name = new TestName();

    @NotNull
    @Override
    protected File getTestDataDir() {
        return new File(new File(INTEGRATION_TEST_DATA_BASE_DIR, "smoke"), name.getMethodName());
    }

    private int runCompiler(String logName, String... arguments) throws Exception {
        String classpath = getCompilerLib().getAbsolutePath() + File.separator + "kotlin-compiler.jar" + File.pathSeparator +
                           getKotlinRuntimePath();

        Collection<String> javaArgs = new ArrayList<String>();
        javaArgs.add("-cp");
        javaArgs.add(classpath);
        javaArgs.add("org.jetbrains.jet.cli.jvm.K2JVMCompiler");
        Collections.addAll(javaArgs, arguments);

        return runJava(logName, ArrayUtil.toStringArray(javaArgs));
    }

    @Test
    public void helloApp() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        runJava("hello.run", "-cp", jar, "Hello.HelloPackage");
    }

    @Test
    public void helloAppFQMain() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        runJava("hello.run", "-cp", jar, "Hello.HelloPackage");
    }

    @Test
    public void helloAppVarargMain() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        runJava("hello.run", "-cp", jar, "Hello.HelloPackage");
    }

    @Test
    public void compileAndRunModule() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "smoke.jar";

        assertEquals("compilation failed", 0, runCompiler("Smoke.compile", "-module", "Smoke.ktm", "-d", jar));
        runJava("Smoke.run", "-cp", jar + File.pathSeparator + getKotlinRuntimePath(), "Smoke.SmokePackage", "1", "2", "3");
    }

    @Test
    public void compilationFailed() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "smoke.jar";

        runCompiler("hello.compile", "hello.kt", "-d", jar);
    }

    @Test
    public void syntaxErrors() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "smoke.jar";

        runCompiler("test.compile", "test.kt", "-d", jar);
    }

    @Test
    public void simpleScript() throws Exception {
        runCompiler("script", "-script", "script.kts", "hi", "there");
    }
}
