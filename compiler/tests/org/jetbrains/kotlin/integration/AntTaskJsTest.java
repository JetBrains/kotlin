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

package org.jetbrains.kotlin.integration;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class AntTaskJsTest extends AntTaskBaseTest {
    private static final String JS_OUT_FILE = "out.js";

    @Rule
    public final TestName name = new TestName();

    @NotNull
    @Override
    protected File getTestDataDir() {
        return new File(new File(ANT_TASK_TEST_DATA_BASE_DIR, "js"), name.getMethodName());
    }

    private void doJsAntTest(String... jsFiles) throws Exception {
        doAntTest(SUCCESSFUL);

        List<String> fileNames = new ArrayList<String>(Arrays.asList(jsFiles));
        fileNames.add(JS_OUT_FILE);

        List<String> filePaths = ContainerUtil.map(fileNames, new Function<String, String>() {
            @Override
            public String fun(String s) {
                return getOutputFileByName(s).getAbsolutePath();
            }
        });

        RhinoUtils.runRhinoTest(filePaths, new RhinoFunctionResultChecker("out", "foo", "box", "OK"));
    }

    private void doJsAntTestForPostfixPrefix(@Nullable String prefix, @Nullable String postfix) throws Exception {
        doJsAntTest();
        File outputFile = getOutputFileByName(JS_OUT_FILE);

        File prefixFile = prefix != null ? new File(getTestDataDir(), prefix) : null;
        File postfixFile = postfix != null ? new File(getTestDataDir(), postfix) : null;

        checkFilePrefixPostfix(outputFile, prefixFile, postfixFile);
    }

    private static void checkFilePrefixPostfix(@NotNull File file, @Nullable File prefix, @Nullable File postfix) throws IOException {
        String fileContent = FileUtil.loadFile(file, true);

        if (prefix != null) {
            String prefixContent = FileUtil.loadFile(prefix, true);
            assertTrue(fileContent.startsWith(prefixContent));
        }

        if (postfix != null) {
            String postfixContent = FileUtil.loadFile(postfix, true);
            assertTrue(fileContent.endsWith(postfixContent));
        }
    }

    @Test
    public void simple() throws Exception {
        doJsAntTest();
    }

    @Test
    public void simpleWithMain() throws Exception {
        doJsAntTest();
    }

    @Test
    public void simpleWithStdlib() throws Exception {
        doJsAntTest();
    }

    @Test
    public void simpleWithStdlibAndAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    @Test
    public void simpleWithStdlibAndFolderAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    @Test
    public void simpleWithoutStdlibAndFolderAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    @Test
    public void simpleWithMainFQArgs() throws Exception {
        doJsAntTest();
    }

    @Test
    public void simpleWithVarargMain() throws Exception {
        doJsAntTest();
    }

    @Test
    public void manySources() throws Exception {
        doJsAntTest();
    }

    @Test
    public void additionalArguments() throws Exception {
        doJsAntTest();
    }

    @Test
    public void suppressWarnings() throws Exception {
        doJsAntTest();
    }

    @Test
    public void verbose() throws Exception {
        doJsAntTest();
    }

    @Test
    public void version() throws Exception {
        doJsAntTest();
    }

    @Test
    public void outputWithoutDirectory() throws Exception {
        doJsAntTest();
    }

    @Test
    public void noSrcParam() throws Exception {
        doAntTest(FAILED);
    }

    @Test
    public void noOutputParam() throws Exception {
        doAntTest(FAILED);
    }

    @Test
    public void outputPrefix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", null);
    }

    @Test
    public void outputPostfix() throws Exception {
        doJsAntTestForPostfixPrefix(null, "postfix");
    }

    @Test
    public void bothPrefixAndPostfix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", "postfix");
    }

    @Test
    public void sourceMap() throws Exception {
        doJsAntTest();

        File sourceMap = getOutputFileByName(JS_OUT_FILE + ".map");
        assertTrue("Source map file \"" + sourceMap.getAbsolutePath() + "\" not found", sourceMap.exists());
    }
}
