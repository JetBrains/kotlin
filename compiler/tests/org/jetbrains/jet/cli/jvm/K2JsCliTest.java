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

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import junit.framework.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class K2JsCliTest extends CliBaseTest {
    @Test
    public void simple() {
        doSimpleTest(/*expectedOutFile =*/ true);
    }

    @Test
    public void outputPrefixFileNotFound() {
        doSimpleTest(/*expectedOutFile =*/ false,
                     "-outputPrefix", NOT_EXISTING_PATH);
    }

    @Test
    public void outputPostfixFileNotFound() {
        doSimpleTest(/*expectedOutFile =*/ false,
                     "-outputPostfix", NOT_EXISTING_PATH);
    }

    private void doSimpleTest(boolean expectedOutFile, String... additionalArgs) {
        File outputFile = new File(tmpdir.getTmpDir(),  "out.js");
        List<String> args = Lists.newArrayList(
                "-sourceFiles", "compiler/testData/cli/simple2js.kt",
                "-output", outputFile.getPath());
        Collections.addAll(args, additionalArgs);

        executeCompilerCompareOutputJS(ArrayUtil.toStringArray(args));

        Assert.assertEquals(expectedOutFile, outputFile.isFile());
    }
}
