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

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.ArrayUtil;
import org.jetbrains.kotlin.cli.AbstractCliTest;
import org.jetbrains.kotlin.utils.StringsKt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@SuppressWarnings("ResultOfMethodCallIgnored")
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

    public void testHelloAppSuspendMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt", "O", "K");
    }

    public void testHelloAppSuspendMainInMultifile() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.Foo", "O", "K");
    }

    public void testHelloAppParameterlessMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt");
    }

    public void testHelloAppOldAndParameterlessMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt");
    }

    public void testHelloAppSuspendParameterlessMain() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "hello.jar";

        assertEquals("compilation failed", 0, runCompiler("hello.compile", "-include-runtime", "hello.kt", "-d", jar));
        run("hello.run", "-cp", jar, "Hello.HelloKt", "O", "K");
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

    public void testScriptException() throws Exception {
        runCompiler("script", "-script", "script.kts");
    }

    public void testScriptFlushBeforeShutdown() throws Exception {
        runCompiler("script", "-script", "script.kts");
    }

    public void testCompileScript() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "script.jar";

        runCompiler("script", "-Xuse-fir-lt=false", "-Xallow-any-scripts-in-source-roots", "script.kts", "-d", jar);
    }

    public void testInlineOnly() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "inlineOnly.jar";

        assertEquals("compilation failed", 0, runCompiler("inlineOnly.compile", "-include-runtime", "inlineOnly.kt", "-d", jar));
        run("inlineOnly.run", "-cp", jar, "InlineOnly.InlineOnlyKt");
    }

    public void testPrintVersion() throws Exception {
        runCompiler("test.compile", "-version");
    }

    public void testBuildFile() throws Exception {
        File buildXml = new File(getTestDataDir(), "build.xml");
        runCompiler(
                "buildFile.compile",
                AbstractCliTest.replacePathsInBuildXml("-Xbuild-file=" + buildXml, getTestDataDir(), tmpdir.getPath())
        );
        run("buildFile.run", "-cp", tmpdir.getAbsolutePath(), "MainKt");
    }

    public void testReflect() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "reflect.jar";
        assertEquals("compilation failed", 0,
                     runCompiler("reflect.compile", "-include-runtime", "reflect.kt", "-d", jar));
        run("reflect.run", "-cp", jar, "reflect.ReflectKt");
    }

    public void testNoReflect() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "noReflect.jar";
        assertEquals("compilation failed", 0,
                     runCompiler("noReflect.compile", "-include-runtime", "-no-reflect", "noReflect.kt", "-d", jar));
        run("noReflect.run", "-cp", jar, "noReflect.NoReflectKt");
    }

    // related to KT-14772, destination is not a jar, and clashing with an existing file
    public void testDestinationDirClashingWithExistingFile() throws Exception {
        String outputDir = tmpdir.getAbsolutePath() + File.separator + "clashingFile";
        File file = new File(outputDir);
        file.createNewFile();
        runCompiler("test.compile", "test.kt", "-d", outputDir);
    }

    // related to KT-18184, destination is not a jar, and permission denied
    public void testDestinationDirNoPermission() throws Exception {
        String outputDir = tmpdir.getAbsolutePath() + File.separator + "noPermissionDir";
        File file = new File(outputDir, "Test.class");
        file.getParentFile().mkdirs();
        if (!SystemInfo.isWindows) {
            file.getParentFile().setReadOnly(); // won't work on Windows
            runCompiler("test.notWindows.compile", "test.kt", "-d", outputDir);
            file.getParentFile().setWritable(true);
        }
        file.createNewFile();
        file.setReadOnly();
        runCompiler("test.compile", "test.kt", "-d", outputDir);
    }

    // related to KT-18184, destination is a jar, and output directory does not exist, we should try to create
    // output directory for it.
    public void testDestinationDirDoesNotExist() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "nonExistingDir" + File.separator + "test.jar";
        runCompiler("test.compile", "test.kt", "-d", jar);
    }

    // related to KT-18184, destination is a jar, and output directory does not exist, and failed to created
    // output directory due to clash with existing file.
    public void testDestinationJarClashingWithExistingFile() throws Exception {
        String jar = tmpdir.getAbsolutePath() + File.separator + "clashingFile" + File.separator + "test.jar";
        File file = new File(jar);
        file.getParentFile().createNewFile();
        runCompiler("test.compile", "test.kt", "-d", jar);
    }

    // related to KT-18184, destination is a jar, and output directory exists, and permission denied to write jar.
    public void testDestinationJarNoPermission() throws Exception {
        String outputDir = tmpdir.getAbsolutePath() + File.separator + "noPermissionDir";
        File jar = new File(outputDir, "test.jar");
        jar.getParentFile().mkdirs();
        if (!SystemInfo.isWindows) {
            jar.getParentFile().setReadOnly(); // won't work on Windows
            runCompiler("test.notWindows.compile", "test.kt", "-d", jar.getCanonicalPath());
            jar.getParentFile().setWritable(true);
        }
        jar.createNewFile();
        jar.setReadOnly();
        runCompiler("test.compile", "test.kt", "-d", jar.getCanonicalPath());
    }

    // related to https://github.com/JetBrains/kotlin/pull/4189
    public void testPathNameDoesNotNameAParent() throws Exception {
        String jar = "hello.jar";
        String workingDirectory = tmpdir.getAbsolutePath();
        Files.write(Paths.get(workingDirectory + "/Hello.kt"), "class Hello".getBytes());

        Collection<String> javaArgs = new ArrayList<>();
        javaArgs.add("-cp");
        javaArgs.add(StringsKt.join(Collections.singletonList(
                getCompilerLib().getAbsolutePath() + File.separator + "kotlin-compiler.jar"
        ), File.pathSeparator));
        javaArgs.add("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler");
        javaArgs.addAll(Arrays.asList("Hello.kt", "-d", jar));

        runJava(workingDirectory, null, ArrayUtil.toStringArray(javaArgs));
    }
}
