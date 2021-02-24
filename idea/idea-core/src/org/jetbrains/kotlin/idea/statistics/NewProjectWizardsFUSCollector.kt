/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

object NewProjectWizardsFUSCollector {
    fun log(name: String, group: String, isKotlinDsl: Boolean) {
        val contextData = mapOf(
            "name" to name,
            "group" to group,
            "isKotlinDsl" to isKotlinDsl.toString()
        )

        KotlinFUSLogger.log(FUSEventGroups.NPWizards, "Finished", contextData)
    }
}