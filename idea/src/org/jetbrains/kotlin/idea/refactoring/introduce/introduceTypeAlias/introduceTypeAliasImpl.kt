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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.LinkedMultiMap
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.compareDescriptors
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.refactoring.introduce.insertDeclaration
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.UnifierParameter
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.utils.findClassifier
import java.util.*

sealed class IntroduceTypeAliasAnalysisResult {
    class Error(val message: String) : IntroduceTypeAliasAnalysisResult()
    class Success(val descriptor: IntroduceTypeAliasDescriptor) : IntroduceTypeAliasAnalysisResult()
}

private fun IntroduceTypeAliasData.getTargetScope() = targetSibling.getResolutionScope(bindingContext, resolutionFacade)

fun IntroduceTypeAliasData.analyze(): IntroduceTypeAliasAnalysisResult {
    val psiFactory = KtPsiFactory(originalTypeElement)

    val contextExpression = originalTypeElement.getStrictParentOfType<KtExpression>()!!
    val targetScope = getTargetScope()

    val dummyVar = psiFactory.createProperty("val a: Int").apply {
        typeReference!!.replace(
                originalTypeElement.parent as? KtTypeReference ?:
                if (originalTypeElement is KtTypeElement) psiFactory.createType(originalTypeElement) else psiFactory.createType(originalTypeElement.text))
    }
    val newTypeReference = dummyVar.typeReference!!
    val newReferences = newTypeReference.collectDescendantsOfType<KtTypeReference> { it.resolveInfo != null }
    val newContext = dummyVar.analyzeInContext(targetScope, contextExpression)
    val project = originalTypeElement.project

    val unifier = KotlinPsiUnifier.DEFAULT
    val groupedReferencesToExtract = LinkedMultiMap<TypeReferenceInfo, TypeReferenceInfo>()

    val forcedCandidates = if (extractTypeConstructor) newTypeReference.typeElement!!.typeArgumentsAsTypes else emptyList()

    for (newReference in newReferences) {
        val resolveInfo = newReference.resolveInfo!!

        if (newReference !in forcedCandidates) {
            val originalDescriptor = resolveInfo.type.constructor.declarationDescriptor
            val newDescriptor = newContext[BindingContext.TYPE, newReference]?.constructor?.declarationDescriptor
            if (compareDescriptors(project, originalDescriptor, newDescriptor)) continue
        }

        val equivalenceRepresentative = groupedReferencesToExtract
                .keySet()
                .firstOrNull { unifier.unify(it.reference, resolveInfo.reference).matched }
        if (equivalenceRepresentative != null) {
            groupedReferencesToExtract.putValue(equivalenceRepresentative, resolveInfo)
        }
        else {
            groupedReferencesToExtract.putValue(resolveInfo, resolveInfo)
        }

        val referencesToExtractIterator = groupedReferencesToExtract.values().iterator()
        while (referencesToExtractIterator.hasNext()) {
            val referenceToExtract = referencesToExtractIterator.next()
            if (resolveInfo.reference.isAncestor(referenceToExtract.reference, true)) {
                referencesToExtractIterator.remove()
            }
        }
    }

    val typeParameterNameValidator = CollectingNameValidator()
    val brokenReferences = groupedReferencesToExtract.keySet().filter { groupedReferencesToExtract[it].isNotEmpty() }
    val typeParameterNames = KotlinNameSuggester.suggestNamesForTypeParameters(brokenReferences.size, typeParameterNameValidator)
    val typeParameters = (typeParameterNames zip brokenReferences).map { TypeParameter(it.first, groupedReferencesToExtract[it.second]) }

    if (typeParameters.any { it.typeReferenceInfos.any { it.reference.typeElement == originalTypeElement } }) {
        return IntroduceTypeAliasAnalysisResult.Error("Type alias cannot refer to types which aren't accessible in the scope where it's defined")
    }

    val descriptor = IntroduceTypeAliasDescriptor(this, "Dummy", null, typeParameters)

    val initialName = KotlinNameSuggester.suggestTypeAliasNameByPsi(descriptor.generateTypeAlias(true).getTypeReference()!!.typeElement!!) {
        targetScope.findClassifier(Name.identifier(it), NoLookupLocation.FROM_IDE) == null
    }

    return IntroduceTypeAliasAnalysisResult.Success(descriptor.copy(name = initialName))
}

fun IntroduceTypeAliasData.getApplicableVisibilities(): List<KtModifierKeywordToken>{
    val parent = targetSibling.parent
    return when (parent) {
        is KtClassBody -> listOf(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)
        is KtFile -> listOf(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)
        else -> emptyList()
    }
}

fun IntroduceTypeAliasDescriptor.validate(): IntroduceTypeAliasDescriptorWithConflicts {
    val conflicts = MultiMap<PsiElement, String>()

    val originalType = originalData.originalTypeElement
    when {
        name.isEmpty() ->
            conflicts.putValue(originalType, "No name provided for type alias")
        !KotlinNameSuggester.isIdentifier(name) ->
            conflicts.putValue(originalType, "Type alias name must be a valid identifier: $name")
        originalData.getTargetScope().findClassifier(Name.identifier(name), NoLookupLocation.FROM_IDE) != null ->
            conflicts.putValue(originalType, "Type $name already exists in the target scope")
    }

    if (typeParameters.distinctBy { it.name }.size != typeParameters.size) {
        conflicts.putValue(originalType, "Type parameter names must be distinct")
    }

    if (visibility != null && visibility !in originalData.getApplicableVisibilities()) {
        conflicts.putValue(originalType, "'$visibility' is not allowed in the target context")
    }

    return IntroduceTypeAliasDescriptorWithConflicts(this, conflicts)
}

fun findDuplicates(typeAlias: KtTypeAlias): Map<KotlinPsiRange, () -> Unit> {
    val aliasName = typeAlias.name?.quoteIfNeeded() ?: return emptyMap()
    val aliasRange = typeAlias.textRange
    val typeAliasDescriptor = typeAlias.unsafeResolveToDescriptor() as TypeAliasDescriptor

    val unifierParameters = typeAliasDescriptor.declaredTypeParameters.map { UnifierParameter(it, null) }
    val unifier = KotlinPsiUnifier(unifierParameters)

    val psiFactory = KtPsiFactory(typeAlias)

    fun replaceOccurrence(occurrence: PsiElement, arguments: List<KtTypeElement>) {
        val typeArgumentsText = if (arguments.isNotEmpty()) "<${arguments.joinToString { it.text }}>" else ""
        when (occurrence) {
            is KtTypeElement -> {
                occurrence.replace(psiFactory.createType("$aliasName$typeArgumentsText").typeElement!!)
            }

            is KtCallElement -> {
                val typeArgumentList = occurrence.typeArgumentList
                if (arguments.isNotEmpty()) {
                    val newTypeArgumentList = psiFactory.createTypeArguments(typeArgumentsText)
                    typeArgumentList?.replace(newTypeArgumentList) ?: occurrence.addAfter(newTypeArgumentList, occurrence)
                }
                else {
                    typeArgumentList?.delete()
                }
                occurrence.calleeExpression?.replace(psiFactory.createExpression(aliasName))
            }

            is KtExpression -> occurrence.replace(psiFactory.createExpression(aliasName))
        }
    }

    val rangesWithReplacers = ArrayList<Pair<KotlinPsiRange, () -> Unit>>()

    val originalTypePsi = typeAliasDescriptor.underlyingType.constructor.declarationDescriptor?.let {
        DescriptorToSourceUtilsIde.getAnyDeclaration(typeAlias.project, it)
    }
    if (originalTypePsi != null) {
        for (reference in ReferencesSearch.search(originalTypePsi, LocalSearchScope(typeAlias.parent))) {
            val element = reference.element as? KtSimpleNameExpression ?: continue
            if ((element.textRange.intersects(aliasRange))) continue

            val arguments: List<KtTypeElement>
            val occurrence: KtElement

            val callElement = element.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
            if (callElement != null) {
                occurrence = callElement
                arguments = callElement.typeArguments.mapNotNull { it.typeReference?.typeElement }
            }
            else {
                val userType = element.getParentOfTypeAndBranch<KtUserType> { referenceExpression }
                if (userType != null) {
                    occurrence = userType
                    arguments = userType.typeArgumentsAsTypes.mapNotNull { it.typeElement }
                }
                else continue
            }
            if (arguments.size != typeAliasDescriptor.declaredTypeParameters.size) continue
            rangesWithReplacers += occurrence.toRange() to { replaceOccurrence(occurrence, arguments) }
        }
    }
    typeAlias
            .getTypeReference()
            ?.typeElement
            .toRange()
            .match(typeAlias.parent, unifier)
            .asSequence()
            .filter { !(it.range.getTextRange().intersects(aliasRange)) }
            .mapNotNullTo(rangesWithReplacers) { match ->
                val occurrence = match.range.elements.singleOrNull() as? KtTypeElement ?: return@mapNotNullTo null
                val arguments = unifierParameters.mapNotNull { (match.substitution[it] as? KtTypeReference)?.typeElement }
                if (arguments.size != unifierParameters.size) return@mapNotNullTo null
                match.range to { replaceOccurrence(occurrence, arguments) }
            }
    return rangesWithReplacers.toMap()
}

private var KtTypeReference.typeParameterInfo : TypeParameter? by CopyableUserDataProperty(Key.create("TYPE_PARAMETER_INFO"))

fun IntroduceTypeAliasDescriptor.generateTypeAlias(previewOnly: Boolean = false): KtTypeAlias {
    val originalElement = originalData.originalTypeElement
    val psiFactory = KtPsiFactory(originalElement)

    for (typeParameter in typeParameters)
        for (it in typeParameter.typeReferenceInfos) {
            it.reference.typeParameterInfo = typeParameter
        }

    val typeParameterNames = typeParameters.map { it.name }
    val typeAlias = if (originalElement is KtTypeElement) {
        psiFactory.createTypeAlias(name, typeParameterNames, originalElement)
    }
    else {
        psiFactory.createTypeAlias(name, typeParameterNames, originalElement.text)
    }
    if (visibility != null && visibility != KtTokens.DEFAULT_VISIBILITY_KEYWORD) {
        typeAlias.addModifier(visibility)
    }

    for (typeParameter in typeParameters)
        for (it in typeParameter.typeReferenceInfos) {
            it.reference.typeParameterInfo = null
        }

    fun replaceUsage() {
        val aliasInstanceText = if (typeParameters.isNotEmpty()) {
            "$name<${typeParameters.joinToString { it.typeReferenceInfos.first().reference.text }}>"
        }
        else {
            name
        }
        when (originalElement) {
            is KtTypeElement -> originalElement.replace(psiFactory.createType(aliasInstanceText).typeElement!!)
            is KtExpression -> originalElement.replace(psiFactory.createExpression(aliasInstanceText))
        }
    }

    fun introduceTypeParameters() {
        typeAlias.getTypeReference()!!.forEachDescendantOfType<KtTypeReference> {
            val typeParameter = it.typeParameterInfo ?: return@forEachDescendantOfType
            val typeParameterReference = psiFactory.createType(typeParameter.name)
            it.replace(typeParameterReference)
        }
    }

    return if (previewOnly) {
        introduceTypeParameters()
        typeAlias
    }
    else {
        replaceUsage()
        introduceTypeParameters()
        insertDeclaration(typeAlias, originalData.targetSibling)
    }
}