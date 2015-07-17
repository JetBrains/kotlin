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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import com.intellij.refactoring.changeSignature.CallerUsageInfo
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.DeferredJavaMethodKotlinCallerUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.JavaMethodKotlinUsageWithDelegate
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinCallerUsage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

private fun JetNamedDeclaration.getDeclarationBody(): JetElement? {
    return when {
        this is JetClassOrObject -> getDelegationSpecifierList()
        this is JetPrimaryConstructor -> getContainingClassOrObject().getDelegationSpecifierList()
        this is JetSecondaryConstructor -> getDelegationCall()
        this is JetNamedFunction -> getBodyExpression()
        else -> null
    }
}

public fun PsiElement.isCaller(allUsages: Array<out UsageInfo>): Boolean {
    val elementToSearch = (this as? JetClass)?.getPrimaryConstructor() ?: this
    return allUsages
            .asSequence()
            .filter {
                val usage = (it as? JavaMethodKotlinUsageWithDelegate<*>)?.delegateUsage ?: it
                usage is KotlinCallerUsage
                || usage is DeferredJavaMethodKotlinCallerUsage
                || usage is CallerUsageInfo
                || (usage is OverriderUsageInfo && !usage.isOriginalOverrider())
            }
            .any { it.getElement() == elementToSearch }
}

public fun JetElement.isInsideOfCallerBody(allUsages: Array<out UsageInfo>): Boolean {
    val container = parentsWithSelf.firstOrNull {
        it is JetNamedFunction || it is JetConstructor<*> || it is JetClassOrObject
    } as? JetNamedDeclaration ?: return false
    val body = container.getDeclarationBody() ?: return false
    return body.getTextRange().contains(getTextRange()) && container.isCaller(allUsages)
}