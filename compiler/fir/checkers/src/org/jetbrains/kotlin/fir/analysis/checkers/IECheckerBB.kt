/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCheckNotNullCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirSafeCallExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.render
import kotlin.reflect.full.memberProperties

class IEReporter(
    private val source: KtSourceElement?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
    private val error: KtDiagnosticFactory1<String>,
) {
    operator fun invoke(v: IEData) {
        val dataStr = buildList {
            addAll(serializeData(v))
        }.joinToString("; ")
        val str = "$borderTag $dataStr $borderTag"
        reporter.reportOn(source, error, str, context)
    }

    private val borderTag: String = "KLEKLE"

    private fun serializeData(v: IEData): List<String> = buildList {
        v::class.memberProperties.forEach { property ->
            add("${property.name}: ${property.getter.call(v)}")
        }
    }
}

data class IEData(
    val operator: String? = null,
    val trailing: Boolean? = null,
    val kind: String? = null,
    val chainCall: String? = null,
)

private fun topDownTraverse(element: FirElement, acc: MutableList<String>) {
    when (element) {
        is FirSafeCallExpression -> {
            acc.add("?")
            val rec = element.receiver
            if (rec is FirQualifiedAccessExpression) {
                topDownTraverse(rec.explicitReceiver ?: return, acc)
            }
            if (rec is FirSafeCallExpression) {
                topDownTraverse(rec, acc)
            }
        }
        is FirQualifiedAccessExpression -> {
            acc.add(".")
            val receiver = element.explicitReceiver ?: return
            topDownTraverse(receiver, acc)
        }
        is FirCheckNotNullCall -> {
            acc.add("!!")
            topDownTraverse(element.argument, acc)
        }
    }
}

private fun downTopTraverse(index: Int, elements: List<FirElement>): FirElement? {
    if (index >= elements.size) return null
    when (elements[index]) {
        is FirCheckNotNullCall -> {
            return downTopTraverse(index + 1, elements) ?: elements[index]
        }
        is FirArgumentList -> {
            val element = elements[index + 1]
            if (element is FirQualifiedAccessExpression) {
                if (element.explicitReceiver != elements[index - 1]) return null
                return downTopTraverse(index + 1, elements)
            }
            if (element is FirCheckNotNullCall) {
                return downTopTraverse(index + 1, elements)
            }
            return null
        }
        is FirQualifiedAccessExpression -> {
            return downTopTraverse(index + 1, elements) ?: elements[index]
        }
        is FirSafeCallExpression -> {
            return downTopTraverse(index + 1, elements) ?: elements[index]
        }
        else -> return null
    }
}

private fun List<String>.kind() = when {
    all { it == "!!" } -> "all-!!"
    all { it == "?" } -> "all-?"
    suffixOf("!!") -> "suffix-!!"
    suffixOf("?") -> "suffix-??"
    contains("?") && contains("!!") -> "mix"
    else -> "unknown"
}

private fun List<String>.suffixOf(str: String): Boolean {
    var inSuffix = false
    for (s in this) {
        if (s == ".") continue
        if (s == str) {
            inSuffix = true
        }
        return false
    }
    return inSuffix
}

object IECheckerBB : FirCheckNotNullCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCheckNotNullCall) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)

        val elements = context.containingElements.asReversed()
        val root = downTopTraverse(0, elements) ?: return
        val chainCall = mutableListOf<String>().also { topDownTraverse(root, it) }

        report(
            IEData(
                operator = "!!",
                trailing = (root === expression),
                kind = chainCall.asReversed().kind(),
                chainCall = chainCall.joinToString(" "),
            )
        )
    }
}

object IECheckerSC : FirSafeCallExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirSafeCallExpression) {
        val report = IEReporter(expression.source, context, reporter, FirErrors.IE_DIAGNOSTIC)

        val elements = context.containingElements.asReversed()
        val root = downTopTraverse(0, elements) ?: return
        val chainCall = mutableListOf<String>().also { topDownTraverse(root, it) }

        report(
            IEData(
                operator = "?",
                kind = chainCall.asReversed().kind(),
                chainCall = chainCall.joinToString(" "),
            )
        )
    }
}
