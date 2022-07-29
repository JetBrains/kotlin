/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.name.FqName
import java.io.File

abstract class IncrementalExtension(private val lookupTracker: LookupTracker) {
    // safe mode
    fun addLookup(
        filePath: String,
        position: Position = Position.NO_POSITION,
        scopeFqName: String,
        scopeKind: ScopeKind,
        name: String
    ) = lookupTracker.record(
        filePath,
        position,
        scopeFqName,
        scopeKind,
        name
    )

    abstract fun registerComplementary(first: File, second: File)
    abstract fun subscribeOnHierarchyChange(type: HierarchyListenerType, fqName: FqName, files: List<File>)

    // pro mode
    abstract fun subscribeOnAllChanges(listener: IncrementalChangesListener)
}

abstract class IncrementalChangesListener

// listener types
interface HierarchyListenerType
class ParentListenerType : HierarchyListenerType
class ChildListenerType : HierarchyListenerType