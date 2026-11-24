/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSpi
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrintWithSettingsFrom
import org.jetbrains.kotlin.lexer.KtTokens

@KaSpi
@KaExperimentalApi
public interface KaClassifierBodyRenderer {
    public fun renderBody(
        analysisSession: KaSession,
        symbol: KaDeclarationContainerSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object NO_BODY : KaClassifierBodyRenderer {
        override fun renderBody(
            analysisSession: KaSession,
            symbol: KaDeclarationContainerSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    @KaExperimentalApi
    public object EMPTY_BRACES : KaClassifierBodyRenderer {
        override fun renderBody(
            analysisSession: KaSession,
            symbol: KaDeclarationContainerSymbol,
            declarationRenderer: KaDeclarationRenderer,
            printer: PrettyPrinter,
        ) {
            printer.append("{\n}")
        }
    }

    @KaExperimentalApi
    public object BODY_WITH_MEMBERS : KaClassifierBodyWithMembersRenderer() {
        override fun renderEmptyBodyForEmptyMemberScope(symbol: KaDeclarationContainerSymbol): Boolean {
            return false
        }
    }

    @KaExperimentalApi
    public object BODY_WITH_MEMBERS_OR_EMPTY_BRACES : KaClassifierBodyWithMembersRenderer() {
        override fun renderEmptyBodyForEmptyMemberScope(symbol: KaDeclarationContainerSymbol): Boolean {
            return true
        }
    }
}

@KaSpi
@KaExperimentalApi
public abstract class KaClassifierBodyWithMembersRenderer : KaClassifierBodyRenderer {
    public abstract fun renderEmptyBodyForEmptyMemberScope(symbol: KaDeclarationContainerSymbol): Boolean

    public override fun renderBody(
        analysisSession: KaSession,
        symbol: KaDeclarationContainerSymbol,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    ) {
        val members = declarationRenderer.bodyMemberScopeProvider.getMemberScope(analysisSession, symbol)
            .filter { it !is KaConstructorSymbol || !it.isPrimary }
            .let { declarationRenderer.bodyMemberScopeSorter.sortMembers(analysisSession, it, symbol) }

        val membersToPrint = members.mapNotNull { member ->
            val rendered = prettyPrintWithSettingsFrom(printer) {
                declarationRenderer.renderDeclaration(analysisSession, member, this)
            }
            if (rendered.isNotEmpty()) member to rendered else null
        }

        if (membersToPrint.isEmpty() && !renderEmptyBodyForEmptyMemberScope(symbol)) return

        val canHaveCompanionBlocks = symbol is KaClassSymbol
        printer.withIndentInBraces {
            var lastPrintedMember: KaDeclarationSymbol? = null
            for (run in membersToPrint.splitIntoRuns { canHaveCompanionBlocks && it.first.isCompanionBlockMember }) {
                val firstMember = run.first().first
                if (lastPrintedMember != null) {
                    val separator = declarationRenderer.codeStyle
                        .getSeparatorBetweenMembers(analysisSession, lastPrintedMember, firstMember)
                    append(separator)
                }
                lastPrintedMember = run.last().first

                if (canHaveCompanionBlocks && firstMember.isCompanionBlockMember) {
                    " ".separated(
                        {
                            declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.COMPANION_KEYWORD, symbol, printer)
                        },
                        { withIndentInBraces { printMembers(analysisSession, run, declarationRenderer, printer) } },
                    )
                } else {
                    printMembers(analysisSession, run, declarationRenderer, printer)
                }
            }
        }
    }

    private fun printMembers(
        analysisSession: KaSession,
        membersToPrint: List<Pair<KaDeclarationSymbol, String>>,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    ) {
        var previous: KaDeclarationSymbol? = null
        for ([member, rendered] in membersToPrint) {
            if (previous != null) {
                printer.append(declarationRenderer.codeStyle.getSeparatorBetweenMembers(analysisSession, previous, member))
            }
            previous = member
            printer.append(rendered)
        }
    }
}

/**
 * Whether the member is rendered in a `companion { }` block of its class: every [companion][KaCallableSymbol.isCompanion] member,
 * including the static members of Java classes, except enum entries, which keep their place at the start of the enum body.
 */
@OptIn(KaExperimentalApi::class)
private val KaDeclarationSymbol.isCompanionBlockMember: Boolean
    get() = this is KaCallableSymbol && this !is KaEnumEntrySymbol && isCompanion

/**
 * Splits the list into runs of consecutive elements with the same [key], e.g., the members of each `companion { }` block and the members
 * between them.
 */
private inline fun <T> List<T>.splitIntoRuns(key: (T) -> Boolean): List<List<T>> = buildList<MutableList<T>> {
    for (element in this@splitIntoRuns) {
        val previousRun = lastOrNull()
        if (previousRun == null || key(previousRun.last()) != key(element)) {
            add(mutableListOf())
        }
        last().add(element)
    }
}
