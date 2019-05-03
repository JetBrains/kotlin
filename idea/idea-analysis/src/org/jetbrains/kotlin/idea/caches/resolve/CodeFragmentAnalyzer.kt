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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.project.ResolveElementCache
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes2
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.*
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

    fun analyzeCodeFragment(codeFragment: KtCodeFragment, bodyResolveMode: BodyResolveMode): BindingTrace {
        val contextAnalysisResult = analyzeCodeFragmentContext(codeFragment, bodyResolveMode)
        return doAnalyzeCodeFragment(codeFragment, contextAnalysisResult)
    }

    private fun doAnalyzeCodeFragment(codeFragment: KtCodeFragment, contextInfo: ContextInfo): BindingTrace {
        val (bindingContext, scope, dataFlowInfo) = contextInfo
        val bindingTrace = DelegatingBindingTrace(bindingContext, "For code fragment analysis")

        when (val contentElement = codeFragment.getContentElement()) {
            is KtExpression -> {
                PreliminaryDeclarationVisitor.createForExpression(
                    contentElement, bindingTrace,
                    expressionTypingServices.languageVersionSettings
                )

                expressionTypingServices.getTypeInfo(
                    scope, contentElement, TypeUtils.NO_EXPECTED_TYPE,
                    dataFlowInfo, bindingTrace, false
                )
            }

            is KtTypeReference -> {
                val context = TypeResolutionContext(
                    scope, bindingTrace,
                    true, true, codeFragment.suppressDiagnosticsInDebugMode()
                ).noBareTypes()

                typeResolver.resolvePossiblyBareType(context, contentElement)
            }
        }

        return bindingTrace
    }

    private data class ContextInfo(val bindingContext: BindingContext, val scope: LexicalScope, val dataFlowInfo: DataFlowInfo)

    private fun analyzeCodeFragmentContext(codeFragment: KtCodeFragment, bodyResolveMode: BodyResolveMode): ContextInfo {
        fun resolutionFactory(element: KtElement): BindingContext {
            return resolveElementCache.resolveToElements(listOf(element), bodyResolveMode)
        }

        val context = refineContextElement(codeFragment.context)
        val info = getContextInfo(context, ::resolutionFactory)
        return info.copy(scope = enrichScopeWithImports(info.scope, codeFragment))
    }

    private fun getContextInfo(context: PsiElement?, resolutionFactory: (KtElement) -> BindingContext): ContextInfo {
        var bindingContext: BindingContext = BindingContext.EMPTY
        var dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY
        var scope: LexicalScope? = null

        when (context) {
            is KtPrimaryConstructor -> {
                val containingClass = context.getContainingClassOrObject()
                val resolutionResult = getClassDescriptor(containingClass, resolutionFactory)
                if (resolutionResult != null) {
                    bindingContext = resolutionResult.bindingContext
                    scope = resolutionResult.descriptor.scopeForInitializerResolution
                }
            }
            is KtSecondaryConstructor -> {
                val expression = (context.bodyExpression ?: context.getDelegationCall().calleeExpression) as? KtExpression
                if (expression != null) {
                    bindingContext = resolutionFactory(expression)
                    scope = bindingContext[BindingContext.LEXICAL_SCOPE, expression]
                }
            }
            is KtClassOrObject -> {
                val resolutionResult = getClassDescriptor(context, resolutionFactory)
                if (resolutionResult != null) {
                    bindingContext = resolutionResult.bindingContext
                    scope = resolutionResult.descriptor.scopeForMemberDeclarationResolution
                }
            }
            is KtFunction -> {
                val bindingContextForFunction = resolutionFactory(context)
                val functionDescriptor = bindingContextForFunction[BindingContext.FUNCTION, context]
                if (functionDescriptor != null) {
                    bindingContext = bindingContextForFunction
                    val outerScope = getContextInfo(context.getParentOfType<KtDeclaration>(true), resolutionFactory).scope
                    val localRedeclarationChecker = LocalRedeclarationChecker.DO_NOTHING
                    scope = FunctionDescriptorUtil.getFunctionInnerScope(outerScope, functionDescriptor, localRedeclarationChecker)
                }
            }
            is KtFile -> {
                bindingContext = resolveSession.bindingContext
                scope = resolveSession.fileScopeProvider.getFileResolutionScope(context)
            }
            is KtElement -> {
                bindingContext = resolutionFactory(context)
                scope = context.getResolutionScope(bindingContext)
                dataFlowInfo = bindingContext.getDataFlowInfoAfter(context)
            }
        }

        if (scope == null) {
            val parentDeclaration = context?.getParentOfTypes2<KtDeclaration, KtFile>()
            if (parentDeclaration != null) {
                return getContextInfo(parentDeclaration, resolutionFactory)
            }
        }

        return ContextInfo(bindingContext, scope ?: createEmptyScope(resolveSession.moduleDescriptor), dataFlowInfo)
    }

    private data class ClassResolutionResult(val bindingContext: BindingContext, val descriptor: ClassDescriptorWithResolutionScopes)

    private fun getClassDescriptor(
        classOrObject: KtClassOrObject,
        resolutionFactory: (KtElement) -> BindingContext
    ): ClassResolutionResult? {
        val bindingContext: BindingContext
        val classDescriptor: ClassDescriptor?

        if (!KtPsiUtil.isLocal(classOrObject)) {
            bindingContext = resolveSession.bindingContext
            classDescriptor = resolveSession.getClassDescriptor(classOrObject, NoLookupLocation.FROM_IDE)
        } else {
            bindingContext = resolutionFactory(classOrObject)
            classDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as ClassDescriptor?
        }

        return (classDescriptor as? ClassDescriptorWithResolutionScopes)?.let { ClassResolutionResult(bindingContext, it) }
    }

    private fun refineContextElement(context: PsiElement?): KtElement? {
        return when (context) {
            is KtParameter -> context.getParentOfType<KtFunction>(true)?.let { it }
            is KtProperty -> context.delegateExpressionOrInitializer
            is KtConstructor<*> -> context
            is KtFunctionLiteral -> context.bodyExpression?.statements?.lastOrNull()
            is KtDeclarationWithBody -> context.bodyExpression
            is KtBlockExpression -> context.statements.lastOrNull()
            else -> null
        } ?: context as? KtElement
    }

    private fun enrichScopeWithImports(scope: LexicalScope, codeFragment: KtCodeFragment): LexicalScope {
        val additionalImportingScopes = mutableListOf<ImportingScope>()

        val externalDescriptors = codeFragment.externalDescriptors ?: emptyList()
        if (externalDescriptors.isNotEmpty()) {
            additionalImportingScopes += ExplicitImportsScope(externalDescriptors)
        }

        val importList = codeFragment.importsAsImportList()
        if (importList != null && importList.imports.isNotEmpty()) {
            additionalImportingScopes += createImportScopes(importList)
        }

        if (additionalImportingScopes.isNotEmpty()) {
            return scope.addImportingScopes(additionalImportingScopes)
        }

        return scope
    }

    private fun createImportScopes(importList: KtImportList): List<ImportingScope> {
        return importList.imports.mapNotNull {
            qualifierResolver.processImportReference(
                it, resolveSession.moduleDescriptor, resolveSession.trace,
                excludedImportNames = emptyList(), packageFragmentForVisibilityCheck = null
            )
        }
    }

    private fun createEmptyScope(moduleDescriptor: ModuleDescriptor): LexicalScope {
        return LexicalScope.Base(ImportingScope.Empty, moduleDescriptor)
    }
}