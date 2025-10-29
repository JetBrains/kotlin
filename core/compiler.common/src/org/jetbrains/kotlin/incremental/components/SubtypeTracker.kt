/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.name.FqName

@DefaultImplementation(SubtypeTracker.DoNothing::class)
interface SubtypeTracker {
    fun report(className: FqName, subtypeName: FqName)

    object DoNothing : SubtypeTracker {
        override fun report(className: FqName, subtypeName: FqName) {}
    }
}
