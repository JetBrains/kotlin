/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.components

import org.jetbrains.kotlin.container.DefaultImplementation

/**
 * EnumWhenTracker is used to track Java enum classes used in Kotlin when expressions for correct build scope expansion in IC during JPS build.
 */
@DefaultImplementation(EnumWhenTracker.DoNothing::class)
interface EnumWhenTracker {

    /**
     * Report Java enum class, which FqName is [enumClassFqName].
     * This enum class is used in Kotlin file with [whenExpressionFilePath] path in when expression.
     * Format of [enumClassFqName] class is "package.Outer$Inner"
     */
    fun report(whenExpressionFilePath: String, enumClassFqName: String)

    object DoNothing : EnumWhenTracker {
        override fun report(whenExpressionFilePath: String, enumClassFqName: String) {}
    }
}