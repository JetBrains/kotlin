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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.impl.BetterPrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.completion.handlers.createKeywordConstructLookupElement
import org.jetbrains.kotlin.idea.completion.smart.ExpectedInfoMatch
import org.jetbrains.kotlin.idea.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.util.supertypesWithAny
import org.jetbrains.kotlin.utils.addToStdlib.check
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

class BasicCompletionSession(
        configuration: CompletionSessionConfiguration,
        parameters: CompletionParameters,
        toFromOriginalFileMapper: ToFromOriginalFileMapper,
        resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {

    private interface CompletionKind {
        val descriptorKindFilter: DescriptorKindFilter?

        fun doComplete()

        fun shouldDisableAutoPopup() = false

        fun addWeighers(sorter: CompletionSorter) = sorter
    }

    private val completionKind by lazy { detectCompletionKind() }

    override val descriptorKindFilter: DescriptorKindFilter?
        get() = completionKind.descriptorKindFilter

    private val smartCompletion = expression?.let {
        SmartCompletion(
                it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter, indicesHelper(false), prefixMatcher,
                GlobalSearchScope.EMPTY_SCOPE, toFromOriginalFileMapper, callTypeAndReceiver,
                isJvmModule, forBasicCompletion = true
        )
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    private fun detectCompletionKind(): CompletionKind {
        if (nameExpression == null) {
            return when  {
                (position.parent as? KtNamedDeclaration)?.nameIdentifier == position -> DECLARATION_NAME

                else -> KEYWORDS_ONLY
            }
        }

        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression)) {
            return NAMED_ARGUMENTS_ONLY
        }

        if (nameExpression.getStrictParentOfType<KtSuperExpression>() != null) {
            return SUPER_QUALIFIER
        }

        return ALL
    }

    fun shouldDisableAutoPopup(): Boolean
            = completionKind.shouldDisableAutoPopup()

    override fun shouldCompleteTopLevelCallablesFromIndex()
            = super.shouldCompleteTopLevelCallablesFromIndex() && prefix.isNotEmpty()

    override fun doComplete() {
        assert(parameters.completionType == CompletionType.BASIC)

        if (parameters.isAutoPopup) {
            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(LookupCancelWatcher.AUTO_POPUP_AT, position.startOffset)
                lookupElement
            }

            if (isInFunctionLiteralStart(position)) {
                collector.addLookupElementPostProcessor { lookupElement ->
                    lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
                    lookupElement
                }
            }
        }

        completionKind.doComplete()
    }

    private fun isInFunctionLiteralStart(position: PsiElement): Boolean {
        var prev = position.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        if (prev?.node?.elementType == KtTokens.LPAR) {
            prev = prev?.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }
        }
        if (prev?.node?.elementType != KtTokens.LBRACE) return false
        val functionLiteral = prev!!.parent as? KtFunctionLiteral ?: return false
        return functionLiteral.lBrace == prev
    }

    override fun createSorter(): CompletionSorter {
        var sorter = super.createSorter()

        if (smartCompletion != null) {
            val smartCompletionInBasicWeigher = SmartCompletionInBasicWeigher(smartCompletion, callTypeAndReceiver.callType, resolutionFacade)
            sorter = sorter.weighBefore(KindWeigher.toString(),
                                        smartCompletionInBasicWeigher,
                                        SmartCompletionPriorityWeigher,
                                        CallableReferenceWeigher(callTypeAndReceiver.callType))
        }

        sorter = completionKind.addWeighers(sorter)

        return sorter
    }

    private val ALL = object : CompletionKind {
        override val descriptorKindFilter: DescriptorKindFilter? by lazy {
            var filter = callTypeAndReceiver.callType.descriptorKindFilter

            if (filter.kindMask.and(DescriptorKindFilter.PACKAGES_MASK) != 0) {
                filter = filter.exclude(DescriptorKindExclude.TopLevelPackages)
            }

            if (callTypeAndReceiver.shouldCompleteCallableExtensions()) {
                filter = filter.exclude(TopLevelExtensionsExclude) // completed via indices
            }

            filter
        }

        override fun doComplete() {
            val declaration = isStartOfExtensionReceiverFor()
            if (declaration != null) {
                completeDeclarationNameFromUnresolvedOrOverride(declaration)

                // no auto-popup on typing after "val", "var" and "fun" because it's likely the name of the declaration which is being typed by user
                if (parameters.invocationCount == 0) {
                    val suppressOtherCompletion = when (declaration) {
                        is KtNamedFunction -> prefixMatcher.prefix.let { it.isEmpty() || it[0].isLowerCase() /* function name usually starts with lower case letter */ }
                        else -> true
                    }
                    if (suppressOtherCompletion) return
                }
            }

            fun completeWithSmartCompletion(lookupElementFactory: LookupElementFactory) {
                if (smartCompletion != null) {
                    val (additionalItems, @Suppress("UNUSED_VARIABLE") inheritanceSearcher) = smartCompletion.additionalItems(lookupElementFactory)

                    // all additional items should have SMART_COMPLETION_ITEM_PRIORITY_KEY to be recognized by SmartCompletionInBasicWeigher
                    for (item in additionalItems) {
                        if (item.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) == null) {
                            item.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.DEFAULT)
                        }
                    }

                    collector.addElements(additionalItems)
                }
            }

            val contextVariableTypesForSmartCompletion = withCollectRequiredContextVariableTypes(::completeWithSmartCompletion)

            val contextVariableTypesForReferenceVariants = withCollectRequiredContextVariableTypes { lookupElementFactory ->
                val (imported, notImported) = referenceVariantsWithNonInitializedVarExcluded!!
                collector.addDescriptorElements(imported, lookupElementFactory)
                collector.addDescriptorElements(notImported, lookupElementFactory, notImported = true)
            }

            KEYWORDS_ONLY.doComplete()

            // getting root packages from scope is very slow so we do this in alternative way
            if (callTypeAndReceiver.receiver == null && callTypeAndReceiver.callType.descriptorKindFilter.kindMask.and(DescriptorKindFilter.PACKAGES_MASK) != 0) {
                //TODO: move this code somewhere else?
                val packageNames = PackageIndexUtil.getSubPackageFqNames(FqName.ROOT, searchScope, project, prefixMatcher.asNameFilter())
                        .toMutableSet()

                if (!ProjectStructureUtil.isJsKotlinModule(parameters.originalFile as KtFile)) {
                    JavaPsiFacade.getInstance(project).findPackage("")?.getSubPackages(searchScope)?.forEach { psiPackage ->
                        val name = psiPackage.name
                        if (Name.isValidIdentifier(name!!)) {
                            packageNames.add(FqName(name))
                        }
                    }
                }

                packageNames.forEach { collector.addElement(basicLookupElementFactory.createLookupElementForPackage(it)) }
            }

            flushToResultSet()

            NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
            flushToResultSet()

            val contextVariablesProvider = RealContextVariablesProvider(referenceVariantsHelper, position)
            withContextVariablesProvider(contextVariablesProvider) { lookupElementFactory ->
                if (receiverTypes != null) {
                    ExtensionFunctionTypeValueCompletion(receiverTypes, callTypeAndReceiver.callType, lookupElementFactory)
                            .processVariables(contextVariablesProvider)
                            .forEach {
                                val lookupElements = it.factory.createStandardLookupElementsForDescriptor(it.invokeDescriptor, useReceiverTypes = true)
                                collector.addElements(lookupElements)
                            }
                }

                if (contextVariableTypesForSmartCompletion.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                    completeWithSmartCompletion(lookupElementFactory)
                }

                if (contextVariableTypesForReferenceVariants.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                    val (imported, notImported) = referenceVariantsWithSingleFunctionTypeParameter()!!
                    collector.addDescriptorElements(imported, lookupElementFactory)
                    collector.addDescriptorElements(notImported, lookupElementFactory, notImported = true)
                }

                val staticMembersCompletion = StaticMembersCompletion(
                        prefixMatcher, resolutionFacade, lookupElementFactory, referenceVariants!!.imported, isJvmModule)
                if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                    staticMembersCompletion.completeFromImports(file, collector)
                }

                completeNonImported(lookupElementFactory)
                flushToResultSet()

                if (isDebuggerContext) {
                    val variantsAndFactory = getRuntimeReceiverTypeReferenceVariants(lookupElementFactory)
                    if (variantsAndFactory != null) {
                        val variants = variantsAndFactory.first
                        @Suppress("NAME_SHADOWING") val lookupElementFactory = variantsAndFactory.second
                        collector.addDescriptorElements(variants.imported, lookupElementFactory, withReceiverCast = true)
                        collector.addDescriptorElements(variants.notImportedExtensions, lookupElementFactory, withReceiverCast = true, notImported = true)
                        flushToResultSet()
                    }
                }

                if (configuration.staticMembers && callTypeAndReceiver is CallTypeAndReceiver.DEFAULT && prefix.isNotEmpty()) {
                    staticMembersCompletion.completeFromIndices(indicesHelper(false), collector)
                }
            }
        }

        private fun completeNonImported(lookupElementFactory: LookupElementFactory) {
            if (shouldCompleteTopLevelCallablesFromIndex()) {
                processTopLevelCallables {
                    collector.addDescriptorElements(it, lookupElementFactory, notImported = true)
                    flushToResultSet()
                }
            }

            if (callTypeAndReceiver.receiver == null && prefix.isNotEmpty()) {
                val classKindFilter: ((ClassKind) -> Boolean)?
                when (callTypeAndReceiver) {
                    is CallTypeAndReceiver.ANNOTATION -> classKindFilter = { it == ClassKind.ANNOTATION_CLASS }
                    is CallTypeAndReceiver.DEFAULT, is CallTypeAndReceiver.TYPE -> classKindFilter = { it != ClassKind.ENUM_ENTRY }
                    else -> classKindFilter = null
                }
                if (classKindFilter != null) {
                    val prefixMatcher = if (configuration.useBetterPrefixMatcherForNonImportedClasses)
                        BetterPrefixMatcher(prefixMatcher, collector.bestMatchingDegree)
                    else
                        prefixMatcher
                    addClassesFromIndex(classKindFilter, prefixMatcher)
                }
            }
        }

        private fun isStartOfExtensionReceiverFor(): KtCallableDeclaration? {
            val userType = nameExpression!!.parent as? KtUserType ?: return null
            if (userType.qualifier != null) return null
            val typeRef = userType.parent as? KtTypeReference ?: return null
            if (userType != typeRef.typeElement) return null
            val parent = typeRef.parent
            return when (parent) {
                is KtNamedFunction -> parent.check { typeRef == it.receiverTypeReference }
                is KtProperty -> parent.check { typeRef == it.receiverTypeReference }
                else -> null
            }
        }
    }

    private val KEYWORDS_ONLY = object : CompletionKind {
        override val descriptorKindFilter: DescriptorKindFilter?
            get() = null

        override fun doComplete() {
            val keywordsToSkip = HashSet<String>()

            val keywordValueConsumer = object : KeywordValues.Consumer {
                override fun consume(lookupString: String, expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch, priority: SmartCompletionItemPriority, factory: () -> LookupElement) {
                    keywordsToSkip.add(lookupString)
                    val lookupElement = factory()
                    val matched = expectedInfos.any {
                        val match = expectedInfoMatcher(it)
                        assert(!match.makeNotNullable) { "Nullable keyword values not supported" }
                        match.isMatch()
                    }
                    if (matched) {
                        lookupElement.putUserData(SmartCompletionInBasicWeigher.KEYWORD_VALUE_MATCHED_KEY, Unit)
                        lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                    }
                    collector.addElement(lookupElement)
                }
            }
            KeywordValues.process(keywordValueConsumer, callTypeAndReceiver, bindingContext, resolutionFacade, moduleDescriptor, isJvmModule)

            val keywordsPrefix = prefix.substringBefore('@') // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
            val isUseSiteAnnotationTarget = position.prevLeaf()?.node?.elementType == KtTokens.AT
            KeywordCompletion.complete(expression ?: parameters.position, keywordsPrefix, isJvmModule) { lookupElement ->
                val keyword = lookupElement.lookupString
                if (keyword in keywordsToSkip) return@complete

                when (keyword) {
                // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                    "this" -> {
                        if (expression != null) {
                            collector.addElements(thisExpressionItems(bindingContext, expression, prefix, resolutionFacade).map { it.createLookupElement() })
                        }
                        else {
                            // for completion in secondary constructor delegation call
                            collector.addElement(lookupElement)
                        }
                    }

                // if "return" is parsed correctly in the current context - insert it and all return@xxx items
                    "return" -> {
                        if (expression != null) {
                            collector.addElements(returnExpressionItems(bindingContext, expression))
                        }
                    }

                    "break", "continue" -> {
                        if (expression != null) {
                            collector.addElements(breakOrContinueExpressionItems(expression, keyword))
                        }
                    }

                    "override" -> {
                        collector.addElement(lookupElement)

                        OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration = null)
                    }

                    "class" -> {
                        if (callTypeAndReceiver !is CallTypeAndReceiver.CALLABLE_REFERENCE) { // otherwise it should be handled by KeywordValues
                            collector.addElement(lookupElement)
                        }
                    }

                    "get" -> {
                        collector.addElement(lookupElement)

                        if (!isUseSiteAnnotationTarget) {
                            collector.addElement(createKeywordConstructLookupElement(project, keyword, "val v:Int get()=caret"))
                            collector.addElement(createKeywordConstructLookupElement(project, keyword, "val v:Int get(){caret}", trimSpacesAroundCaret = true))
                        }
                    }

                    "set" -> {
                        collector.addElement(lookupElement)

                        if (!isUseSiteAnnotationTarget) {
                            collector.addElement(createKeywordConstructLookupElement(project, keyword, "var v:Int set(value)=caret"))
                            collector.addElement(createKeywordConstructLookupElement(project, keyword, "var v:Int set(value){caret}", trimSpacesAroundCaret = true))
                        }
                    }

                    else -> collector.addElement(lookupElement)
                }
            }
        }
    }

    private val NAMED_ARGUMENTS_ONLY = object : CompletionKind {
        override val descriptorKindFilter: DescriptorKindFilter?
            get() = null

        override fun doComplete() {
            NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
        }
    }

    private val DECLARATION_NAME = object : CompletionKind {
        override val descriptorKindFilter: DescriptorKindFilter?
            get() = null

        override fun doComplete() {
            val declaration = declaration()
            if (declaration is KtParameter && !shouldCompleteParameterNameAndType()) return // do not complete also keywords and from unresolved references in such case

            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
                lookupElement
            }

            KEYWORDS_ONLY.doComplete()

            completeDeclarationNameFromUnresolvedOrOverride(declaration)

            when (declaration) {
                is KtParameter ->
                    completeParameterNameAndType()

                is KtClassOrObject -> {
                    if (declaration.isTopLevel()) {
                        completeTopLevelClassName()
                    }
                }
            }
        }

        override fun shouldDisableAutoPopup(): Boolean {
            if (TemplateManager.getInstance(project).getActiveTemplate(parameters.editor) != null) return true

            if (declaration() is KtParameter) {
                if (LookupCancelWatcher.getInstance(project).wasAutoPopupRecentlyCancelled(parameters.editor, position.startOffset)) {
                    return true
                }
            }

            return false
        }

        override fun addWeighers(sorter: CompletionSorter): CompletionSorter {
            if (shouldCompleteParameterNameAndType()) {
                return sorter.weighBefore(DeprecatedWeigher.toString(), ParameterNameAndTypeCompletion.Weigher)
            }
            return sorter
        }

        private fun completeParameterNameAndType() {
            val parameterNameAndTypeCompletion = ParameterNameAndTypeCompletion(collector, basicLookupElementFactory, prefixMatcher, resolutionFacade)

            // if we are typing parameter name, restart completion each time we type an upper case letter because new suggestions will appear (previous words can be used as user prefix)
            val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
            })
            collector.restartCompletionOnPrefixChange(prefixPattern)

            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(KotlinCompletionCharFilter.HIDE_LOOKUP_ON_COLON, Unit)
                lookupElement
            }

            parameterNameAndTypeCompletion.addFromParametersInFile(position, resolutionFacade, isVisibleFilterCheckAlways)
            flushToResultSet()

            parameterNameAndTypeCompletion.addFromImportedClasses(position, bindingContext, isVisibleFilterCheckAlways)
            flushToResultSet()

            parameterNameAndTypeCompletion.addFromAllClasses(parameters, indicesHelper(false))
        }

        private fun completeTopLevelClassName() {
            val name = parameters.originalFile.virtualFile.nameWithoutExtension
            if (!(Name.isValidIdentifier(name) && Name.identifier(name).render() == name && name[0].isUpperCase())) return
            if ((parameters.originalFile as KtFile).declarations.any { it is KtClassOrObject && it.name == name }) return

            collector.addElement(LookupElementBuilder.create(name))
        }

        private fun declaration() = position.parent as KtNamedDeclaration

        private fun shouldCompleteParameterNameAndType(): Boolean {
            val parameter = declaration() as? KtParameter ?: return false
            val list = parameter.parent as? KtParameterList ?: return false
            val owner = list.parent
            return when (owner) {
                is KtCatchClause, is KtPropertyAccessor, is KtFunctionLiteral -> false
                is KtNamedFunction -> owner.nameIdentifier != null
                is KtPrimaryConstructor -> !owner.getContainingClassOrObject().isAnnotation()
                else -> true
            }
        }
    }

    private val SUPER_QUALIFIER = object : CompletionKind {
        override val descriptorKindFilter: DescriptorKindFilter?
            get() = DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS

        override fun doComplete() {
            val classOrObject = position.parents.firstIsInstanceOrNull<KtClassOrObject>() ?: return
            val classDescriptor = resolutionFacade.resolveToDescriptor(classOrObject) as ClassDescriptor
            var superClasses = classDescriptor.defaultType.constructor.supertypesWithAny()
                    .mapNotNull { it.constructor.declarationDescriptor as? ClassDescriptor }

            if (callTypeAndReceiver.receiver != null) {
                val referenceVariantsSet = referenceVariants!!.imported.toSet()
                superClasses = superClasses.filter { it in referenceVariantsSet }
            }

            superClasses
                    .map { basicLookupElementFactory.createLookupElement(it, qualifyNestedClasses = true, includeClassTypeArguments = false) }
                    .forEach { collector.addElement(it) }
        }
    }

    private fun completeDeclarationNameFromUnresolvedOrOverride(declaration: KtNamedDeclaration) {
        if (declaration is KtCallableDeclaration && declaration.hasModifier(KtTokens.OVERRIDE_KEYWORD)) {
            OverridesCompletion(collector, basicLookupElementFactory).complete(position, declaration)
        }
        else {
            val referenceScope = referenceScope(declaration) ?: return
            val originalScope = toFromOriginalFileMapper.toOriginalFile(referenceScope) ?: return
            val afterOffset = if (referenceScope is KtBlockExpression) parameters.offset else null
            val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            FromUnresolvedNamesCompletion(collector, prefixMatcher).addNameSuggestions(originalScope, afterOffset, descriptor)
        }
    }

    private fun referenceScope(declaration: KtNamedDeclaration): KtElement? {
        val parent = declaration.parent
        when (parent) {
            is KtParameterList -> return parent.parent as KtElement

            is KtClassBody -> {
                val classOrObject = parent.parent as KtClassOrObject
                if (classOrObject is KtObjectDeclaration && classOrObject.isCompanion()) {
                    return classOrObject.containingClassOrObject
                }
                else {
                    return classOrObject
                }
            }

            is KtFile -> return parent

            is KtBlockExpression -> return parent

            else -> return null
        }
    }

    private fun addClassesFromIndex(kindFilter: (ClassKind) -> Boolean, prefixMatcher: PrefixMatcher) {
        val classDescriptorCollector = { descriptor: ClassDescriptor ->
            collector.addElement(basicLookupElementFactory.createLookupElement(descriptor), notImported = true)
        }
        val javaClassCollector = { javaClass: PsiClass ->
            collector.addElement(basicLookupElementFactory.createLookupElementForJavaClass(javaClass), notImported = true)
        }
        AllClassesCompletion(parameters, indicesHelper(true), prefixMatcher, resolutionFacade,
                             kindFilter, configuration.javaClassesNotToBeUsed
        ).collect(classDescriptorCollector, javaClassCollector)
    }

}