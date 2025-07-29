/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import CACHE_FILE
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class CacheHandler {
    private val fingerprints = mutableMapOf<String, String>()
    private val cacheFile = File(CACHE_FILE)

    fun dumpCache() {
        if (!cacheFile.exists()) {
            cacheFile.parentFile?.mkdirs()
            cacheFile.createNewFile()
        }

        val properties = Properties()
        fingerprints.forEach { (key, value) ->
            properties.setProperty(key, value)
        }

        FileOutputStream(cacheFile).use { output ->
            properties.store(output, "Source file cache")
        }
    }

    fun loadCache() {
        if (cacheFile.exists()) {
            val properties = Properties()
            FileInputStream(cacheFile).use { input ->
                properties.load(input)
            }

            fingerprints.clear()
            fingerprints.putAll(properties.stringPropertyNames().associateWith { properties.getProperty(it) })
        }
    }


    fun isFilePresent(fingerprint: String): Boolean = fingerprints.containsKey(fingerprint)

    fun addFile(fingerprint: String, path: String) {
        fingerprints[fingerprint] = path
    }

    fun getFilePath(fingerprint: String): String? = fingerprints[fingerprint]
}