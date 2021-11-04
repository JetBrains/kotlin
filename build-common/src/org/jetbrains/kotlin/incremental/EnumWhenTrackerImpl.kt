/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.EnumWhenTracker

@Suppress("unused")
class EnumWhenTrackerImpl: EnumWhenTracker {
    private val pathWhenToEnumClass = hashMapOf<String, MutableSet<String>>()

    val pathWhenToEnumClassMap: Map<String, Collection<String>>
        get() = pathWhenToEnumClass

    override fun report(whenUsageClassPath: String, enumClassFqName: String) {
        pathWhenToEnumClass.getOrPut(whenUsageClassPath) { hashSetOf() }.add(enumClassFqName)
    }
}