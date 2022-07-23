/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.api

import org.jetbrains.kotlin.incremental.AbstractIncrementalCache
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.FqName

class IncrementalExtensionImpl(lookupTracker: LookupTracker, private val getCachedFqNames: (String) -> List<FqName>) : IncrementalExtension(lookupTracker) {
    val incrementalChangesManager = IncrementalChangesManager()

    override fun addListener(listener: ICListener) {
        if (incrementalChangesManager.listenersMap[listener] != null)
            throw Exception("Listener with the same id has already been installed") // TODO: is it ok to throw here?
        incrementalChangesManager.listenersMap[listener] = getCachedFqNames(listener.id)
    }

    override fun subscribe(listenerId: String, fqNames: List<FqName>) {
        incrementalChangesManager.subscribe(listenerId, fqNames)
    }

    override fun unsubscribe(listenerId: String, fqNames: List<FqName>) {
        incrementalChangesManager.unsubscribe(listenerId, fqNames)
    }

    fun processChangesAndNotify(changedFqNames: List<FqName>, platformCache: AbstractIncrementalCache<*>) {
        for (entry in groupListeners()) {
            val listenerType = entry.key
            val listener = entry.value
            val fqNamesToNotify = when (listenerType) {
                ParentListenerType -> {
                    changedFqNames.flatMap { platformCache.getSupertypesOf(it) }
                }
                ChildListenerType -> {
                    changedFqNames.flatMap { platformCache.getSubtypesOf(it) }
                }
            }
            listener.forEach { it.onChange(fqNamesToNotify.intersect(it.getSubscribedFqNames())) }
        }
    }

    private fun groupListeners(): MutableMap<IncrementalListenerType, List<ICListener>> {
        val groupedListeners = mutableMapOf<IncrementalListenerType, List<ICListener>>()
        incrementalChangesManager.listenersMap.keys.map {
            if (groupedListeners[it.type] == null) {
                groupedListeners[it.type] = listOf(it)
            } else {
                groupedListeners[it.type] = groupedListeners[it.type]!! + it
            }
        }
        return groupedListeners
    }
}

class IncrementalChangesManager {
    var listenersMap = mutableMapOf<ICListener, List<FqName>>()

    fun subscribe(listenerId: String, fqNames: List<FqName>) {
        val listener = listenersMap.keys.singleOrNull { it.id == listenerId }
            ?: throw Exception("There is no listener with id: $listenerId")
        listenersMap[listener] = listenersMap[listener]!! + fqNames
    }

    fun unsubscribe(listenerId: String, fqNames: List<FqName>) {
        val listener = listenersMap.keys.singleOrNull { it.id == listenerId }
            ?: throw Exception("There is no listener with id: $listenerId")
        listenersMap[listener] = listenersMap[listener]!! - fqNames
    }
}

fun ICListener.getSubscribedFqNames(): List<FqName> {
    TODO("Not yet implemented")
}