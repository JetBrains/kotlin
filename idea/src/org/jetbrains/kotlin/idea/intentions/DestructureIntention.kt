/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
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
import java.util.*

class DestructureInspection : IntentionBasedInspection<KtDeclaration>(
        DestructureIntention::class,
        { element, _ ->
            val usagesToRemove = DestructureIntention.collectUsagesToRemove(element)?.data
            if (element is KtParameter) {
                usagesToRemove != null &&
                (usagesToRemove.any { it.declarationToDrop is KtDestructuringDeclaration } ||
                 usagesToRemove.filter { it.usagesToReplace.isNotEmpty() }.size > usagesToRemove.size / 2)
            }
            else {
                usagesToRemove?.any { it.declarationToDrop is KtDestructuringDeclaration } ?: false
            }
        }
)

class DestructureIntention : SelfTargetingRangeIntention<KtDeclaration>(
        KtDeclaration::class.java,
        "Use destructuring declaration"
) {
    override fun applyTo(element: KtDeclaration, editor: Editor?) {
        val (usagesToRemove, removeSelectorInLoopRange) = collectUsagesToRemove(element)!!

        val factory = KtPsiFactory(element)
        val validator = NewDeclarationNameValidator(
                container = element.parent, anchor = element, target = NewDeclarationNameValidator.Target.VARIABLES,
                excludedDeclarations = usagesToRemove.map { listOfNotNull(it.declarationToDrop) }.flatten()
        )
        val names = ArrayList<String>()
        val underscoreSupported = element.languageVersionSettings.supportsFeature(LanguageFeature.SingleUnderscoreForParameterName)
        // For all unused we generate normal names, not underscores
        val allUnused = usagesToRemove.all { (_, usagesToReplace, variableToDrop) ->
            usagesToReplace.isEmpty() && variableToDrop == null
        }

        usagesToRemove.forEach { (descriptor, usagesToReplace, variableToDrop, name) ->
            val suggestedName =
                    if (usagesToReplace.isEmpty() && variableToDrop == null && underscoreSupported && !allUnused) {
                        "_"
                    }
                    else {
                        name ?: KotlinNameSuggester.suggestNameByName(descriptor.name.asString(), validator)
                    }
            variableToDrop?.delete()
            usagesToReplace.forEach {
                it.replace(factory.createExpression(suggestedName))
            }
            names.add(suggestedName)
        }

        val joinedNames = names.joinToString()
        when (element) {
            is KtParameter -> {
                val loopRange = (element.parent as? KtForExpression)?.loopRange
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
        if (!element.isSuitableDeclaration()) return null

        val usagesToRemove = collectUsagesToRemove(element)?.data ?: return null
        if (usagesToRemove.isEmpty()) return null

        return when (element) {
            is KtFunctionLiteral -> element.lBrace.textRange
            is KtNamedDeclaration -> element.nameIdentifier?.textRange
            else -> null
        }
    }

    companion object {

        internal fun KtDeclaration.isSuitableDeclaration() = getUsageScopeElement() != null

        private fun KtDeclaration.getUsageScopeElement(): PsiElement? {
            val lambdaSupported = languageVersionSettings.supportsFeature(LanguageFeature.DestructuringLambdaParameters)
            return when (this) {
                is KtParameter -> {
                    val parent = parent
                    when {
                        parent is KtForExpression -> parent
                        parent.parent is KtFunctionLiteral -> if (lambdaSupported) parent.parent else null
                        else -> null
                    }
                }
                is KtProperty -> parent.takeIf { isLocal }
                is KtFunctionLiteral -> if (!hasParameterSpecification() && lambdaSupported) this else null
                else -> null
            }
        }

        internal data class UsagesToRemove(val data: List<UsageData>, val removeSelectorInLoopRange: Boolean)

        internal fun collectUsagesToRemove(declaration: KtDeclaration): UsagesToRemove? {
            val context = declaration.analyze()

            val variableDescriptor =
                    when (declaration) {
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
            var noBadUsages = true
            var removeSelectorInLoopRange = false
            val forLoop = declaration.parent as? KtForExpression
            if (forLoop != null && DescriptorUtils.isSubclass(classDescriptor, mapEntryClassDescriptor)) {
                val loopRangeDescriptor = forLoop.loopRange.getResolvedCall(context)?.resultingDescriptor
                if (loopRangeDescriptor != null) {
                    val loopRangeDescriptorOwner = loopRangeDescriptor.containingDeclaration
                    val mapClassDescriptor = classDescriptor.builtIns.map
                    if (loopRangeDescriptorOwner is ClassDescriptor &&
                        DescriptorUtils.isSubclass(loopRangeDescriptorOwner, mapClassDescriptor)) {
                        removeSelectorInLoopRange = loopRangeDescriptor.name.asString().let { it == "entries" || it == "entrySet" }
                    }
                }

                listOf("key", "value").mapTo(usagesToRemove) {
                    UsageData(descriptor = mapEntryClassDescriptor.unsubstitutedMemberScope.getContributedVariables(
                            Name.identifier(it), NoLookupLocation.FROM_BUILTINS).single())
                }

                ReferencesSearch.search(declaration).iterateOverMapEntryPropertiesUsages(
                        context,
                        { index, usageData -> noBadUsages = usagesToRemove[index].add(usageData, index) && noBadUsages },
                        { noBadUsages = false }
                )
            }
            else if (classDescriptor.isData) {

                val valueParameters = classDescriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: return null
                valueParameters.mapTo(usagesToRemove) { UsageData(descriptor = it) }

                val usageScopeElement = declaration.getUsageScopeElement() ?: return null

                val nameToSearch = when (declaration) {
                                       is KtParameter -> declaration.nameAsName
                                       is KtVariableDeclaration -> declaration.nameAsName
                                       else -> Name.identifier("it")
                                   } ?: return null

                val constructorParameterNameMap = mutableMapOf<Name, ValueParameterDescriptor>()
                valueParameters.forEach { constructorParameterNameMap[it.name] = it }

                usageScopeElement.iterateOverDataClassPropertiesUsagesWithIndex(
                        context,
                        nameToSearch,
                        constructorParameterNameMap,
                        { index, usageData -> noBadUsages = usagesToRemove[index].add(usageData, index) && noBadUsages },
                        { noBadUsages = false }
                )
            }
            else {
                return null
            }
            if (!noBadUsages) return null

            val droppedLastUnused = usagesToRemove.dropLastWhile { it.usagesToReplace.isEmpty() && it.declarationToDrop == null }
            return if (droppedLastUnused.isEmpty()) {
                UsagesToRemove(usagesToRemove, removeSelectorInLoopRange)
            }
            else {
                UsagesToRemove(droppedLastUnused, removeSelectorInLoopRange)
            }
        }

        private fun Query<PsiReference>.iterateOverMapEntryPropertiesUsages(
                context: BindingContext,
                process: (Int, SingleUsageData) -> Unit,
                cancel: () -> Unit
        ) {
            // TODO: Remove SAM-constructor when KT-11265 will be fixed
            forEach(Processor forEach@{
                val applicableUsage = getDataIfUsageIsApplicable(it, context)
                if (applicableUsage != null) {
                    val usageDescriptor = applicableUsage.descriptor
                    if (usageDescriptor == null) {
                        process(0, applicableUsage)
                        process(1, applicableUsage)
                        return@forEach true
                    }
                    when (usageDescriptor.name.asString()) {
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
                false
            })
        }

        private fun PsiElement.iterateOverDataClassPropertiesUsagesWithIndex(
                context: BindingContext,
                parameterName: Name,
                constructorParameterNameMap: Map<Name, ValueParameterDescriptor>,
                process: (Int, SingleUsageData) -> Unit,
                cancel: () -> Unit
        ) {
            anyDescendantOfType<KtNameReferenceExpression> {
                if (it.getReferencedNameAsName() != parameterName) false
                else {
                    val applicableUsage = getDataIfUsageIsApplicable(it, context)
                    if (applicableUsage != null) {
                        val usageDescriptor = applicableUsage.descriptor
                        if (usageDescriptor == null) {
                            for (parameter in constructorParameterNameMap.values) {
                                process(parameter.index, applicableUsage)
                            }
                            return@anyDescendantOfType false
                        }
                        val parameter = constructorParameterNameMap[usageDescriptor.name]
                        if (parameter != null) {
                            process(parameter.index, applicableUsage)
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

        private fun getDataIfUsageIsApplicable(dataClassUsage: KtReferenceExpression, context: BindingContext): SingleUsageData? {
            val destructuringDecl = dataClassUsage.parent as? KtDestructuringDeclaration
            if (destructuringDecl != null && destructuringDecl.initializer == dataClassUsage) {
                return SingleUsageData(descriptor = null, usageToReplace = null, declarationToDrop = destructuringDecl)
            }
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
            if (!descriptor.isVisible(dataClassUsage, qualifiedExpression.receiverExpression,
                                      context, dataClassUsage.containingKtFile.getResolutionFacade())) {
                return null
            }
            return SingleUsageData(descriptor = descriptor, usageToReplace = qualifiedExpression, declarationToDrop = property)
        }

        internal data class SingleUsageData(
                val descriptor: CallableDescriptor?,
                val usageToReplace: KtExpression?,
                val declarationToDrop: KtDeclaration?
        )

        internal data class UsageData(
                val descriptor: CallableDescriptor,
                val usagesToReplace: MutableList<KtExpression> = mutableListOf(),
                var declarationToDrop: KtDeclaration? = null,
                var name: String? = null
        ) {
            // Returns true if data is successfully added, false otherwise
            fun add(newData: SingleUsageData, componentIndex: Int): Boolean {
                if (newData.declarationToDrop is KtDestructuringDeclaration) {
                    val destructuringEntries = newData.declarationToDrop.entries
                    if (componentIndex < destructuringEntries.size) {
                        if (declarationToDrop != null) return false
                        name = destructuringEntries[componentIndex].name ?: return false
                        declarationToDrop = newData.declarationToDrop
                    }
                }
                else {
                    name = name ?: newData.declarationToDrop?.name
                    declarationToDrop = declarationToDrop ?: newData.declarationToDrop
                }
                newData.usageToReplace?.let { usagesToReplace.add(it) }
                return true
            }
        }
    }
}