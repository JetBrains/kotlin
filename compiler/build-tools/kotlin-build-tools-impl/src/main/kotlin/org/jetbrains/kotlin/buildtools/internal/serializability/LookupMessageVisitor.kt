/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.internal.serializability.Messages

internal class LookupMessageVisitor(private val lookupTracker: CompilerLookupTracker) : MessageVisitor {

    override fun accept(message: Messages): Boolean = when (message) {
        is Messages.LookupMessage -> {
            lookupTracker.recordLookup(message.filePath, message.scopeFqName, message.scopeKind, message.name)
            true
        }
        is Messages.LookupClear -> {
            lookupTracker.clear()
            true
        }
        else -> false
    }
}

internal class LogLineVisitor(private val logger: KotlinLogger) : MessageVisitor {

    override fun accept(message: Messages): Boolean = when (message) {
        is Messages.LogLine -> {
            when (message.level) {
                ERROR -> logger.error(message.logLine)
                WARN -> logger.warn(message.logLine)
                LIFECYCLE -> logger.lifecycle(message.logLine)
                INFO -> logger.info(message.logLine)
                DEBUG -> logger.debug(message.logLine)
            }
            true
        }
        else -> false
    }
}
