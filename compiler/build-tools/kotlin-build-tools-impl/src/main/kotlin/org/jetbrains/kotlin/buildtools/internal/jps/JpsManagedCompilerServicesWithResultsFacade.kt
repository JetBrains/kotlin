/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jps

import org.jetbrains.kotlin.buildtools.internal.KotlinLoggerMessageCollectorAdapter
import org.jetbrains.kotlin.daemon.client.CompilerCallbackServicesFacadeServer
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.JpsCompilerServicesFacade
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.Serializable

internal class JpsManagedCompilerServicesWithResultsFacade(
    private val loggerAdapter: KotlinLoggerMessageCollectorAdapter,
    incrementalCompilationComponents: IncrementalCompilationComponents? = null,
    lookupTracker: LookupTracker? = null,
    expectActualTracker: ExpectActualTracker? = null,
    inlineConstTracker: InlineConstTracker? = null,
    enumWhenTracker: EnumWhenTracker? = null,
    importTracker: ImportTracker? = null,
    icFileMappingTracker: ICFileMappingTracker? = null,
    compilationCanceledStatus: CompilationCanceledStatus? = null,
) : JpsCompilerServicesFacade, CompilerCallbackServicesFacadeServer(
    incrementalCompilationComponents,
    lookupTracker,
    compilationCanceledStatus,
    expectActualTracker,
    inlineConstTracker,
    enumWhenTracker,
    importTracker,
    icFileMappingTracker
) {
    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        loggerAdapter.reportFromDaemon(
            null, category, severity, message, attachment
        )
    }
}
