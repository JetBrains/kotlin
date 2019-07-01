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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiReferenceList
import com.intellij.psi.PsiReferenceList.Role
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.addSuperTypeEntry
import org.jetbrains.kotlin.asJava.classes.findEntry
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

class KtLightPsiReferenceList(
    override val clsDelegate: PsiReferenceList,
    private val owner: KtLightClass
) : KtLightElement<KtSuperTypeList, PsiReferenceList>, PsiReferenceList by clsDelegate {
    inner class KtLightSuperTypeReference(
        override val clsDelegate: PsiJavaCodeReferenceElement
    ) : KtLightElement<KtSuperTypeListEntry, PsiJavaCodeReferenceElement>, PsiJavaCodeReferenceElement by clsDelegate {

        override val kotlinOrigin by lazyPub {
            clsDelegate.qualifiedName?.let { this@KtLightPsiReferenceList.kotlinOrigin?.findEntry(it) }
        }

        override fun getParent() = this@KtLightPsiReferenceList

        override fun delete() {
            val superTypeList = this@KtLightPsiReferenceList.kotlinOrigin ?: return
            val entry = kotlinOrigin ?: return
            superTypeList.removeEntry(entry)
        }

        override fun getTextRange(): TextRange? = kotlinOrigin?.typeReference?.textRange ?: TextRange.EMPTY_RANGE
    }

    override val kotlinOrigin: KtSuperTypeList?
        get() = owner.kotlinOrigin?.getSuperTypeList()

    private val _referenceElements by lazyPub {
        clsDelegate.referenceElements.map { KtLightSuperTypeReference(it) }.toTypedArray()
    }

    override fun getParent() = owner

    override fun getReferenceElements() = _referenceElements

    override fun add(element: PsiElement): PsiElement? {
        if (element !is KtLightSuperTypeReference) throw UnsupportedOperationException("Unexpected element: ${element.getElementTextWithContext()}")

        val superTypeList = kotlinOrigin ?: return element
        val entry = element.kotlinOrigin ?: return element

        this.addSuperTypeEntry(superTypeList, entry, element)

        return element
    }
}
