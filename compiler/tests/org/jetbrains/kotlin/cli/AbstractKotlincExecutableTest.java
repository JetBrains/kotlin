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

package org.jetbrains.kotlin.cli;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.util.List;

public abstract class AbstractKotlincExecutableTest extends TestCaseWithTmpdir {
    private void doTest(@NotNull String argsFilePath, @NotNull String executableName) throws Exception {
        String executableFileName = SystemInfo.isWindows ? executableName + ".bat" : executableName;
        File kotlincFile = new File(PathUtil.getKotlinPathsForDistDirectory().getHomePath(), "bin/" + executableFileName);
        assertTrue("kotlinc executable not found, probably you need to invoke 'dist' Ant target: " + kotlincFile.getAbsolutePath(), kotlincFile.exists());

        List<String> args = AbstractCliTest.readArgs(argsFilePath, tmpdir.getAbsolutePath());
        args.add(0, kotlincFile.getAbsolutePath());
        ProcessOutput processOutput = ExecUtil.execAndGetOutput(args, null);

        String stdout = processOutput.getStdout();
        String stderr = processOutput.getStderr();
        int exitCode = processOutput.getExitCode();

        String normalizedOutput =
                AbstractCliTest.getNormalizedCompilerOutput(stderr, ExitCode.values()[exitCode], new File(argsFilePath).getParent());
        File outFile = new File(argsFilePath.replace(".args", ".out"));

        try {
            KotlinTestUtils.assertEqualsToFile(outFile, normalizedOutput);
        }
        catch (Exception e) {
            System.err.println("exit code " + exitCode);
            System.err.println("<stdout>" + stdout + "</stdout>");
            System.err.println("<stderr>" + stderr + "</stderr>");

            throw e;
        }
    }

    protected void doJvmTest(@NotNull String argsFilePath) throws Exception {
        doTest(argsFilePath, "kotlinc-jvm");
    }

    protected void doJsTest(@NotNull String argsFilePath) throws Exception {
        doTest(argsFilePath, "kotlinc-js");
    }
}
