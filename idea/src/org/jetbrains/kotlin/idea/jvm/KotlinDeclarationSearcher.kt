/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.jvm

import com.intellij.lang.jvm.JvmElement
import com.intellij.lang.jvm.source.JvmDeclarationSearcher
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.toPsiParameters
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinDeclarationSearcher : JvmDeclarationSearcher {
    override fun findDeclarations(declaringElement: PsiElement): Collection<JvmElement> =
        getLightElements(declaringElement).mapNotNull { it as? JvmElement }

    private fun getLightElements(declaringElement: PsiElement): List<PsiNamedElement> = when (declaringElement) {
        is KtClass -> {
            val primaryConstructor = declaringElement.primaryConstructor
            if (primaryConstructor?.hasConstructorKeyword() != false)
                declaringElement.toLightElements()
            else
                declaringElement.toLightElements() + primaryConstructor.toLightElements()
        }
        is KtParameter -> {
            SmartList<PsiNamedElement>().apply {
                addAll(declaringElement.toPsiParameters())
                addIfNotNull(LightClassUtil.getLightClassBackingField(declaringElement))
                addAll(LightClassUtil.getLightClassPropertyMethods(declaringElement))
            }
        }
        is KtElement -> declaringElement.toLightElements()
        else -> emptyList()
    }
}