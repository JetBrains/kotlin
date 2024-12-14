/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

class AutoMute() {
    private val mutedTests: MutableList<String> = mutableListOf()

    fun muteTest(testKey: String) {
        mutedTests.add(testKey)
    }

    fun isMuted(testKey: String): Boolean = mutedTests.contains(testKey)

    fun muted(testKey: String) {
        System.err.println("MUTED TEST: $testKey")
        mutedTests.remove(testKey)
    }
}

val DO_AUTO_MUTE: AutoMute by lazy { AutoMute() }

internal fun wrapWithAutoMute(f: () -> Unit, testKey: String): (() -> Unit) {
    return {
        if (DO_AUTO_MUTE.isMuted(testKey)) {
            DO_AUTO_MUTE.muted(testKey)
        } else {
            try {
                f()
            } catch (e: Throwable) {
                DO_AUTO_MUTE.muteTest(testKey)
                throw e
            }
        }
    }
}
