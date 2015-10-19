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
import org.jetbrains.kotlin.psi.*

public class RemoveExplicitTypeIntention : JetSelfTargetingIntention<KtCallableDeclaration>(javaClass(), "Remove explicit type specification") {
    override fun isApplicableTo(element: KtCallableDeclaration, caretOffset: Int): Boolean {
        if (element.getContainingFile() is KtCodeFragment) return false
        if (element.getTypeReference() == null) return false

        val initializer = (element as? KtWithExpressionInitializer)?.getInitializer()
        if (initializer != null && initializer.getTextRange().containsOffset(caretOffset)) return false

        return when (element) {
            is KtProperty -> initializer != null
            is KtNamedFunction -> !element.hasBlockBody() && initializer != null
            is KtParameter -> element.isLoopParameter()
            else -> false
        }
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor) {
        element.setTypeReference(null)
    }
}