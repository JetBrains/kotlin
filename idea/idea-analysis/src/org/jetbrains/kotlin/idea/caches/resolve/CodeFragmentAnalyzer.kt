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

    // component dependency cycle
    var resolveElementCache: ResolveElementCache? = null
        @Inject set

    fun analyzeCodeFragment(codeFragment: KtCodeFragment, trace: BindingTrace, bodyResolveMode: BodyResolveMode) {
        val codeFragmentElement = codeFragment.getContentElement()

        val (scopeForContextElement, dataFlowInfo) = getScopeAndDataFlowForAnalyzeFragment(codeFragment) {
            resolveElementCache!!.resolveToElements(listOf(it), bodyResolveMode)
        } ?: return

        when (codeFragmentElement) {
            is KtExpression -> {
                PreliminaryDeclarationVisitor.createForExpression(codeFragmentElement, trace,
                                                                  expressionTypingServices.languageVersionSettings)
                expressionTypingServices.getTypeInfo(
                        scopeForContextElement,
                        codeFragmentElement,
                        TypeUtils.NO_EXPECTED_TYPE,
                        dataFlowInfo,
                        trace,
                        false
                )
            }

            is KtTypeReference -> {
                val context = TypeResolutionContext(scopeForContextElement, trace, true, true, codeFragment.suppressDiagnosticsInDebugMode()).noBareTypes()
                typeResolver.resolvePossiblyBareType(context, codeFragmentElement)
            }
        }
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

    private fun getScopeAndDataFlowForAnalyzeFragment(
            codeFragment: KtCodeFragment,
            resolveToElement: (KtElement) -> BindingContext
    ): Pair<LexicalScope, DataFlowInfo>? {
        val context = codeFragment.context

        val scopeForContextElement: LexicalScope?
        val dataFlowInfo: DataFlowInfo

        fun getClassDescriptor(classOrObject: KtClassOrObject): ClassDescriptor? {
            if (!KtPsiUtil.isLocal(classOrObject)) {
                return resolveSession.getClassDescriptor(classOrObject, NoLookupLocation.FROM_IDE)
            }

            return resolveToElement(classOrObject)[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as ClassDescriptor?
        }

        when (context) {
            is KtPrimaryConstructor -> {
                val descriptor = (getClassDescriptor(context.getContainingClassOrObject()) as? ClassDescriptorWithResolutionScopes) ?: return null

                scopeForContextElement = descriptor.scopeForInitializerResolution
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is KtSecondaryConstructor -> {
                val correctedContext = context.getDelegationCall().calleeExpression!!

                val contextForElement = resolveToElement(correctedContext)

                scopeForContextElement = contextForElement[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is KtClassOrObject -> {
                val descriptor = (getClassDescriptor(context) as? ClassDescriptorWithResolutionScopes) ?: return null

                scopeForContextElement = descriptor.scopeForMemberDeclarationResolution
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is KtFile -> {
                scopeForContextElement = resolveSession.fileScopeProvider.getFileResolutionScope(context)
                dataFlowInfo = DataFlowInfo.EMPTY
            }
            is KtElement -> {
                val correctedContext = context.correctContextForElement()

                val contextForElement = resolveToElement(correctedContext)

                scopeForContextElement = contextForElement[BindingContext.LEXICAL_SCOPE, correctedContext]
                dataFlowInfo = contextForElement.getDataFlowInfoAfter(correctedContext)
            }
            else -> return null
        }

        if (scopeForContextElement == null) return null

        val importList = codeFragment.importsAsImportList()
        if (importList == null || importList.imports.isEmpty()) {
            return scopeForContextElement to dataFlowInfo
        }

        val importScopes = importList.imports.mapNotNull {
            qualifierResolver.processImportReference(it, resolveSession.moduleDescriptor, resolveSession.trace,
                                                     excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null)
        }

        return scopeForContextElement.addImportingScopes(importScopes) to dataFlowInfo
    }
}
