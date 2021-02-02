/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import kotlin.reflect.KClass

@RequiresOptIn
annotation class ForKtQuickFixesListBuilder()


class KtQuickFixesListBuilder private constructor() {
    val quickFixes = mutableMapOf<KClass<out KtDiagnosticWithPsi<*>>, MutableList<QuickFixFactory>>()

    inline fun <reified PSI : PsiElement, reified DIAGNOSTIC : KtDiagnosticWithPsi<PSI>> register(
        quickFixFactory: QuickFixesPsiBasedFactory<PSI>
    ) {
        quickFixes.getOrPut(DIAGNOSTIC::class) { mutableListOf() }.add(quickFixFactory)
    }

    inline fun <reified PSI : PsiElement, reified DIAGNOSTIC : KtDiagnosticWithPsi<PSI>> register(
        quickFixFactory: QuickFixesHLApiBasedFactory<PSI, DIAGNOSTIC>
    ) {
        quickFixes.getOrPut(DIAGNOSTIC::class) { mutableListOf() }.add(quickFixFactory)
    }

    @OptIn(ForKtQuickFixesListBuilder::class)
    private fun build() = KtQuickFixesList(quickFixes)

    companion object {
        fun register(init: KtQuickFixesListBuilder.() -> Unit) = KtQuickFixesListBuilder().apply(init).build()
    }
}

class KtQuickFixesList @ForKtQuickFixesListBuilder constructor(private val quickFixes: Map<KClass<out KtDiagnosticWithPsi<*>>, List<QuickFixFactory>>) {
    fun KtAnalysisSession.getQuickFixesFor(diagnostic: KtDiagnosticWithPsi<*>): List<IntentionAction> {
        val factories = quickFixes[diagnostic.diagnosticClass] ?: return emptyList()
        return factories.flatMap { createQuickFixes(it, diagnostic) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun KtAnalysisSession.createQuickFixes(
        quickFixFactory: QuickFixFactory,
        diagnostic: KtDiagnosticWithPsi<PsiElement>
    ): List<IntentionAction> = when (quickFixFactory) {
        is QuickFixesPsiBasedFactory<*> -> quickFixFactory.createQuickFix(diagnostic.psi)
        is QuickFixesHLApiBasedFactory<*, *> -> with(quickFixFactory as QuickFixesHLApiBasedFactory<PsiElement, KtDiagnosticWithPsi<*>>) {
            createQuickFix(diagnostic)
        }
        else -> error("Unsupported QuickFixFactory $quickFixFactory")
    }

    companion object {
        @OptIn(ForKtQuickFixesListBuilder::class)
        fun createCombined(registrars: List<KtQuickFixesList>): KtQuickFixesList {
            val allQuickFixes = registrars.map { it.quickFixes }.merge()
            return KtQuickFixesList(allQuickFixes)
        }

        fun createCombined(vararg registrars: KtQuickFixesList): KtQuickFixesList {
            return createCombined(registrars.toList())
        }
    }
}

private fun <K, V> List<Map<K, List<V>>>.merge(): Map<K, List<V>> {
    return flatMap { it.entries }
        .groupingBy { it.key }
        .aggregate<Map.Entry<K, List<V>>, K, MutableList<V>> { _, accumulator, element, _ ->
            val list = accumulator ?: mutableListOf()
            list.addAll(element.value)
            list
        }
}