/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrintWithSettingsFrom

public interface KtClassifierBodyRenderer {
    context(KtAnalysisSession, KtDeclarationRenderer)
    public fun renderBody(symbol: KtSymbolWithMembers, printer: PrettyPrinter)

    public object NO_BODY : KtClassifierBodyRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderBody(symbol: KtSymbolWithMembers, printer: PrettyPrinter) {
        }
    }

    public object EMPTY_BRACES : KtClassifierBodyRenderer {
        context(KtAnalysisSession, KtDeclarationRenderer)
        override fun renderBody(symbol: KtSymbolWithMembers, printer: PrettyPrinter) {
            printer.append("{\n}")
        }
    }

    public object BODY_WITH_MEMBERS : KtClassifierBodyWithMembersRenderer() {
        override fun renderEmptyBodyForEmptyMemberScope(symbol: KtSymbolWithMembers): Boolean {
            return false
        }
    }

    public object BODY_WITH_MEMBERS_OR_EMPTY_BRACES : KtClassifierBodyWithMembersRenderer() {
        override fun renderEmptyBodyForEmptyMemberScope(symbol: KtSymbolWithMembers): Boolean {
            return true
        }
    }
}

public abstract class KtClassifierBodyWithMembersRenderer : KtClassifierBodyRenderer {
    public abstract fun renderEmptyBodyForEmptyMemberScope(symbol: KtSymbolWithMembers): Boolean

    context(KtAnalysisSession, KtDeclarationRenderer)
    public override fun renderBody(symbol: KtSymbolWithMembers, printer: PrettyPrinter) {
        val members = bodyMemberScopeProvider.getMemberScope(symbol).filter { it !is KtConstructorSymbol || !it.isPrimary }
            .let { bodyMemberScopeSorter.sortMembers(it, symbol) }
        val membersToPrint = members.mapNotNull { member ->
            val rendered = prettyPrintWithSettingsFrom(printer) {
                renderDeclaration(member, this)
            }
            if (rendered.isNotEmpty()) member to rendered else null
        }
        if (membersToPrint.isEmpty() && !renderEmptyBodyForEmptyMemberScope(symbol)) return

        printer.withIndentInBraces {
            var previous: KtDeclarationSymbol? = null
            for ((member, rendered) in membersToPrint) {
                if (previous != null) {
                    printer.append(codeStyle.getSeparatorBetweenMembers(previous, member))
                }
                previous = member
                printer.append(rendered)
            }
        }
    }

}
