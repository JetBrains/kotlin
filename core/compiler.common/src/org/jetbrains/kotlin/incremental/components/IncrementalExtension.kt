/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.name.FqName

abstract class IncrementalExtension(val lookupTracker: LookupTracker) {
    abstract fun addListener(listener: ICListener)
    abstract fun subscribe(listenerId: String, fqNames: List<FqName>)
    abstract fun unsubscribe(listenerId: String, fqNames: List<FqName>)
    abstract fun markLookupAsDirty(fqNames: List<FqName>)
}

abstract class ICListener {
    abstract val id: String // unique
    abstract val type: IncrementalListenerType
    abstract fun onChange(fqNames: Set<FqName>)
}

// listener types
sealed class IncrementalListenerType
object ParentListenerType : IncrementalListenerType()
object ChildListenerType : IncrementalListenerType()
