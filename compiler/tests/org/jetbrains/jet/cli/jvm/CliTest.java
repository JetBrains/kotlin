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

package org.jetbrains.jet.cli.jvm;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.test.Tmpdir;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.StringReader;

/**
 * @author Stepan Koltsov
 */
public class CliTest {

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();
    @Rule
    public final TestName testName = new TestName();

    @NotNull
    private String executeCompilerGrabOutput(@NotNull String[] args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream origOut = System.out;
        try {
            // we change System.out because scripts ignore passed OutputStream and write to System.out
            System.setOut(new PrintStream(bytes));
            ExitCode exitCode = new K2JVMCompiler().exec(System.out, args);
            return bytes.toString("utf-8") + exitCode + "\n";
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
        finally {
            System.setOut(origOut);
        }
    }

    private void executeCompilerCompareOutput(@NotNull String[] args) {
        try {
            String actual = normalize(executeCompilerGrabOutput(args))
                    .replace(new File("compiler/testData/cli/").getAbsolutePath(), "$TESTDATA_DIR$")
                    .replace("\\", "/");

            String expected = normalize(FileUtil.loadFile(new File("compiler/testData/cli/" + testName.getMethodName() + ".out")));

            Assert.assertEquals(expected, actual);
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private String normalize(String input) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new StringReader(input));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return sb.toString();
                }
                sb.append(line + "\n");
            }
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    @Test
    public void simple() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/simple.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "namespace.class").isFile());
    }

    @Test
    public void help() throws Exception {
        executeCompilerCompareOutput(new String[] {"--help"});
    }

    @Test
    public void script() throws Exception {
        executeCompilerCompareOutput(new String[]{ "-script", "compiler/testData/cli/script.ktscript", "hi", "there" });
    }

    @Test
    public void ideTemplates() {
        executeCompilerCompareOutput(new String[]{ "-src", "compiler/testData/cli/ideTemplates.kt", "-output", tmpdir.getTmpDir().getPath()});
    }

}
