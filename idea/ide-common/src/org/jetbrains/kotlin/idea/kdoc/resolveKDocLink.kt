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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.components.ServiceManager
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.QualifiedExpressionResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.*
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

fun resolveKDocLink(context: BindingContext,
                    resolutionFacade: ResolutionFacade,
                    fromDescriptor: DeclarationDescriptor,
                    fromSubjectOfTag: KDocTag?,
                    qualifiedName: List<String>): Collection<DeclarationDescriptor> =
        when (fromSubjectOfTag?.knownTag) {
            KDocKnownTag.PARAM -> resolveParamLink(fromDescriptor, qualifiedName)
            KDocKnownTag.SAMPLE -> resolveKDocSampleLink(context, resolutionFacade, fromDescriptor, qualifiedName)
            else -> resolveDefaultKDocLink(context, resolutionFacade, fromDescriptor, qualifiedName)
        }


fun getParamDescriptors(fromDescriptor: DeclarationDescriptor): List<DeclarationDescriptor> {
    // TODO resolve parameters of functions passed as parameters
    when (fromDescriptor) {
        is CallableDescriptor ->
            return fromDescriptor.valueParameters + fromDescriptor.typeParameters
        is ClassifierDescriptor -> {
            val typeParams = fromDescriptor.typeConstructor.parameters
            if (fromDescriptor is ClassDescriptor) {
                val constructorDescriptor = fromDescriptor.unsubstitutedPrimaryConstructor
                if (constructorDescriptor != null) {
                    return typeParams + constructorDescriptor.valueParameters
                }
            }
            return typeParams
        }
    }

    return listOf()
}

private fun resolveParamLink(fromDescriptor: DeclarationDescriptor, qualifiedName: List<String>): List<DeclarationDescriptor> {
    val name = qualifiedName.singleOrNull() ?: return listOf()
    return getParamDescriptors(fromDescriptor).filter { it.name.asString() == name }
}

fun resolveKDocSampleLink(context: BindingContext,
                          resolutionFacade: ResolutionFacade,
                          fromDescriptor: DeclarationDescriptor,
                          qualifiedName: List<String>): Collection<DeclarationDescriptor> {

    val resolvedViaService = SampleResolutionService.resolveSample(context, fromDescriptor, resolutionFacade, qualifiedName)
    if (resolvedViaService.isNotEmpty()) return resolvedViaService

    return resolveDefaultKDocLink(context, resolutionFacade, fromDescriptor, qualifiedName)
}

private fun resolveDefaultKDocLink(
        context: BindingContext,
        resolutionFacade: ResolutionFacade,
        fromDescriptor: DeclarationDescriptor,
        qualifiedName: List<String>
): Collection<DeclarationDescriptor> {

    val scope = getKDocLinkResolutionScope(resolutionFacade, fromDescriptor)

    if (qualifiedName.size == 1) {
        val shortName = Name.identifier(qualifiedName.single())
        val descriptorsByName = SmartList<DeclarationDescriptor>()
        scope.collectAllByName(shortName, descriptorsByName)
        
        // Try to find a matching local descriptor (parameter or type parameter) first
        val localDescriptors = descriptorsByName.filter { it.containingDeclaration == fromDescriptor }
        if (localDescriptors.isNotEmpty()) return localDescriptors

        descriptorsByName.addIfNotNull(fromDescriptor.module.getPackage(FqName.topLevel(shortName)))

        return descriptorsByName
    }

    val moduleDescriptor = fromDescriptor.module
    val qualifiedExpressionResolver = resolutionFacade.getFrontendService(moduleDescriptor, QualifiedExpressionResolver::class.java)

    val contextElement = DescriptorToSourceUtils.descriptorToDeclaration(fromDescriptor)
    val factory = KtPsiFactory(resolutionFacade.project)
    // TODO escape identifiers
    val codeFragment = factory.createExpressionCodeFragment(qualifiedName.joinToString("."), contextElement)
    val qualifiedExpression = codeFragment.findElementAt(codeFragment.textLength - 1)?.getStrictParentOfType<KtQualifiedExpression>() ?: return emptyList()
    val (descriptor, memberName) = qualifiedExpressionResolver.resolveClassOrPackageInQualifiedExpression(qualifiedExpression, scope, context)
    if (descriptor == null) return emptyList()
    if (memberName != null) {
        val memberScope = getKDocLinkResolutionScope(resolutionFacade, descriptor)
        return memberScope.collectAllByName(memberName)
    }
    return listOf(descriptor)
}

private fun LexicalScope.collectAllByName(shortName: Name, toCollection: MutableCollection<DeclarationDescriptor> = SmartList()): Collection<DeclarationDescriptor> {
    toCollection.addIfNotNull(findClassifier(shortName, NoLookupLocation.FROM_IDE))
    toCollection.addIfNotNull(findPackage(shortName))
    toCollection.addAll(collectFunctions(shortName, NoLookupLocation.FROM_IDE))
    toCollection.addAll(collectVariables(shortName, NoLookupLocation.FROM_IDE))
    return toCollection
}

private fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): MemberScope {
    return descriptor.containingDeclaration.getPackage(descriptor.fqName).memberScope
}

private fun getClassInnerScope(outerScope: LexicalScope, descriptor: ClassDescriptor): LexicalScope {

    val headerScope = LexicalScopeImpl(outerScope, descriptor, false, descriptor.thisAsReceiverParameter,
                                       LexicalScopeKind.SYNTHETIC) {
        for (typeParameter in descriptor.declaredTypeParameters) {
            addClassifierDescriptor(typeParameter)
        }
        for (constructor in descriptor.constructors) {
            addFunctionDescriptor(constructor)
        }
    }

    val scopeChain = arrayListOf(descriptor.defaultType.memberScope,
                                 descriptor.staticScope)

    descriptor.companionObjectDescriptor?.let {
        scopeChain.add(it.defaultType.memberScope)
    }

    return LexicalChainedScope(headerScope, descriptor, false, null,
                               LexicalScopeKind.SYNTHETIC,
                               scopeChain)
}

fun getKDocLinkResolutionScope(resolutionFacade: ResolutionFacade, descriptor: DeclarationDescriptor): LexicalScope {
    return when (descriptor) {
        is PackageFragmentDescriptor ->
            LexicalScope.Base(getPackageInnerScope(descriptor).memberScopeAsImportingScope(), descriptor)

        is PackageViewDescriptor ->
            LexicalScope.Base(descriptor.memberScope.memberScopeAsImportingScope(), descriptor)

        is ClassDescriptor ->
            getClassInnerScope(getOuterScope(descriptor, resolutionFacade), descriptor)

        is FunctionDescriptor ->
            FunctionDescriptorUtil.getFunctionInnerScope(getOuterScope(descriptor, resolutionFacade),
                                                         descriptor, LocalRedeclarationChecker.DO_NOTHING)

        is PropertyDescriptor ->
            ScopeUtils.makeScopeForPropertyHeader(getOuterScope(descriptor, resolutionFacade), descriptor)

        is DeclarationDescriptorNonRoot ->
            getOuterScope(descriptor, resolutionFacade)

        else -> throw IllegalArgumentException("Cannot find resolution scope for root $descriptor")
    }
}

private fun getOuterScope(descriptor: DeclarationDescriptorWithSource, resolutionFacade: ResolutionFacade): LexicalScope {
    val parent = descriptor.containingDeclaration
    if (parent is PackageFragmentDescriptor) {
        val containingFile = (descriptor.source as? PsiSourceElement)?.psi?.containingFile as? KtFile
        if (containingFile != null) {
            val kotlinCacheService = ServiceManager.getService(containingFile.project, KotlinCacheService::class.java)
            val facadeToUse = kotlinCacheService?.getResolutionFacade(listOf(containingFile)) ?: resolutionFacade
            return facadeToUse.getFileResolutionScope(containingFile)
        }
    }
    return getKDocLinkResolutionScope(resolutionFacade, parent!!)
}
