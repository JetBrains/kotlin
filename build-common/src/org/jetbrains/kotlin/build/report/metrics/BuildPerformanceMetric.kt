/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.io.Serializable


@Suppress("Reformat")
enum class BuildPerformanceMetric(val parent: BuildPerformanceMetric? = null, val readableString: String) : Serializable {
    OUTPUT_SIZE(readableString = "Total output size"),
        LOOKUP_SIZE(OUTPUT_SIZE, "Lookups size"),
        SNAPSHOT_SIZE(OUTPUT_SIZE, "ABI snapshot size"),
    ;

    companion object {
        const val serialVersionUID = 0L

        val children by lazy {
            values().filter { it.parent != null }.groupBy { it.parent }
        }
    }
}