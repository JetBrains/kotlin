/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import khttp.structures.authorization.BasicAuthorization
import java.io.FileInputStream
import java.util.*

object ESUploader {
    var host: String? = null
    var username: String? = null
    var password: String? = null

    var indexName = "kotlin_ide_benchmarks"

    init {
        host = System.getenv("es.hostname")
        username = System.getenv("es.username")
        password = System.getenv("es.password")
        logMessage { "initialized es details $username @ $host" }
    }

    fun upload(benchmark: Benchmark) {
        if (host == null) {
            logMessage { "ES host is not specified, ${benchmark.id()} would not be uploaded" }
            return
        }

        val url = "$host/$indexName/_doc/${benchmark.id()}"
        val auth = if (username != null && password != null) {
            BasicAuthorization(username!!, password!!)
        } else {
            null
        }
        val json = kotlinJsonMapper.writeValueAsString(benchmark)

        val response = khttp.put(
            url = url,
            auth = auth,
            headers = mapOf("Content-Type" to "application/json"),
            data = json
        )
        logMessage { "${response.statusCode} -> ${response.jsonObject}" }
        if (response.statusCode != 200 && response.statusCode != 201) {
            throw IllegalStateException("Error code ${response.statusCode} -> ${response.text}")
        }
    }
}