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
import org.jetbrains.kotlin.idea.quickfix.NullQuickFix
import org.jetbrains.kotlin.idea.quickfix.QuickFixWithDelegateFactory
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.CreateFromUsageFactory
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetUserType

abstract class CreateClassFromUsageFactory<E : JetElement>(
        val createPackageIsAvailable: Boolean = false
) : CreateFromUsageFactory<E, ClassInfo>() {
    protected abstract fun getPossibleClassKinds(element: E, diagnostic: Diagnostic): List<ClassKind>

    override fun createQuickFixes(
            originalElementPointer: SmartPsiElementPointer<E>,
            diagnostic: Diagnostic,
            quickFixDataFactory: (SmartPsiElementPointer<E>) -> ClassInfo?
    ): List<QuickFixWithDelegateFactory> {
        val originalElement = originalElementPointer.element ?: return emptyList()

        val classFixes = getPossibleClassKinds(originalElement, diagnostic).map { classKind ->
            QuickFixWithDelegateFactory {
                val currentElement = originalElementPointer.element
                val data = quickFixDataFactory(originalElementPointer)
                if (currentElement != null && data != null) {
                    CreateClassFromUsageFix(originalElement, data.copy(kind = classKind))
                } else NullQuickFix
            }
        }

        if (!createPackageIsAvailable) return classFixes
        val refExpr = (originalElement as? JetUserType)?.referenceExpression ?: return classFixes
        val packageFix = QuickFixWithDelegateFactory {
            quickFixDataFactory(originalElementPointer)?.let {
                refExpr.getCreatePackageFixIfApplicable(it.targetParent)
            } ?: NullQuickFix
        }

        return classFixes + packageFix
    }
}