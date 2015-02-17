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

package org.jetbrains.kotlin.parsing;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.TestCase;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JetCodeConformanceTest extends TestCase {
    private static final Pattern JAVA_FILE_PATTERN = Pattern.compile(".+\\.java");
    private static final Pattern SOURCES_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)");
    private static final List<File> EXCLUDED_FILES_AND_DIRS = Arrays.asList(
            new File("android.tests.dependencies"),
            new File("core/reflection.jvm/src/kotlin/reflect/jvm/internal/pcollections"),
            new File("libraries/tools/kotlin-reflect/target/copied-sources"),
            new File("dependencies"),
            new File("js/js.translator/qunit/qunit.js"),
            new File("libraries/tools/kotlin-js-tests/src/test/web/qunit.js"),
            new File("out"),
            new File("dist"),
            new File("docs"),
            new File("ideaSDK"),
            new File("libraries/tools/kotlin-gradle-plugin-core/gradle_api_jar/build/tmp"),
            new File("compiler/testData/psi/kdoc"),
            new File("compiler/tests/org/jetbrains/kotlin/parsing/JetCodeConformanceTest.java")
    );
    public static final Pattern AUTHOR_JAVADOC_PATTERN = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL);

    public void testParserCode() throws Exception {
        for (File sourceFile : FileUtil.findFilesByMask(JAVA_FILE_PATTERN, new File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            String source = FileUtil.loadFile(sourceFile, true);

            Pattern atPattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE);
            Matcher matcher = atPattern.matcher(source);

            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: " + matcher.group() + "\nin file: " + sourceFile);
            }
        }
    }

    public void testForAuthorJavadoc() throws IOException {
        List<File> filesWithAuthorJavadoc = filterSourceFiles(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String source) {
                // .contains() is invoked for optimization
                return source.contains("@author") && AUTHOR_JAVADOC_PATTERN.matcher(source).find();
            }
        });

        if (!filesWithAuthorJavadoc.isEmpty()) {
            fail(String.format("%d source files contain @author javadoc tag. Please remove them or exclude in this test:\n%s",
                               filesWithAuthorJavadoc.size(), StringUtil.join(filesWithAuthorJavadoc, "\n")));
        }
    }

    public void testNoJCommanderInternalImports() throws IOException {
        List<File> filesWithJCommander = filterSourceFiles(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String source) {
                return source.contains("com.beust.jcommander.internal");
            }
        });

        Assert.assertTrue(
                "It seems that you've used something from com.beust.jcommander.internal package. This code won't work when there's " +
                "no TestNG in the classpath of our IDEA plugin, because there's only an optional dependency on testng.jar.\n" +
                "Most probably you meant to use Guava's Lists, Maps or Sets instead. Please change references in these files to " +
                "com.google.common.collect: " + filesWithJCommander,
                filesWithJCommander.isEmpty()
        );
    }

    public void testNoOrgJetbrainsJet() throws IOException {
        List<File> filesWithOrgJetbrainsJet = filterSourceFiles(new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String source) {
                return source.contains("org.jetbrains.jet");
            }
        });

        Assert.assertTrue(
                "Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                "Please consider changing the package in these files: " + filesWithOrgJetbrainsJet,
                filesWithOrgJetbrainsJet.isEmpty()
        );
    }

    @NotNull
    private static List<File> filterSourceFiles(@NotNull Function1<String, Boolean> predicate) throws IOException {
        List<File> result = new ArrayList<File>();
        for (File sourceFile : FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, new File("."))) {
            if (!excludeFile(sourceFile) && predicate.invoke(FileUtil.loadFile(sourceFile, true))) {
                result.add(sourceFile);
            }
        }
        return result;
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
