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

import junit.framework.Assert;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.junit.Test;

import java.io.File;

public class CliCommonTest extends CliBaseTest {
    @Test
    public void help() throws Exception {
        executeCompilerCompareOutputJVM(new String[] {"-help"});
    }

    @Test
    public void wrongArgument() {
        executeCompilerCompareOutputJVM(new String[] {"-wrongArgument"});
    }

    @Test
    public void printArguments() {
        executeCompilerCompareOutputJVM(new String[] {"-printArgs", "-script", "compiler/testData/cli/hello.ktscript"});
    }

    @Test
    public void printArgumentsWithManyValue() {
        executeCompilerCompareOutputJS(new String[] {
                "-printArgs",
                "-sourceFiles", "compiler/testData/cli/simple2js.kt,compiler/testData/cli/warnings.kt",
                "-suppress", "warnings"});
    }

    @Test
    public void simple() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/simple.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);

        Assert.assertTrue(new File(tmpdir.getTmpDir(), PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class").isFile());
    }

    @Test
    public void diagnosticsOrder() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/diagnosticsOrder1.kt"
                        + File.pathSeparator
                        + "compiler/testData/cli/diagnosticsOrder2.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void multipleTextRangesInDiagnosticsOrder() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/multipleTextRangesInDiagnosticsOrder.kt",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void suppressAllWarningsLowercase() {
        String[] args = {
                "-src", "compiler/testData/cli/warnings.kt",
                "-suppress", "warnings",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void suppressAllWarningsMixedCase() {
        String[] args = {
                "-src", "compiler/testData/cli/warnings.kt",
                "-suppress", "WaRnInGs",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void suppressAllWarningsJS() {
        String[] args = {
                "-sourceFiles", "compiler/testData/cli/warnings.kt",
                "-suppress", "WaRnInGs",
                "-output", new File(tmpdir.getTmpDir(), "out.js").getPath()};
        executeCompilerCompareOutputJS(args);
    }
}
