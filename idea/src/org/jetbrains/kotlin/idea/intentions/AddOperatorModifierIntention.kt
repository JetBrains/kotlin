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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.refactoring.withExpectedActuals
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.util.OperatorChecks

class AddOperatorModifierIntention : SelfTargetingRangeIntention<KtNamedFunction>(KtNamedFunction::class.java, "Add 'operator' modifier") {
    override fun applicabilityRange(element: KtNamedFunction): TextRange? {
        val nameIdentifier = element.nameIdentifier ?: return null
        val functionDescriptor = element.resolveToDescriptorIfAny() as? FunctionDescriptor ?: return null
        if (functionDescriptor.isOperator || !OperatorChecks.check(functionDescriptor).isSuccess) return null
        return nameIdentifier.textRange
    }

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        element.withExpectedActuals().forEach { it.addModifier(KtTokens.OPERATOR_KEYWORD) }
    }
}

