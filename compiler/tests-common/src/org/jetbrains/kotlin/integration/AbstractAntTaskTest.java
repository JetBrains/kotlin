/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;

import java.io.File;

public abstract class AbstractAntTaskTest extends KotlinIntegrationTestBase {
    protected void doTest(String testFile) throws Exception {
        String testDataDir = new File(testFile).getAbsolutePath();

        runJava(
                testDataDir,
                "build.log",
                "-Xmx192m",
                "-jar", getAntHome() + File.separator + "lib" + File.separator + "ant-launcher.jar",
                "-Dkotlin.lib=" + KotlinIntegrationTestBase.getCompilerLib(),
                "-Dkotlin.runtime.jar=" + ForTestCompileRuntime.runtimeJarForTests().getAbsolutePath(),
                "-Dkotlin.reflect.jar=" + ForTestCompileRuntime.reflectJarForTests().getAbsolutePath(),
                "-Dkotlin.stdlib.jre7.jar=" + new File("dist/kotlinc/lib/kotlin-stdlib-jre7.jar").getAbsolutePath(),
                "-Dkotlin.stdlib.jre8.jar=" + new File("dist/kotlinc/lib/kotlin-stdlib-jre8.jar").getAbsolutePath(),
                "-Dkotlin.stdlib.jdk7.jar=" + new File("dist/kotlinc/lib/kotlin-stdlib-jdk7.jar").getAbsolutePath(),
                "-Dkotlin.stdlib.jdk8.jar=" + new File("dist/kotlinc/lib/kotlin-stdlib-jdk8.jar").getAbsolutePath(),
                "-Dtest.data=" + testDataDir,
                "-Dtemp=" + tmpdir,
                "-f", "build.xml"
        );
    }

    @Override
    @NotNull
    protected String normalizeOutput(@NotNull File testDataDir, @NotNull String content) {
        return super.normalizeOutput(testDataDir, content)
                .replaceAll("Total time: .+\n", "Total time: [time]\n");
    }

    @NotNull
    private static String getAntHome() {
        return KotlinIntegrationTestBase.getKotlinProjectHome().getAbsolutePath() + File.separator + "dependencies" + File.separator + "ant-1.8";
    }
}
