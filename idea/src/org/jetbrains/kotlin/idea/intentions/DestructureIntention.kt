/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class DestructureInspection : IntentionBasedInspection<KtDeclaration>(
        DestructureIntention::class,
        additionalChecker = { declaration ->
            declaration !is KtVariableDeclaration && declaration !is KtFunctionLiteral
        }
)

class DestructureIntention : SelfTargetingRangeIntention<KtDeclaration>(
        KtDeclaration::class.java,
        "Use destructuring declaration"
) {
    override fun applyTo(element: KtDeclaration, editor: Editor?) {
        val forLoop = element.parent as? KtForExpression
        val loopRange = forLoop?.loopRange
        val (usagesToRemove, removeSelectorInLoopRange) = collectUsagesToRemove(element, forLoop)!!

        val factory = KtPsiFactory(element)
        val validator = NewDeclarationNameValidator(
                element.parent, element, NewDeclarationNameValidator.Target.VARIABLES,
                excludedDeclarations = usagesToRemove.map { it.variableToDrop.singletonOrEmptyList() }.flatten()
        )
        val names = ArrayList<String>()
        usagesToRemove.forEach { (descriptor, usagesToReplace, variableToDrop, name) ->
            val suggestedName = name ?: KotlinNameSuggester.suggestNameByName(descriptor.name.asString(), validator)
            variableToDrop?.delete()
            usagesToReplace.forEach {
                it.replace(factory.createExpression(suggestedName))
            }
            names.add(suggestedName)
        }

        val joinedNames = names.joinToString()
        when (element) {
            is KtParameter -> {
                element.replace(factory.createDestructuringParameter("($joinedNames)"))
                if (removeSelectorInLoopRange && loopRange is KtDotQualifiedExpression) {
                    loopRange.replace(loopRange.receiverExpression)
                }
            }

            is KtFunctionLiteral -> {
                val lambda = element.parent as KtLambdaExpression
                SpecifyExplicitLambdaSignatureIntention().applyTo(lambda, editor)
                lambda.functionLiteral.valueParameters.singleOrNull()?.replace(
                        factory.createDestructuringParameter("($joinedNames)")
                )
            }

            is KtVariableDeclaration -> {
                val rangeAfterEq = PsiChildRange(element.initializer, element.lastChild)
                val modifierList = element.modifierList
                if (modifierList == null) {
                    element.replace(factory.createDestructuringDeclarationByPattern(
                            "val ($joinedNames) = $0", rangeAfterEq))
                }
                else {
                    val rangeBeforeVal = PsiChildRange(element.firstChild, modifierList)
                    element.replace(factory.createDestructuringDeclarationByPattern(
                            "$0:'@xyz' val ($joinedNames) = $1", rangeBeforeVal, rangeAfterEq))
                }
            }
        }
    }

    override fun applicabilityRange(element: KtDeclaration): TextRange? {
        if (!isSuitableDeclaration(element)) return null

        val usagesToRemove = collectUsagesToRemove(element, element.parent as? KtForExpression)?.data ?: return null
        if (usagesToRemove.isEmpty()) return null

        return when (element) {
            is KtFunctionLiteral -> element.lBrace.textRange
            is KtNamedDeclaration -> element.nameIdentifier?.textRange
            else -> null
        }
    }

    private fun isSuitableDeclaration(declaration: KtDeclaration) = when (declaration) {
        is KtParameter -> {
            val parent = declaration.parent
            when {
                parent is KtForExpression -> true
                parent?.parent is KtFunctionLiteral -> true
                else -> false
            }
        }
        is KtVariableDeclaration -> true
        is KtFunctionLiteral -> !declaration.hasParameterSpecification() // replace implicit 'it' with destructuring declaration
        else -> false
    }

    private data class UsagesToRemove(val data: List<UsageData>, val removeSelectorInLoopRange: Boolean)

    private fun collectUsagesToRemove(declaration: KtDeclaration, forLoop: KtForExpression?): UsagesToRemove? {
        val context = declaration.analyze()

        val variableDescriptor = when (declaration) {
            is KtParameter -> context.get(BindingContext.VALUE_PARAMETER, declaration)
            is KtFunctionLiteral -> context.get(BindingContext.FUNCTION, declaration)?.valueParameters?.singleOrNull()
            is KtVariableDeclaration -> context.get(BindingContext.VARIABLE, declaration)
            else -> null
        } ?: return null

        val variableType = variableDescriptor.type
        if (variableType.isMarkedNullable) return null
        val classDescriptor = variableType.constructor.declarationDescriptor as? ClassDescriptor ?: return null

        val mapEntryClassDescriptor = classDescriptor.builtIns.mapEntry

        // Note: list should contains properties in order to create destructuring declaration
        val usagesToRemove = mutableListOf<UsageData>()
        var badUsageFound = false
        val removeSelectorInLoopRange: Boolean
        if (forLoop != null && DescriptorUtils.isSubclass(classDescriptor, mapEntryClassDescriptor)) {
            val loopRangeDescriptorName = forLoop.loopRange.getResolvedCall(context)?.resultingDescriptor?.name
            removeSelectorInLoopRange = loopRangeDescriptorName?.asString().let { it == "entries" || it == "entrySet" }

            listOf("key", "value").mapTo(usagesToRemove) {
                UsageData(mapEntryClassDescriptor.unsubstitutedMemberScope.getContributedVariables(
                        Name.identifier(it), NoLookupLocation.FROM_BUILTINS).single())
            }

            ReferencesSearch.search(declaration).iterateOverMapEntryPropertiesUsages(
                    context,
                    { index, usageData -> usagesToRemove[index].add(usageData) },
                    { badUsageFound = true }
            )
        }
        else if (classDescriptor.isData) {
            removeSelectorInLoopRange = false

            val valueParameters = classDescriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: return null
            valueParameters.mapTo(usagesToRemove) { UsageData(it) }

            // inference bug: remove as? PsiElement when fixed
            val usageScopeElement: PsiElement = forLoop as? PsiElement
                                                ?: (declaration as? KtFunctionLiteral)
                                                ?: (declaration.parent?.parent as? KtFunctionLiteral)
                                                ?: (declaration as? KtVariableDeclaration)?.parent
                                                ?: return null

            val nameToSearch = when (declaration) {
                is KtParameter -> declaration.nameAsName
                is KtVariableDeclaration -> declaration.nameAsName
                else -> Name.identifier("it")
            } ?: return null

            val descriptorToIndex = mutableMapOf<CallableDescriptor, Int>()
            for (valueParameter in valueParameters) {
                val propertyDescriptor = context.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, valueParameter) ?: continue
                descriptorToIndex[propertyDescriptor] = valueParameter.index
            }
            usageScopeElement.iterateOverDataClassPropertiesUsagesWithIndex(
                    context,
                    nameToSearch,
                    descriptorToIndex,
                    { index, usageData -> usagesToRemove[index].add(usageData) },
                    { badUsageFound = true }
            )
        }
        else {
            return null
        }
        if (badUsageFound) return null

        val droppedLastUnused = usagesToRemove.dropLastWhile { it.usagesToReplace.isEmpty() && it.variableToDrop == null }
        return UsagesToRemove(droppedLastUnused, removeSelectorInLoopRange)
    }

    private fun Query<PsiReference>.iterateOverMapEntryPropertiesUsages(
            context: BindingContext,
            process: (Int, UsageData) -> Unit,
            cancel: () -> Unit
    ) {
        // TODO: Remove SAM-constructor when KT-11265 will be fixed
        forEach(Processor forEach@{
            val applicableUsage = getDataIfUsageIsApplicable(it, context)
            if (applicableUsage != null) {
                when (applicableUsage.descriptor.name.asString()) {
                    "key", "getKey" -> {
                        process(0, applicableUsage)
                        return@forEach true
                    }
                    "value", "getValue" -> {
                        process(1, applicableUsage)
                        return@forEach true
                    }
                }
            }

            cancel()
            return@forEach false
        })
    }

    private fun PsiElement.iterateOverDataClassPropertiesUsagesWithIndex(
            context: BindingContext,
            parameterName: Name,
            descriptorToIndex: Map<CallableDescriptor, Int>,
            process: (Int, UsageData) -> Unit,
            cancel: () -> Unit
    ) {
        anyDescendantOfType<KtNameReferenceExpression> {
            if (it.getReferencedNameAsName() != parameterName) false
            else {
                val applicableUsage = getDataIfUsageIsApplicable(it, context)
                if (applicableUsage != null) {
                    val index = descriptorToIndex[applicableUsage.descriptor]
                    if (index != null) {
                        process(index, applicableUsage)
                        return@anyDescendantOfType false
                    }
                }

                cancel()
                true
            }
        }
    }

    private fun getDataIfUsageIsApplicable(dataClassUsage: PsiReference, context: BindingContext) =
            (dataClassUsage.element as? KtReferenceExpression)?.let { getDataIfUsageIsApplicable(it, context) }

    private fun getDataIfUsageIsApplicable(dataClassUsage: KtReferenceExpression, context: BindingContext): UsageData? {
        val qualifiedExpression = dataClassUsage.getQualifiedExpressionForReceiver() ?: return null
        val parent = qualifiedExpression.parent
        when (parent) {
            is KtBinaryExpression -> {
                if (parent.operationToken in KtTokens.ALL_ASSIGNMENTS && parent.left == qualifiedExpression) return null
            }
            is KtUnaryExpression -> {
                if (parent.operationToken == KtTokens.PLUSPLUS || parent.operationToken == KtTokens.MINUSMINUS) return null
            }
        }

        val property = parent as? KtProperty // val x = d.y
        if (property != null && property.isVar) return null

        val descriptor = qualifiedExpression.getResolvedCall(context)?.resultingDescriptor ?: return null
        return UsageData(
                descriptor = descriptor,
                usagesToReplace = mutableListOf(qualifiedExpression),
                variableToDrop = property)
    }

    private data class UsageData(
            val descriptor: CallableDescriptor,
            val usagesToReplace: MutableList<KtExpression> = mutableListOf(),
            var variableToDrop: KtProperty? = null,
            var name: String? = variableToDrop?.name
    ) {
        fun add(newData: UsageData) {
            variableToDrop = variableToDrop ?: newData.variableToDrop
            usagesToReplace.addAll(newData.usagesToReplace)
            name = name ?: newData.name
        }
    }
}