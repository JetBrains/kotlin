/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages

import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.ShortenReferences.Options
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinChangeInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.KotlinParameterInfo
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier

// Explicit reference to function parameter or outer this
abstract class KotlinExplicitReferenceUsage<T : KtElement>(element: T) : KotlinUsageInfo<T>(element) {
    abstract fun getReplacementText(changeInfo: KotlinChangeInfo): String

    protected open fun processReplacedElement(element: KtElement) {

    }

    override fun processUsage(changeInfo: KotlinChangeInfo, element: T, allUsages: Array<out UsageInfo>): Boolean {
        val newElement = KtPsiFactory(element.project).createExpression(getReplacementText(changeInfo))
        val elementToReplace = (element.parent as? KtThisExpression) ?: element
        processReplacedElement(elementToReplace.replace(newElement) as KtElement)
        return false
    }
}

class KotlinParameterUsage(
    element: KtElement,
    private val parameterInfo: KotlinParameterInfo,
    val containingCallable: KotlinCallableDefinitionUsage<*>
) : KotlinExplicitReferenceUsage<KtElement>(element) {
    override fun processReplacedElement(element: KtElement) {
        val qualifiedExpression = element.parent as? KtQualifiedExpression
        val elementToShorten = if (qualifiedExpression?.receiverExpression == element) qualifiedExpression else element
        elementToShorten.addToShorteningWaitSet(Options(removeThis = true, removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: KotlinChangeInfo): String {
        if (changeInfo.receiverParameterInfo != parameterInfo) return parameterInfo.getInheritedName(containingCallable)

        val newName = changeInfo.newName
        if (newName.isIdentifier()) return "this@$newName"

        return "this"
    }
}

class KotlinNonQualifiedOuterThisUsage(
    element: KtThisExpression,
    val targetDescriptor: DeclarationDescriptor
) : KotlinExplicitReferenceUsage<KtThisExpression>(element) {
    override fun processReplacedElement(element: KtElement) {
        element.addToShorteningWaitSet(Options(removeThisLabels = true))
    }

    override fun getReplacementText(changeInfo: KotlinChangeInfo): String = "this@${targetDescriptor.name.asString()}"
}