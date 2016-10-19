/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.canBeResolvedViaImport
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.lazy.DefaultImportProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.ClassResolutionScopesSupport
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.chainImportingScopes
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor
import java.util.*

data class ReplaceWith(val pattern: String, val imports: List<String>)

object ReplaceWithAnnotationAnalyzer {
    val PARAMETER_USAGE_KEY: Key<Name> = Key("PARAMETER_USAGE")
    val TYPE_PARAMETER_USAGE_KEY: Key<Name> = Key("TYPE_PARAMETER_USAGE")

    data class ReplacementExpression(
            val expression: KtExpression,
            val fqNamesToImport: Collection<FqName>
    ) {
        fun copy() = ReplacementExpression(expression.copied(), fqNamesToImport)
    }

    fun analyzeCallableReplacement(
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
        val psiFactory = KtPsiFactory(resolutionFacade.project)
        var expression = try {
            psiFactory.createExpression(annotation.pattern)
        }
        catch(t: Throwable) {
            return null
        }

        val module = symbolDescriptor.module
        val explicitImportsScope = buildExplicitImportsScope(annotation, resolutionFacade, module)
        val defaultImportsScopes = buildDefaultImportsScopes(resolutionFacade, module)
        val scope = getResolutionScope(symbolDescriptor, symbolDescriptor,
                                       listOf(explicitImportsScope) + defaultImportsScopes) ?: return null

        var bindingContext = analyzeInContext(expression, module, scope, resolutionFacade)

        val typeArgsToAdd = ArrayList<Pair<KtCallExpression, KtTypeArgumentList>>()
        expression.forEachDescendantOfType<KtCallExpression> {
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

        val receiversToAdd = ArrayList<Pair<KtExpression, KtExpression>>()
        val importFqNames = importFqNames(annotation).toMutableSet()

        expression.forEachDescendantOfType<KtSimpleNameExpression> { expression ->
            val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return@forEachDescendantOfType

            //TODO: other types of references ('[]' etc)
            if (expression.mainReference.canBeResolvedViaImport(target)) {
                importFqNames.add(target.importableFqName!!)
            }

            if (expression.getReceiverExpression() == null) {
                if (target is ValueParameterDescriptor && target.containingDeclaration == symbolDescriptor) {
                    expression.putCopyableUserData(PARAMETER_USAGE_KEY, target.name)
                }
                else if (target is TypeParameterDescriptor && target.containingDeclaration == symbolDescriptor) {
                    expression.putCopyableUserData(TYPE_PARAMETER_USAGE_KEY, target.name)
                }

                val resolvedCall = expression.getResolvedCall(bindingContext)
                if (resolvedCall != null && resolvedCall.isReallySuccess()) {
                    val receiver = if (resolvedCall.resultingDescriptor.isExtension)
                        resolvedCall.extensionReceiver
                    else
                        resolvedCall.dispatchReceiver
                    if (receiver is ImplicitReceiver) {
                        val receiverExpression = receiver.asExpression(scope, psiFactory)
                        if (receiverExpression != null) {
                            receiversToAdd.add(expression to receiverExpression)
                        }
                    }
                }
            }
        }

        // add receivers in reverse order because arguments of a call were processed after the callee's name
        for ((expr, receiverExpression) in receiversToAdd.asReversed()) {
            val expressionToReplace = expr.parent as? KtCallExpression ?: expr
            val newExpr = expressionToReplace.replaced(psiFactory.createExpressionByPattern("$0.$1", receiverExpression, expressionToReplace))
            if (expressionToReplace == expression) {
                expression = newExpr
            }
        }

        return ReplacementExpression(expression, importFqNames)
    }

    fun analyzeClassReplacement(
            annotation: ReplaceWith,
            symbolDescriptor: ClassDescriptor,
            resolutionFacade: ResolutionFacade
    ): KtUserType? {
        val psiFactory = KtPsiFactory(resolutionFacade.project)
        val typeReference = try {
            psiFactory.createType(annotation.pattern)
        }
        catch(e: Exception) {
            return null
        }
        if (typeReference.typeElement !is KtUserType) return null

        val module = symbolDescriptor.module

        val explicitImportsScope = buildExplicitImportsScope(annotation, resolutionFacade, module)
        val defaultImportScopes = buildDefaultImportsScopes(resolutionFacade, module)
        val scope = getResolutionScope(symbolDescriptor, symbolDescriptor, listOf(explicitImportsScope) + defaultImportScopes) ?: return null

        val typeResolver = resolutionFacade.getFrontendService(TypeResolver::class.java)
        val bindingTrace = BindingTraceContext()
        typeResolver.resolvePossiblyBareType(TypeResolutionContext(scope, bindingTrace, false, true, false), typeReference)

        val typesToQualify = ArrayList<Pair<KtNameReferenceExpression, FqName>>()

        typeReference.forEachDescendantOfType<KtNameReferenceExpression> { expression ->
            val parentType = expression.parent as? KtUserType ?: return@forEachDescendantOfType
            if (parentType.qualifier != null) return@forEachDescendantOfType
            val targetClass = bindingTrace.bindingContext[BindingContext.REFERENCE_TARGET, expression] as? ClassDescriptor ?: return@forEachDescendantOfType
            val fqName = targetClass.fqNameUnsafe
            if (fqName.isSafe) {
                typesToQualify.add(expression to fqName.toSafe())
            }
        }

        for ((nameExpression, fqName) in typesToQualify) {
            nameExpression.mainReference.bindToFqName(fqName, KtSimpleNameReference.ShorteningMode.NO_SHORTENING)
        }

        return typeReference.typeElement as KtUserType
    }

    private fun buildDefaultImportsScopes(resolutionFacade: ResolutionFacade, module: ModuleDescriptor): List<ImportingScope> {
        val (allUnderImports, aliasImports) = resolutionFacade.frontendService<DefaultImportProvider>().defaultImports.partition { it.isAllUnder }
        // this solution doesn't support aliased default imports with a different alias
        // TODO: Create import directives from ImportPath, create ImportResolver, create LazyResolverScope, see FileScopeProviderImpl

        return listOf(buildExplicitImportsScope(aliasImports.map { it.fqnPart() }, resolutionFacade, module)) +
               allUnderImports.map { module.getPackage(it.fqnPart()).memberScope.memberScopeAsImportingScope() }.asReversed()
    }

    private fun buildExplicitImportsScope(annotation: ReplaceWith, resolutionFacade: ResolutionFacade, module: ModuleDescriptor): ExplicitImportsScope {
        return buildExplicitImportsScope(importFqNames(annotation), resolutionFacade, module)
    }

    private fun buildExplicitImportsScope(importFqNames: List<FqName>, resolutionFacade: ResolutionFacade, module: ModuleDescriptor): ExplicitImportsScope {
        val importedSymbols = importFqNames.flatMap { resolutionFacade.resolveImportReference(module, it) }
        return ExplicitImportsScope(importedSymbols)
    }

    private fun importFqNames(annotation: ReplaceWith): List<FqName> {
        return annotation.imports
                .filter { FqNameUnsafe.isValid(it) }
                .map(::FqNameUnsafe)
                .filter(FqNameUnsafe::isSafe)
                .map(FqNameUnsafe::toSafe)
    }

    private fun analyzeInContext(
            expression: KtExpression,
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
        PreliminaryDeclarationVisitor.createForExpression(expression, traceContext)
        frontendService.getTypeInfo(scope, expression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, traceContext, false)
        return traceContext.bindingContext
    }

    private fun getResolutionScope(descriptor: DeclarationDescriptor, ownerDescriptor: DeclarationDescriptor, additionalScopes: Collection<ImportingScope>): LexicalScope? {
        return when (descriptor) {
            is PackageFragmentDescriptor -> {
                val moduleDescriptor = descriptor.containingDeclaration
                getResolutionScope(moduleDescriptor.getPackage(descriptor.fqName), ownerDescriptor, additionalScopes)
            }

            is PackageViewDescriptor ->
                LexicalScope.Empty(chainImportingScopes(listOf(descriptor.memberScope.memberScopeAsImportingScope()) + additionalScopes)!!, ownerDescriptor)

            is ClassDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                ClassResolutionScopesSupport(descriptor, LockBasedStorageManager.NO_LOCKS, { outerScope }).scopeForMemberDeclarationResolution()
            }

            is FunctionDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                FunctionDescriptorUtil.getFunctionInnerScope(outerScope, descriptor, LocalRedeclarationChecker.DO_NOTHING)
            }

            is PropertyDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                val propertyHeader = ScopeUtils.makeScopeForPropertyHeader(outerScope, descriptor)
                LexicalScopeImpl(propertyHeader, descriptor, false, descriptor.extensionReceiverParameter, LexicalScopeKind.PROPERTY_ACCESSOR_BODY)
            }

            else -> return null // something local, should not work with ReplaceWith
        }
    }
}
