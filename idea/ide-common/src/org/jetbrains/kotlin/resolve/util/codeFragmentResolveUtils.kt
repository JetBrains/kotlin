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

import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

//TODO: this code should be moved into debugger which should set correct context for its code fragment
private fun JetExpression.correctContextForExpression(): JetExpression {
    return when (this) {
        is JetProperty -> this.getDelegateExpressionOrInitializer()
        is JetFunctionLiteral -> this.getBodyExpression()?.getStatements()?.lastOrNull()
        is JetDeclarationWithBody -> this.getBodyExpression()
        is JetBlockExpression -> this.getStatements().lastOrNull()
        else -> {
            this.siblings(forward = false, withItself = false).firstIsInstanceOrNull<JetExpression>()
                    ?: this.parents.firstIsInstanceOrNull<JetExpression>()
        }
    } ?: this
}

public fun JetCodeFragment.getScopeAndDataFlowForAnalyzeFragment(
        resolveSession: KotlinCodeAnalyzer,
        resolveToElement: (JetElement) -> BindingContext
): Pair<JetScope, DataFlowInfo>? {
    val context = getContext()
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
            val correctedContext = context.correctContextForExpression()

            val contextForElement = resolveToElement(correctedContext)

            scopeForContextElement = contextForElement[BindingContext.RESOLUTION_SCOPE, correctedContext]
            dataFlowInfo = contextForElement.getDataFlowInfo(correctedContext)
        }
        is JetFile -> {
            scopeForContextElement = resolveSession.getFileScopeProvider().getFileScope(context)
            dataFlowInfo = DataFlowInfo.EMPTY
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
