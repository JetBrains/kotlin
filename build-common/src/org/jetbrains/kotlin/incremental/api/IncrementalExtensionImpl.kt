/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.api

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.FqName
import java.io.File

class IncrementalExtensionImpl(lookupTracker: LookupTracker, val cache: AbstractIncrementalCache<*>) : IncrementalExtension(lookupTracker) {
    val complementaryFilesTracker = ExpectActualTrackerImpl()
    var parentListenersMap = hashMapOf<FqName, MutableSet<File>>()
    var childListenersMap = hashMapOf<FqName, MutableSet<File>>()
    private var allChangesListeners = mutableListOf<ChangesListener>()

    override fun registerComplementary(first: File, second: File) {
        complementaryFilesTracker.report(first, second)
    }

    override fun subscribeOnHierarchyChange(type: HierarchyListenerType, fqName: FqName, files: List<File>) {
        when (type) {
            is ParentListenerType -> parentListenersMap
            is ChildListenerType -> childListenersMap
            else -> error("Unknown listener type")
        }.getOrPut(fqName) { HashSet() }.addAll(files)
    }

    override fun subscribeOnAllChanges(listener: IncrementalChangesListener) {
        if (listener !is ChangesListener) throw ClassCastException("Listener should be derived from org.jetbrains.kotlin.incremental.api.ChangesListener")
        allChangesListeners.add(listener)
    }

    fun getDirtyDataFromExternalListeners(
        changesCollector: ChangesCollector,
        caches: Iterable<IncrementalCacheCommon>,
        reporter: ICReporter
    ): DirtyData {
        var dirtyData = DirtyData()
        allChangesListeners.forEach {
            dirtyData += it.onChange(changesCollector, caches, reporter)
        }
        return dirtyData
    }
}

abstract class ChangesListener : IncrementalChangesListener() {
    abstract fun onChange(changes: ChangesCollector, caches: Iterable<IncrementalCacheCommon>, reporter: ICReporter): DirtyData
}