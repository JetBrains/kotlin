/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import kotlin.time.Duration

object MachineryTiming {
    val phaseTimes = mutableMapOf<String, Double>()

    fun submit(key: String, duration: Duration) {
        submit(key, duration.inWholeNanoseconds)
    }

    fun submit(key: String, incrementNanos: Long) {
        synchronized(phaseTimes) {
            val oldval = phaseTimes[key] ?: 0.0
            phaseTimes[key] = oldval + incrementNanos / 1000000000.0
        }
    }

    // invoked via reflection from testinfra
    fun getPhaseTimezz(): Map<String, Double> {
        synchronized(phaseTimes) {
            val retval = phaseTimes.toMap()
            phaseTimes.clear()
            return retval
        }
    }
}