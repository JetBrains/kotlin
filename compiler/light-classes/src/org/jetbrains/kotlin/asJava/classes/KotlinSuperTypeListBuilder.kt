/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceList
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

class KotlinSuperTypeListBuilder(kotlinOrigin: KtSuperTypeList?, manager: PsiManager, language: Language, role: PsiReferenceList.Role) :
    KotlinLightReferenceListBuilder(
        manager,
        language,
        role
    ) {

    private val myKotlinOrigin: KtSuperTypeList? = kotlinOrigin

    inner class KotlinSuperTypeReference(private val element: PsiJavaCodeReferenceElement) : PsiJavaCodeReferenceElement by element {

        override fun getParent() = this@KotlinSuperTypeListBuilder

        val kotlinOrigin by lazyPub {
            element.qualifiedName?.let { this@KotlinSuperTypeListBuilder.myKotlinOrigin?.findEntry(it) }
        }

        override fun delete() {
            val superTypeList = this@KotlinSuperTypeListBuilder.myKotlinOrigin ?: return
            val entry = kotlinOrigin ?: return
            superTypeList.removeEntry(entry)
        }
    }

    private val referenceElementsCache by lazyPub {
        super.getReferenceElements().map { KotlinSuperTypeReference(it) }.toTypedArray()
    }

    override fun getReferenceElements() = referenceElementsCache

    override fun add(element: PsiElement): PsiElement {

        if (element !is KotlinSuperTypeReference) throw UnsupportedOperationException("Unexpected element: ${element.getElementTextWithContext()}")

        val superTypeList = myKotlinOrigin ?: return element
        val entry = element.kotlinOrigin ?: return element

        this.addSuperTypeEntry(superTypeList, entry, element)

        return element
    }
}
