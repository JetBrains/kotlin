/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus

public abstract class KotlinGlobalModificationServiceBase(private val project: Project) : KotlinGlobalModificationService {
    @TestOnly
    protected abstract fun incrementModificationTrackers(includeBinaryTrackers: Boolean)

    @TestOnly
    override fun publishGlobalModuleStateModification() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        incrementModificationTrackers(includeBinaryTrackers = true)
        project.analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_MODULE_STATE_MODIFICATION).onModification()
    }

    @TestOnly
    override fun publishGlobalSourceModuleStateModification() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        incrementModificationTrackers(includeBinaryTrackers = false)
        project.analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION).onModification()
    }

    @TestOnly
    override fun publishGlobalSourceOutOfBlockModification() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        incrementModificationTrackers(includeBinaryTrackers = false)
        project.analysisMessageBus.syncPublisher(KotlinModificationTopics.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION).onModification()
    }
}
