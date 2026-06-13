/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.versioncoverage

import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

internal class MavenMetadataFetcher(private val url: String) {

    fun fetch(): List<KotlinToolingVersion> {
        val xml = fetchXml()
        return parseVersions(xml)
    }

    private fun fetchXml(): String {
        val connection = URI(url).toURL().openConnection().apply {
            connectTimeout = 10_000
            readTimeout = 30_000
        }
        return connection.getInputStream().use { it.reader().readText() }
    }

    private fun parseVersions(xml: String): List<KotlinToolingVersion> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml.byteInputStream())

        val nodes = doc.getElementsByTagName("version")
        return buildList {
            for (i in 0 until nodes.length) {
                val versionString = nodes.item(i).textContent
                runCatching { KotlinToolingVersion(versionString) }
                    .onSuccess { add(it) }
                    .onFailure { System.err.println("WARNING: Skipping unparseable version '$versionString'") }
            }
        }
    }
}
