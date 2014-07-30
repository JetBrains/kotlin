/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.caches.JetShortNamesCache
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.codeInsight.TipsManager
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion
import org.jetbrains.jet.plugin.references.JetSimpleNameReference

class CompletionSession(public val parameters: CompletionParameters,
                        resultSet: CompletionResultSet,
                        private val jetReference: JetSimpleNameReference,
                        position: PsiElement) {

    private val resolveSession = (position.getContainingFile() as JetFile).getLazyResolveSession()
    private val bindingContext = resolveSession.resolveToElement(jetReference.expression)
    private val position = parameters.getPosition()

    private val inDescriptor: DeclarationDescriptor? = bindingContext.get(BindingContext.RESOLUTION_SCOPE, jetReference.expression)?.getContainingDeclaration()

    // set prefix matcher here to override default one which relies on CompletionUtil.findReferencePrefix()
    // which sometimes works incorrectly for Kotlin
    private val resultSet = resultSet
            .withPrefixMatcher(CompletionUtil.findJavaIdentifierPrefix(parameters))
            .addKotlinSorting(parameters)

    public val result: CompletionResultSetWrapper = CompletionResultSetWrapper(this.resultSet, resolveSession, { isVisibleDescriptor(it) })

    public fun completeForReference() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        if (isOnlyKeywordCompletion(position)) return

        if (shouldRunOnlyTypeCompletion()) {
            if (parameters.getInvocationCount() >= 2) {
                TypesCompletion(parameters, resolveSession).addAllTypes(result)
            }
            else {
                addReferenceVariants { isPartOfTypeDeclaration(it) }
                JavaCompletionContributor.advertiseSecondCompletion(parameters.getPosition().getProject(), resultSet)
            }

            return
        }

        addReferenceVariants { true }

        val prefix = result.prefixMatcher.getPrefix()

        // Try to avoid computing not-imported descriptors for empty prefix
        if (prefix.isEmpty()) {
            if (parameters.getInvocationCount() < 2) return

            if (PsiTreeUtil.getParentOfType(jetReference.expression, javaClass<JetDotQualifiedExpression>()) == null) return
        }

        if (shouldRunTopLevelCompletion()) {
            TypesCompletion(parameters, resolveSession).addAllTypes(result)
            addJetTopLevelFunctions()
            addJetTopLevelObjects()
        }

        if (shouldRunExtensionsCompletion()) {
            addJetExtensions()
        }
    }

    public fun completeSmart() {
        assert(parameters.getCompletionType() == CompletionType.SMART)

        val descriptors = TipsManager.getReferenceVariants(jetReference.expression, bindingContext)
        val completion = SmartCompletion(jetReference.expression, resolveSession, { isVisibleDescriptor(it) }, parameters.getOriginalFile() as JetFile)
        completion.buildLookupElements(descriptors)?.forEach { result.addElement(it) }
    }

    private fun isOnlyKeywordCompletion(position: PsiElement)
            = PsiTreeUtil.getParentOfType(position, javaClass<JetModifierList>()) != null

    private fun addJetExtensions() {
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        result.addDescriptorElements(namesCache.getJetCallableExtensions({ result.prefixMatcher.prefixMatches(it!!) }, jetReference.expression, resolveSession, GlobalSearchScope.allScope(project)))
    }

    private fun isPartOfTypeDeclaration(descriptor: DeclarationDescriptor): Boolean {
        return when (descriptor) {
            is PackageViewDescriptor, is TypeParameterDescriptor -> true

            is ClassDescriptor -> {
                val kind = descriptor.getKind()
                KotlinBuiltIns.getInstance().isUnit(descriptor.getDefaultType()) ||
                        kind != ClassKind.OBJECT && kind != ClassKind.CLASS_OBJECT
            }

            else -> false
        }
    }

    private fun addJetTopLevelFunctions() {
        val actualPrefix = result.prefixMatcher.getPrefix()
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val functionNames = namesCache.getAllTopLevelFunctionNames()

        // TODO: Fix complete extension not only on contains
        for (name in functionNames) {
            if (name.contains(actualPrefix)) {
                result.addDescriptorElements(namesCache.getTopLevelFunctionDescriptorsByName(name, jetReference.expression, resolveSession, scope))
            }
        }
    }

    private fun addJetTopLevelObjects() {
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val objectNames = namesCache.getAllTopLevelObjectNames()

        for (name in objectNames) {
            if (result.prefixMatcher.prefixMatches(name)) {
                result.addDescriptorElements(namesCache.getTopLevelObjectsByName(name, jetReference.expression, resolveSession, scope))
            }
        }
    }

    private fun shouldRunOnlyTypeCompletion(): Boolean {
        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = PsiTreeUtil.getParentOfType(position, javaClass<JetTypeReference>())
        if (typeReference != null) {
            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            return firstPartReference == jetReference.expression
        }

        return false
    }

    private fun shouldRunTopLevelCompletion(): Boolean {
        if (parameters.getInvocationCount() < 2) {
            return false
        }

        if (position.getNode()!!.getElementType() == JetTokens.IDENTIFIER) {
            val parent = position.getParent()
            if (parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)) return true
        }

        return false
    }

    private fun shouldRunExtensionsCompletion(): Boolean {
        return parameters.getInvocationCount() > 1 || result.prefixMatcher.getPrefix().length >= 3
    }

    private fun addReferenceVariants(filterCondition: (DeclarationDescriptor) -> Boolean) {
        val descriptors = TipsManager.getReferenceVariants(jetReference.expression, bindingContext)
        result.addDescriptorElements(descriptors.filter { filterCondition(it) })
    }

    private fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        // Show everything if user insist on showing completion list
        if (parameters.getInvocationCount() >= 2) return true

        if (descriptor is DeclarationDescriptorWithVisibility && inDescriptor != null) {
            return Visibilities.isVisible(descriptor as DeclarationDescriptorWithVisibility, inDescriptor)
        }

        return true
    }
}
