/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.declarations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrintWithSettingsFrom

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

        printer.withIndentInBraces {
            var previous: KaDeclarationSymbol? = null
            for ((member, rendered) in membersToPrint) {
                if (previous != null) {
                    printer.append(declarationRenderer.codeStyle.getSeparatorBetweenMembers(analysisSession, previous, member))
                }
                previous = member
                printer.append(rendered)
            }
        }
    }
}
