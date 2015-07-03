/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory

public class QuickFixes {
    private val factories: Multimap<DiagnosticFactory<*>, JetIntentionActionsFactory> = HashMultimap.create<DiagnosticFactory<*>, JetIntentionActionsFactory>()
    private val actions: Multimap<DiagnosticFactory<*>, IntentionAction> = HashMultimap.create<DiagnosticFactory<*>, IntentionAction>()

    init {
        Extensions.getExtensions(QuickFixContributor.EP_NAME).forEach { it.registerQuickFixes(this) }
    }

    public fun register(diagnosticFactory: DiagnosticFactory<*>, vararg factory: JetIntentionActionsFactory) {
        factories.putAll(diagnosticFactory, factory.toList())
    }

    public fun register(diagnosticFactory: DiagnosticFactory<*>, vararg action: IntentionAction) {
        actions.putAll(diagnosticFactory, action.toList())
    }

    public fun getActionFactories(diagnosticFactory: DiagnosticFactory<*>): Collection<JetIntentionActionsFactory> {
        return factories.get(diagnosticFactory)
    }

    public fun getActions(diagnosticFactory: DiagnosticFactory<*>): Collection<IntentionAction> {
        return actions.get(diagnosticFactory)
    }

    companion object {
        public fun getInstance(): QuickFixes = ServiceManager.getService(javaClass<QuickFixes>())
    }
}

public interface QuickFixContributor {
    companion object {
        val EP_NAME: ExtensionPointName<QuickFixContributor> = ExtensionPointName.create("org.jetbrains.kotlin.quickFixContributor")
    }

    fun registerQuickFixes(quickFixes: QuickFixes)
}