/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence

import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallCheckerWithNameHeuristics
import org.jetbrains.kotlin.idea.debugger.sequence.psi.StreamCallChecker

class SequenceCallCheckerWithNameHeuristics(nestedChecker: StreamCallChecker) : CallCheckerWithNameHeuristics(nestedChecker) {
    private companion object {

        val TERMINATION_CALLS: Set<String> = setOf(
            "all", "any", "associate", "associateBy", "associateByTo", "associateTo", "average", "chunked", "contains", "count", "distinct",
            "distinctBy", "elementAt", "elementAtOrElse", "elementAtOrNull", "find", "findLast", "first", "firstOrNull", "fold",
            "foldIndexed", "forEach", "forEachIndexed", "groupBy", "groupByTo", "indexOf", "indexOfFirst", "indexOfLast", "joinToString",
            "joinTo", "last", "lastIndexOf", "lastOrNull", "max", "maxBy", "maxWith", "min", "minBy", "minWith", "none", "partition",
            "reduce", "reduceIndexed", "single", "singleOrNull", "sum", "sumBy", "sumByDouble", "toCollection", "toHashSet", "toList",
            "toMutableList", "toMutableSet", "toSet", "toSortedSet", "unzip"
        )
    }

    override fun isTerminalCallName(callName: String): Boolean = TERMINATION_CALLS.contains(callName)
}
