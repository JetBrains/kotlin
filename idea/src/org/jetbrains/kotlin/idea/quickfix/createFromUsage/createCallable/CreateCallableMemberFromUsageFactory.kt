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
import org.jetbrains.kotlin.idea.quickfix.NullQuickFix
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.CallableInfo
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

public abstract class CreateCallableMemberFromUsageFactory<E : JetElement>(
        val extensionsSupported: Boolean = true
) : CreateFromUsageFactory<E, List<CallableInfo>>() {
    private fun newCallableQuickFix(
            originalElementPointer: SmartPsiElementPointer<E>,
            quickFixDataFactory: (SmartPsiElementPointer<E>) -> List<CallableInfo>?,
            quickFixFactory: (E, List<CallableInfo>) -> CreateCallableFromUsageFixBase<E>
    ): QuickFixWithDelegateFactory {
        return QuickFixWithDelegateFactory {
            val data = quickFixDataFactory(originalElementPointer).orEmpty()
            val originalElement = originalElementPointer.element
            if (data.isNotEmpty() && originalElement != null) quickFixFactory(originalElement, data) else NullQuickFix
        }
    }

    protected open fun createCallableInfo(element: E, diagnostic: Diagnostic): CallableInfo? = null

    override fun createQuickFixData(element: E, diagnostic: Diagnostic): List<CallableInfo>
            = createCallableInfo(element, diagnostic).singletonOrEmptyList()

    override fun createQuickFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: (SmartPsiElementPointer<E>) -> List<CallableInfo>?
    ): List<QuickFixWithDelegateFactory> {
        val memberFix = newCallableQuickFix(originalElementPointer, quickFixDataFactory) { element, data ->
            CreateCallableFromUsageFix(element, data)
        }
        if (!extensionsSupported) return listOf(memberFix)

        val extensionFix = newCallableQuickFix(originalElementPointer, quickFixDataFactory) { element, data ->
            CreateExtensionCallableFromUsageFix(element, data)
        }
        return listOf(memberFix, extensionFix)
    }
}