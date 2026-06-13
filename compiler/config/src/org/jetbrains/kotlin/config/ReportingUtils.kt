/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

@OptIn(MessageCollectorAccess::class)
fun CompilerConfiguration.reportLog(message: String, location: CompilerMessageSourceLocation? = null) {
    messageCollector.report(CompilerMessageSeverity.LOGGING, message, location)
}
