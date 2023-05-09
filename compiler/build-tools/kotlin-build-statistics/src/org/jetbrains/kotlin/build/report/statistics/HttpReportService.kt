/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

import com.google.gson.Gson
import org.jetbrains.kotlin.compilerRunner.KotlinLogger
import java.io.IOException
import java.io.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

class HttpReportService(
    private val url: String,
    private val password: String?,
    private val user: String?,
) : Serializable {

    private var invalidUrl = false
    private var requestPreviousFailed = false

    private fun checkResponseAndLog(connection: HttpURLConnection, log: KotlinLogger) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            val message = "Failed to send statistic to ${connection.url} with ${connection.responseCode}: ${connection.responseMessage}"
            if (!requestPreviousFailed) {
                log.warn(message)
            } else {
                log.debug(message)
            }
            requestPreviousFailed = true
        }
    }

    fun sendData(data: Any, log: KotlinLogger) {
        val elapsedTime = measureTimeMillis {
            if (invalidUrl) {
                return
            }
            val connection = try {
                URL(url).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                log.warn("Unable to open connection to ${url}: ${e.message}")
                invalidUrl = true
                return
            }

            try {
                if (user != null && password != null) {
                    val auth = Base64.getEncoder()
                        .encode("${user}:${password}".toByteArray())
                        .toString(Charsets.UTF_8)
                    connection.addRequestProperty("Authorization", "Basic $auth")
                }
                connection.addRequestProperty("Content-Type", "application/json")
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(Gson().toJson(data).toByteArray())
                }
                connection.connect()
                checkResponseAndLog(connection, log)
            } catch (e: Exception) {
                log.warn("Unexpected exception happened ${e.message}: ${e.stackTrace}")
                checkResponseAndLog(connection, log)
            } finally {
                connection.disconnect()
            }
        }
        log.debug("Report statistic by http takes $elapsedTime ms")
    }
}
