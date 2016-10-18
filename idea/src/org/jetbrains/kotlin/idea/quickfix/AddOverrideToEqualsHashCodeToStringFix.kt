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

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens.OVERRIDE_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.PUBLIC_KEYWORD
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

object AddOverrideToEqualsHashCodeToStringActionFactory : KotlinSingleIntentionActionFactory() {
    private val NAME = "Add 'override' to equals, hashCode, toString in project"

    private fun isEqualsHashCodeOrToString(element: KtNamedFunction): Boolean {
        return when (element.name) {
            "equals" -> {
                val paramTypeRef = element.valueParameters.singleOrNull()?.typeReference ?: return false
                val paramType = paramTypeRef.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, paramTypeRef] ?: return false
                KotlinBuiltIns.isNullableAny(paramType)
            }

            "hashCode", "toString" -> element.valueParameters.isEmpty()

            else -> false
        }
    }

    private fun KtNamedFunction.doInvoke() {
        if (!FileModificationService.getInstance().preparePsiElementForWrite(this)) return
        addModifier(OVERRIDE_KEYWORD)
        removeModifier(PUBLIC_KEYWORD)
    }

    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        return WholeProjectForEachElementOfTypeFix.createByPredicate<KtNamedFunction>(
                predicate = { isEqualsHashCodeOrToString(it) },
                taskProcessor = { it.doInvoke() },
                name = NAME
        )
    }
}