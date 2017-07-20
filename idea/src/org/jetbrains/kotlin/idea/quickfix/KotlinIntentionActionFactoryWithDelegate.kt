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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.util.Ref
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

abstract class KotlinSingleIntentionActionFactoryWithDelegate<E : KtElement, D : Any>(
        private val actionPriority: IntentionActionPriority = IntentionActionPriority.NORMAL
) : KotlinIntentionActionFactoryWithDelegate<E, D>() {

    protected abstract fun createFix(originalElement: E, data: D): IntentionAction?

    override final fun createFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: () -> D?
    ): List<QuickFixWithDelegateFactory> {
        return QuickFixWithDelegateFactory(actionPriority) factory@ {
            val originalElement = originalElementPointer.element ?: return@factory null
            val data = quickFixDataFactory() ?: return@factory null
            createFix(originalElement, data)
        }.let(::listOf)
    }
}

abstract class KotlinIntentionActionFactoryWithDelegate<E : KtElement, D : Any> : KotlinIntentionActionsFactory() {
    abstract fun getElementOfInterest(diagnostic: Diagnostic): E?

    protected abstract fun createFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: () -> D?
    ): List<QuickFixWithDelegateFactory>

    abstract fun extractFixData(element: E, diagnostic: Diagnostic): D?

    override final fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val diagnosticMessage = DefaultErrorMessages.render(diagnostic)
        val diagnosticElementPointer = diagnostic.psiElement.createSmartPointer()
        val originalElement = getElementOfInterest(diagnostic) ?: return emptyList()
        val originalElementPointer = originalElement.createSmartPointer()

        val file = originalElement.containingFile
        val project = file.project

        // Cache data so that it can be shared between quick fixes bound to the same element & diagnostic
        // Cache null values
        val cachedData: Ref<D> = Ref.create(extractFixData(originalElement, diagnostic))

        return try {
            createFixes(originalElementPointer, diagnostic) factory@ {
                val element = originalElementPointer.element ?: return@factory null
                val diagnosticElement = diagnosticElementPointer.element ?: return@factory null
                if (!diagnosticElement.isValid || !element.isValid) return@factory null

                val currentDiagnostic =
                        element.analyze(BodyResolveMode.PARTIAL_WITH_DIAGNOSTICS)
                                .diagnostics
                                .forElement(diagnosticElement)
                                .firstOrNull { DefaultErrorMessages.render(it) == diagnosticMessage } ?: return@factory null

                cachedData.get() ?: extractFixData(element, currentDiagnostic)
            }.filter { it.isAvailable(project, null, file) }
        }
        finally {
            cachedData.set(null) // Do not keep cache after all actions are initialized
        }
    }
}
