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

package org.jetbrains.kotlin.kdoc.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import org.jetbrains.kotlin.psi.JetElementImpl
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class KDocLink(node: ASTNode) : JetElementImpl(node) {
    public fun getLinkText(): String = getLinkTextRange().substring(getText())

    public fun getLinkTextRange(): TextRange {
        val text = getText()
        if (text.startsWith('[') && text.endsWith(']')) {
            return TextRange(1, text.length() - 1)
        }
        return TextRange(0, text.length())
    }

    /**
     * If this link is the subject of a tag, returns the tag. Otherwise, returns null.
     */
    public fun getTagIfSubject(): KDocTag? {
        val tag = getStrictParentOfType<KDocTag>()
        return if (tag != null && tag.getSubjectLink() == this) tag else null
    }

    override fun getReferences(): Array<out PsiReference>? =
        ReferenceProvidersRegistry.getReferencesFromProviders(this)
}
