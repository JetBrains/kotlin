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

package org.jetbrains.jet.parsing;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JetCodeConformanceTest extends TestCase {
    private static final Pattern JAVA_FILE_PATTERN = Pattern.compile(".+\\.java");

    public void testParserCode() throws Exception {
        for (File sourceFile : FileUtil.findFilesByMask(JAVA_FILE_PATTERN, new File("compiler/frontend/src/org/jetbrains/jet/lang/parsing"))) {
            String source = FileUtil.loadFile(sourceFile);

            Pattern atPattern = Pattern.compile("assert.*?[^_]at.*?$", Pattern.MULTILINE);
            Matcher matcher = atPattern.matcher(source);

            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: " + matcher.group() + "\nin file: " + sourceFile);
            }
        }
    }
}
