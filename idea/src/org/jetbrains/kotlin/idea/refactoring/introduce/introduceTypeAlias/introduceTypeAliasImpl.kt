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
import com.intellij.util.containers.LinkedMultiMap
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.compareDescriptors
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
import org.jetbrains.kotlin.utils.SmartList

sealed class IntroduceTypeAliasAnalysisResult {
    class Error(val message: String) : IntroduceTypeAliasAnalysisResult()
    class Success(val descriptor: IntroduceTypeAliasDescriptor) : IntroduceTypeAliasAnalysisResult()
}

private fun IntroduceTypeAliasData.getTargetScope() = targetSibling.getResolutionScope(bindingContext, resolutionFacade)

fun IntroduceTypeAliasData.analyze(): IntroduceTypeAliasAnalysisResult {
    val psiFactory = KtPsiFactory(originalType)

    val contextExpression = originalType.getStrictParentOfType<KtExpression>()!!
    val targetScope = getTargetScope()

    val dummyVar = psiFactory.createProperty("val a: Int").apply {
        typeReference!!.replace(originalType.parent as? KtTypeReference ?: psiFactory.createType(originalType))
    }
    val newReferences = dummyVar.typeReference!!.collectDescendantsOfType<KtTypeReference> { it.resolveInfo != null }
    val newContext = dummyVar.analyzeInContext(targetScope, contextExpression)
    val project = originalType.project

    val unifier = KotlinPsiUnifier.DEFAULT
    val groupedBrokenReferences = LinkedMultiMap<TypeReferenceInfo, TypeReferenceInfo>()
    for (newReference in newReferences) {
        val resolveInfo = newReference.resolveInfo!!

        val originalDescriptor = resolveInfo.type.constructor.declarationDescriptor
        val newDescriptor = newContext[BindingContext.TYPE, newReference]?.constructor?.declarationDescriptor
        if (compareDescriptors(project, originalDescriptor, newDescriptor)) continue

        val equivalenceRepresentative = groupedBrokenReferences
                .keySet()
                .firstOrNull { unifier.unify(it.reference, resolveInfo.reference).matched }
        if (equivalenceRepresentative != null) {
            groupedBrokenReferences.putValue(equivalenceRepresentative, resolveInfo)
        }
        else {
            groupedBrokenReferences.putValue(resolveInfo, resolveInfo)
        }

        val brokenReferenceInfoIterator = groupedBrokenReferences.values().iterator()
        while (brokenReferenceInfoIterator.hasNext()) {
            val brokenReferenceInfo = brokenReferenceInfoIterator.next()
            if (resolveInfo.reference.isAncestor(brokenReferenceInfo.reference, true)) {
                brokenReferenceInfoIterator.remove()
            }
        }
    }

    val typeParameterNameValidator = CollectingNameValidator()
    val brokenReferences = groupedBrokenReferences.keySet().filter { groupedBrokenReferences[it].isNotEmpty() }
    val typeParameterNames = KotlinNameSuggester.suggestNamesForTypeParameters(brokenReferences.size, typeParameterNameValidator)
    val typeParameters = (typeParameterNames zip brokenReferences).map { TypeParameter(it.first, groupedBrokenReferences[it.second]) }

    if (typeParameters.any { it.typeReferenceInfos.any { it.reference.typeElement == originalType } }) {
        return IntroduceTypeAliasAnalysisResult.Error("Type alias cannot refer to types which aren't accessible in the scope where it's defined")
    }

    return IntroduceTypeAliasAnalysisResult.Success(IntroduceTypeAliasDescriptor(this, "", null, typeParameters))
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

    val originalType = originalData.originalType
    if (name.isEmpty()) {
        conflicts.putValue(originalType, "No name provided for type alias")
    }
    else if (!KotlinNameSuggester.isIdentifier(name)) {
        conflicts.putValue(originalType, "Type alias name must be a valid identifier: $name")
    }
    else if (originalData.getTargetScope().findClassifier(Name.identifier(name), NoLookupLocation.FROM_IDE) != null) {
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
    val aliasName = typeAlias.name ?: return emptyMap()
    val typeAliasDescriptor = typeAlias.resolveToDescriptor() as TypeAliasDescriptor

    val unifierParameters = typeAliasDescriptor.declaredTypeParameters.map { UnifierParameter(it, null) }
    val unifier = KotlinPsiUnifier(unifierParameters)

    val psiFactory = KtPsiFactory(typeAlias)

    fun replaceOccurrence(occurrence: KtTypeElement, arguments: List<KtTypeElement>) {
        val typeText = if (arguments.isNotEmpty()) "$aliasName<${arguments.joinToString { it.text }}>" else aliasName
        occurrence.replace(psiFactory.createType(typeText).typeElement!!)
    }

    val aliasRange = typeAlias.textRange
    return typeAlias
            .getTypeReference()
            ?.typeElement
            .toRange()
            .match(typeAlias.parent, unifier)
            .asSequence()
            .filter { !(it.range.getTextRange().intersects(aliasRange)) }
            .mapNotNull { match ->
                val occurrence = match.range.elements.singleOrNull() as? KtTypeElement ?: return@mapNotNull null
                val arguments = unifierParameters.mapNotNull { (match.substitution[it] as? KtTypeReference)?.typeElement }
                if (arguments.size != unifierParameters.size) return@mapNotNull null
                match.range to { replaceOccurrence(occurrence, arguments) }
            }
            .toMap()
}

private var KtTypeReference.typeParameterInfo : TypeParameter? by CopyableUserDataProperty(Key.create("TYPE_PARAMETER_INFO"))

fun IntroduceTypeAliasDescriptor.generateTypeAlias(previewOnly: Boolean = false): KtTypeAlias {
    val originalType = originalData.originalType
    val targetSibling = originalData.targetSibling
    val psiFactory = KtPsiFactory(originalType)

    for (typeParameter in typeParameters)
        for (it in typeParameter.typeReferenceInfos) {
            it.reference.typeParameterInfo = typeParameter
        }

    val typeAlias = psiFactory.createTypeAlias(name, typeParameters.map { it.name }, originalType)
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
        originalType.replace(psiFactory.createType(aliasInstanceText).typeElement!!)
    }

    fun introduceTypeParameters() {
        typeAlias.getTypeReference()!!.forEachDescendantOfType<KtTypeReference> {
            val typeParameter = it.typeParameterInfo ?: return@forEachDescendantOfType
            val typeParameterReference = psiFactory.createType(typeParameter.name)
            it.replace(typeParameterReference)
        }
    }

    fun insertDeclaration(): KtTypeAlias {
        val targetParent = originalData.targetSibling.parent

        val anchorCandidates = SmartList<PsiElement>()
        anchorCandidates.add(targetSibling)
        if (targetSibling is KtEnumEntry) {
            anchorCandidates.add(targetSibling.siblings().last { it is KtEnumEntry })
        }

        val anchor = anchorCandidates.minBy { it.startOffset }!!.parentsWithSelf.first { it.parent == targetParent }
        val targetContainer = anchor.parent!!
        return (targetContainer.addBefore(typeAlias, anchor) as KtTypeAlias).apply {
            targetContainer.addBefore(psiFactory.createWhiteSpace("\n\n"), anchor)
        }
    }

    return if (previewOnly) {
        introduceTypeParameters()
        typeAlias
    }
    else {
        replaceUsage()
        introduceTypeParameters()
        insertDeclaration()
    }
}