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

package org.jetbrains.kotlin.cli.common.messages;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class OutputMessageUtilTest extends TestCase {
    enum TargetOS {
        UNIX {
            @Override
            public String convert(String path) {
                return path.replace('\\', '/');
            }
        },
        WINDOWS {
            @Override
            public String convert(String path) {
                // convert absolute paths to declare their disk as C:
                if (path.startsWith("/")) {
                    path = "C:" + path;
                }
                return path.replace('/', '\\');
            }
        };

        public abstract String convert(String path);
    }

    private static void doTest(String outputPath, String... sourcePathArray) {
        doTest(TargetOS.UNIX, outputPath, sourcePathArray);
        doTest(TargetOS.WINDOWS, outputPath, sourcePathArray);
    }

    private static void doTest(TargetOS os, String outputPath, String... sourcePathArray) {
        File[] sourceFileArray = new File[sourcePathArray.length];
        for (int i = 0; i < sourcePathArray.length; i++) {
            String path = sourcePathArray[i];
            sourceFileArray[i] = new File(os.convert(path));
        }
        doTest(new File(os.convert(outputPath)), sourceFileArray);
    }

    private static void doTest(File outputFile, File... sourceFileArray) {
        List<File> sourceFiles = Arrays.asList(sourceFileArray);
        String message = OutputMessageUtil.formatOutputMessage(sourceFiles, outputFile);
        OutputMessageUtil.Output output = OutputMessageUtil.parseOutputMessage(message);
        assertNotNull("Output is null", output);
        assertEquals(sourceFiles, output.sourceFiles);
        assertEquals(outputFile, output.outputFile);
    }

    public void testOneInOneOut() throws Exception {
        doTest("foo/bar.class", "foo/bar.kt");
        doTest("/foo/bar.class", "/foo/bar.kt");
        doTest("/foo/bar.class", "foo/bar.kt");
        doTest("foo/bar.class", "/foo/bar.kt");
    }

    public void testTwoInOneOut() throws Exception {
        doTest("foo/bar.class", "foo/bar.kt", "foo/buzz.kt");
        doTest("/foo/bar.class", "/foo/bar.kt", "/foo/buzz.kt");
        doTest("foo/bar.class", "/foo/bar.kt", "foo/buzz.kt");
        doTest("foo/bar.class", "foo/bar.kt", "/foo/buzz.kt");
    }

    public void testOneInNoneOut() throws Exception {
        doTest("foo/bar.class");
        doTest("/foo/bar.class");
    }

    public void testWrongStart() throws Exception {
        assertNull(OutputMessageUtil.parseOutputMessage("foo\nOutput:\nfoo"));
    }

    public void testTwoOuts() throws Exception {
        assertNull(OutputMessageUtil.parseOutputMessage("Output:\nfoo\nbar\nInputs:\n"));
    }

    public void testTooFewStrings() throws Exception {
        assertNull(OutputMessageUtil.parseOutputMessage("Output:\nInputs:"));
    }
}
