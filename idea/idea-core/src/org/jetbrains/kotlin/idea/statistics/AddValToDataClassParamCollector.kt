/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object AddValToDataClassParamCollector {

    fun log(timeStarted: Long, timeFinished: Long, isValAdded: Boolean, onSymbol: Char, isBeforeTyping: Boolean) {

        val data = mapOf(
            "lagging" to (timeFinished - timeStarted).toString(),
            "isValAdded" to isValAdded.toString(),
            "onSymbol" to onSymbol.toString(),
            "isBeforeTyping" to isBeforeTyping.toString()
        )

        KotlinFUSLogger.log(FUSEventGroups.Editor, "addValToDataClassParameters", data)
    }
}