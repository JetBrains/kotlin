/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.fileUtils.FileUtilsPackage;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class AntTaskJsTest extends AntTaskBaseTest {
    private static final String JS_OUT_FILE = "out.js";

    private void doJsAntTest() throws Exception {
        doAntTest(SUCCESSFUL);

        String outputFilePath = getOutputFileByName(JS_OUT_FILE).getAbsolutePath();
        RhinoUtils.runRhinoTest(Collections.singletonList(outputFilePath),
                                new RhinoFunctionResultChecker("out", "foo", "box", "OK"));
    }

    private void doJsAntTestForPostfixPrefix(@Nullable String prefix, @Nullable String postfix) throws Exception {
        doJsAntTest();
        File outputFile = getOutputFileByName(JS_OUT_FILE);

        File prefixFile = prefix != null ? new File(testDataDir, prefix) : null;
        File postfixFile = postfix != null ? new File(testDataDir, postfix) : null;

        checkFilePrefixPostfix(outputFile, prefixFile, postfixFile);
    }

    private static void checkFilePrefixPostfix(@NotNull File file, @Nullable File prefix, @Nullable File postfix) throws IOException {
        String fileContent = FileUtil.loadFile(file, true);

        String prefixContent = FileUtilsPackage.readTextOrEmpty(prefix);
        assertTrue(fileContent.startsWith(prefixContent));

        String postfixContent = FileUtilsPackage.readTextOrEmpty(postfix);
        assertTrue(fileContent.endsWith(postfixContent));
    }

    @Test
    public void k2jsSimple() throws Exception {
        doJsAntTest();
    }

    @Test
    public void k2jsWithMain() throws Exception {
        doJsAntTest();
    }

    @Test
    public void k2jsWithMainFQArgs() throws Exception {
        doJsAntTest();
    }

    @Test
    public void k2jsWithMainVarargs() throws Exception {
        doJsAntTest();
    }

    @Test
    public void k2jsManySources() throws Exception {
        doJsAntTest();
    }

    @Test
    public void k2jsWithoutSrcParam() throws Exception {
        doAntTest(FAILED);
    }

    @Test
    public void k2jsWithoutOutputParam() throws Exception {
        doAntTest(FAILED);
    }

    @Test
    public void k2jsWithPrefix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", null);
    }

    @Test
    public void k2jsWithPostfix() throws Exception {
        doJsAntTestForPostfixPrefix(null, "postfix");
    }

    @Test
    public void k2jsWithPrefixAndPostfix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", "postfix");
    }

    @Test
    public void k2jsWithSourcemap() throws Exception {
        doJsAntTest();

        File sourcemap = getOutputFileByName(JS_OUT_FILE + ".map");
        assertTrue("Sourcemap file \"" + sourcemap.getAbsolutePath() + "\" not found", sourcemap.exists());
    }
}
