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

import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.JetMultiReference
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorNonRoot
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.resolve.scopes.RedeclarationHandler
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.resolve.scopes.ChainedScope
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.scopes.JetScopeUtils
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource

public class KDocReference(element: KDocName): JetMultiReference<KDocName>(element) {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val cacheService = KotlinCacheService.getInstance(getElement().getProject())
        val session = cacheService.getLazyResolveSession(getElement())
        val declaration = getElement().getContainingDoc().getOwner()
        if (declaration == null) {
            return arrayListOf()
        }
        val declarationDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration)
        val kdocLink = getElement().getStrictParentOfType<KDocLink>()!!
        return resolveKDocLink(session, declarationDescriptor, kdocLink.getTagIfSubject(), getElement().getQualifiedName())
    }

    override fun getRangeInElement(): TextRange = getElement().getNameTextRange()

    override fun canRename(): Boolean = true

    override fun handleElementRename(newElementName: String?): PsiElement? {
        val textRange = getElement().getNameTextRange()
        val newText = textRange.replace(getElement().getText(), newElementName)
        val newLink = KDocElementFactory(getElement().getProject()).createNameFromText(newText)
        return getElement().replace(newLink)
    }
}

public fun resolveKDocLink(session: KotlinCodeAnalyzer,
                           fromDescriptor: DeclarationDescriptor,
                           fromSubjectOfTag: KDocTag?,
                           qualifiedName: List<String>): Collection<DeclarationDescriptor> {
    if (fromSubjectOfTag?.getName() == "param") {
        return resolveParamLink(fromDescriptor, qualifiedName)
    }

    var result: Collection<DeclarationDescriptor> = listOf(fromDescriptor)
    qualifiedName.forEach { nameComponent ->
        if (result.size() != 1) return listOf()
        val scope = getResolutionScope(session, result.first())
        result = scope.getDescriptors().filter { it.getName().asString() == nameComponent }
    }

    return result
}

private fun resolveParamLink(fromDescriptor: DeclarationDescriptor, qualifiedName: List<String>): List<DeclarationDescriptor> {
    if (qualifiedName.size() != 1) {
        return listOf()
    }
    if (fromDescriptor is CallableDescriptor) {
        val valueParams = fromDescriptor.getValueParameters().filter { it.getName().asString() == qualifiedName[0] }
        if (valueParams.isNotEmpty()) {
            return valueParams
        }
    }
    if (fromDescriptor is ClassifierDescriptor) {
        val typeParams = fromDescriptor.getTypeConstructor().getParameters().filter { it.getName().asString() == qualifiedName[0] }
        if (typeParams.isNotEmpty()) {
            return typeParams
        }
    }

    return listOf()
}

private fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): JetScope {
    val module = descriptor.getContainingDeclaration()
    val packageView = module.getPackage(descriptor.fqName)
    val packageScope = packageView!!.getMemberScope()
    return packageScope
}

private fun getClassInnerScope(outerScope: JetScope, descriptor: ClassDescriptor): JetScope {
    val redeclarationHandler = RedeclarationHandler.DO_NOTHING

    val headerScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Class ${descriptor.getName()} header scope")
    for (typeParameter in descriptor.getTypeConstructor().getParameters()) {
        headerScope.addTypeParameterDescriptor(typeParameter)
    }
    for (constructor in descriptor.getConstructors()) {
        headerScope.addFunctionDescriptor(constructor)
    }
    headerScope.addLabeledDeclaration(descriptor)
    headerScope.changeLockLevel(WritableScope.LockLevel.READING)

    val classScope = ChainedScope(descriptor, "Class ${descriptor.getName()} scope", descriptor.getDefaultType().getMemberScope(), headerScope)
    return classScope
}

private fun getResolutionScope(session: KotlinCodeAnalyzer, descriptor: DeclarationDescriptor): JetScope {
    when (descriptor) {
        is PackageFragmentDescriptor ->
            return getPackageInnerScope(descriptor)

        is PackageViewDescriptor ->
            return descriptor.getMemberScope()

        is ClassDescriptor ->
            return getClassInnerScope(getOuterScope(descriptor, session), descriptor)

        is FunctionDescriptor ->
            return FunctionDescriptorUtil.getFunctionInnerScope(getOuterScope(descriptor, session),
                                                                descriptor, RedeclarationHandler.DO_NOTHING)

        is PropertyDescriptor ->
            return JetScopeUtils.getPropertyDeclarationInnerScope(descriptor,
                                                                  getOuterScope(descriptor, session),
                                                                  RedeclarationHandler.DO_NOTHING)
    }

    if (descriptor is DeclarationDescriptorNonRoot)
        return getOuterScope(descriptor, session)

    throw IllegalArgumentException("Cannot find resolution scope for root $descriptor")
}

private fun getOuterScope(descriptor: DeclarationDescriptorWithSource, session: KotlinCodeAnalyzer): JetScope {
    val parent = descriptor.getContainingDeclaration()
    if (parent is PackageFragmentDescriptor) {
        val containingFile = (descriptor.getSource() as? PsiSourceElement)?.psi?.getContainingFile() as? JetFile
        if (containingFile != null) {
            return session.getScopeProvider().getFileScope(containingFile)
        }
    }
    return getResolutionScope(session, parent)
}
