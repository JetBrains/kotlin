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

        val [companionBlockMembers, otherMembers] = members.partition { symbol is KaClassSymbol && it.isCompanionBlockMember }
        val otherMembersToPrint = renderMembers(analysisSession, otherMembers, declarationRenderer, printer)
        val companionBlockMembersToPrint = renderMembers(analysisSession, companionBlockMembers, declarationRenderer, printer)

        if (otherMembersToPrint.isEmpty() && companionBlockMembersToPrint.isEmpty() && !renderEmptyBodyForEmptyMemberScope(symbol)) return

        printer.withIndentInBraces {
            printMembers(analysisSession, otherMembersToPrint, declarationRenderer, printer)

            if (symbol is KaClassSymbol && companionBlockMembersToPrint.isNotEmpty()) {
                val lastOtherMember = otherMembersToPrint.lastOrNull()?.first
                if (lastOtherMember != null) {
                    val firstCompanionBlockMember = companionBlockMembersToPrint.first().first
                    val separator = declarationRenderer.codeStyle
                        .getSeparatorBetweenMembers(analysisSession, lastOtherMember, firstCompanionBlockMember)
                    append(separator)
                }

                " ".separated(
                    { declarationRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.COMPANION_KEYWORD, symbol, printer) },
                    { withIndentInBraces { printMembers(analysisSession, companionBlockMembersToPrint, declarationRenderer, printer) } },
                )
            }
        }
    }

    private fun renderMembers(
        analysisSession: KaSession,
        members: List<KaDeclarationSymbol>,
        declarationRenderer: KaDeclarationRenderer,
        printer: PrettyPrinter,
    ): List<Pair<KaDeclarationSymbol, String>> = members.mapNotNull { member ->
        val rendered = prettyPrintWithSettingsFrom(printer) {
            declarationRenderer.renderDeclaration(analysisSession, member, this)
        }
        if (rendered.isNotEmpty()) member to rendered else null
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
 * Whether the member is rendered in the `companion { }` block of its class: every [companion][KaCallableSymbol.isCompanion] member,
 * including the static members of Java classes, except enum entries, which keep their place at the start of the enum body.
 */
@OptIn(KaExperimentalApi::class)
private val KaDeclarationSymbol.isCompanionBlockMember: Boolean
    get() = this is KaCallableSymbol && this !is KaEnumEntrySymbol && isCompanion
