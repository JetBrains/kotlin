/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.fir.caches.FirLazyValue

class LLFirSoftLazyValue<V>(project: Project, createValue: () -> V) : FirLazyValue<V>() {
    val cachedValue = softCachedValue(project) { createValue() }

    override fun getValue(): V = cachedValue.getValue()
}
