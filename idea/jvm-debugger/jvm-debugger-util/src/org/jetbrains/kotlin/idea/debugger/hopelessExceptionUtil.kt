/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.openapi.diagnostic.Logger
import com.sun.jdi.IncompatibleThreadStateException
import com.sun.jdi.VMDisconnectedException

private val LOG = Logger.getInstance("HopelessExceptionUtils")

inline fun <T : Any> hopelessAware(block: () -> T?): T? {
    return try {
        block()
    } catch (e: Exception) {
        handleHopelessException(e)
        null
    }
}

fun handleHopelessException(e: Exception) {
    when (if (e is EvaluateException) e.cause ?: e else e) {
        is IncompatibleThreadStateException, is VMDisconnectedException -> {}
        else -> {
            if (e is EvaluateException) {
                LOG.debug("Cannot evaluate async stack trace", e)
            } else {
                throw e
            }
        }
    }
}