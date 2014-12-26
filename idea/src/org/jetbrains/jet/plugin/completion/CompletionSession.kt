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
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.codeInsight.ReferenceVariantsHelper
import org.jetbrains.jet.plugin.completion.smart.SmartCompletion
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.plugin.caches.KotlinIndicesHelper
import com.intellij.openapi.project.Project
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptorKindExclude
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindFilter
import org.jetbrains.jet.lang.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.jet.plugin.refactoring.comparePossiblyOverridingDescriptors
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.psi.psiUtil.getStrictParentOfType
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.plugin.util.CallType
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindExclude
import org.jetbrains.jet.lang.resolve.lazy.BodyResolveMode
import com.intellij.patterns.StandardPatterns
import com.intellij.util.ProcessingContext
import com.intellij.patterns.PatternCondition

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
    private val file = position.getContainingFile() as JetFile
    protected val resolutionFacade: ResolutionFacade = file.getResolutionFacade()
    protected val moduleDescriptor: ModuleDescriptor = resolutionFacade.findModuleDescriptor(file)

    protected val reference: JetSimpleNameReference?
    protected val expression: JetExpression?

    ;{
        val reference = position.getParent()?.getReferences()?.firstIsInstanceOrNull<JetSimpleNameReference>()
        if (reference != null) {
            if (reference.expression is JetLabelReferenceExpression) {
                this.expression = reference.expression.getParent().getParent() as? JetExpressionWithLabel
                this.reference = null
            }
            else {
                this.expression = reference.expression
                this.reference = reference
            }
        }
        else {
            this.reference = null
            this.expression = null
        }
    }

    protected val bindingContext: BindingContext? = expression?.let { resolutionFacade.analyze(it, BodyResolveMode.PARTIAL_FOR_COMPLETION) }
    protected val inDescriptor: DeclarationDescriptor? = expression?.let { bindingContext!!.get(BindingContext.RESOLUTION_SCOPE, it)?.getContainingDeclaration() }

    protected val prefix: String = CompletionUtil.findIdentifierPrefix(
            parameters.getPosition().getContainingFile(),
            parameters.getOffset(),
            StandardPatterns.or(StandardPatterns.character().javaIdentifierPart(),
                                StandardPatterns.character().with(
                                        object : PatternCondition<Char>("@") {
                                            override fun accepts(c: Char?, context: ProcessingContext?): Boolean {
                                                return c == '@'
                                            }
                                        })
            ),
            StandardPatterns.character().javaIdentifierStart())

    protected val resultSet: CompletionResultSet = resultSet
            .withPrefixMatcher(prefix)
            .addKotlinSorting(parameters)

    protected val prefixMatcher: PrefixMatcher = this.resultSet.getPrefixMatcher()

    protected val referenceVariantsHelper: ReferenceVariantsHelper?
            = if (bindingContext != null) ReferenceVariantsHelper(bindingContext) { isVisibleDescriptor(it) } else null

    protected val lookupElementFactory: LookupElementFactory = run {
        val receiverTypes = if (reference != null) {
            val expression = reference.expression
            val (receivers, callType) = referenceVariantsHelper!!.getReferenceVariantsReceivers(expression)
            val dataFlowInfo = bindingContext!!.getDataFlowInfo(expression)
            var receiverTypes = receivers.flatMap {
                SmartCastUtils.getSmartCastVariantsWithLessSpecificExcluded(it, bindingContext, dataFlowInfo)
            }
            if (callType == CallType.SAFE) {
                receiverTypes = receiverTypes.map { it.makeNotNullable() }
            }
            receiverTypes
        }
        else {
            listOf()
        }
        LookupElementFactory(receiverTypes)
    }

    protected val collector: LookupElementsCollector = LookupElementsCollector(prefixMatcher, parameters, resolutionFacade, lookupElementFactory)

    protected val project: Project = position.getProject()


    protected val originalSearchScope: GlobalSearchScope = parameters.getOriginalFile().getResolveScope()

    // we need to exclude the original file from scope because our resolve session is built with this file replaced by synthetic one
    protected val searchScope: GlobalSearchScope = object : DelegatingGlobalSearchScope(originalSearchScope) {
        override fun contains(file: VirtualFile) = super.contains(file) && file != parameters.getOriginalFile().getVirtualFile()
    }

    protected val indicesHelper: KotlinIndicesHelper
        get() = KotlinIndicesHelper(project, resolutionFacade, bindingContext!!, searchScope, moduleDescriptor) { isVisibleDescriptor(it) }

    protected fun isVisibleDescriptor(descriptor: DeclarationDescriptor): Boolean {
        if (configuration.completeNonAccessibleDeclarations) return true

        if (descriptor is DeclarationDescriptorWithVisibility && inDescriptor != null) {
            return descriptor.isVisible(inDescriptor, bindingContext, reference?.expression)
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

    // set is used only for completion in code fragments
    private var alreadyAddedDescriptors: Collection<DeclarationDescriptor> by Delegates.notNull()

    fun getReferenceVariants(kindFilter: DescriptorKindFilter, shouldCastToRuntimeType: Boolean): Collection<DeclarationDescriptor> {
        val descriptors = referenceVariantsHelper!!.getReferenceVariants(reference!!.expression, kindFilter, shouldCastToRuntimeType, prefixMatcher.asNameFilter())
        if (!shouldCastToRuntimeType) {
            if (position.getContainingFile() is JetCodeFragment) {
                alreadyAddedDescriptors = descriptors
            }
            return descriptors
        }
        else {
            return descriptors.filter { desc ->
                !alreadyAddedDescriptors.any {
                    comparePossiblyOverridingDescriptors(it, desc)
                }
            }
        }
    }

    protected fun shouldRunTopLevelCompletion(): Boolean
            = configuration.completeNonImportedDeclarations && isNoQualifierContext()

    protected fun isNoQualifierContext(): Boolean {
        val parent = position.getParent()
        return parent is JetSimpleNameExpression && !JetPsiUtil.isSelectorInQualified(parent)
    }

    protected fun shouldRunExtensionsCompletion(): Boolean
            = configuration.completeNonImportedDeclarations || prefix.length() >= 3

    protected fun getKotlinTopLevelCallables(): Collection<DeclarationDescriptor>
            = indicesHelper.getTopLevelCallables({ prefixMatcher.prefixMatches(it) }, reference!!.expression)

    protected fun getKotlinExtensions(): Collection<CallableDescriptor>
            = indicesHelper.getCallableExtensions({ prefixMatcher.prefixMatches(it) }, reference!!.expression)

    protected fun addAllClasses(kindFilter: (ClassKind) -> Boolean) {
        AllClassesCompletion(
                parameters, lookupElementFactory, resolutionFacade, bindingContext!!, moduleDescriptor,
                searchScope, prefixMatcher, kindFilter, { isVisibleDescriptor(it) }
        ).collect(collector)
    }
}

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        if (!NamedParametersCompletion.isOnlyNamedParameterExpected(position)) {
            val completeReference = reference != null && !isOnlyKeywordCompletion()
            val onlyTypes = completeReference && shouldRunOnlyTypeCompletion()

            val kindFilter = if (onlyTypes)
                DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude DescriptorKindExclude.EnumEntry
            else
                DescriptorKindFilter(DescriptorKindFilter.ALL_KINDS_MASK)

            if (completeReference) {
                addReferenceVariants(kindFilter, shouldCastToRuntimeType = false)
            }

            val ampIndex = prefix.indexOf("@")
            val keywordsPrefix = if (ampIndex < 0) prefix else prefix.substring(0, ampIndex) // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
            KeywordCompletion.complete(expression ?: parameters.getPosition(), keywordsPrefix) { lookupElement ->
                val keyword = lookupElement.getLookupString()
                when (keyword) {
                    // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                    "this" -> {
                        if (expression != null) {
                            collector.addElements(thisExpressionItems(bindingContext!!, expression).map { it.factory() })
                        }
                    }

                    // if "return" is parsed correctly in the current context - insert it and all return@xxx items
                    "return" -> {
                        if (expression != null) {
                            collector.addElements(returnExpressionItems(bindingContext!!, expression))
                        }
                    }

                    "break", "continue" -> {
                        if (expression != null) {
                            collector.addElements(breakOrContinueExpressionItems(expression, keyword))
                        }
                    }

                    else -> collector.addElement(lookupElement)
                }
            }

            if (completeReference) {
                if (!configuration.completeNonImportedDeclarations && isNoQualifierContext()) {
                    JavaCompletionContributor.advertiseSecondCompletion(project, resultSet)
                }

                flushToResultSet()
                addNonImported(onlyTypes)
            }

            if (completeReference && position.getContainingFile() is JetCodeFragment) {
                flushToResultSet()
                addReferenceVariants(kindFilter, shouldCastToRuntimeType = true)
            }
        }

        NamedParametersCompletion.complete(position, collector)
    }

    private fun addNonImported(onlyTypes: Boolean) {
        if (shouldRunTopLevelCompletion()) {
            addAllClasses { it != ClassKind.ENUM_ENTRY }

            if (!onlyTypes) {
                collector.addDescriptorElements(getKotlinTopLevelCallables(), suppressAutoInsertion = true)
            }
        }

        if (!onlyTypes && shouldRunExtensionsCompletion()) {
            collector.addDescriptorElements(getKotlinExtensions(), suppressAutoInsertion = true)
        }
    }

    private fun isOnlyKeywordCompletion()
            = position.getStrictParentOfType<JetModifierList>() != null

    private fun shouldRunOnlyTypeCompletion(): Boolean {
        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = position.getStrictParentOfType<JetTypeReference>()
        if (typeReference != null) {
            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            return firstPartReference == reference!!.expression
        }

        return false
    }

    private fun addReferenceVariants(kindFilter: DescriptorKindFilter, shouldCastToRuntimeType: Boolean) {
        collector.addDescriptorElements(
                getReferenceVariants(kindFilter, shouldCastToRuntimeType),
                suppressAutoInsertion = false,
                shouldCastToRuntimeType = shouldCastToRuntimeType)
    }
}

class SmartCompletionSession(configuration: CompletionSessionConfiguration, parameters: CompletionParameters, resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

    // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
    private val DESCRIPTOR_KIND_MASK = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

    override fun doComplete() {
        if (expression != null) {
            val mapper = ToFromOriginalFileMapper(parameters.getOriginalFile() as JetFile, position.getContainingFile() as JetFile, parameters.getOffset())
            val completion = SmartCompletion(expression, resolutionFacade, moduleDescriptor,
                                             bindingContext!!, { isVisibleDescriptor(it) }, prefixMatcher, originalSearchScope,
                                             mapper, lookupElementFactory)
            val result = completion.execute()
            if (result != null) {
                collector.addElements(result.additionalItems)

                if (reference != null) {
                    val filter = result.declarationFilter
                    if (filter != null) {
                        getReferenceVariants(DESCRIPTOR_KIND_MASK, false).forEach { collector.addElements(filter(it)) }
                        flushToResultSet()

                        processNonImported { collector.addElements(filter(it)) }
                        flushToResultSet()

                        if (position.getContainingFile() is JetCodeFragment) {
                            getReferenceVariants(DESCRIPTOR_KIND_MASK, true).forEach { collector.addElementsWithReceiverCast(filter(it)) }
                            flushToResultSet()
                        }
                    }

                    // it makes no sense to search inheritors if there is no reference because it means that we have prefix like "this@"
                    result.inheritanceSearcher?.search({ prefixMatcher.prefixMatches(it) }) {
                        collector.addElement(it)
                        flushToResultSet()
                    }
                }
            }
        }
    }

    private fun processNonImported(processor: (DeclarationDescriptor) -> Unit) {
        if (shouldRunTopLevelCompletion()) {
            getKotlinTopLevelCallables().forEach(processor)
        }

        if (shouldRunExtensionsCompletion()) {
            getKotlinExtensions().forEach(processor)
        }
    }
}