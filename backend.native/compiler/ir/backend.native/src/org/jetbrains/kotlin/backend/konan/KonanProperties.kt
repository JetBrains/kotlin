package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import java.util.Properties

public class KonanProperties(config: CompilerConfiguration) {

    val properties = Properties()

    init {
        val file = File(config.get(KonanConfigKeys.PROPERTY_FILE))
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

