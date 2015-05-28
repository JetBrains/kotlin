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

package org.jetbrains.kotlin.code

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase
import org.junit.Assert.assertTrue
import java.io.File
import java.util.regex.Pattern
import kotlin.test.fail

public class JetCodeConformanceTest : TestCase() {
    companion object {
        private val JAVA_FILE_PATTERN = Pattern.compile(".+\\.java")
        private val SOURCES_FILE_PATTERN = Pattern.compile("(.+\\.java|.+\\.kt|.+\\.js)")
        private val EXCLUDED_FILES_AND_DIRS = listOf(
                "android.tests.dependencies",
                "core/reflection.jvm/src/kotlin/reflect/jvm/internal/pcollections",
                "libraries/tools/kotlin-reflect/target/copied-sources",
                "dependencies",
                "js/js.translator/qunit/qunit.js",
                "libraries/tools/kotlin-js-tests/src/test/web/qunit.js",
                "out",
                "dist",
                "ideaSDK",
                "libraries/tools/kotlin-gradle-plugin-core/gradle_api_jar/build/tmp",
                "libraries/tools/kotlin-maven-plugin/target/generated-sources",
                "compiler/testData/psi/kdoc",
                "compiler/tests/org/jetbrains/kotlin/code/JetCodeConformanceTest.kt"
        ).map { File(it) }
    }

    public fun testParserCode() {
        val pattern = Pattern.compile("assert.*?\\b[^_]at.*?$", Pattern.MULTILINE)

        for (sourceFile in FileUtil.findFilesByMask(JAVA_FILE_PATTERN, File("compiler/frontend/src/org/jetbrains/kotlin/parsing"))) {
            val matcher = pattern.matcher(sourceFile.readText())
            if (matcher.find()) {
                fail("An at-method with side-effects is used inside assert: ${matcher.group()}\nin file: $sourceFile")
            }
        }
    }

    public fun testForAuthorJavadoc() {
        val pattern = Pattern.compile("/\\*.+@author.+\\*/", Pattern.DOTALL)

        val found = filterSourceFiles { source ->
            // substring check is an optimization
            "@author" in source && pattern.matcher(source).find()
        }

        assertTrue("%d source files contain @author javadoc tag. " +
                   "Please remove them or exclude in this test:\n%s".format(found.size(), found.joinToString("\n")), found.isEmpty())
    }

    public fun testNoJCommanderInternalImports() {
        val found = filterSourceFiles { source ->
            "com.beust.jcommander.internal" in source
        }

        assertTrue("It seems that you've used something from com.beust.jcommander.internal package. " +
                   "This code won't work when there's no TestNG in the classpath of our IDEA plugin, " +
                   "because there's only an optional dependency on testng.jar.\n" +
                   "Most probably you meant to use Guava's Lists, Maps or Sets instead. " +
                   "Please change references in these files to com.google.common.collect: $found", found.isEmpty())
    }

    public fun testNoOrgJetbrainsJet() {
        val found = filterSourceFiles { source ->
            "org.jetbrains.jet" in source
        }

        assertTrue("Package org.jetbrains.jet is deprecated now in favor of org.jetbrains.kotlin. " +
                   "Please consider changing the package in these files: $found", found.isEmpty())
    }

    private fun filterSourceFiles(predicate: (String) -> Boolean): List<File> {
        return FileUtil.findFilesByMask(SOURCES_FILE_PATTERN, File(".")).filter { sourceFile ->
            EXCLUDED_FILES_AND_DIRS.none { FileUtil.isAncestor(it, sourceFile, false) } &&
            predicate(sourceFile.readText())
        }
    }
}
