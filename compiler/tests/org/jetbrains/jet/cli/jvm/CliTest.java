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

package org.jetbrains.jet.cli.jvm;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.CLICompiler;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.lang.parsing.JetScriptDefinition;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.JetTypeName;
import org.jetbrains.jet.test.Tmpdir;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;

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
            System.setOut(new PrintStream(bytes));
            ExitCode exitCode = CLICompiler.doMainNoExit(new K2JVMCompiler(), args);
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

        Assert.assertTrue(new File(tmpdir.getTmpDir(), PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class").isFile());
    }

    @Test
    public void diagnosticsOrder() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/diagnosticsOrder1.kt"
                        + File.pathSeparator
                        + "compiler/testData/cli/diagnosticsOrder2.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);
    }

    @Test
    public void multipleTextRangesInDiagnosticsOrder() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/multipleTextRangesInDiagnosticsOrder.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);
    }

    @Test
    public void wrongKotlinSignature() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/wrongKotlinSignature.kt",
                "-classpath", "compiler/testData/cli/wrongKotlinSignatureLib",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);
    }

    @Test
    public void wrongAbiVersion() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/wrongAbiVersion.kt",
                "-classpath", "compiler/testData/cli/wrongAbiVersionLib",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);
    }

    @Test
    public void help() throws Exception {
        executeCompilerCompareOutput(new String[] {"-help"});
    }

    @Test
    public void script() throws Exception {
        executeCompilerCompareOutput(new String[]{ "-script", "compiler/testData/cli/script.ktscript", "hi", "there" });
    }

    @Test
    public void ideTemplates() {
        executeCompilerCompareOutput(new String[]{ "-src", "compiler/testData/cli/ideTemplates.kt", "-output", tmpdir.getTmpDir().getPath()});
    }

    @Test
    public void wrongArgument() {
        executeCompilerCompareOutput(new String[] { "-wrongArgument" });
    }

    @Test
    public void printArguments() {
        try {
            System.setProperty("kotlin.print.cmd.args", "true");
            executeCompilerCompareOutput(new String[] {"-script", "compiler/testData/cli/hello.ktscript"});
        }
        finally {
            System.clearProperty("kotlin.print.cmd.args");
        }
    }

    @Test
    public void nonExistingClassPathAndAnnotationsPath() {
        String[] args = {
                "-src", "compiler/testData/cli/simple.kt",
                "-classpath", "not/existing/path",
                "-annotations", "yet/another/not/existing/path",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);

        Assert.assertTrue(new File(tmpdir.getTmpDir(), PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class").isFile());
    }

    @Test
    public void nonExistingSourcePath() {
        String[] args = {
                "-src", "not/existing/path",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutput(args);
    }

    @Test
    public void testScript() {
        LinkedList<AnalyzerScriptParameter> scriptParameters = new LinkedList<AnalyzerScriptParameter>();
        AnalyzerScriptParameter parameter = new AnalyzerScriptParameter(Name.identifier("num"), JetTypeName.parse("jet.Int"));
        scriptParameters.add(parameter);
        Class aClass = KotlinToJVMBytecodeCompiler
                .compileScript(getClass().getClassLoader(), JetTestUtils.getPathsForTests(), "compiler/testData/cli/fib.ktscript", scriptParameters, null);
        Assert.assertNotNull(aClass);

        try {
            aClass.getConstructor(int.class).newInstance(4);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testScriptStandardExt() {
        LinkedList<AnalyzerScriptParameter> scriptParameters = new LinkedList<AnalyzerScriptParameter>();
        AnalyzerScriptParameter parameter = new AnalyzerScriptParameter(Name.identifier("num"), JetTypeName.parse("jet.Int"));
        scriptParameters.add(parameter);
        Class aClass = KotlinToJVMBytecodeCompiler
                .compileScript(getClass().getClassLoader(), JetTestUtils.getPathsForTests(), "compiler/testData/cli/fib.kt", scriptParameters, null);
        Assert.assertNotNull(aClass);

        try {
            aClass.getConstructor(int.class).newInstance(4);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void testScriptWithScriptDefinition() {
        LinkedList<AnalyzerScriptParameter> scriptParameters = new LinkedList<AnalyzerScriptParameter>();
        AnalyzerScriptParameter parameter = new AnalyzerScriptParameter(Name.identifier("num"), JetTypeName.parse("jet.Int"));
        scriptParameters.add(parameter);
        Class aClass = KotlinToJVMBytecodeCompiler
                .compileScript(getClass().getClassLoader(), JetTestUtils.getPathsForTests(), "compiler/testData/cli/fib.fib.kt", null, Arrays.asList(new JetScriptDefinition(".fib.kt",scriptParameters)));
        Assert.assertNotNull(aClass);

        try {
            aClass.getConstructor(int.class).newInstance(4);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
