/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence

import org.jetbrains.kotlin.idea.debugger.sequence.psi.StreamCallChecker
import org.jetbrains.kotlin.psi.KtCallExpression

class SequenceCallCheckerWithNameHeuristics(private val nestedChecker: StreamCallChecker) : StreamCallChecker {
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

    override fun isIntermediateCall(expression: KtCallExpression): Boolean {
        return nestedChecker.isIntermediateCall(expression)
    }

    override fun isTerminationCall(expression: KtCallExpression): Boolean {
        val name = expression.calleeExpression?.text
        if (name != null) {
            return TERMINATION_CALLS.contains(name) && nestedChecker.isTerminationCall(expression)
        }

        return nestedChecker.isTerminationCall(expression)
    }
}