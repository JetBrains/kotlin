/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.File
import java.lang.reflect.Type

val buildExecutionDataGson = GsonBuilder()
    .registerTypeAdapter(File::class.java, object : JsonSerializer<File> {
        override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return src?.path?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        }
    })
    .registerTypeAdapter(File::class.java, object : JsonDeserializer<File> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File? {
            val path = context?.deserialize<String>(json, String::class.java)
            return path?.let { File(it) }
        }
    })
    .create()