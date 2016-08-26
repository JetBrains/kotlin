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

package org.jetbrains.kotlin.idea.maven

import com.google.gson.JsonParser
import org.jetbrains.idea.maven.model.MavenArchetype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KotlinMavenArchetypesProviderTest {
    private val BASE_PATH = "idea/testData/configuration/"

    @Test
    fun extractVersions() {
        val file = File(BASE_PATH, "extractVersions/maven-central-response.json")
        assertTrue("Test data is missing", file.exists())

        val json = file.bufferedReader().use {
            JsonParser().parse(it)
        }

        val versions = KotlinMavenArchetypesProvider("1.0.0-Release-Something-1886", false).extractVersions(json)

        assertEquals(
                listOf(
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.0.1-2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-js", "1.0.0", null, null)
                ).sortedBy { it.artifactId + "." + it.version },
                versions.sortedBy { it.artifactId + "." + it.version }
        )
    }

    @Test
    fun extractVersionsNewPlugin() {
        val file = File(BASE_PATH, "extractVersions/maven-central-response.json")
        assertTrue("Test data is missing", file.exists())

        val json = file.bufferedReader().use {
            JsonParser().parse(it)
        }

        val versions = KotlinMavenArchetypesProvider("1.1.0-Next-Release-Something-9999", false).extractVersions(json)

        assertEquals(
                listOf(
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.1.2", null, null)
                ).sortedBy { it.artifactId + "." + it.version },
                versions.sortedBy { it.artifactId + "." + it.version }
        )
    }

    @Test
    fun extractVersionsInternalMode() {
        val file = File(BASE_PATH, "extractVersions/maven-central-response.json")
        assertTrue("Test data is missing", file.exists())

        val json = file.bufferedReader().use {
            JsonParser().parse(it)
        }

        val versions = KotlinMavenArchetypesProvider("1.0.0-Release-Something-1886", true).extractVersions(json)

        assertEquals(
                listOf(
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.0.1-2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.1.2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-js", "1.0.0", null, null)
                ).sortedBy { it.artifactId + "." + it.version },
                versions.sortedBy { it.artifactId + "." + it.version }
        )
    }

    @Test
    fun extractVersionsTooDifferentPluginVersion() {
        val file = File(BASE_PATH, "extractVersions/maven-central-response.json")
        assertTrue("Test data is missing", file.exists())

        val json = file.bufferedReader().use {
            JsonParser().parse(it)
        }

        val versions = KotlinMavenArchetypesProvider("1.9.0-Missing-Release-Something-1886", false).extractVersions(json)

        assertEquals(
                listOf(
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.0.1-2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.1.2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-js", "1.0.0", null, null)
                ).sortedBy { it.artifactId + "." + it.version },
                versions.sortedBy { it.artifactId + "." + it.version }
        )
    }

    @Test
    fun extractVersionsSnapshotPlugin() {
        val file = File(BASE_PATH, "extractVersions/maven-central-response.json")
        assertTrue("Test data is missing", file.exists())

        val json = file.bufferedReader().use {
            JsonParser().parse(it)
        }

        val versions = KotlinMavenArchetypesProvider("@snapshot@", false).extractVersions(json)

        assertEquals(
                listOf(
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.0.1-2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.1.2", null, null),
                        MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-js", "1.0.0", null, null)
                ).sortedBy { it.artifactId + "." + it.version },
                versions.sortedBy { it.artifactId + "." + it.version }
        )
    }
}