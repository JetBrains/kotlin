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

package org.jetbrains.jet.parsing;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JetCodeConformanceTest extends TestCase {
    private static final Pattern JAVA_FILE_PATTERN = Pattern.compile(".+\\.java");
    private static final Pattern SOURCES_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.jet|.+\\.js)");
    private static final List<File> EXCLUDED_FILES_AND_DIRS = Arrays.asList(
            new File("dependencies"),
            new File("examples"),
            new File("js/js.translator/qunit/qunit.js"),
            new File("libraries/tools/kotlin-js-tests/src/test/web/qunit.js"),
            new File("out"),
            new File("dist"),
            new File("docs"),
            new File("ideaSDK"),
            new File("compiler/tests/org/jetbrains/jet/parsing/JetCodeConformanceTest.java"));
    public static final Pattern JAVADOC_PATTERN = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL);

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

    public void testForAuthorJavadoc() throws IOException {
        List<File> filesWithAuthorJavadoc = Lists.newArrayList();

        for (File sourceFile : FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, new File("."))) {
            if (excludeFile(sourceFile)) {
                continue;
            }

            String source = FileUtil.loadFile(sourceFile);

            if (source.contains("@author") && JAVADOC_PATTERN.matcher(source).find()) { // .contains() is invoked for optimization
                filesWithAuthorJavadoc.add(sourceFile);
            }
        }

        if (!filesWithAuthorJavadoc.isEmpty()) {
            fail(String.format("%d source files contain @author javadoc tag. Please remove them:\n%s",
                               filesWithAuthorJavadoc.size(), StringUtil.join(filesWithAuthorJavadoc, "\n")));
        }
    }

    private static boolean excludeFile(@NotNull File file) {
        for (File excludedFileOrDir : EXCLUDED_FILES_AND_DIRS) {
            if (FileUtil.isAncestor(excludedFileOrDir, file, false)) {
                return true;
            }
        }
        return false;
    }
}
