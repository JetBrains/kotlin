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

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class AntTaskBaseTest extends KotlinIntegrationTestBase {
    protected static final File ANT_TASK_TEST_DATA_BASE_DIR = new File(INTEGRATION_TEST_DATA_BASE_DIR, "ant");

    protected static final int SUCCESSFUL = 0;
    protected static final int FAILED = 1;

    protected void doAntTest(int expectedExitCode, String... extraArgs) throws Exception {
        assertEquals("Compilation failed", expectedExitCode, runAnt("build.log", "build.xml", extraArgs));
    }

    @Override
    protected String normalizeOutput(String content) {
        return super.normalizeOutput(content).replaceAll("Total time: .+\n", "Total time: [time]\n");
    }

    private int runAnt(String logName, String scriptName, String... extraArgs) throws Exception {
        String[] basicArgs = {
                "-jar", getAntHome() + File.separator + "lib" + File.separator + "ant-launcher.jar",
                "-Dkotlin.lib=" + getCompilerLib(),
                "-Dtest.data=" + getTestDataDir(),
                "-Dtemp=" + tmpdir.getTmpDir(),
                "-f", scriptName
        };
        List<String> strings = new ArrayList<String>();
        strings.addAll(Arrays.asList(basicArgs));
        strings.addAll(Arrays.asList(extraArgs));
        return runJava(logName, ArrayUtil.toStringArray(strings));
    }

    private static String getAntHome() {
        return getKotlinProjectHome().getAbsolutePath() + File.separator + "dependencies" + File.separator + "ant-1.8";
    }

    @NotNull
    protected File getOutputFileByName(@NotNull String name) {
        return new File(tmpdir.getTmpDir(), name);
    }
}
