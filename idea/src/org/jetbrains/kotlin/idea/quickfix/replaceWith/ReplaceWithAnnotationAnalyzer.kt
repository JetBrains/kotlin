/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInliner.CodeToInline
import org.jetbrains.kotlin.idea.codeInliner.CodeToInlineBuilder
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.lazy.DefaultImportProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.ClassResolutionScopesSupport
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.chainImportingScopes
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import java.util.*

data class ReplaceWith(val pattern: String, val imports: List<String>)

object ReplaceWithAnnotationAnalyzer {
    fun analyzeCallableReplacement(
            annotation: ReplaceWith,
            symbolDescriptor: CallableDescriptor,
            resolutionFacade: ResolutionFacade
    ): CodeToInline? {
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
    ): CodeToInline? {
        val psiFactory = KtPsiFactory(resolutionFacade.project)
        val expression = try {
            psiFactory.createExpression(annotation.pattern)
        }
        catch(t: Throwable) {
            return null
        }

        val module = resolutionFacade.moduleDescriptor
        val explicitImportsScope = buildExplicitImportsScope(annotation, resolutionFacade, module)
        val defaultImportsScopes = buildDefaultImportsScopes(resolutionFacade, module)
        val scope = getResolutionScope(symbolDescriptor, symbolDescriptor,
                                       listOf(explicitImportsScope) + defaultImportsScopes) ?: return null

        val expressionTypingServices = resolutionFacade.getFrontendService(module, ExpressionTypingServices::class.java)

        fun analyzeExpression() = expression.analyzeInContext(scope, expressionTypingServices = expressionTypingServices)

        return CodeToInlineBuilder(symbolDescriptor, resolutionFacade).prepareCodeToInline(expression, emptyList(), ::analyzeExpression)
    }

    fun analyzeClassifierReplacement(
            annotation: ReplaceWith,
            symbolDescriptor: ClassifierDescriptorWithTypeParameters,
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

        val module = resolutionFacade.moduleDescriptor

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

        return listOf(buildExplicitImportsScope(aliasImports.map { it.fqName }, resolutionFacade, module)) +
               allUnderImports.map { module.getPackage(it.fqName).memberScope.memberScopeAsImportingScope() }.asReversed()
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

    private fun getResolutionScope(descriptor: DeclarationDescriptor, ownerDescriptor: DeclarationDescriptor, additionalScopes: Collection<ImportingScope>): LexicalScope? {
        return when (descriptor) {
            is PackageFragmentDescriptor -> {
                val moduleDescriptor = descriptor.containingDeclaration
                getResolutionScope(moduleDescriptor.getPackage(descriptor.fqName), ownerDescriptor, additionalScopes)
            }

            is PackageViewDescriptor -> {
                LexicalScope.Base(
                        chainImportingScopes(listOf(descriptor.memberScope.memberScopeAsImportingScope()) + additionalScopes)!!,
                        ownerDescriptor)
            }

            is ClassDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                ClassResolutionScopesSupport(descriptor, LockBasedStorageManager.NO_LOCKS, { outerScope }).scopeForMemberDeclarationResolution()
            }

            is TypeAliasDescriptor -> {
                val outerScope = getResolutionScope(descriptor.containingDeclaration, ownerDescriptor, additionalScopes) ?: return null
                LexicalScopeImpl(outerScope, descriptor, false, null, LexicalScopeKind.TYPE_ALIAS_HEADER, LocalRedeclarationChecker.DO_NOTHING) {
                    for (typeParameter in descriptor.declaredTypeParameters) {
                        addClassifierDescriptor(typeParameter)
                    }
                }
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
