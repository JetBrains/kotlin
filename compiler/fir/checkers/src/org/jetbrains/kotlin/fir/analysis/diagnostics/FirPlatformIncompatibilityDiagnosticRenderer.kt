/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.ContextIndependentParameterRenderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers.ThreatOfTailLoss
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

class FirPlatformIncompatibilityDiagnosticRenderer(
    private val mode: MultiplatformDiagnosticRenderingMode
) : ContextIndependentParameterRenderer<Map<out ExpectActualMatchingCompatibility, Collection<FirBasedSymbol<*>>>> {
    override fun render(
        obj: Map<out ExpectActualMatchingCompatibility, Collection<FirBasedSymbol<*>>>,
    ): String {
        if (obj.isEmpty()) return ""

        return buildString {
            mode.newLine(this)
            renderIncompatibilityInformation(obj, "", mode)
        }
    }

    companion object {
        @JvmField
        val TEXT: FirPlatformIncompatibilityDiagnosticRenderer =
            FirPlatformIncompatibilityDiagnosticRenderer(MultiplatformDiagnosticRenderingMode())
    }
}

class FirIncompatibleExpectedActualClassScopesRenderer(
    private val mode: MultiplatformDiagnosticRenderingMode
) : ContextIndependentParameterRenderer<List<Pair<FirBasedSymbol<*>, Map<out ExpectActualMatchingCompatibility.Mismatch, Collection<FirBasedSymbol<*>>>>>> {
    override fun render(
        obj: List<Pair<FirBasedSymbol<*>, Map<out ExpectActualMatchingCompatibility.Mismatch, Collection<FirBasedSymbol<*>>>>>
    ): String {
        if (obj.isEmpty()) return ""

        return buildString {
            mode.newLine(this)
            renderIncompatibleClassScopes(obj, "", mode)
        }
    }

    companion object {
        @JvmField
        val TEXT: FirIncompatibleExpectedActualClassScopesRenderer =
            FirIncompatibleExpectedActualClassScopesRenderer(MultiplatformDiagnosticRenderingMode())
    }
}

open class MultiplatformDiagnosticRenderingMode {
    open fun newLine(sb: StringBuilder) {
        sb.appendLine()
    }

    open fun renderList(sb: StringBuilder, elements: List<() -> Unit>) {
        sb.appendLine()
        for (element in elements) {
            element()
        }
    }

    open fun renderSymbol(sb: StringBuilder, symbol: FirBasedSymbol<*>, indent: String) {
        sb.append(indent)
        sb.append(INDENTATION_UNIT)
        @OptIn(ThreatOfTailLoss::class)
        sb.appendLine(FirDiagnosticRenderers.SYMBOL.render(symbol))
    }
}

private fun StringBuilder.renderIncompatibilityInformation(
    map: Map<out ExpectActualMatchingCompatibility, Collection<FirBasedSymbol<*>>>,
    indent: String,
    mode: MultiplatformDiagnosticRenderingMode
) {
    for ((compatibility, descriptors) in map) {
        append(indent)
        append("The following declaration")
        if (descriptors.size == 1) append(" is") else append("s are")
        append(" incompatible")
        (compatibility as? ExpectActualMatchingCompatibility.Mismatch)?.reason?.let { append(" because $it") }
        append(":")

        mode.renderList(this, descriptors.map { descriptor ->
            { mode.renderSymbol(this, descriptor, indent) }
        })
    }
}

private fun StringBuilder.renderIncompatibleClassScopes(
    unfulfilled: List<Pair<FirBasedSymbol<*>, Map<out ExpectActualMatchingCompatibility, Collection<FirBasedSymbol<*>>>>>,
    indent: String,
    mode: MultiplatformDiagnosticRenderingMode
) {
    mode.renderList(this, unfulfilled.indices.map { index ->
        {
            val (descriptor, mapping) = unfulfilled[index]
            mode.renderSymbol(this, descriptor, indent)
            if (mapping.isNotEmpty()) {
                mode.newLine(this)
                renderIncompatibilityInformation(mapping, indent + INDENTATION_UNIT, mode)
            }
            if (index != unfulfilled.lastIndex) {
                mode.newLine(this)
            }
        }
    })
}

internal const val INDENTATION_UNIT = "    "
