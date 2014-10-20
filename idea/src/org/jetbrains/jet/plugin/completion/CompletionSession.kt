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
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.codeInsight.TipsManager
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.caches.KotlinIndicesHelper
import com.intellij.openapi.project.Project
import org.jetbrains.jet.plugin.search.searchScopeForSourceElementDependencies

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

    protected val collector: LookupElementsCollector = LookupElementsCollector(prefixMatcher, resolveSession, { isVisibleDescriptor(it) })

    protected val project: Project = position.getProject()
    protected val indicesHelper: KotlinIndicesHelper = KotlinIndicesHelper(project)
    protected val searchScope: GlobalSearchScope = searchScopeForSourceElementDependencies(parameters.getOriginalFile()) ?: GlobalSearchScope.EMPTY_SCOPE

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (configuration.completeNonAccessibleDeclarations) return true

        if (descriptor is DeclarationDescriptorWithVisibility && inDescriptor != null) {
            return Visibilities.isVisible(descriptor, inDescriptor)
        }

        return true
    }

    protected fun flushToResultSet() {
        collector.flushToResultSet(resultSet)
    }

    public fun complete(): Boolean {
        doComplete()
        flushToResultSet()
        return !collector.isResultEmpty
    }

    protected abstract fun doComplete()

    protected fun shouldRunTopLevelCompletion(): Boolean {
        if (!configuration.completeNonImportedDeclarations) return false

        if (position.getNode()!!.getElementType() == JetTokens.IDENTIFIER) {
            val parent = position.getParent()
            if (parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)) return true
        }

        return false
    }

    protected fun shouldRunExtensionsCompletion(): Boolean {
        return configuration.completeNonImportedDeclarations || prefixMatcher.getPrefix().length >= 3
    }

    protected fun getKotlinTopLevelDeclarations(): Collection<DeclarationDescriptor> {
        val filter = { (name: String) -> prefixMatcher.prefixMatches(name) }
        return (indicesHelper.getTopLevelCallables(filter, jetReference!!.expression, resolveSession, searchScope) +
                   indicesHelper.getTopLevelObjects(filter, resolveSession, searchScope)).filter { isVisibleDescriptor(it) }
    }

    protected fun getKotlinExtensions(): Collection<CallableDescriptor> {
        return indicesHelper.getCallableExtensions({ prefixMatcher.prefixMatches(it) }, jetReference!!.expression, resolveSession, searchScope).filter { isVisibleDescriptor(it) }
    }

    protected fun addAllTypes() {
        TypesCompletion(parameters, resolveSession, prefixMatcher, { isVisibleDescriptor(it) }).addAllTypes(collector)
    }
}

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        if (!NamedParametersCompletion.isOnlyNamedParameterExpected(position)) {
            val completeReference = jetReference != null && !isOnlyKeywordCompletion()

            if (completeReference) {
                if (shouldRunOnlyTypeCompletion()) {
                    if (configuration.completeNonImportedDeclarations) {
                        addAllTypes()
                    }
                    else {
                        addReferenceVariants { isPartOfTypeDeclaration(it) }
                        JavaCompletionContributor.advertiseSecondCompletion(project, resultSet)
                    }
                }
                else {
                    addReferenceVariants()
                }
            }

            KeywordCompletion.complete(parameters, prefixMatcher.getPrefix(), collector)

            if (completeReference && !shouldRunOnlyTypeCompletion()) {
                flushToResultSet()
                addNonImported()
            }
        }

        NamedParametersCompletion.complete(position, collector)
    }

    private fun addNonImported() {
        if (shouldRunTopLevelCompletion()) {
            addAllTypes()
            collector.addDescriptorElements(getKotlinTopLevelDeclarations(), suppressAutoInsertion = true)
        }

        if (shouldRunExtensionsCompletion()) {
            collector.addDescriptorElements(getKotlinExtensions(), suppressAutoInsertion = true)
        }
    }

    private fun isOnlyKeywordCompletion()
            = PsiTreeUtil.getParentOfType(position, javaClass<JetModifierList>()) != null

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

    private fun addReferenceVariants(filterCondition: (DeclarationDescriptor) -> Boolean = { true }) {
        val descriptors = TipsManager.getReferenceVariants(jetReference!!.expression, bindingContext!!)
        collector.addDescriptorElements(descriptors.filter { filterCondition(it) }, suppressAutoInsertion = false)
    }
}

class SmartCompletionSession(configuration: CompletionSessionConfiguration, parameters: CompletionParameters, resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    override fun doComplete() {
        if (jetReference != null) {
            val completion = SmartCompletion(jetReference.expression, resolveSession, { isVisibleDescriptor(it) }, parameters.getOriginalFile() as JetFile)
            val result = completion.execute()
            if (result != null) {
                collector.addElements(result.additionalItems)

                val filter = result.declarationFilter
                if (filter != null) {
                    TipsManager.getReferenceVariants(jetReference.expression, bindingContext!!)
                            .forEach { if (prefixMatcher.prefixMatches(it.getName().asString()) && isVisibleDescriptor(it)) collector.addElements(filter(it)) }

                    flushToResultSet()

                    processNonImported { collector.addElements(filter(it)) }
                }
            }
        }
    }

    private fun processNonImported(processor: (DeclarationDescriptor) -> Unit) {
        if (shouldRunTopLevelCompletion()) {
            getKotlinTopLevelDeclarations().forEach(processor)
        }

        if (shouldRunExtensionsCompletion()) {
            getKotlinExtensions().forEach(processor)
        }
    }
}