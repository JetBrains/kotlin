/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.KotlinFirModificationTrackerService

internal fun Module.incModificationTracker() {
    project.service<KotlinFirModificationTrackerService>().increaseModificationCountForModule(this)
}