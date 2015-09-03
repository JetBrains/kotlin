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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.util.Ref
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.JetIntentionActionsFactory
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.singletonOrEmptyList

public abstract class CreateFromUsageFactory<E : JetElement, D : Any> : JetIntentionActionsFactory() {
    protected abstract fun getElementOfInterest(diagnostic: Diagnostic): E?

    protected open fun createQuickFix(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: (SmartPsiElementPointer<E>) -> D?
    ): QuickFixWithDelegateFactory? = null

    protected open fun createQuickFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: (SmartPsiElementPointer<E>) -> D?
    ): List<QuickFixWithDelegateFactory> = createQuickFix(originalElementPointer, diagnostic, quickFixDataFactory).singletonOrEmptyList()

    protected abstract fun createQuickFixData(element: E, diagnostic: Diagnostic): D?

    override final fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction>? {
        val diagnosticMessage = DefaultErrorMessages.render(diagnostic)
        val diagnosticElementPointer = diagnostic.psiElement.createSmartPointer()
        val originalElement = getElementOfInterest(diagnostic) ?: return null
        val originalElementPointer = originalElement.createSmartPointer()

        val file = originalElement.containingFile
        val project = file.project

        // Cache data so that it can be shared between quick fixes bound to the same element & diagnostic
        // Cache null values
        var cachedData: Ref<D>? = null
        val actions: List<QuickFixWithDelegateFactory>
        try {
            actions = createQuickFixes(originalElementPointer, diagnostic) factory@ {
                val element = it.element ?: return@factory null
                val diagnosticElement = diagnosticElementPointer.element ?: return@factory null
                if (!diagnosticElement.isValid || !element.isValid) return@factory null

                val currentDiagnostic =
                        element.analyze(BodyResolveMode.PARTIAL)
                                .diagnostics
                                .forElement(diagnosticElement)
                                .firstOrNull { DefaultErrorMessages.render(it) == diagnosticMessage } ?: return@factory null
                if (cachedData == null) {
                    cachedData = Ref(createQuickFixData(element, currentDiagnostic))
                }
                cachedData!!.get()
            }.filter { it.isAvailable(project, null, file) }
        }
        finally {
            cachedData = null // Do not keep cache after all actions are initialized
        }

        return actions
    }
}
