/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.build.report.CompositeICReporter
import org.jetbrains.kotlin.build.report.RemoteICReporter

internal class RemoteCompositeICReporter(
    private val remoteReporters: Iterable<RemoteICReporter>
) : CompositeICReporter(remoteReporters), RemoteICReporter {
    override fun flush() {
        remoteReporters.forEach { it.flush() }
    }
}
