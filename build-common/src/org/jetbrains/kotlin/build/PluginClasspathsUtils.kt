/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

internal class PluginClasspath(private val classpath: Array<String>?) {
    companion object {
        fun deserializeWithHashes(str: String): List<Pair<String, String>> =
            str.split(":")
                .filter(String::isNotBlank)
                .map { Pair(it.substringBeforeLast("-"), it.substringAfterLast("-")) }
    }

    fun serialize() = classpath?.mapNotNull { it ->
        val jar = File(it).takeIf { it.exists() } ?: return@mapNotNull null
        val jarName = jar.name
        val jarHash = jar.sha256()
        "$jarName-$jarHash"
    }?.joinToString(":") ?: ""

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        DigestInputStream(this.inputStream(), digest).use { dis ->
            val buffer = ByteArray(8192)
            var bytesRead = 0
            while (bytesRead != -1) {
                bytesRead = dis.read(buffer, 0, buffer.size)
            }
        }
        // Convert to hex:
        return digest.digest().joinToString("") {
            Integer.toHexString((it.toInt() and 0xff) + 0x100).substring(1)
        }
    }
}

internal class PluginClasspathComparator(old: String, new: String) {
    private val oldPluginClasspath: List<Pair<String, String>> by lazy { PluginClasspath.deserializeWithHashes(old) }
    private val newPluginClasspath: List<Pair<String, String>> by lazy { PluginClasspath.deserializeWithHashes(new) }

    fun equals() = oldPluginClasspath == newPluginClasspath
}

