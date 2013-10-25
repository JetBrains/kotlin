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

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class AntTaskTest extends KotlinIntegrationTestBase {
    private void doAntTest(String... extraJavaArgs) throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "hello.jar";
        String runtime = new File("dist/kotlinc/lib/kotlin-runtime.jar").getAbsolutePath();

        assertEquals("compilation failed", 0, runAnt("build.log", "build.xml", extraJavaArgs));
        runJava("hello.run", "-cp", jar + File.pathSeparator + runtime, "hello.HelloPackage");
    }

    @Test
    public void antTaskJvm() throws Exception {
        doAntTest();
    }

    @Test
    public void antTaskJvmManyRoots() throws Exception {
        doAntTest();
    }

    @Test
    public void javacCompiler() throws Exception {
        doAntTest("-cp", getCompilerLib() + "/kotlin-ant.jar");
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
        return runJava(logName, strings.toArray(new String[strings.size()]));
    }

    private static String getAntHome() {
        return getKotlinProjectHome().getAbsolutePath() + File.separator + "dependencies" + File.separator + "ant-1.7";
    }
}
