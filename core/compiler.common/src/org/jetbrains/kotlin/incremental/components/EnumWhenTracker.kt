/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation

@DefaultImplementation(EnumWhenTracker.DoNothing::class)
interface EnumWhenTracker {
    fun report(whenUsageClassPath: String, enumClassFqName: String)

    object DoNothing : EnumWhenTracker {
        override fun report(whenUsageClassPath: String, enumClassFqName: String) {}
    }
}