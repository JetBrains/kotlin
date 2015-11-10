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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CompileEnvironmentTest extends TestCase {

    public void testSmokeWithCompilerOutput() throws IOException {
        File tempDir = FileUtil.createTempDirectory("compilerTest", "compilerTest");
        try {
            File out = new File(tempDir, "out");
            File stdlib = ForTestCompileRuntime.runtimeJarForTests();
            ExitCode exitCode = new K2JVMCompiler().exec(
                    System.out,
                    KotlinTestUtils.getTestDataPathBase() + "/compiler/smoke/Smoke.kt",
                    "-d", out.getAbsolutePath(),
                    "-no-stdlib",
                    "-classpath", stdlib.getAbsolutePath()
            );
            Assert.assertEquals(ExitCode.OK, exitCode);
            File[] files = out.listFiles();
            Arrays.sort(files);
            assertEquals(2, files.length);
            assertEquals(1, files[0].listFiles().length); //META-INF
            assertEquals(1, files[1].listFiles().length); // SmokeKt
        }
        finally {
            FileUtil.delete(tempDir);
        }
    }
}
