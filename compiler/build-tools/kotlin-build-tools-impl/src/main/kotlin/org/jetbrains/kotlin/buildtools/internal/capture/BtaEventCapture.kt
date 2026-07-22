/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.capture

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Per-operation identifying context attached to every captured event.
 *
 * @property buildId the [org.jetbrains.kotlin.buildtools.api.ProjectId] string of the build session
 *   (one value per Gradle/Maven build, shared by all module compilations in that build).
 * @property modulePath the compiler output directory of the compiled module — the per-module key an IDE
 *   can join against a module's known output path. `null` if the operation exposes no output path.
 * @property moduleName the Kotlin module name (`-module-name`), if known. Auxiliary only — not a reliable key.
 */
internal class EventContext(
    val buildId: String,
    val modulePath: String?,
    val moduleName: String?,
)

/**
 * Opt-in capture of the Build Tools API compilation-event stream (compiler diagnostics + incremental
 * compilation reports) to a structured NDJSON file, one file per build session.
 *
 * This is a diagnostic hook consumed by external tooling (e.g. an IntelliJ build visualizer). It is
 * completely disabled — and imposes zero overhead — unless a capture directory is configured via the
 * `KOTLIN_BTA_CAPTURE_DIR` environment variable (preferred; propagates to forked daemon/worker JVMs) or
 * the `kotlin.build-tools-api.capture.dir` system property.
 *
 * Each line is a JSON object: `{ts, buildId, module, moduleName, severity, message}`. Concurrent module
 * compilations in one build interleave in the file; every line self-identifies its module via [EventContext.modulePath],
 * so a consumer can isolate the event sequence of any single module by filtering on that key.
 */
internal object BtaEventCapture {
    private val captureDir: Path? = run {
        val raw = System.getenv("KOTLIN_BTA_CAPTURE_DIR")
            ?: System.getProperty("kotlin.build-tools-api.capture.dir")
        raw?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
    }

    private val lock = Any()

    /**
     * `true` when a capture directory has been configured; when `false` the capture hook must not be installed.
     */
    val isEnabled: Boolean
        get() = captureDir != null

    /**
     * Append a single event to this build's NDJSON file. Thread-safe. No-op when capture is disabled.
     */
    fun record(context: EventContext, severity: String, message: String) {
        val dir = captureDir ?: return
        val line = buildString {
            append('{')
            appendField("ts", System.currentTimeMillis().toString(), quoted = false)
            append(',')
            appendField("buildId", context.buildId)
            append(',')
            appendField("module", context.modulePath)
            append(',')
            appendField("moduleName", context.moduleName)
            append(',')
            appendField("severity", severity)
            append(',')
            appendField("message", message)
            append('}')
        }
        val file = dir.resolve("bta-capture-${context.buildId.sanitizeForFileName()}.ndjson")
        synchronized(lock) {
            Files.createDirectories(dir)
            Files.write(
                file,
                (line + "\n").toByteArray(Charsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
            )
        }
    }

    private fun StringBuilder.appendField(name: String, value: String?, quoted: Boolean = true) {
        append('"').append(name).append("\":")
        when {
            value == null -> append("null")
            quoted -> append('"').append(value.jsonEscape()).append('"')
            else -> append(value)
        }
    }

    private fun String.jsonEscape(): String = buildString {
        for (c in this@jsonEscape) {
            when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }

    private fun String.sanitizeForFileName(): String = map { if (it.isLetterOrDigit() || it == '-' || it == '_') it else '_' }.joinToString("")
}
