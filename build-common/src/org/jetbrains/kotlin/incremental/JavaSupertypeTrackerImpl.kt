/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.JavaSupertypeTracker
import org.jetbrains.kotlin.name.FqName
import java.util.concurrent.ConcurrentHashMap

class JavaSupertypeTrackerImpl : JavaSupertypeTracker {
    val supertypesMap: Map<FqName, Set<FqName>>
        field = ConcurrentHashMap<FqName, Set<FqName>>()

    override fun report(classFqName: FqName, supertypeFqNames: Collection<FqName>) {
        supertypesMap[classFqName] = supertypeFqNames.toSet()
    }
}
