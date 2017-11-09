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

import com.intellij.lang.Language
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag

class KDocImpl(buffer: CharSequence?) : LazyParseablePsiElement(KDocTokens.KDOC, buffer), KDoc {

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun toString(): String = node.elementType.toString()

    override fun getTokenType(): IElementType = KtTokens.DOC_COMMENT

    override fun getOwner(): KtDeclaration? = getParentOfType<KtDeclaration>(true)

    override fun getDefaultSection(): KDocSection = getChildOfType<KDocSection>()!!

    override fun findSectionByName(name: String): KDocSection? =
        getChildrenOfType<KDocSection>().firstOrNull { it.name == name }

    override fun findSectionByTag(tag: KDocKnownTag): KDocSection? =
        findSectionByName(tag.name.toLowerCase())

    override fun findSectionByTag(tag: KDocKnownTag, subjectName: String): KDocSection? =
        getChildrenOfType<KDocSection>().firstOrNull {
            it.name == tag.name.toLowerCase() && it.getSubjectName() == subjectName
        }
}
