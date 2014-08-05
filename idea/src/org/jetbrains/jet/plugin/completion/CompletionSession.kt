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
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.caches.JetShortNamesCache
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.codeInsight.TipsManager
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies

class CompletionSessionConfiguration(
        val completeNonImportedDeclarations: Boolean,
        val completeNonAccessibleDeclarations: Boolean)

fun CompletionSessionConfiguration(parameters: CompletionParameters) = CompletionSessionConfiguration(
        completeNonImportedDeclarations = parameters.getInvocationCount() >= 2,
        completeNonAccessibleDeclarations = parameters.getInvocationCount() >= 2)

abstract class CompletionSessionBase(protected val configuration: CompletionSessionConfiguration,
                                     protected val parameters: CompletionParameters,
                                     resultSet: CompletionResultSet) {

    protected val position: PsiElement = parameters.getPosition()
    protected val jetReference: JetSimpleNameReference? = position.getParent()?.getReferences()?.filterIsInstance(javaClass<JetSimpleNameReference>())?.firstOrNull()
    protected val resolveSession: ResolveSessionForBodies = (position.getContainingFile() as JetFile).getLazyResolveSession()
    protected val bindingContext: BindingContext? = jetReference?.let { resolveSession.resolveToElement(it.expression) }
    protected val inDescriptor: DeclarationDescriptor? = jetReference?.let { bindingContext!!.get(BindingContext.RESOLUTION_SCOPE, it.expression)?.getContainingDeclaration() }

    // set prefix matcher here to override default one which relies on CompletionUtil.findReferencePrefix()
    // which sometimes works incorrectly for Kotlin
    protected val resultSet: CompletionResultSet = resultSet
            .withPrefixMatcher(CompletionUtil.findJavaIdentifierPrefix(parameters))
            .addKotlinSorting(parameters)

    protected val prefixMatcher: PrefixMatcher = this.resultSet.getPrefixMatcher()

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (configuration.completeNonAccessibleDeclarations) return true

        if (descriptor is DeclarationDescriptorWithVisibility && inDescriptor != null) {
            return Visibilities.isVisible(descriptor as DeclarationDescriptorWithVisibility, inDescriptor)
        }

        return true
    }
}

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    private val collector: LookupElementsCollector = LookupElementsCollector(prefixMatcher, resolveSession, { isVisibleDescriptor(it) })

    public fun complete(): Boolean {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        collectElements()

        collector.flushToResultSet(resultSet)

        return !collector.isEmpty
    }

    private fun collectElements() {
        if (!NamedParametersCompletion.isOnlyNamedParameterExpected(position)) {
            if (jetReference != null && !isOnlyKeywordCompletion()) {
                if (shouldRunOnlyTypeCompletion()) {
                    if (configuration.completeNonImportedDeclarations) {
                        TypesCompletion(parameters, resolveSession, prefixMatcher).addAllTypes(collector)
                    }
                    else {
                        addReferenceVariants { isPartOfTypeDeclaration(it) }
                        JavaCompletionContributor.advertiseSecondCompletion(position.getProject(), resultSet)
                    }
                }
                else {
                    addReferenceVariants { true }

                    addNonImported()
                }
            }

            KeywordCompletion().complete(parameters, collector)
        }

        NamedParametersCompletion.complete(position, collector)
    }

    private fun addNonImported() {
        val prefix = prefixMatcher.getPrefix()

        // Try to avoid computing not-imported descriptors for empty prefix
        if (prefix.isEmpty()) {
            if (!configuration.completeNonImportedDeclarations) return

            if (PsiTreeUtil.getParentOfType(jetReference!!.expression, javaClass<JetDotQualifiedExpression>()) == null) return
        }

        if (shouldRunTopLevelCompletion()) {
            TypesCompletion(parameters, resolveSession, prefixMatcher).addAllTypes(collector)
            addJetTopLevelFunctions()
            addJetTopLevelObjects()
        }

        if (shouldRunExtensionsCompletion()) {
            addJetExtensions()
        }
    }

    private fun isOnlyKeywordCompletion()
            = PsiTreeUtil.getParentOfType(position, javaClass<JetModifierList>()) != null

    private fun addJetExtensions() {
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        collector.addDescriptorElements(namesCache.getJetCallableExtensions({ prefixMatcher.prefixMatches(it!!) }, jetReference!!.expression, resolveSession, GlobalSearchScope.allScope(project)))
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
        val actualPrefix = prefixMatcher.getPrefix()
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val functionNames = namesCache.getAllTopLevelFunctionNames()

        // TODO: Fix complete extension not only on contains
        for (name in functionNames) {
            if (name.contains(actualPrefix)) {
                collector.addDescriptorElements(namesCache.getTopLevelFunctionDescriptorsByName(name, jetReference!!.expression, resolveSession, scope))
            }
        }
    }

    private fun addJetTopLevelObjects() {
        val project = position.getProject()
        val namesCache = JetShortNamesCache.getKotlinInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        val objectNames = namesCache.getAllTopLevelObjectNames()

        for (name in objectNames) {
            if (prefixMatcher.prefixMatches(name)) {
                collector.addDescriptorElements(namesCache.getTopLevelObjectsByName(name, jetReference!!.expression, resolveSession, scope))
            }
        }
    }

    private fun shouldRunOnlyTypeCompletion(): Boolean {
        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = PsiTreeUtil.getParentOfType(position, javaClass<JetTypeReference>())
        if (typeReference != null) {
            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            return firstPartReference == jetReference!!.expression
        }

        return false
    }

    private fun shouldRunTopLevelCompletion(): Boolean {
        if (!configuration.completeNonImportedDeclarations) {
            return false
        }

        if (position.getNode()!!.getElementType() == JetTokens.IDENTIFIER) {
            val parent = position.getParent()
            if (parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)) return true
        }

        return false
    }

    private fun shouldRunExtensionsCompletion(): Boolean {
        return configuration.completeNonImportedDeclarations || prefixMatcher.getPrefix().length >= 3
    }

    private fun addReferenceVariants(filterCondition: (DeclarationDescriptor) -> Boolean) {
        val descriptors = TipsManager.getReferenceVariants(jetReference!!.expression, bindingContext!!)
        collector.addDescriptorElements(descriptors.filter { filterCondition(it) })
    }
}

class SmartCompletionSession(configuration: CompletionSessionConfiguration, parameters: CompletionParameters, resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {
    public fun complete() {
        if (jetReference != null) {
            val descriptors = TipsManager.getReferenceVariants(jetReference.expression, bindingContext!!)
            val completion = SmartCompletion(jetReference.expression, resolveSession, { isVisibleDescriptor(it) }, parameters.getOriginalFile() as JetFile)
            completion.buildLookupElements(descriptors)?.forEach { resultSet.addElement(it) }
        }
    }
}