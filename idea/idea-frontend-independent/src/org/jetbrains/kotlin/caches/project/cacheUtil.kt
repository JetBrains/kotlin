/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.project

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

fun <T> Module.cacheByClass(classForKey: Class<*>, vararg dependencies: Any, provider: () -> T): T {
    return CachedValuesManager.getManager(project).cache(this, dependencies, classForKey, provider)
}

fun <T> Module.cacheByClassInvalidatingOnRootModifications(classForKey: Class<*>, provider: () -> T): T {
    return cacheByClass(classForKey, ProjectRootModificationTracker.getInstance(project), provider = provider)
}

/**
 * Note that it uses lambda's class for caching (essentially, anonymous class), which means that all invocations will be cached
 * by the one and the same key.
 * It is encouraged to use explicit class, just for the sake of readability.
 */
fun <T> Module.cacheInvalidatingOnRootModifications(provider: () -> T): T {
    return cacheByClassInvalidatingOnRootModifications(provider::class.java, provider)
}

fun <T> Project.cacheByClass(classForKey: Class<*>, vararg dependencies: Any, provider: () -> T): T {
    return CachedValuesManager.getManager(this).cache(this, dependencies, classForKey, provider)
}

fun <T> Project.cacheByClassInvalidatingOnRootModifications(classForKey: Class<*>, provider: () -> T): T {
    return cacheByClass(classForKey, ProjectRootModificationTracker.getInstance(this), provider = provider)
}

/**
 * Note that it uses lambda's class for caching (essentially, anonymous class), which means that all invocations will be cached
 * by the one and the same key.
 * It is encouraged to use explicit class, just for the sake of readability.
 */
fun <T> Project.cacheInvalidatingOnRootModifications(provider: () -> T): T {
    return cacheByClassInvalidatingOnRootModifications(provider::class.java, provider)
}

private fun <T> CachedValuesManager.cache(
    holder: UserDataHolder,
    dependencies: Array<out Any>,
    classForKey: Class<*>,
    provider: () -> T
): T {
    return getCachedValue(
        holder,
        getKeyForClass(classForKey),
        { CachedValueProvider.Result.create(provider(), dependencies) },
        false
    )
}

