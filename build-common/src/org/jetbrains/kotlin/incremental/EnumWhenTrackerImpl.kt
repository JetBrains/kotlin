/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.components.EnumWhenTracker
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
class EnumWhenTrackerImpl: EnumWhenTracker {
    private val whenExpressionFilePathToEnumClass = ConcurrentHashMap<String, MutableSet<String>>()

    val whenExpressionFilePathToEnumClassMap: Map<String, Collection<String>>
        get() = whenExpressionFilePathToEnumClass

    override fun report(whenExpressionFilePath: String, enumClassFqName: String) {
        whenExpressionFilePathToEnumClass.getOrPut(whenExpressionFilePath) { hashSetOf() }.add(enumClassFqName)
    }
}