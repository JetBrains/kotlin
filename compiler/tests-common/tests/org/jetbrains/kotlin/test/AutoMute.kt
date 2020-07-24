/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import java.io.File

class AutoMute(
    val file: String,
    val issue: String
)

val DO_AUTO_MUTE: AutoMute? by lazy {
    val autoMuteFile = File("tests/automute")
    if (autoMuteFile.exists()) {
        val lines = autoMuteFile.readLines().filter { it.isNotBlank() }.map { it.trim() }
        AutoMute(
            lines.getOrNull(0) ?: error("A file path is expected in tne first line"),
            lines.getOrNull(1) ?: error("An issue description is the second line")
        )
    } else {
        null
    }
}

fun AutoMute.muteTest(testKey: String) {
    val file = File(file)
    val lines = file.readLines()
    val firstLine = lines[0] // Drop file header
    val muted = lines.drop(1).toMutableList()
    muted.add("$testKey, $issue")
    val newMuted: List<String> = mutableListOf<String>() + firstLine + muted.sorted()
    file.writeText(newMuted.joinToString("\n"))
}

internal fun wrapWithAutoMute(f: () -> Unit, testKey: String): (() -> Unit)? {
    val doAutoMute = DO_AUTO_MUTE
    if (doAutoMute != null) {
        return {
            try {
                f()
            } catch (e: Throwable) {
                doAutoMute.muteTest(testKey)
                throw e
            }
        }
    } else {
        return null
    }
}

internal inline fun RunNotifier.withAutoMuteListener(
    testKey: String,
    crossinline run: () -> Unit,
) {
    val doAutoMute = DO_AUTO_MUTE
    if (doAutoMute != null) {
        val autoMuteListener = object : RunListener() {
            override fun testFailure(failure: Failure) {
                doAutoMute.muteTest(testKey)
                super.testFailure(failure)
            }
        }

        try {
            addListener(autoMuteListener)
            run()
        } finally {
            removeListener(autoMuteListener)
        }
    } else {
        run()
    }
}
