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
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.references.JetMultiReference
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.source.PsiSourceElement

public class KDocReference(element: KDocName): JetMultiReference<KDocName>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val declaration = getElement().getContainingDoc().getOwner()
        if (declaration == null) {
            return arrayListOf()
        }
        val declarationDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        if (declarationDescriptor == null) {
            return arrayListOf()
        }
        val kdocLink = getElement().getStrictParentOfType<KDocLink>()!!
        return resolveKDocLink(getElement().getResolutionFacade(),
                               declarationDescriptor,
                               kdocLink.getTagIfSubject(),
                               getElement().getQualifiedName())
    }

    override fun getRangeInElement(): TextRange = getElement().getNameTextRange()

    override fun canRename(): Boolean = true

    override fun handleElementRename(newElementName: String?): PsiElement? {
        val textRange = getElement().getNameTextRange()
        val newText = textRange.replace(getElement().getText(), newElementName!!)
        val newLink = KDocElementFactory(getElement().getProject()).createNameFromText(newText)
        return getElement().replace(newLink)
    }

    override fun getCanonicalText(): String = getElement().getNameText()
}

public fun resolveKDocLink(resolutionFacade: ResolutionFacade,
                           fromDescriptor: DeclarationDescriptor,
                           fromSubjectOfTag: KDocTag?,
                           qualifiedName: List<String>): Collection<DeclarationDescriptor> {
    if (fromSubjectOfTag?.knownTag == KDocKnownTag.PARAM) {
        return resolveParamLink(fromDescriptor, qualifiedName)
    }

    // Try to find a matching local descriptor (parameter or type parameter) first.
    if (qualifiedName.size() == 1) {
        val localResult = resolveInLocalScope(fromDescriptor, qualifiedName.single(), resolutionFacade)
        if (!localResult.isEmpty()) {
            return localResult
        }
    }

    var result: Collection<DeclarationDescriptor> = listOf(fromDescriptor)
    qualifiedName.forEach { nameComponent ->
        if (result.size() != 1) return listOf()
        val scope = getResolutionScope(resolutionFacade, result.first())
        result = scope.getDescriptors().filter { it.getName().asString() == nameComponent }
    }

    return result
}

private fun resolveInLocalScope(fromDescriptor: DeclarationDescriptor,
                                name: String,
                                resolutionFacade: ResolutionFacade): List<DeclarationDescriptor> {
    val scope = getResolutionScope(resolutionFacade, fromDescriptor)
    return scope.getDescriptors().filter {
        it.getName().asString() == name && it.getContainingDeclaration() == fromDescriptor
    }
}

public fun getParamDescriptors(fromDescriptor: DeclarationDescriptor): List<DeclarationDescriptor> {
    // TODO resolve parameters of functions passed as parameters
    when (fromDescriptor) {
        is CallableDescriptor ->
            return fromDescriptor.getValueParameters()
        is ClassifierDescriptor -> {
            val typeParams = fromDescriptor.getTypeConstructor().getParameters()
            if (fromDescriptor is ClassDescriptor) {
                val constructorDescriptor = fromDescriptor.getUnsubstitutedPrimaryConstructor()
                if (constructorDescriptor != null) {
                    return typeParams + constructorDescriptor.getValueParameters()
                }
            }
            return typeParams
        }
    }

    return listOf()
}

private fun resolveParamLink(fromDescriptor: DeclarationDescriptor, qualifiedName: List<String>): List<DeclarationDescriptor> {
    val name = qualifiedName.singleOrNull() ?: return listOf()
    return getParamDescriptors(fromDescriptor).filter { it.getName().asString() == name }
}

private fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): JetScope {
    return descriptor.getContainingDeclaration().getPackage(descriptor.fqName).memberScope
}

private fun getClassInnerScope(outerScope: JetScope, descriptor: ClassDescriptor): JetScope {
    val redeclarationHandler = RedeclarationHandler.DO_NOTHING

    val headerScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Class ${descriptor.getName()} header scope")
    for (typeParameter in descriptor.getTypeConstructor().getParameters()) {
        headerScope.addClassifierDescriptor(typeParameter)
    }
    for (constructor in descriptor.getConstructors()) {
        headerScope.addFunctionDescriptor(constructor)
    }
    headerScope.changeLockLevel(WritableScope.LockLevel.READING)

    val classScope = ChainedScope(descriptor, "Class ${descriptor.getName()} scope", descriptor.getDefaultType().getMemberScope(), headerScope)
    return classScope
}

public fun getResolutionScope(resolutionFacade: ResolutionFacade, descriptor: DeclarationDescriptor): JetScope {
    return when (descriptor) {
        is PackageFragmentDescriptor ->
            getPackageInnerScope(descriptor)

        is PackageViewDescriptor ->
            descriptor.memberScope

        is ClassDescriptor ->
            getClassInnerScope(getOuterScope(descriptor, resolutionFacade), descriptor)

        is FunctionDescriptor ->
            FunctionDescriptorUtil.getFunctionInnerScope(getOuterScope(descriptor, resolutionFacade),
                                                         descriptor, RedeclarationHandler.DO_NOTHING)

        is PropertyDescriptor ->
            JetScopeUtils.getPropertyDeclarationInnerScope(descriptor,
                                                           getOuterScope(descriptor, resolutionFacade),
                                                           RedeclarationHandler.DO_NOTHING)

        is DeclarationDescriptorNonRoot ->
            getOuterScope(descriptor, resolutionFacade)

        else -> throw IllegalArgumentException("Cannot find resolution scope for root $descriptor")
    }
}

private fun getOuterScope(descriptor: DeclarationDescriptorWithSource, resolutionFacade: ResolutionFacade): JetScope {
    val parent = descriptor.getContainingDeclaration()
    if (parent is PackageFragmentDescriptor) {
        val containingFile = (descriptor.getSource() as? PsiSourceElement)?.psi?.getContainingFile() as? JetFile
        if (containingFile != null) {
            return resolutionFacade.getFileTopLevelScope(containingFile)
        }
    }
    return getResolutionScope(resolutionFacade, parent!!)
}
