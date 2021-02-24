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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.types.typeUtil.isUnit

class KotlinFindUsagesProvider : KotlinFindUsagesProviderBase() {

    override fun getDescriptiveName(element: PsiElement): String {

        if (element !is KtFunction) return super.getDescriptiveName(element)

        val name = element.name ?: ""
        val descriptor = element.unsafeResolveToDescriptor() as FunctionDescriptor
        val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS
        val paramsDescription =
            descriptor.valueParameters.joinToString(prefix = "(", postfix = ")") { renderer.renderType(it.type) }
        val returnType = descriptor.returnType
        val returnTypeDescription = if (returnType != null && !returnType.isUnit()) renderer.renderType(returnType) else null
        val funDescription = "$name$paramsDescription" + (returnTypeDescription?.let { ": $it" } ?: "")
        return funDescription + (element.containerDescription?.let { " of $it" } ?: "")
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
