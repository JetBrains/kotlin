/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopeUtil
import com.intellij.psi.search.impl.VirtualFileEnumeration
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaResolutionScope

/**
 * Converts a [GlobalSearchScope] to a string representation for testing purposes.
 *
 * Handles special cases like union scopes, file scopes and intersection scopes.
 */
fun GlobalSearchScope.renderAsTestOutput(): String {
    val builder = IndentedStringBuilder()
    builder.renderScope(this)
    return builder.toString()
}

private fun IndentedStringBuilder.renderScope(scope: GlobalSearchScope) {
    val unionComponents = GlobalSearchScopeUtil.flattenUnionScope(scope)
    when {
        unionComponents.size > 1 -> {
            appendLine("UnionScope")
            renderChildren(unionComponents)
        }

        scope is KaResolutionScope -> {
            renderScope(scope.underlyingSearchScope)
        }

        GlobalSearchScopeUtil.isIntersectionScope(scope) -> {
            appendLine("IntersectionScope")
            renderChildren(GlobalSearchScopeUtil.flattenIntersectionScope(scope))
        }

        scope is GlobalSearchScope.FilesScope -> {
            renderClassName(scope)
            indent {
                appendLine("files:")
                indent {
                    scope.files.map { it.name }.sorted().forEach { fileName ->
                        appendLine(fileName)
                    }
                }
            }
        }

        // This case covers `NotScope`.
        scope is DelegatingGlobalSearchScope -> {
            renderClassName(scope)
            indent {
                renderScope(scope.delegate)
            }
        }

        scope::class.qualifiedName == "com.intellij.psi.search.FileScope" && scope is VirtualFileEnumeration -> {
            appendLine("${scope::class.simpleName}: ${scope.filesIfCollection?.singleOrNull()?.name}")
        }

        else -> {
            renderClassName(scope)
        }
    }
}

private fun IndentedStringBuilder.renderChildren(children: Collection<GlobalSearchScope>) {
    indent {
        children
            .map(GlobalSearchScope::renderAsTestOutput)
            .sorted()
            .forEach(::appendBlock)
    }
}

private fun IndentedStringBuilder.renderClassName(searchScope: GlobalSearchScope) {
    appendLine(searchScope::class.simpleName ?: searchScope.displayName)
}

private class IndentedStringBuilder {
    private val builder = StringBuilder()
    private var currentIndent = ""

    fun indent(action: IndentedStringBuilder.() -> Unit) {
        val previousIndent = currentIndent
        currentIndent += "    "
        action()
        currentIndent = previousIndent
    }

    fun appendLine(content: String) {
        builder.append(currentIndent)
            .append(content)
            .append("\n")
    }

    /** Appends an already rendered [block], re-indenting each of its lines relative to the current indent. */
    fun appendBlock(block: String) {
        block.lineSequence().filter { it.isNotEmpty() }.forEach(::appendLine)
    }

    override fun toString(): String = builder.toString()
}
