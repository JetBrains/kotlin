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

package org.jetbrains.jet.cli;

import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;

public abstract class AbstractKotlincExecutableTest extends TestCaseWithTmpdir {
    private void doTest(@NotNull String argsFilePath, @NotNull String executableName, @NotNull String testDataDir) throws Exception {
        String executableFileName = SystemInfo.isWindows ? executableName + ".bat" : executableName;
        File kotlincFile = new File(PathUtil.getKotlinPathsForDistDirectory().getHomePath(), "bin/" + executableFileName);
        assertTrue("kotlinc executable not found, probably you need to invoke 'dist' Ant target: " + kotlincFile.getAbsolutePath(), kotlincFile.exists());

        String[] args = CliBaseTest.readArgs(argsFilePath, testDataDir, tmpdir.getAbsolutePath());

        final Process process = Runtime.getRuntime().exec(ArrayUtil.prepend(kotlincFile.getAbsolutePath(), args));

        final Ref<String> stderr = Ref.create();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stderr.set(FileUtil.loadTextAndClose(process.getErrorStream()));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        String output = FileUtil.loadTextAndClose(process.getInputStream());

        int intExitCode = process.waitFor();
        ExitCode exitCode = ExitCode.values()[intExitCode];

        String normalizedOutput = CliBaseTest.getNormalizedCompilerOutput(output, exitCode, testDataDir);
        File outFile = new File(argsFilePath.replace(".args", ".out"));

        try {
            JetTestUtils.assertEqualsToFile(outFile, normalizedOutput);
        }
        catch (Exception e) {
            System.out.println("exitcode " + intExitCode);
            System.out.println("<stdout>" + output + "</stdout>");
            System.out.println("<stderr>" + stderr + "</stderr>");

            throw e;
        }
    }

    protected void doJvmTest(@NotNull String argsFilePath) throws Exception {
        doTest(argsFilePath, "kotlinc-jvm", CliBaseTest.JVM_TEST_DATA);
    }

    protected void doJsTest(@NotNull String argsFilePath) throws Exception {
        doTest(argsFilePath, "kotlinc-js", CliBaseTest.JS_TEST_DATA);
    }
}
