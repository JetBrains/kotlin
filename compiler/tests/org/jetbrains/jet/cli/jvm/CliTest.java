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

package org.jetbrains.jet.cli.jvm;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.Assert;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.test.Tmpdir;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

/**
 * @author Stepan Koltsov
 */
public class CliTest {

    @Rule
    public final Tmpdir tmpdir = new Tmpdir();

    @Test
    public void simple() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ExitCode exitCode = new K2JVMCompiler().exec(new PrintStream(bytes),
                "-src", "compiler/testData/cli/simple.kt",
                "-output", tmpdir.getTmpDir().getPath());
        String actual = bytes.toString("utf-8") + exitCode + "\n";

        String expected = FileUtil.loadFile(new File("compiler/testData/cli/simple.out"));

        Assert.assertEquals(expected, actual);

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "namespace.class").isFile());
    }

}
