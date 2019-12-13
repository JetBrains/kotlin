/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory

class QuickFixes {
    private val factories: Multimap<DiagnosticFactory<*>, KotlinIntentionActionsFactory> =
        HashMultimap.create<DiagnosticFactory<*>, KotlinIntentionActionsFactory>()
    private val actions: Multimap<DiagnosticFactory<*>, IntentionAction> = HashMultimap.create<DiagnosticFactory<*>, IntentionAction>()

    init {
        @Suppress("DEPRECATION")
        Extensions.getExtensions(QuickFixContributor.EP_NAME).forEach { it.registerQuickFixes(this) }
    }

    fun register(diagnosticFactory: DiagnosticFactory<*>, vararg factory: KotlinIntentionActionsFactory) {
        factories.putAll(diagnosticFactory, factory.toList())
    }

    fun register(diagnosticFactory: DiagnosticFactory<*>, vararg action: IntentionAction) {
        actions.putAll(diagnosticFactory, action.toList())
    }

    fun getActionFactories(diagnosticFactory: DiagnosticFactory<*>): Collection<KotlinIntentionActionsFactory> {
        return factories.get(diagnosticFactory)
    }

    fun getActions(diagnosticFactory: DiagnosticFactory<*>): Collection<IntentionAction> {
        return actions.get(diagnosticFactory)
    }

    fun getDiagnostics(factory: KotlinIntentionActionsFactory): Collection<DiagnosticFactory<*>> {
        return factories.keySet().filter { factory in factories.get(it) }
    }

    companion object {
        fun getInstance(): QuickFixes = ServiceManager.getService(QuickFixes::class.java)
    }
}

interface QuickFixContributor {
    companion object {
        val EP_NAME: ExtensionPointName<QuickFixContributor> = ExtensionPointName.create("org.jetbrains.kotlin.quickFixContributor")
    }

    fun registerQuickFixes(quickFixes: QuickFixes)
}