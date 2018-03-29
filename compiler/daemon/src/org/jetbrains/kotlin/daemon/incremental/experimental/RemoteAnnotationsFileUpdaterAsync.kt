/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationServicesFacade
import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacadeAsync
import org.jetbrains.kotlin.incremental.ICReporter
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class RemoteAnnotationsFileUpdaterAsync(private val servicesFacade: IncrementalCompilerServicesFacadeAsync) : AnnotationFileUpdater {

    override fun updateAnnotations(outdatedClasses: Iterable<JvmClassName>) = runBlocking {
        servicesFacade.updateAnnotations(outdatedClasses.map { it.internalName })
    }

    override fun revert() = runBlocking {
        servicesFacade.revert()
    }
}