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
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class BasicCompletionSession(configuration: CompletionSessionConfiguration,
                             parameters: CompletionParameters,
                             resultSet: CompletionResultSet)
: CompletionSessionBase(configuration, parameters, resultSet) {

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

        PARAMETER_NAME(descriptorKindFilter = null, classKindFilter = null)
    }

    private val completionKind = calcCompletionKind()

    override val descriptorKindFilter = completionKind.descriptorKindFilter

    private val parameterNameAndTypeCompletion = if (shouldCompleteParameterNameAndType())
        ParameterNameAndTypeCompletion(collector, lookupElementFactory, prefixMatcher, resolutionFacade)
    else
        null

    private fun calcCompletionKind(): CompletionKind {
        if (NamedArgumentCompletion.isOnlyNamedArgumentExpected(position)) {
            return CompletionKind.NAMED_ARGUMENTS_ONLY
        }

        if (reference == null) {
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
            val firstPartReference = PsiTreeUtil.findChildOfType(typeReference, javaClass<JetSimpleNameExpression>())
            if (firstPartReference == reference.expression) {
                return CompletionKind.TYPES
            }
        }

        return CompletionKind.ALL
    }

    private fun shouldCompleteParameterNameAndType(): Boolean {
        when (completionKind) {
            CompletionKind.PARAMETER_NAME, CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME -> {
                val parameter = position.getNonStrictParentOfType<JetParameter>()!!
                val list = parameter.getParent() as? JetParameterList ?: return false
                val owner = list.getParent()
                return owner !is JetCatchClause &&
                       owner !is JetPropertyAccessor &&
                       !((owner as? JetPrimaryConstructor)?.getContainingClassOrObject()?.isAnnotation() ?: false)
            }

            else -> return false
        }
    }

    public fun shouldDisableAutoPopup(): Boolean {
        return when (completionKind) {
            CompletionKind.PARAMETER_NAME, CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME -> !shouldCompleteParameterNameAndType()
            else -> false
        }
    }

    override fun doComplete() {
        assert(parameters.getCompletionType() == CompletionType.BASIC)

        // if we are typing parameter name, restart completion each time we type an upper case letter because new suggestions will appear (previous words can be used as user prefix)
        if (parameterNameAndTypeCompletion != null) {
            val prefixPattern = StandardPatterns.string().with(object : PatternCondition<String>("Prefix ends with uppercase letter") {
                override fun accepts(prefix: String, context: ProcessingContext?) = prefix.isNotEmpty() && prefix.last().isUpperCase()
            })
            (CompletionService.getCompletionService().getCurrentCompletion() as CompletionProgressIndicator)
                    .addWatchedPrefix(parameters.getOffset(), prefixPattern)
        }

        if (completionKind == CompletionKind.PARAMETER_NAME || completionKind == CompletionKind.ANNOTATION_TYPES_OR_PARAMETER_NAME) {
            collector.suppressItemSelectionByCharsOnTyping = true
        }

        if (completionKind != CompletionKind.NAMED_ARGUMENTS_ONLY) {
            parameterNameAndTypeCompletion?.addFromParametersInFile(position, resolutionFacade, isVisibleFilter)
            flushToResultSet()

            parameterNameAndTypeCompletion?.addFromImportedClasses(position, bindingContext, isVisibleFilter)
            flushToResultSet()

            collector.addDescriptorElements(referenceVariants, suppressAutoInsertion = false)

            val keywordsPrefix = prefix.substringBefore('@') // if there is '@' in the prefix - use shorter prefix to not loose 'this' etc
            KeywordCompletion.complete(expression ?: parameters.getPosition(), keywordsPrefix) { lookupElement ->
                val keyword = lookupElement.getLookupString()
                when (keyword) {
                // if "this" is parsed correctly in the current context - insert it and all this@xxx items
                    "this" -> {
                        if (expression != null) {
                            collector.addElements(thisExpressionItems(bindingContext, expression, prefix).map { it.factory() })
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

                    else -> collector.addElement(lookupElement)
                }
            }

            if (completionKind != CompletionKind.KEYWORDS_ONLY) {
                flushToResultSet()

                if (!configuration.completeNonImportedDeclarations && isNoQualifierContext()) {
                    collector.advertiseSecondCompletion()
                }

                addNonImported(completionKind)

                if (position.getContainingFile() is JetCodeFragment) {
                    flushToResultSet()
                    collector.addDescriptorElements(getRuntimeReceiverTypeReferenceVariants(), suppressAutoInsertion = false, withReceiverCast = true)
                }
            }
        }

        NamedArgumentCompletion.complete(position, collector, bindingContext)
    }

    private fun addNonImported(completionKind: CompletionKind) {
        if (completionKind == CompletionKind.ALL) {
            collector.addDescriptorElements(getTopLevelExtensions(), suppressAutoInsertion = true)
        }

        flushToResultSet()

        if (shouldRunTopLevelCompletion()) {
            completionKind.classKindFilter?.let { addAllClasses(it) }

            if (completionKind == CompletionKind.ALL) {
                collector.addDescriptorElements(getTopLevelCallables(), suppressAutoInsertion = true)
            }
        }

        parameterNameAndTypeCompletion?.addFromAllClasses(parameters, indicesHelper)
    }

    private companion object {
        object NonAnnotationClassifierExclude : DescriptorKindExclude {
            override fun matches(descriptor: DeclarationDescriptor): Boolean {
                return if (descriptor is ClassDescriptor)
                    descriptor.getKind() != ClassKind.ANNOTATION_CLASS
                else
                    descriptor !is ClassifierDescriptor
            }
        }

        val ANNOTATION_TYPES_FILTER = DescriptorKindFilter(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK) exclude NonAnnotationClassifierExclude
    }
}