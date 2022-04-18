/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

class PluginClasspaths(val classpaths: Array<String>?) {
    companion object {
        fun deserializeWithHashes(str: String): List<Pair<String, String>> =
            str.split(":")
                .filter(String::isNotBlank)
                .map { Pair(File(it.substringBeforeLast("-")).name, it.substringAfterLast("-")) }
    }

    fun serialize() = classpaths?.mapNotNull { it ->
        val jar = File(it).takeIf { it.exists() } ?: return@mapNotNull null
        val jarPath = jar.absolutePath
        val jarHash = jar.sha256()
        "$jarPath-$jarHash"
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

class PluginClasspathsComparator(old: String, new: String) {
    private val oldPluginClasspath: List<Pair<String, String>> by lazy { PluginClasspaths.deserializeWithHashes(old) }
    private val newPluginClasspath: List<Pair<String, String>> by lazy { PluginClasspaths.deserializeWithHashes(new) }

    fun equals() = oldPluginClasspath == newPluginClasspath

    fun describeDifferencesOrNull(): String? {
        val changed = oldPluginClasspath != newPluginClasspath
        if (!changed) return null
        val added = newPluginClasspath.subtract(oldPluginClasspath).takeIf { it.isNotEmpty() }?.toList()
        val deleted = oldPluginClasspath.subtract(newPluginClasspath).takeIf { it.isNotEmpty() }?.toList()
        return StringBuilder().apply {
            if (added == null && deleted == null) {
                this.append("Plugin classpaths was reordered.")
                return@apply
            }
            this.append("Plugin classpaths was changed.")
            if (added != null) this.append(" Added: ${added.joinToString(", ")}.")
            if (deleted != null) this.append(" Deleted: ${deleted.joinToString(", ")}.")
            this.append(" Rebuild initiated.")
        }.toString()
    }
}

