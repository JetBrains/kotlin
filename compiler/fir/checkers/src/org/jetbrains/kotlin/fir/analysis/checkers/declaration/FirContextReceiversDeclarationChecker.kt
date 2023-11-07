/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes.CONTEXT_RECEIVER_LIST
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.util.getChildren

object FirContextReceiversDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.ContextReceivers)) return
        if (declaration.source?.kind is KtFakeSourceElementKind) return
        if (!declaration.hasContextReceiver()) return
        val source = declaration.source?.findContextReceiverListSource() ?: return

        reporter.reportOn(
            source,
            FirErrors.UNSUPPORTED_FEATURE,
            LanguageFeature.ContextReceivers to context.languageVersionSettings,
            context
        )
    }

    private fun FirDeclaration.hasContextReceiver(): Boolean {
        val contextReceivers = when (this) {
            is FirCallableDeclaration -> contextReceivers
            is FirRegularClass -> contextReceivers
            is FirScript -> contextReceivers
            else -> emptyList()
        }
        return contextReceivers.isNotEmpty()
    }


    private fun KtSourceElement.findContextReceiverListSource(): KtLightSourceElement? {
        var contextReceiverList: LighterASTNode? = null
        var contextReceiverListOffset = startOffset

        val nodes = lighterASTNode.getChildren(treeStructure)
        for (node in nodes) {
            if (node.tokenType == CONTEXT_RECEIVER_LIST) {
                contextReceiverList = node
                break
            } else {
                contextReceiverListOffset += node.endOffset - node.startOffset
            }
        }
        if (contextReceiverList == null) return null

        return contextReceiverList.toKtLightSourceElement(
            treeStructure,
            startOffset = contextReceiverListOffset,
            endOffset = contextReceiverListOffset + contextReceiverList.textLength,
        )
    }
}
