/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

import com.google.gson.Gson
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.io.IOException
import java.io.Serializable
import java.lang.AutoCloseable
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

data class HttpReportParameters(
    internal val url: String,
    internal val password: String?,
    internal val user: String?,
    internal val useExecutor: Boolean = true,
) : Serializable

//non-serializable part of HttpReportService
class HttpReportService : AutoCloseable {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val retryQueue: ConcurrentLinkedQueue<Any> = ConcurrentLinkedQueue<Any>()
    private val requestPreviousFailed = ConcurrentHashMap<HttpReportParameters, Boolean>()
    private val invalidUrl = ConcurrentHashMap<HttpReportParameters, Boolean>()
    override fun close() {
        //It's expected that bad internet connection can cause a significant delay for big project
        executorService.shutdown()
    }

    fun close(httpReportParameters: HttpReportParameters, log: KotlinLogger) {
        resentData(httpReportParameters, log)
        close()
    }

    fun sendData(
        httpReportParameters: HttpReportParameters,
        log: KotlinLogger,
        prepareData: () -> Any?,
    ) {
        submit(httpReportParameters.useExecutor) {
            val data = prepareData.invoke()
            if (data != null && !sendData(httpReportParameters, data, log)) {
                retryQueue.add(data)
            }
        }
    }

    private fun resentData(httpReportParameters: HttpReportParameters, log: KotlinLogger) {
        submit(httpReportParameters.useExecutor) {
            retryQueue.removeIf { sendData(httpReportParameters, it, log) }
        }
    }

    private fun sendData(httpReportParameters: HttpReportParameters, data: Any, log: KotlinLogger): Boolean {
        log.debug("Http report: send data $data")
        val elapsedTime = measureTimeMillis {
            if (invalidUrl[httpReportParameters] == true) {
                return true
            }
            val connection = try {
                URL(httpReportParameters.url).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                log.warn("Http report: Unable to open connection to ${httpReportParameters.url}: ${e.message}")
                invalidUrl[httpReportParameters] = true
                return true
            }

            try {
                if (httpReportParameters.user != null && httpReportParameters.password != null) {
                    val auth = Base64.getEncoder()
                        .encode("${httpReportParameters.user}:${httpReportParameters.password}".toByteArray())
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
                checkResponseAndLog(httpReportParameters, connection, log)
            } catch (e: Exception) {
                log.info("Http report: Unexpected exception happened: '${e.message}': ${e.stackTraceToString()}")
                return false
            } finally {
                connection.disconnect()
            }
        }
        log.debug("Report statistic by http takes $elapsedTime ms")
        return true
    }

    private fun checkResponseAndLog(httpReportParameters: HttpReportParameters, connection: HttpURLConnection, log: KotlinLogger) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            val message = "Failed to send statistic to ${connection.url} with ${connection.responseCode}: ${connection.responseMessage}"
            if (requestPreviousFailed[httpReportParameters] != true) {
                log.warn(message)
            } else {
                log.debug(message)
            }
            requestPreviousFailed[httpReportParameters] = true
        }
    }

    private fun submit(
        useExecutor: Boolean,
        action: () -> Unit,
    ) {
        if (useExecutor) {
            executorService.submit {
                action.invoke()
            }
        } else {
            action.invoke()
        }
    }
}
