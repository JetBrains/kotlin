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

package org.jetbrains.jet.cli.js;

import junit.framework.Assert;
import org.jetbrains.jet.cli.CliBaseTest;
import org.junit.Test;

import java.io.File;

public class K2JsCliTest extends CliBaseTest {
    @Test
    public void simple2js() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertTrue(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void outputPrefixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }

    @Test
    public void outputPostfixFileNotFound() throws Exception {
        executeCompilerCompareOutputJS();

        Assert.assertFalse(new File(tmpdir.getTmpDir(), "out.js").isFile());
    }
}
