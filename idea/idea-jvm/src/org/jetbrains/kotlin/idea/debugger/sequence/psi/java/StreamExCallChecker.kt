/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import org.jetbrains.kotlin.idea.debugger.sequence.psi.CallCheckerWithNameHeuristics
import org.jetbrains.kotlin.idea.debugger.sequence.psi.StreamCallChecker

class StreamExCallChecker(nestedChecker: StreamCallChecker): CallCheckerWithNameHeuristics(nestedChecker) {
    private companion object {
        val TERMINATION_CALLS: Set<String> = setOf(
            "forEach", "toArray", "reduce", "collect", "min", "max", "count", "sum", "anyMatch", "allMatch", "noneMatch", "findFirst",
            "findAny", "forEachOrdered", "average", "summaryStatistics", "toList", "toSet", "toCollection", "toListAndThen", "toSetAndThen",
            "toImmutableList", "toImmutableSet", "toMap", "toSortedMap", "toNavigableMap", "toImmutableMap", "toMapAndThen", "toCustomMap",
            "partitioningBy", "partitioningTo", "groupingBy", "groupingTo", "grouping", "joining", "toFlatList", "toFlatCollection",
            "maxBy", "maxByInt", "maxByLong", "maxByDouble", "minBy", "minByInt", "minByLong", "minByDouble", "has", "indexOf", "foldLeft",
            "foldRight", "scanLeft", "scanRight", "toByteArray", "toCharArray", "toShortArray", "toBitSet", "toFloatArray", "charsToString",
            "codePointsToString", "forPairs", "forKeyValue", "asByteInputStream", "into"
        )
    }

    override fun isTerminalCallName(callName: String): Boolean = TERMINATION_CALLS.contains(callName)
}