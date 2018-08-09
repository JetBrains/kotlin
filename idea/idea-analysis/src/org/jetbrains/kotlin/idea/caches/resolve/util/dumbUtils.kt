/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project


private val LOG = Logger.getInstance("kotlin.resolve.dumb.mode")

fun Project.isInDumbMode(): Boolean {
    val dumbService = DumbService.getInstance(this)
    val dumb = dumbService.isDumb
    if (dumb && dumbService.isAlternativeResolveEnabled) {
        LOG.warn("Kotlin does not support alternative resolve", Throwable("<stacktrace>"))
    }
    return dumb
}

