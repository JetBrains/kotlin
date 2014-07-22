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

package org.jetbrains.kotlin;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.fileUtils.FileUtilsPackage;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class AntTaskTest extends KotlinIntegrationTestBase {
    private static final int SUCCESSFUL = 0;
    private static final int FAILED = 1;

    private static final String JS_OUT_FILE = "out.js";
    private static final String JVM_OUT_FILE = "hello.jar";

    private void doAntTest(int expectedExitCode, String... extraArgs) throws Exception {
        assertEquals("compilation failed", expectedExitCode, runAnt("build.log", "build.xml", extraArgs));
    }

    private void doJvmAntTest(String... extraJavaArgs) throws Exception {
        doAntTest(SUCCESSFUL, extraJavaArgs);

        String jar = getOutputFileByName(JVM_OUT_FILE).getAbsolutePath();

        runJava("hello.run", "-cp", jar + File.pathSeparator + getKotlinRuntimePath(), "hello.HelloPackage");
    }

    private void doJsAntTest() throws Exception {
        doAntTest(SUCCESSFUL);

        String outputFilePath = getOutputFileByName(JS_OUT_FILE).getAbsolutePath();
        RhinoUtils.runRhinoTest(Collections.singletonList(outputFilePath),
                                new RhinoFunctionResultChecker("out", "foo", "box", "OK"));
    }

    private void doJsAntTestForPostfixPrefix(@Nullable String prefix, @Nullable String postfix) throws Exception {
        doJsAntTest();
        File outputFile = getOutputFileByName(JS_OUT_FILE);

        File prefixFile = null;
        if (prefix != null) {
            prefixFile = new File(testDataDir, prefix);
        }

        File postfixFile = null;
        if (postfix != null) {
            postfixFile = new File(testDataDir, postfix);
        }

        checkFilePrefixPostfix(outputFile, prefixFile, postfixFile);
    }

    @Test
    public void antTaskJvm() throws Exception {
        doJvmAntTest();
    }

    @Test
    public void inlineDisabled() throws Exception {
        doJvmAntTest();
    }

    @Test
    public void inlineWrongArg() throws Exception {
        doAntTest(FAILED);
    }

    @Test
    public void jvmClasspath() throws Exception {
        doJvmAntTest();
    }

    @Test
    public void antTaskJvmManyRoots() throws Exception {
        doJvmAntTest();
    }

    @Test
    public void javacCompiler() throws Exception {
        doJvmAntTest("-cp", getClassPathForAnt(),
                     "-Dkotlin.home", getCompilerLib().getAbsolutePath());
    }

    @Test
    public void externalAnnotations() throws Exception {
        doJvmAntTest("-cp", getClassPathForAnt(),
                     "-Didea.sdk", getIdeaSdkHome(),
                     "-Dkotlin.home", getCompilerLib().getAbsolutePath());
    }

    @Test
    public void kotlinCompiler() throws Exception {
        doJvmAntTest("-cp", getClassPathForAnt(),
                     "-Didea.sdk", getIdeaSdkHome(),
                     "-Dkotlin.home", getCompilerLib().getAbsolutePath());
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

    @Override
    protected String normalizeOutput(String content) {
        return super.normalizeOutput(content)
                .replaceAll("Total time: .+\n", "Total time: [time]\n");
    }

    private int runAnt(String logName, String scriptName, String... extraJavaArgs) throws Exception {
        String[] basicArgs = {
                "-jar", getAntHome() + File.separator + "lib" + File.separator + "ant-launcher.jar",
                "-Dkotlin.lib=" + getCompilerLib(),
                "-Dtest.data=" + testDataDir,
                "-Dtemp=" + tmpdir.getTmpDir(),
                "-f", scriptName
        };
        List<String> strings = new ArrayList<String>();
        strings.addAll(Arrays.asList(basicArgs));
        strings.addAll(Arrays.asList(extraJavaArgs));
        return runJava(logName, ArrayUtil.toStringArray(strings));
    }

    private static String getClassPathForAnt() {
        return getCompilerLib() + File.separator + "kotlin-ant.jar" + File.pathSeparator + getKotlinRuntimePath();
    }

    private static String getIdeaSdkHome() {
        return getKotlinProjectHome().getAbsolutePath() + File.separator + "ideaSDK";
    }

    private static String getAntHome() {
        return getKotlinProjectHome().getAbsolutePath() + File.separator + "dependencies" + File.separator + "ant-1.8";
    }

    private static void checkFilePrefixPostfix(@NotNull File file, @Nullable File prefix, @Nullable File postfix) throws IOException {
        String fileContent = FileUtil.loadFile(file, true);

        String prefixContent = FileUtilsPackage.readTextOrEmpty(prefix);
        assertTrue(fileContent.startsWith(prefixContent));

        String postfixContent = FileUtilsPackage.readTextOrEmpty(postfix);
        assertTrue(fileContent.endsWith(postfixContent));
    }

    private File getOutputFileByName(@NotNull String name) {
        return new File(tmpdir.getTmpDir(), name);
    }
}
