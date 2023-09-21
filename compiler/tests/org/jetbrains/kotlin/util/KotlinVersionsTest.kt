/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.util

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest
import org.jetbrains.kotlin.test.runTest
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.Assert
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import javax.xml.parsers.SAXParserFactory

@WithMutedInDatabaseRunTest
class KotlinVersionsTest : KtUsefulTestCase() {
    fun testVersionsAreConsistent() {
        val versionPattern = "(\\d+)\\.(\\d+)(\\.(\\d+))?(?:-(\\p{Alpha}*\\p{Alnum}|[\\p{Alpha}-]*))?(?:-(\\d+))?".toRegex()

        data class Version(val major: Int, val minor: Int, val patch: Int?, val versionString: String, val source: String) {
            fun isConsistentWith(other: Version): Boolean {
                return major == other.major &&
                        minor == other.minor &&
                        (patch == null || other.patch == null || patch == other.patch)
            }
        }

        fun String.toVersionOrNull(source: String): Version? {
            val result = versionPattern.matchEntire(this) ?: return null
            val (major, minor, _, patch) = result.destructured
            return Version(major.toInt(), minor.toInt(), patch.takeUnless(String::isEmpty)?.toInt(), this, source)
        }

        fun String.toVersion(source: String): Version =
            toVersionOrNull(source) ?: error("Version ($source) is in an unknown format: $this")

        val versions = arrayListOf<Version>()

        // This version is null in case of a local build when KotlinCompilerVersion.VERSION = "@snapshot@"
        versions.addIfNotNull(
            KotlinCompilerVersion.VERSION.substringBefore('-').toVersionOrNull("KotlinCompilerVersion.VERSION")
        )

        versions.add(
            ForTestCompileRuntime.runtimeJarClassLoader().loadClass(KotlinVersion::class.qualifiedName!!)
                .getDeclaredField(KotlinVersion.Companion::CURRENT.name)
                .get(null)
                .toString()
                .toVersion("KotlinVersion.CURRENT")
        )

        versions.add(
            loadValueFromPomXml("libraries/pom.xml", listOf("project", "version"))
                ?.toVersion("version in pom.xml")
                ?: error("No version in libraries/pom.xml")
        )

        val latestStableVersion = LanguageVersion.LATEST_STABLE.versionString.toVersion("LanguageVersion.LATEST_STABLE")
        // Note: comment the next line before a language version update
        versions.add(latestStableVersion)

        if (versions.any { v1 -> versions.any { v2 -> !v1.isConsistentWith(v2) } }) {
            Assert.fail(
                "Some versions are inconsistent. Please change the versions so that they are consistent:\n\n" +
                        versions.joinToString(separator = "\n") { with(it) { "$versionString ($source)" } }
            )
        }

        // Note: uncomment this fragment before a language version update
//        if (versions.any { it.isConsistentWith(latestStableVersion) }) {
//            Assert.fail(
//                "Some versions are consistent with LATEST_STABLE ${latestStableVersion.versionString}. Please edit KotlinVersionsTest accordingly:\n\n" +
//                        versions.joinToString(separator = "\n") { with(it) { "$versionString ($source)" } }
//            )
//        }
    }

    fun testMavenProjectVersionsAreEqual() {
        data class Pom(val path: String, val version: String)

        val poms = arrayListOf<Pom>()

        FileUtil.processFilesRecursively(File("libraries")) { file ->
            if (file.name == "pom.xml" && file.toPath().none { it.fileName.toString() == "target" }) {
                println(file.path)
                if (loadValueFromPomXml(file.path, listOf("project", "parent", "artifactId")) == "kotlin-project") {
                    val version = loadValueFromPomXml(file.path, listOf("project", "parent", "version"))
                        ?: error("No version found in pom.xml at $file")
                    poms.add(Pom(file.path, version))
                }
            }
            true
        }

        Assert.assertTrue(
            "Too few (<= 10) pom.xml files found. Something must be wrong in the test or in the project structure",
            poms.size > 10
        )

        if (!poms.map(Pom::version).areEqual()) {
            Assert.fail(
                "Some versions in pom.xml files are different. Please change the versions so that they are equal:\n\n" +
                        poms.joinToString(separator = "\n") { (path, version) -> "$version $path" }
            )
        }
    }

    private fun loadValueFromPomXml(filePath: String, query: List<String>): String? {
        assert(filePath.endsWith("pom.xml")) { filePath }

        var result: String? = null

        SAXParserFactory.newInstance().newSAXParser().parse(File(filePath), object : DefaultHandler() {
            val currentPath = mutableListOf<String>()

            override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
                currentPath.add(qName)
            }

            override fun endElement(uri: String, localName: String, qName: String) {
                assert(currentPath.lastOrNull() == qName) { "Invalid XML at $filePath: mismatched tag '$qName'" }
                currentPath.removeAt(currentPath.lastIndex)
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (currentPath == query) {
                    assert(result == null) { "More than one value found for $query in $filePath" }
                    result = String(ch, start, length).trim()
                }
            }
        })

        return result
    }

    private fun Collection<Any>.areEqual(): Boolean = all(first()::equals)

    override fun runTest() {
        runTest { super.runTest() }
    }
}
