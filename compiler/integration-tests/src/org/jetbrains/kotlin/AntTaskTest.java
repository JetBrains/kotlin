/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

import static junit.framework.Assert.assertEquals;

public class AntTaskTest extends KotlinIntegrationTestBase {
    @Test
    public void antTaskJvm() throws Exception {
        String jar = tmpdir.getTmpDir().getAbsolutePath() + File.separator + "hello.jar";
        String runtime = new File("dist/kotlinc/lib/kotlin-runtime.jar").getAbsolutePath();

        assertEquals("compilation failed", 0, runAnt("build.log", "build.xml"));
        runJava("hello.run", "-cp", jar + File.pathSeparator + runtime, "Hello.HelloPackage");
    }

    @Override
    protected String normalizeOutput(String content) {
        return super.normalizeOutput(content)
                .replaceAll("Total time: .+\n", "Total time: [time]\n");
    }

    private int runAnt(String logName, String scriptName) throws Exception {
        return runJava(logName, "-jar", getAntHome() + File.separator + "lib" + File.separator + "ant-launcher.jar",
                       "-Dkotlin.lib=" + getCompilerLib(),
                       "-Dtest.data=" + testDataDir,
                       "-Dtemp=" + tmpdir.getTmpDir(),
                       "-f", scriptName);
    }

    private static String getAntHome() {
        return getKotlinProjectHome().getAbsolutePath() + File.separator + "dependencies" + File.separator + "ant";
    }
}
