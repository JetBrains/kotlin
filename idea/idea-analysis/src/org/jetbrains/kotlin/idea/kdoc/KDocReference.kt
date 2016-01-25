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

package org.jetbrains.kotlin.idea.kdoc

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.references.KtMultiReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.getFileResolutionScope
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.utils.memberScopeAsImportingScope
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

class KDocReference(element: KDocName): KtMultiReference<KDocName>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val declaration = element.getContainingDoc().getOwner() ?: return arrayListOf()
        val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration] ?: return arrayListOf()

        val kdocLink = element.getStrictParentOfType<KDocLink>()!!
        return resolveKDocLink(element.getResolutionFacade(),
                               declarationDescriptor,
                               kdocLink.getTagIfSubject(),
                               element.getQualifiedName())
    }

    override fun getRangeInElement(): TextRange = element.getNameTextRange()

    override fun canRename(): Boolean = true

    override fun resolve(): PsiElement? = multiResolve(false).firstOrNull()?.element

    override fun handleElementRename(newElementName: String?): PsiElement? {
        val textRange = element.getNameTextRange()
        val newText = textRange.replace(element.text, newElementName!!)
        val newLink = KDocElementFactory(element.project).createNameFromText(newText)
        return element.replace(newLink)
    }

    override fun getCanonicalText(): String = element.getNameText()
}

fun resolveKDocLink(resolutionFacade: ResolutionFacade,
                           fromDescriptor: DeclarationDescriptor,
                           fromSubjectOfTag: KDocTag?,
                           qualifiedName: List<String>): Collection<DeclarationDescriptor> {
    if (fromSubjectOfTag?.knownTag == KDocKnownTag.PARAM) {
        return resolveParamLink(fromDescriptor, qualifiedName)
    }

    // Try to find a matching local descriptor (parameter or type parameter) first.
    if (qualifiedName.size == 1) {
        val localResult = resolveInLocalScope(fromDescriptor, qualifiedName.single(), resolutionFacade)
        if (!localResult.isEmpty()) {
            return localResult
        }
    }

    var result: Collection<DeclarationDescriptor> = listOf(fromDescriptor)
    qualifiedName.forEach { nameComponent ->
        val scope = getResolutionScope(resolutionFacade, result.singleOrNull() ?: return emptyList())
        result = scope.collectDescriptorsFiltered(nameFilter = { it.asString() == nameComponent})
    }

    return result
}

private fun resolveInLocalScope(fromDescriptor: DeclarationDescriptor,
                                name: String,
                                resolutionFacade: ResolutionFacade): List<DeclarationDescriptor> {
    val scope = getResolutionScope(resolutionFacade, fromDescriptor)
    return scope.collectDescriptorsFiltered(nameFilter = { it.asString() == name }).filter {
        it.containingDeclaration == fromDescriptor
    }
}

fun getParamDescriptors(fromDescriptor: DeclarationDescriptor): List<DeclarationDescriptor> {
    // TODO resolve parameters of functions passed as parameters
    when (fromDescriptor) {
        is CallableDescriptor ->
            return fromDescriptor.valueParameters
        is ClassifierDescriptor -> {
            val typeParams = fromDescriptor.typeConstructor.parameters
            if (fromDescriptor is ClassDescriptor) {
                val constructorDescriptor = fromDescriptor.unsubstitutedPrimaryConstructor
                if (constructorDescriptor != null) {
                    return typeParams as List<DeclarationDescriptor> + constructorDescriptor.valueParameters
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

fun getResolutionScope(resolutionFacade: ResolutionFacade, descriptor: DeclarationDescriptor): LexicalScope {
    return when (descriptor) {
        is PackageFragmentDescriptor ->
            LexicalScope.empty(getPackageInnerScope(descriptor).memberScopeAsImportingScope(), descriptor)

        is PackageViewDescriptor ->
            LexicalScope.empty(descriptor.memberScope.memberScopeAsImportingScope(), descriptor)

        is ClassDescriptor ->
            getClassInnerScope(getOuterScope(descriptor, resolutionFacade), descriptor)

        is FunctionDescriptor ->
            FunctionDescriptorUtil.getFunctionInnerScope(getOuterScope(descriptor, resolutionFacade),
                                                         descriptor, RedeclarationHandler.DO_NOTHING)

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
            return resolutionFacade.getFileResolutionScope(containingFile)
        }
    }
    return getResolutionScope(resolutionFacade, parent!!)
}
