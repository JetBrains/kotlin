/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.fir.caches.FirLazyValue

internal class LLFirSoftLazyValue<V>(project: Project, createValue: () -> V) : FirLazyValue<V>() {
    private val cachedValue: CachedValue<V> = CachedValuesManager.getManager(project).createCachedValue {
        CachedValueProvider.Result(createValue(), ModificationTracker.NEVER_CHANGED)
    }

    override fun getValue(): V = cachedValue.getValue()
}
