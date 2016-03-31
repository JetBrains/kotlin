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

package org.jetbrains.kotlin.idea.configuration

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.util.net.HttpConfigurable
import org.jetbrains.idea.maven.dom.MavenVersionComparable
import org.jetbrains.idea.maven.indices.MavenArchetypesProvider
import org.jetbrains.idea.maven.model.MavenArchetype
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.utils.ifEmpty
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class KotlinMavenArchetypesProvider(val kotlinPluginVersion: String) : MavenArchetypesProvider {
    constructor() : this(KotlinPluginUtil.getPluginVersion())

    val VERSIONS_LIST_URL = mavenSearchUrl("org.jetbrains.kotlin", packaging = "maven-archetype", rowsLimit = 1000)
    private val versionPrefix by lazy { """^\d+\.\d+\.""".toRegex().find(kotlinPluginVersion)?.value ?: "1.0." }

    private val archetypesBlocking by lazy {
        try {
            loadVersions().ifEmpty { defaultArchetypes() }
        } catch (t: Throwable) {
            defaultArchetypes()
        }
    }

    override fun getArchetypes() = archetypesBlocking.toMutableList()

    private fun defaultArchetypes() = listOf(MavenArchetype("org.jetbrains.kotlin", "kotlin-archetype-jvm", "1.0.0", null, null))

    private fun loadVersions(): List<MavenArchetype> {
        return connectAndApply(VERSIONS_LIST_URL) { urlConnection ->
            urlConnection.inputStream.bufferedReader().use { reader ->
                extractVersions(JsonParser().parse(reader))
            }
        }
    }

    internal fun extractVersions(root: JsonElement) =
            root.asJsonObject.get("response")
            .asJsonObject.get("docs")
            .asJsonArray
            .map { it.asJsonObject }
            .map { MavenArchetype(it.get("g").asString, it.get("a").asString, it.get("v").asString, null, null) }
            .filter { it.version?.startsWith(versionPrefix) ?: false }
            .maxBy { MavenVersionComparable(it.version) }
            .let { listOfNotNull(it) }

    private fun mavenSearchUrl(group: String, artifactId: String? = null, version: String? = null, packaging: String? = null, rowsLimit: Int = 20): String {
        val q = listOf(
                "g" to group,
                "a" to artifactId,
                "v" to version,
                "p" to packaging
        )
                .filter { it.second != null }
                .map { "${it.first}:\"${it.second}\"" }
                .joinToString(separator = " AND ")

        return "http://search.maven.org/solrsearch/select?q=${q.encodeURL()}&core=gav&rows=$rowsLimit&wt=json"
    }

    private fun <R> connectAndApply(url: String, timeoutSeconds: Int = 15, block: (HttpURLConnection) -> R): R {
        return HttpConfigurable.getInstance().openHttpConnection(url).use { urlConnection ->
            val timeout = TimeUnit.SECONDS.toMillis(timeoutSeconds.toLong()).toInt()
            urlConnection.connectTimeout = timeout
            urlConnection.readTimeout = timeout

            urlConnection.connect()
            block(urlConnection)
        }
    }

    private fun <R> HttpURLConnection.use(block: (HttpURLConnection) -> R): R =
        try {
            block(this)
        }
        finally {
            disconnect()
        }

    private fun String.encodeURL() = URLEncoder.encode(this, "UTF-8")
}
