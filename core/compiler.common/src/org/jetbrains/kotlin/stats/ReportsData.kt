/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.UnitStats
import java.util.SortedSet

sealed class ReportsData {
    abstract val unitStats: Collection<UnitStats>
}

class SingleReportsData(val report: UnitStats) : ReportsData() {
    override val unitStats: Collection<UnitStats> = listOf(report)
}

class ModulesReportsData(val reports: Map<String, UnitStats>) : ReportsData() {
    override val unitStats: Collection<UnitStats>
        get() = reports.values
}

class TimestampReportsData(val moduleName: String, val timeStamps: SortedSet<UnitStats>) : ReportsData() {
    override val unitStats: Collection<UnitStats>
        get() = timeStamps
}