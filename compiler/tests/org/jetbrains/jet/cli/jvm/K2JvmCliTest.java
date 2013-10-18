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

import junit.framework.Assert;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.junit.Test;

import java.io.File;

public class K2JvmCliTest extends CliBaseTest {
    @Test
    public void wrongKotlinSignature() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/wrongKotlinSignature.kt",
                "-classpath", "compiler/testData/cli/wrongKotlinSignatureLib",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void wrongAbiVersion() throws Exception {
        String[] args = {
                "-src", "compiler/testData/cli/wrongAbiVersion.kt",
                "-classpath", "compiler/testData/cli/wrongAbiVersionLib",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }

    @Test
    public void nonExistingClassPathAndAnnotationsPath() {
        String[] args = {
                "-src", "compiler/testData/cli/simple.kt",
                "-classpath", "not/existing/path",
                "-annotations", "yet/another/not/existing/path",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);

        Assert.assertTrue(new File(tmpdir.getTmpDir(), PackageClassUtils.getPackageClassName(FqName.ROOT) + ".class").isFile());
    }

    @Test
    public void nonExistingSourcePath() {
        String[] args = {
                "-src", "not/existing/path",
                "-output", tmpdir.getTmpDir().getPath()};
        executeCompilerCompareOutputJVM(args);
    }
}
