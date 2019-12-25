/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.reporter

import com.intellij.diagnostic.ITNReporter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.util.Consumer
import java.awt.Component

abstract class ITNReporterCompat : ITNReporter() {
    final override fun submit(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component?,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        return submitCompat(events, additionalInfo, parentComponent, consumer)
    }

    open fun submitCompat(
        events: Array<IdeaLoggingEvent>,
        additionalInfo: String?,
        parentComponent: Component?,
        consumer: Consumer<SubmittedReportInfo>
    ): Boolean {
        @Suppress("IncompatibleAPI")
        return super.submit(events, additionalInfo, parentComponent, consumer)
    }
}
