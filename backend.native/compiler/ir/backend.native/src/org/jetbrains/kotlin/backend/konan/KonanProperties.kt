package org.jetbrains.kotlin.backend.konan

import java.io.File
import java.util.Properties

public class KonanProperties(val propertyFile: String) {

    val properties = Properties()

    init {
        val file = File(propertyFile)
        file.bufferedReader()?.use { reader ->
            properties.load(reader)
        }
    }

    fun propertyString(key: String): String? = properties.getProperty(key)

    fun propertyList(key: String): List<String> {
        val value =  properties.getProperty(key)
        return value?.split(' ') ?: listOf<String>()
    }
}

