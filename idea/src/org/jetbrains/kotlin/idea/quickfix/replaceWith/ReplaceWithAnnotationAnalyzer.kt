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

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.core.asExpression
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.InsertExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.ClassResolutionScopesSupport
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.asJetScope
import org.jetbrains.kotlin.resolve.scopes.utils.asLexicalScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

data class ReplaceWith(val pattern: String, val imports: List<String>)

object ReplaceWithAnnotationAnalyzer {
    public val PARAMETER_USAGE_KEY: Key<Name> = Key("PARAMETER_USAGE")
    public val TYPE_PARAMETER_USAGE_KEY: Key<Name> = Key("TYPE_PARAMETER_USAGE")

    public data class ReplacementExpression(
            val expression: JetExpression,
            val fqNamesToImport: Collection<FqName>
    ) {
        fun copy() = ReplacementExpression(expression.copied(), fqNamesToImport)
    }

    public fun analyzeCallableReplacement(
            annotation: ReplaceWith,
            symbolDescriptor: CallableDescriptor,
            resolutionFacade: ResolutionFacade
    ): ReplacementExpression? {
        val originalDescriptor = (if (symbolDescriptor is CallableMemberDescriptor)
            DescriptorUtils.unwrapFakeOverride(symbolDescriptor)
        else
            symbolDescriptor).original
        return analyzeOriginal(annotation, originalDescriptor, resolutionFacade)
    }

    private fun analyzeOriginal(
            annotation: ReplaceWith,
            symbolDescriptor: CallableDescriptor,
            resolutionFacade: ResolutionFacade
    ): ReplacementExpression? {
        val psiFactory = JetPsiFactory(resolutionFacade.project)
        var expression = try {
            psiFactory.createExpression(annotation.pattern)
        }
        catch(e: Exception) {
            return null
        }

        val module = symbolDescriptor.module
        val explicitImportsScope = buildExplicitImportsScope(annotation, resolutionFacade, module)
        val additionalScopes = resolutionFacade.getFrontendService(FileScopeProvider.AdditionalScopes::class.java)
        val scope = getResolutionScope(symbolDescriptor, symbolDescriptor,
                                       listOf(explicitImportsScope) + additionalScopes.scopes) ?: return null

        var bindingContext = analyzeInContext(expression, module, scope, resolutionFacade)

        val typeArgsToAdd = ArrayList<Pair<JetCallExpression, JetTypeArgumentList>>()
        expression.forEachDescendantOfType<JetCallExpression> {
            if (InsertExplicitTypeArgumentsIntention.isApplicableTo(it, bindingContext)) {
                typeArgsToAdd.add(it to InsertExplicitTypeArgumentsIntention.createTypeArguments(it, bindingContext)!!)
            }
        }

        if (typeArgsToAdd.isNotEmpty()) {
            for ((callExpr, typeArgs) in typeArgsToAdd) {
                callExpr.addAfter(typeArgs, callExpr.calleeExpression)
            }

            // reanalyze expression - new usages of type parameters may be added
            bindingContext = analyzeInContext(expression, module, scope, resolutionFacade)
        }

        val receiversToAdd = ArrayList<Pair<JetExpression, JetExpression>>()
        val importFqNames = importFqNames(annotation).toMutableSet()

        expression.forEachDescendantOfType<JetSimpleNameExpression> { expression ->
            val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return@forEachDescendantOfType

            if (target.isExtension || expression.getReceiverExpression() == null) {
                importFqNames.addIfNotNull(target.importableFqName)
            }

            if (expression.getReceiverExpression() == null) {
                if (target is ValueParameterDescriptor && target.containingDeclaration == symbolDescriptor) {
                    expression.putCopyableUserData(PARAMETER_USAGE_KEY, target.name)
                }
                else if (target is TypeParameterDescriptor && target.containingDeclaration == symbolDescriptor) {
                    expression.putCopyableUserData(TYPE_PARAMETER_USAGE_KEY, target.name)
                }

                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null && resolvedCall.status.isSuccess) {
                    val receiver = if (resolvedCall.resultingDescriptor.isExtension)
                        resolvedCall.extensionReceiver
                    else
                        resolvedCall.dispatchReceiver
                    if (receiver is ThisReceiver) {
                        val receiverExpression = receiver.asExpression(scope.asJetScope(), psiFactory)
                        if (receiverExpression != null) {
                            receiversToAdd.add(expression to receiverExpression)
                        }
                    }
                }
            }
        }

        // add receivers in reverse order because arguments of a call were processed after the callee's name
        for ((expr, receiverExpression) in receiversToAdd.reversed()) {
            val expressionToReplace = expr.parent as? JetCallExpression ?: expr
            val newExpr = expressionToReplace.replaced(psiFactory.createExpressionByPattern("$0.$1", receiverExpression, expressionToReplace))
            if (expressionToReplace == expression) {
                expression = newExpr
            }
        }

        return ReplacementExpression(expression, importFqNames)
    }

    public fun analyzeClassReplacement(
            annotation: ReplaceWith,
            symbolDescriptor: ClassDescriptor,
            resolutionFacade: ResolutionFacade
    ): JetUserType? {
        val psiFactory = JetPsiFactory(resolutionFacade.project)
        val typeReference = try {
            psiFactory.createType(annotation.pattern)
        }
        catch(e: Exception) {
            return null
        }
        if (typeReference.typeElement !is JetUserType) return null

        val module = symbolDescriptor.module

        val explicitImportsScope = buildExplicitImportsScope(annotation, resolutionFacade, module)
        val scope = getResolutionScope(symbolDescriptor, symbolDescriptor, listOf(explicitImportsScope)) ?: return null

        val typeResolver = resolutionFacade.getFrontendService(TypeResolver::class.java)
        val bindingTrace = BindingTraceContext()
        typeResolver.resolvePossiblyBareType(TypeResolutionContext(scope, bindingTrace, false, true), typeReference)

        val typesToQualify = ArrayList<Pair<JetNameReferenceExpression, FqName>>()

        typeReference.forEachDescendantOfType<JetNameReferenceExpression> { expression ->
            val parentType = expression.parent as? JetUserType ?: return@forEachDescendantOfType
            if (parentType.qualifier != null) return@forEachDescendantOfType
            val targetClass = bindingTrace.bindingContext[BindingContext.REFERENCE_TARGET, expression] as? ClassDescriptor ?: return@forEachDescendantOfType
            val fqName = targetClass.fqNameUnsafe
            if (fqName.isSafe) {
                typesToQualify.add(expression to fqName.toSafe())
            }
        }

        for ((nameExpression, fqName) in typesToQualify) {
            nameExpression.mainReference.bindToFqName(fqName, JetSimpleNameReference.ShorteningMode.NO_SHORTENING)
        }

        return typeReference.typeElement as JetUserType
    }

    private fun buildExplicitImportsScope(annotation: ReplaceWith, resolutionFacade: ResolutionFacade, module: ModuleDescriptor): ExplicitImportsScope {
        val importedSymbols = importFqNames(annotation)
                .flatMap { resolutionFacade.resolveImportReference(module, it) }
        return ExplicitImportsScope(importedSymbols)
    }

    private fun importFqNames(annotation: ReplaceWith): List<FqName> {
        return annotation.imports
                .filter { FqNameUnsafe.isValid(it) }
                .map { FqNameUnsafe(it) }
                .filter { it.isSafe }
                .map { it.toSafe() }
    }

    private fun analyzeInContext(
            expression: JetExpression,
            module: ModuleDescriptor,
            scope: LexicalScope,
            resolutionFacade: ResolutionFacade
    ): BindingContext {
        val traceContext = BindingTraceContext()
        val frontendService = if (module.builtIns.builtInsModule == module) {
            // TODO: doubtful place, do we require this module or not? Built-ins module doesn't have some necessary components...
            resolutionFacade.getFrontendService(ExpressionTypingServices::class.java)
        }
        else {
            resolutionFacade.getFrontendService(module, ExpressionTypingServices::class.java)
        }
        frontendService.getTypeInfo(scope, expression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, traceContext, false)
        return traceContext.bindingContext
    }

    private fun getResolutionScope(descriptor: DeclarationDescriptor, ownerDescriptor: DeclarationDescriptor, additionalScopes: Collection<JetScope>): LexicalScope? {
        return when (descriptor) {
            is PackageFragmentDescriptor -> {
                val moduleDescriptor = descriptor.containingDeclaration
                getResolutionScope(moduleDescriptor.getPackage(descriptor.fqName), ownerDescriptor, additionalScopes)
            }

            is PackageViewDescriptor ->
                ChainedScope(ownerDescriptor, "ReplaceWith resolution scope", descriptor.memberScope, *additionalScopes.toTypedArray()).asLexicalScope()

            is ClassDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                ClassResolutionScopesSupport(descriptor, LockBasedStorageManager.NO_LOCKS, { outerScope }).scopeForMemberDeclarationResolution()
            }

            is FunctionDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                FunctionDescriptorUtil.getFunctionInnerScope(outerScope, descriptor, RedeclarationHandler.DO_NOTHING)
            }

            is PropertyDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                JetScopeUtils.getPropertyDeclarationInnerScope(descriptor, outerScope, RedeclarationHandler.DO_NOTHING)
            }

            else -> return null // something local, should not work with ReplaceWith
        }
    }
}