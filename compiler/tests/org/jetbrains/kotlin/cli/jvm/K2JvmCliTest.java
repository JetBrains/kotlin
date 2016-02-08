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

package org.jetbrains.kotlin.cli.jvm;

import org.jetbrains.kotlin.cli.CliBaseTest;

public class K2JvmCliTest extends CliBaseTest {
    public void testWrongAbiVersion() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testWrongAbiVersionNoErrors() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testNonExistingClassPathAndAnnotationsPath() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testNonExistingSourcePath() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testEmptySources() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testClasspath() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testSignatureClash() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testConflictingOverloads() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testWarningsInDummy() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testHelp() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testWrongArgument() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testSimple() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testDuplicateSources() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testDuplicateSourcesInModule() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testNonexistentPathInModule() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testNonexistentScript() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testDiagnosticsOrder() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testMultipleTextRangesInDiagnosticsOrder() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testSuppressAllWarningsJvm() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testPluginSimple() throws Exception {
        executeCompilerCompareOutputJVM();
    }

    public void testPluginSimpleUsage() throws Exception {
        executeCompilerCompareOutputJVM();
    }
}
