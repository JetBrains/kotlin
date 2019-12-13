/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.ResolvableCollisionUsageInfo
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.dropDefaultValue
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.source.getPsi

fun checkConflictsAndReplaceUsageInfos(
    element: PsiElement,
    allRenames: Map<out PsiElement?, String>,
    result: MutableList<UsageInfo>
) {
    element.getOverriddenFunctionWithDefaultValues(allRenames)?.let { baseFunction ->
        result += LostDefaultValuesInOverridingFunctionUsageInfo(element.unwrapped as KtNamedFunction, baseFunction)
    }

    val usageIterator = result.listIterator()
    while (usageIterator.hasNext()) {
        val usageInfo = usageIterator.next() as? MoveRenameUsageInfo ?: continue
        val ref = usageInfo.reference as? AbstractKtReference<*> ?: continue
        if (!ref.canRename()) {
            val refElement = usageInfo.element ?: continue
            val referencedElement = usageInfo.referencedElement ?: continue
            usageIterator.set(UnresolvableConventionViolationUsageInfo(refElement, referencedElement))
        }
    }
}

private fun PsiElement.getOverriddenFunctionWithDefaultValues(allRenames: Map<out PsiElement?, String>): KtNamedFunction? {
    val elementsToRename = allRenames.keys.mapNotNull { it?.unwrapped }
    val function = unwrapped as? KtNamedFunction ?: return null
    val descriptor = function.unsafeResolveToDescriptor() as FunctionDescriptor
    return descriptor.overriddenDescriptors
        .mapNotNull { it.source.getPsi() as? KtNamedFunction }
        .firstOrNull { it !in elementsToRename && it.valueParameters.any { it.hasDefaultValue() } }
}

class UnresolvableConventionViolationUsageInfo(
    element: PsiElement,
    referencedElement: PsiElement
) : UnresolvableCollisionUsageInfo(element, referencedElement) {
    override fun getDescription(): String = KotlinRefactoringBundle.message("naming.convention.will.be.violated.after.rename")
}

class LostDefaultValuesInOverridingFunctionUsageInfo(
    function: KtNamedFunction,
    private val baseFunction: KtNamedFunction
) : ResolvableCollisionUsageInfo(function, function) {
    fun apply() {
        val function = element as? KtNamedFunction ?: return
        for ((subParam, superParam) in (function.valueParameters zip baseFunction.valueParameters)) {
            val defaultValue = superParam.defaultValue ?: continue
            subParam.dropDefaultValue()
            subParam.addRange(superParam.equalsToken, defaultValue)
        }
    }
}

inline fun <reified T : PsiElement> PsiFile.findElementForRename(offset: Int): T? {
    return PsiTreeUtil.findElementOfClassAtOffset(this, offset, T::class.java, false)
        ?: PsiTreeUtil.findElementOfClassAtOffset(this, (offset - 1).coerceAtLeast(0), T::class.java, false)
}