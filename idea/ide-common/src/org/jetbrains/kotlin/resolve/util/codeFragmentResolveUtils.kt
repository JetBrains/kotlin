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

package org.jetbrains.kotlin.resolve.util

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope

//TODO: this code should be moved into debugger which should set correct context for its code fragment
private fun correctContext(oldContext: PsiElement?): PsiElement? {
    if (oldContext is JetBlockExpression) {
        return oldContext.getStatements().lastOrNull() ?: oldContext
    }
    return oldContext
}

public fun JetCodeFragment.getScopeAndDataFlowForAnalyzeFragment(
        resolveSession: KotlinCodeAnalyzer,
        resolveToElement: (JetElement) -> BindingContext
): Pair<JetScope, DataFlowInfo>? {
    val context = correctContext(getContext())
    if (context !is JetExpression) return null

    val scopeForContextElement: JetScope?
    val dataFlowInfo: DataFlowInfo

    when (context) {
        is JetClassOrObject -> {
            val descriptor = resolveSession.getClassDescriptor(context) as ClassDescriptorWithResolutionScopes

            scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution()
            dataFlowInfo = DataFlowInfo.EMPTY
        }
        is JetExpression -> {
            val contextForElement = resolveToElement(context)

            scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, context]
            dataFlowInfo = contextForElement.getDataFlowInfo(context)
        }
        else -> return null
    }

    if (scopeForContextElement == null) return null

    val codeFragmentScope = resolveSession.getFileScopeProvider().getFileScope(this)
    val chainedScope = ChainedScope(
            scopeForContextElement.getContainingDeclaration(),
            "Scope for resolve code fragment",
            scopeForContextElement, codeFragmentScope)

    return chainedScope to dataFlowInfo
}
