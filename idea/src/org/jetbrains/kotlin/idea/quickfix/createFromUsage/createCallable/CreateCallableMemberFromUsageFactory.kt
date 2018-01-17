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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createCallable

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.IntentionActionPriority
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFixBase
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.PropertyInfo
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.createVariable.CreateParameterFromUsageFix
import org.jetbrains.kotlin.psi.KtElement
import java.util.*

abstract class CreateCallableMemberFromUsageFactory<E : KtElement>(
        private val extensionsSupported: Boolean = true
) : KotlinIntentionActionFactoryWithDelegate<E, List<CallableInfo>>() {
    private fun newCallableQuickFix(
            originalElementPointer: SmartPsiElementPointer<E>,
            priority: IntentionActionPriority,
            quickFixDataFactory: () -> List<CallableInfo>?,
            quickFixFactory: (E, List<CallableInfo>) -> CreateFromUsageFixBase<E>?
    ): QuickFixWithDelegateFactory {
        return QuickFixWithDelegateFactory(priority) {
            val data = quickFixDataFactory().orEmpty()
            val originalElement = originalElementPointer.element
            if (data.isNotEmpty() && originalElement != null) quickFixFactory(originalElement, data) else null
        }
    }

    protected open fun createCallableInfo(element: E, diagnostic: Diagnostic): CallableInfo? = null

    override fun extractFixData(element: E, diagnostic: Diagnostic): List<CallableInfo>
            = listOfNotNull(createCallableInfo(element, diagnostic))

    override fun createFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: () -> List<CallableInfo>?
    ): List<QuickFixWithDelegateFactory> {
        val fixes = ArrayList<QuickFixWithDelegateFactory>(3)

        newCallableQuickFix(originalElementPointer, IntentionActionPriority.NORMAL, quickFixDataFactory) { element, data ->
            CreateCallableFromUsageFix(element, data)
        }.let { fixes.add(it) }

        newCallableQuickFix(originalElementPointer, IntentionActionPriority.NORMAL, quickFixDataFactory) f@ { element, data ->
            (data.singleOrNull() as? PropertyInfo)?.let {
                CreateParameterFromUsageFix.createFixForPrimaryConstructorPropertyParameter(element, it)
            }
        }.let { fixes.add(it) }

        if (extensionsSupported) {
            newCallableQuickFix(originalElementPointer, IntentionActionPriority.LOW, quickFixDataFactory) { element, data ->
                if (data.any { it.isAbstract }) return@newCallableQuickFix null
                CreateExtensionCallableFromUsageFix(element, data)
            }.let { fixes.add(it) }
        }

        return fixes
    }
}