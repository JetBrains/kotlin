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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.addImportingScopes
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor
import javax.inject.Inject

class CodeFragmentAnalyzer(
    private val resolveSession: ResolveSession,
    private val qualifierResolver: QualifiedExpressionResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val typeResolver: TypeResolver
) {
    @set:Inject // component dependency cycle
    lateinit var resolveElementCache: ResolveElementCache

    fun analyzeCodeFragment(codeFragment: KtCodeFragment, trace: BindingTrace, bodyResolveMode: BodyResolveMode): BindingTrace {
        val codeFragmentElement = codeFragment.getContentElement()

        val (scopeForContextElement, dataFlowInfo, newBindingContext) = doAnalyzeCoreFragment(codeFragment) {
            resolveElementCache!!.resolveToElements(listOf(it), bodyResolveMode)
        } ?: return trace

        val newBindingTrace = DelegatingBindingTrace(newBindingContext, "For code fragment analysis")

        when (codeFragmentElement) {
            is KtExpression -> {
                PreliminaryDeclarationVisitor.createForExpression(
                    codeFragmentElement, newBindingTrace,
                    expressionTypingServices.languageVersionSettings
                )
                expressionTypingServices.getTypeInfo(
                    scopeForContextElement,
                    codeFragmentElement,
                    TypeUtils.NO_EXPECTED_TYPE,
                    dataFlowInfo,
                    newBindingTrace,
                    false
                )
            }

            is KtTypeReference -> {
                val context = TypeResolutionContext(
                    scopeForContextElement,
                    newBindingTrace,
                    true,
                    true,
                    codeFragment.suppressDiagnosticsInDebugMode()
                ).noBareTypes()
                typeResolver.resolvePossiblyBareType(context, codeFragmentElement)
            }
        }

        return newBindingTrace
    }

    //TODO: this code should be moved into debugger which should set correct context for its code fragment
    private fun KtElement.correctContextForElement(): KtElement {
        return when (this) {
            is KtProperty -> this.delegateExpressionOrInitializer
            is KtFunctionLiteral -> this.bodyExpression?.statements?.lastOrNull()
            is KtDeclarationWithBody -> this.bodyExpression
            is KtBlockExpression -> this.statements.lastOrNull()
            else -> null
        } ?: this
    }

    private fun doAnalyzeCoreFragment(
        codeFragment: KtCodeFragment,
        resolveToElement: (KtElement) -> BindingContext
    ): Triple<LexicalScope, DataFlowInfo, BindingContext>? {
        val context = codeFragment.context

        val scopeForContextElement: LexicalScope?
        val dataFlowInfo: DataFlowInfo

        fun getClassDescriptor(classOrObject: KtClassOrObject): Pair<BindingContext, ClassDescriptorWithResolutionScopes>? {
            val bindingContext: BindingContext
            val classDescriptor: ClassDescriptor?

            if (!KtPsiUtil.isLocal(classOrObject)) {
                bindingContext = resolveSession.bindingContext
                classDescriptor = resolveSession.getClassDescriptor(classOrObject, NoLookupLocation.FROM_IDE)
            } else {
                bindingContext = resolveToElement(classOrObject)
                classDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as ClassDescriptor?
            }

            return (classDescriptor as? ClassDescriptorWithResolutionScopes)?.let { Pair(bindingContext, it) }
        }

        val bindingContextForContext: BindingContext
        when (context) {
            is KtPrimaryConstructor -> {
                val (bindingContext, classDescriptor) = getClassDescriptor(context.getContainingClassOrObject()) ?: return null

                scopeForContextElement = classDescriptor.scopeForInitializerResolution
                dataFlowInfo = DataFlowInfo.EMPTY
                bindingContextForContext = bindingContext
            }
            is KtSecondaryConstructor -> {
                val correctedContext = context.getDelegationCall().calleeExpression!!
                bindingContextForContext = resolveToElement(correctedContext)

                scopeForContextElement = bindingContextForContext[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is KtClassOrObject -> {
                val (bindingContext, classDescriptor) = getClassDescriptor(context) ?: return null
                scopeForContextElement = classDescriptor.scopeForMemberDeclarationResolution
                dataFlowInfo = DataFlowInfo.EMPTY
                bindingContextForContext = bindingContext
            }
            is KtFile -> {
                scopeForContextElement = resolveSession.fileScopeProvider.getFileResolutionScope(context)
                dataFlowInfo = DataFlowInfo.EMPTY
                bindingContextForContext = BindingContext.EMPTY
            }
            is KtElement -> {
                val correctedContext = context.correctContextForElement()
                bindingContextForContext = resolveToElement(correctedContext)

                scopeForContextElement = bindingContextForContext[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = bindingContextForContext.getDataFlowInfoAfter(correctedContext)
            }
            else -> return null
        }

        if (scopeForContextElement == null) return null

        val importList = codeFragment.importsAsImportList()
        if (importList == null || importList.imports.isEmpty()) {
            return Triple(scopeForContextElement, dataFlowInfo, bindingContextForContext)
        }

        val importScopes = importList.imports.mapNotNull {
            qualifierResolver.processImportReference(
                it, resolveSession.moduleDescriptor, resolveSession.trace,
                excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null
            )
        }

        return Triple(scopeForContextElement.addImportingScopes(importScopes), dataFlowInfo, bindingContextForContext)
    }
}
