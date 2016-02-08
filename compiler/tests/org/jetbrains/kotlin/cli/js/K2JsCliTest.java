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

package org.jetbrains.kotlin.cli.js;

import org.jetbrains.kotlin.cli.AbstractCliTest;

public class K2JsCliTest extends AbstractCliTest {
    public void testSimple2js() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testOutputIsDirectory() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testNonExistingSourcePath() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testEmptySources() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testOutputPrefixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testOutputPostfixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testWrongAbiVersion() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testJsHelp() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testWithLib() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testWithFolderAsLib() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testCreateMetadata() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testCreateKjsm() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testLibraryDirNotFound() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testNotValidLibraryDir() throws Exception {
        executeCompilerCompareOutputJS();
    }

    public void testSuppressAllWarningsJS() throws Exception {
        executeCompilerCompareOutputJS();
    }
}
