/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.incremental

import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.common.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationServicesFacade
import org.jetbrains.kotlin.incremental.ICReporter
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class RemoteAnnotationsFileUpdater(private val servicesFacade: IncrementalCompilerServicesFacade) : AnnotationFileUpdater {
    override fun updateAnnotations(outdatedClasses: Iterable<JvmClassName>) {
        servicesFacade.updateAnnotations(outdatedClasses.map { it.internalName })
    }

    override fun revert() {
        servicesFacade.revert()
    }
}