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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.completion.smart.SMART_COMPLETION_ITEM_PRIORITY_KEY
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletion
import org.jetbrains.kotlin.idea.completion.smart.SmartCompletionItemPriority
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSession(configuration, parameters, resultSet) {

    private enum class CompletionKind(
            val descriptorKindFilter: DescriptorKindFilter?,
            val classKindFilter: ((ClassKind) -> Boolean)?
    ) {
        ALL(
                descriptorKindFilter = DescriptorKindFilter(DescriptorKindFilter.ALL_KINDS_MASK),
                classKindFilter = { it != ClassKind.ENUM_ENTRY }
        ),

        TYPES(
                descriptorKindFilter = DescriptorKindFilter(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude DescriptorKindExclude.EnumEntry,
                classKindFilter = { it != ClassKind.ENUM_ENTRY }
        ),

        ANNOTATION_TYPES(
                descriptorKindFilter = ANNOTATION_TYPES_FILTER,
                classKindFilter = { it == ClassKind.ANNOTATION_CLASS }
        ),

        ANNOTATION_TYPES_OR_PARAMETER_NAME(
                descriptorKindFilter = ANNOTATION_TYPES_FILTER,
                classKindFilter = { it == ClassKind.ANNOTATION_CLASS }
        ),

        KEYWORDS_ONLY(descriptorKindFilter = null, classKindFilter = null),

        NAMED_ARGUMENTS_ONLY(descriptorKindFilter = null, classKindFilter = null),

        PARAMETER_NAME(descriptorKindFilter = null, classKindFilter = null),

        SUPER_QUALIFIER(descriptorKindFilter = DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, classKindFilter = null)
    }

    private val completionKind = calcCompletionKind()

    override val descriptorKindFilter = if (isNoQualifierContext()) {
        // it's an optimization because obtaining top-level packages from scope is very slow, we obtains them in other way
        completionKind.descriptorKindFilter?.exclude(DescriptorKindExclude.TopLevelPackages)
    }
    else {
        completionKind.descriptorKindFilter
    }

    private val parameterNameAndTypeCompletion = if (shouldCompleteParameterNameAndType())
        ParameterNameAndTypeCompletion(collector, lookupElementFactory, prefixMatcher, resolutionFacade)
    else
        null

    private val smartCompletion = expression?.let {
        SmartCompletion(
                it, resolutionFacade, bindingContext, isVisibleFilter, prefixMatcher,
                GlobalSearchScope.EMPTY_SCOPE, toFromOriginalFileMapper, lookupElementFactory, forBasicCompletion = true
        )
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    private fun calcCompletionKind(): CompletionKind {
        if (nameExpression != null && NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression)) {
            return CompletionKind.NAMED_ARGUMENTS_ONLY
        }

        if (nameExpression == null) {
            val parameter = position.getParent() as? JetParameter
            return if (parameter != null && position == parameter.getNameIdentifier())
                CompletionKind.PARAMETER_NAME
            else
                CompletionKind.KEYWORDS_ONLY
        }

        val annotationEntry = position.getStrictParentOfType<JetAnnotationEntry>()
        if (annotationEntry != null) {
            val valueArgList = position.getStrictParentOfType<JetValueArgumentList>()
            if (valueArgList == null || !annotationEntry.isAncestor(valueArgList)) {
                val parent = annotationEntry.getParent()
                if (parent is JetDeclarationModifierList && parent.getParent() is JetParameter) {
                    return CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME
                }
                return CompletionKind.ANNOTATION_TYPES
            }
        }

        // Check that completion in the type annotation context and if there's a qualified
        // expression we are at first of it
        val typeReference = position.getStrictParentOfType<JetTypeReference>()
        if (typeReference != null) {
            if (typeReference.parent is JetSuperExpression) {
                return CompletionKind.SUPER_QUALIFIER
            }

            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            if (firstPartReference == nameExpression) {
                return CompletionKind.TYPES
            }
        }

        return CompletionKind.ALL
    }

    private fun shouldCompleteParameterNameAndType(): Boolean {
        if (completionKind != CompletionKind.PARAMETER_NAME && completionKind != CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME) return false

        val parameter = position.getNonStrictParentOfType<JetParameter>()!!
        val list = parameter.parent as? JetParameterList ?: return false
        val owner = list.parent
        return when (owner) {
            is JetCatchClause, is JetPropertyAccessor, is JetFunctionLiteral -> false
            is JetNamedFunction -> owner.nameIdentifier != null
            is JetPrimaryConstructor -> !owner.getContainingClassOrObject().isAnnotation()
            else -> true
        }
    }

    public fun shouldDisableAutoPopup(): Boolean {
        if (completionKind == CompletionKind.PARAMETER_NAME || completionKind == CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME) {
            if (!shouldCompleteParameterNameAndType() || TemplateManager.getInstance(project).getActiveTemplate(parameters.editor) != null) {
                return true
            }

            if (LookupCancelWatcher.getInstance(project).wasAutoPopupRecentlyCancelled(parameters.editor, position.startOffset)) {
                return true
            }
        }

        return false
    }

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        if (parameters.isAutoPopup) {
            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(LookupCancelWatcher.AUTO_POPUP_AT, position.startOffset)
                lookupElement
            }
        }

        if (completionKind == CompletionKind.SUPER_QUALIFIER) {
            completeSuperQualifier()
            return
        }

        // if we are typing parameter name, restart completion each time we type an upper case letter because new suggestions will appear (previous words can be used as user prefix)
        if (parameterNameAndTypeCompletion != null) {
            val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
            })
            collector.restartCompletionOnPrefixChange(prefixPattern)

            collector.addLookupElementPostProcessor { lookupElement ->
                lookupElement.putUserData(KotlinCompletionCharFilter.SUPPRESS_ITEM_SELECTION_BY_CHARS_ON_TYPING, Unit)
                lookupElement.putUserData(KotlinCompletionCharFilter.HIDE_LOOKUP_ON_COLON, Unit)
                lookupElement
            }

            parameterNameAndTypeCompletion.addFromParametersInFile(position, resolutionFacade, isVisibleFilter)
            flushToResultSet()

            parameterNameAndTypeCompletion.addFromImportedClasses(position, bindingContext, isVisibleFilter)
            flushToResultSet()
        }

        if (completionKind != CompletionKind.NAMED_ARGUMENTS_ONLY) {
            if (smartCompletion != null) {
                val (additionalItems, @Suppress("UNUSED_VARIABLE") inheritanceSearcher) = smartCompletion.additionalItems()

                // all additional items should have SMART_COMPLETION_ITEM_PRIORITY_KEY to be recognized by SmartCompletionInBasicWeigher
                for (item in additionalItems) {
                    if (item.getUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY) == null) {
                        item.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, SmartCompletionItemPriority.DEFAULT)
                    }
                }

                collector.addElements(additionalItems)
            }

            collector.addDescriptorElements(referenceVariants)

            val keywordsPrefix = prefix.substringBefore('@') // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
            KeywordCompletion.complete(expression ?: parameters.getPosition(), keywordsPrefix) { lookupElement ->
                val keyword = lookupElement.getLookupString()
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

                        OverridesCompletion(collector, lookupElementFactory).complete(position)
                    }

                    else -> collector.addElement(lookupElement)
                }
            }

            // getting root packages from scope is very slow so we do this in alternative way
            if (isNoQualifierContext() && (descriptorKindFilter?.kindMask ?: 0).and(DescriptorKindFilter.PACKAGES_MASK) != 0) {
                //TODO: move this code somewhere else?
                val packageNames = PackageIndexUtil.getSubPackageFqNames(FqName.ROOT, originalSearchScope, project, prefixMatcher.asNameFilter())
                        .toMutableSet()

                if (!ProjectStructureUtil.isJsKotlinModule(parameters.getOriginalFile() as JetFile)) {
                    JavaPsiFacade.getInstance(project).findPackage("")?.getSubPackages(originalSearchScope)?.forEach { psiPackage ->
                        val name = psiPackage.getName()
                        if (Name.isValidIdentifier(name!!)) {
                            packageNames.add(FqName(name))
                        }
                    }
                }

                packageNames.forEach { collector.addElement(lookupElementFactory.createLookupElementForPackage(it)) }
            }

            if (completionKind != CompletionKind.KEYWORDS_ONLY) {
                flushToResultSet()

                if (!configuration.completeNonImportedDeclarations && isNoQualifierContext()) {
                    collector.advertiseSecondCompletion()
                }

                addNonImported(completionKind)

                if (position.getContainingFile() is JetCodeFragment) {
                    flushToResultSet()
                    collector.addDescriptorElements(getRuntimeReceiverTypeReferenceVariants(), withReceiverCast = true)
                }
            }
        }

        NamedArgumentCompletion.complete(collector, expectedInfos)
    }

    private fun addNonImported(completionKind: CompletionKind) {
        if (completionKind == CompletionKind.ALL) {
            collector.addDescriptorElements(getTopLevelExtensions(), notImported = true)
        }

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            completionKind.classKindFilter?.let { addAllClasses(it) }

            if (completionKind == CompletionKind.ALL) {
                collector.addDescriptorElements(getTopLevelCallables(), notImported = true)
            }
        }

        parameterNameAndTypeCompletion?.addFromAllClasses(parameters, indicesHelper)
    }

    private fun completeSuperQualifier() {
        val classOrObject = position.parents.firstIsInstanceOrNull<JetClassOrObject>() ?: return
        val classDescriptor = resolutionFacade.resolveToDescriptor(classOrObject) as ClassDescriptor
        var superClasses = classDescriptor.defaultType.constructor.supertypes
                .map { it.constructor.declarationDescriptor as? ClassDescriptor }
                .filterNotNull()

        //TODO: IMO it's not good that Any is to be added manually
        if (superClasses.all { it.kind == ClassKind.INTERFACE }) {
            superClasses += KotlinBuiltIns.getInstance().any
        }

        if (!isNoQualifierContext()) {
            val referenceVariantsSet = referenceVariants.toSet()
            superClasses = superClasses.filter { it in referenceVariantsSet }
        }

        superClasses
                .map { lookupElementFactory.createLookupElement(it, useReceiverTypes = false, qualifyNestedClasses = true, includeClassTypeArguments = false) }
                .forEach { collector.addElement(it) }
    }

    override fun createSorter(): CompletionSorter {
        var sorter = super.createSorter()

        if (shouldCompleteParameterNameAndType()) {
            sorter = sorter.weighBefore(DeprecatedWeigher.toString(), ParameterNameAndTypeCompletion.Weigher)
        }

        if (smartCompletion != null) {
            sorter = sorter.weighBefore(KindWeigher.toString(), SmartCompletionInBasicWeigher(smartCompletion), SmartCompletionPriorityWeigher)
        }

        return sorter
    }

    private companion object {
        object NonAnnotationClassifierExclude : DescriptorKindExclude {
            override fun excludes(descriptor: DeclarationDescriptor): Boolean {
                if (descriptor !is ClassifierDescriptor) return false
                return descriptor !is ClassDescriptor || descriptor.getKind() != ClassKind.ANNOTATION_CLASS
            }

            override val fullyExcludedDescriptorKinds: Int get() = 0
        }

        val ANNOTATION_TYPES_FILTER = DescriptorKindFilter(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude NonAnnotationClassifierExclude
    }
}