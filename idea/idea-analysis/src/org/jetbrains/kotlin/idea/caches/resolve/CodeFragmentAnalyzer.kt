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

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.addImportScope
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import javax.inject.Inject

public class CodeFragmentAnalyzer(
        private val resolveSession: ResolveSession,
        private val qualifierResolver: QualifiedExpressionResolver,
        private val expressionTypingServices: ExpressionTypingServices
) {

    // component dependency cycle
    public var resolveElementCache: ResolveElementCache? = null
        @Inject set

    public fun analyzeCodeFragment(codeFragment: JetCodeFragment, trace: BindingTrace, bodyResolveMode: BodyResolveMode) {
        val codeFragmentExpression = codeFragment.getContentElement()
        if (codeFragmentExpression !is JetExpression) return

        val (scopeForContextElement, dataFlowInfo) = getScopeAndDataFlowForAnalyzeFragment(codeFragment) {
            resolveElementCache!!.resolveToElement(it, bodyResolveMode)
        } ?: return

        expressionTypingServices.getTypeInfo(
                scopeForContextElement,
                codeFragmentExpression,
                TypeUtils.NO_EXPECTED_TYPE,
                dataFlowInfo,
                trace,
                false
        )
    }

    //TODO: this code should be moved into debugger which should set correct context for its code fragment
    private fun JetExpression.correctContextForExpression(): JetExpression {
        return when (this) {
                   is JetProperty -> this.getDelegateExpressionOrInitializer()
                   is JetFunctionLiteral -> this.getBodyExpression()?.getStatements()?.lastOrNull()
                   is JetDeclarationWithBody -> this.getBodyExpression()
                   is JetBlockExpression -> this.getStatements().lastOrNull()
                   else -> {
                       val previousSibling = this.siblings(forward = false, withItself = false).firstIsInstanceOrNull<JetExpression>()
                       if (previousSibling != null) return previousSibling
                       for (parent in this.parents) {
                           if (parent is JetWhenEntry || parent is JetIfExpression || parent is JetBlockExpression) return this
                           if (parent is JetExpression) return parent
                       }
                       null
                   }
               } ?: this
    }

    private fun getScopeAndDataFlowForAnalyzeFragment(
            codeFragment: JetCodeFragment,
            resolveToElement: (JetElement) -> BindingContext
    ): Pair<LexicalScope, DataFlowInfo>? {
        val context = codeFragment.getContext()
        if (context !is JetExpression) return null

        val scopeForContextElement: LexicalScope?
        val dataFlowInfo: DataFlowInfo

        when (context) {
            is JetPrimaryConstructor -> {
                val descriptor = resolveSession.getClassDescriptor(context.getContainingClassOrObject(), NoLookupLocation.FROM_IDE) as ClassDescriptorWithResolutionScopes

                scopeForContextElement = descriptor.getScopeForInitializerResolution()
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is JetSecondaryConstructor -> {
                val correctedContext = context.getDelegationCall().calleeExpression!!

                val contextForElement = resolveToElement(correctedContext)

                scopeForContextElement = contextForElement[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is JetClassOrObject -> {
                val descriptor = resolveSession.getClassDescriptor(context, NoLookupLocation.FROM_IDE) as ClassDescriptorWithResolutionScopes

                scopeForContextElement = descriptor.getScopeForMemberDeclarationResolution()
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is JetExpression -> {
                val correctedContext = context.correctContextForExpression()

                val contextForElement = resolveToElement(correctedContext)

                scopeForContextElement = contextForElement[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = contextForElement.getDataFlowInfo(correctedContext)
            }
            is JetFile -> {
                scopeForContextElement = resolveSession.getFileScopeProvider().getFileScope(context)
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            else -> return null
        }

        if (scopeForContextElement == null) return null

        val importList = codeFragment.importsAsImportList()
        if (importList == null || importList.imports.isEmpty()) {
            return  scopeForContextElement to dataFlowInfo
        }

        val importScopes = importList.imports.map {
            qualifierResolver.processImportReference(it, resolveSession.moduleDescriptor, resolveSession.trace, null)
        }.filterNotNull()

        val chainedScope = ChainedScope(
                scopeForContextElement.ownerDescriptor,
                "Scope for resolve code fragment",
                *importScopes.toTypedArray())

        return scopeForContextElement.addImportScope(chainedScope) to dataFlowInfo
    }
}
