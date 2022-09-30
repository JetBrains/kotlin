/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.caches

import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

public abstract class SoftCachedMap<K : Any, V : Any> {
    public abstract fun getOrPut(key: K, create: () -> V): V

    @TestOnly
    public abstract fun clear()

    public companion object {
        public fun <K : Any, V : Any> create(
            project: Project,
            trackers: List<Any>
        ): SoftCachedMap<K, V> = when {
            trackers.isEmpty() -> SoftCachedMapWithoutTrackers()
            else -> SoftCachedMapWithTrackers(project, trackers.toTypedArray())
        }
    }
}

private class SoftCachedMapWithTrackers<K : Any, V : Any>(
    private val project: Project,
    private val trackers: Array<Any>
) : SoftCachedMap<K, V>() {
    private val cache = ConcurrentHashMap<K, CachedValue<V>>()

    override fun clear() {
        cache.clear()
    }

    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key) {
            CachedValuesManager.getManager(project).createCachedValue {
                CachedValueProvider.Result(create(), *trackers)
            }
        }.value
    }
}

private class SoftCachedMapWithoutTrackers<K : Any, V : Any> : SoftCachedMap<K, V>() {
    private val cache = ContainerUtil.createConcurrentSoftMap<K, V>()

    override fun clear() {
        cache.clear()
    }

    override fun getOrPut(key: K, create: () -> V): V {
        return cache.getOrPut(key, create)
    }
}