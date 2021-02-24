/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.test.mutes.DO_AUTO_MUTE
import org.jetbrains.kotlin.test.mutes.muteTest
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import java.io.File

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
