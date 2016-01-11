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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.createClass

import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.KotlinIntentionActionFactoryWithDelegate
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.psi.KtElement

abstract class CreateClassFromUsageFactory<E : KtElement> : KotlinIntentionActionFactoryWithDelegate<E, ClassInfo>() {
    protected abstract fun getPossibleClassKinds(element: E, diagnostic: Diagnostic): List<ClassKind>

    override fun createFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: () -> ClassInfo?
    ): List<QuickFixWithDelegateFactory> {
        val possibleClassKinds = getPossibleClassKinds(originalElementPointer.element ?: return emptyList(), diagnostic)

        val classFixes = possibleClassKinds.map { classKind ->
            QuickFixWithDelegateFactory(classKind.actionPriority) {
                val currentElement = originalElementPointer.element ?: return@QuickFixWithDelegateFactory null
                val data = quickFixDataFactory() ?: return@QuickFixWithDelegateFactory null
                CreateClassFromUsageFix.create(currentElement, data.copy(kind = classKind))
            }
        }

        return classFixes
    }
}