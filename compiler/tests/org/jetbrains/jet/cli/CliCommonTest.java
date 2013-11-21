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

import junit.framework.Assert;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.junit.Test;

import java.io.File;

public class CliCommonTest extends CliBaseTest {
    @Test
    public void help() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void wrongArgument() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void printArguments() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void printArgumentsWithManyValue() throws Exception {
        executeCompilerCompareOutputJS();
    }

    @Test
    public void simple() throws Exception {
        executeCompilerCompareOutputJVM();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class").isFile());
    }

    @Test
    public void diagnosticsOrder() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void multipleTextRangesInDiagnosticsOrder() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void suppressAllWarningsLowercase() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void suppressAllWarningsMixedCase() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    @Test
    public void suppressAllWarningsJS() throws Exception {
        executeCompilerCompareOutputJS();
    }
}
