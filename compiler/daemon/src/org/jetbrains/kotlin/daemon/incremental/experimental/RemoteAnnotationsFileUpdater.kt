/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.incremental.experimental

import org.jetbrains.kotlin.annotation.AnnotationFileUpdater
import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacade
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

internal class RemoteAnnotationsFileUpdater(private val servicesFacade: IncrementalCompilerServicesFacade) : AnnotationFileUpdater {
    override fun updateAnnotations(outdatedClasses: Iterable<JvmClassName>) {
        servicesFacade.updateAnnotations(outdatedClasses.map { it.internalName })
    }

    override fun revert() {
        servicesFacade.revert()
    }
}