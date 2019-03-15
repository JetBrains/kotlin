/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

fun <T> Module.cached(provider: CachedValueProvider<T>): T {
    return CachedValuesManager.getManager(project).getCachedValue(this, provider)
}

fun <T> Project.cached(provider: CachedValueProvider<T>): T {
    return CachedValuesManager.getManager(this).getCachedValue(this, provider)
}
