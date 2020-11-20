/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.java.JavaFindUsagesProvider
import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.KotlinBundleIndependent
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter

open class KotlinFindUsagesProviderBase : FindUsagesProvider {
    private val javaProvider by lazy { JavaFindUsagesProvider() }

    override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
        psiElement is KtNamedDeclaration

    override fun getWordsScanner(): WordsScanner? = KotlinWordsScanner()

    override fun getHelpId(psiElement: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is KtNamedFunction -> KotlinBundleIndependent.message("find.usages.function")
            is KtClass -> KotlinBundleIndependent.message("find.usages.class")
            is KtParameter -> KotlinBundleIndependent.message("find.usages.parameter")
            is KtProperty -> if (element.isLocal)
                KotlinBundleIndependent.message("find.usages.variable")
            else
                KotlinBundleIndependent.message("find.usages.property")
            is KtDestructuringDeclarationEntry -> KotlinBundleIndependent.message("find.usages.variable")
            is KtTypeParameter -> KotlinBundleIndependent.message("find.usages.type.parameter")
            is KtSecondaryConstructor -> KotlinBundleIndependent.message("find.usages.constructor")
            is KtObjectDeclaration -> KotlinBundleIndependent.message("find.usages.object")
            else -> ""
        }
    }

    protected val KtDeclaration.containerDescription: String?
        get() {
            containingClassOrObject?.let { return getDescriptiveName(it) }
            (parent as? KtFile)?.parent?.let { return getDescriptiveName(it) }
            return null
        }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PsiDirectory, is PsiPackage, is PsiFile -> javaProvider.getDescriptiveName(element)
            is KtClassOrObject -> {
                if (element is KtObjectDeclaration && element.isObjectLiteral()) return "<unnamed>"
                element.fqName?.asString() ?: element.name ?: "<unnamed>"
            }
            is KtProperty -> (element.name ?: "") + (element.containerDescription?.let { " of $it" } ?: "")
            is KtFunction -> {
                //TODO: Correct FIR implementation
                return element.name?.let { "$it(...)" } ?: ""
            }
            is KtLabeledExpression -> element.getLabelName() ?: ""
            is KtImportAlias -> element.name ?: ""
            is KtLightElement<*, *> -> element.kotlinOrigin?.let { getDescriptiveName(it) } ?: ""
            is KtParameter -> {
                if (element.isPropertyParameter()) {
                    (element.name ?: "") + (element.containerDescription?.let { " of $it" } ?: "")
                } else {
                    element.name ?: ""
                }
            }
            is PsiNamedElement -> element.name ?: ""
            else -> ""
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
