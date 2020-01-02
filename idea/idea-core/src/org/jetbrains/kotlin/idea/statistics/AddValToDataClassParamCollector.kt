/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object AddValToDataClassParamCollector {

    fun log(timeStarted: Long, timeFinished: Long, isValAdded: Boolean, onSymbol: Char, isBeforeTyping: Boolean) {

        val symbol = when (onSymbol) {
            ',' -> "comma"
            ')' -> "bracket"
            else -> "unknown"
        }

        val data = mapOf(
            "lagging" to (timeFinished - timeStarted).toString(),
            "is_val_added" to isValAdded.toString(),
            "on_symbol" to symbol,
            "is_before_typing" to isBeforeTyping.toString()
        )

        KotlinFUSLogger.log(FUSEventGroups.Editor, "AddValToDataClassParameters", data)
    }
}