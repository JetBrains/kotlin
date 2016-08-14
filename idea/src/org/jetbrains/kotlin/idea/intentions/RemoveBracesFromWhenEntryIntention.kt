/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtWhenEntry

class RemoveBracesFromWhenEntryIntention : SelfTargetingIntention<KtWhenEntry>(KtWhenEntry::class.java, "Remove braces from 'when' entry") {

    override fun isApplicableTo(element: KtWhenEntry, caretOffset: Int): Boolean {
        val block = element.expression as? KtBlockExpression ?: return false
        val singleExpression = block.statements.singleOrNull() ?: return false
        return singleExpression !is KtNamedDeclaration
    }

    override fun applyTo(element: KtWhenEntry, editor: Editor?) {
        val block = element.expression as KtBlockExpression
        block.replace(block.statements.single())
    }
}
