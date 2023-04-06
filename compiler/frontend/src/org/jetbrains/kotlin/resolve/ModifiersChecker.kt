/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.calls.checkers.checkCoroutinesFeature

object ModifierCheckerCore {
    fun check(
        listOwner: KtModifierListOwner,
        trace: BindingTrace,
        descriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (listOwner is KtDeclarationWithBody) {
            // KtFunction or KtPropertyAccessor
            for (parameter in listOwner.valueParameters) {
                if (!parameter.hasValOrVar()) {
                    check(parameter, trace, trace[BindingContext.VALUE_PARAMETER, parameter], languageVersionSettings)
                }
            }
        }
        val actualTargets = AnnotationChecker.getDeclarationSiteActualTargetList(
            listOwner, descriptor as? ClassDescriptor, trace.bindingContext
        )
        val list = listOwner.modifierList ?: return
        checkModifierList(list, trace, descriptor?.containingDeclaration, actualTargets, languageVersionSettings)
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.orSet(SOFT_KEYWORDS, TokenSet.create(IN_KEYWORD, FUN_KEYWORD))

    private fun checkModifierList(
        list: KtModifierList,
        trace: BindingTrace,
        parentDescriptor: DeclarationDescriptor?,
        actualTargets: List<KotlinTarget>,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (list.stub != null) return

        // It's a list of all nodes with error already reported
        // General strategy: report no more than one error but any number of warnings
        val incorrectNodes = hashSetOf<ASTNode>()

        val children = list.node.getChildren(MODIFIER_KEYWORD_SET)
        for (second in children) {
            for (first in children) {
                if (first == second) {
                    break
                }
                checkCompatibility(trace, first, second, list.owner, incorrectNodes)
            }
            if (second !in incorrectNodes) {
                when {
                    !checkTarget(trace, second, actualTargets) -> incorrectNodes += second
                    !checkParent(trace, second, parentDescriptor, languageVersionSettings) -> incorrectNodes += second
                    !checkLanguageLevelSupport(trace, second, languageVersionSettings, actualTargets) -> incorrectNodes += second
                }
            }
        }
    }

    private fun checkCompatibility(
        trace: BindingTrace,
        firstNode: ASTNode,
        secondNode: ASTNode,
        owner: PsiElement,
        incorrectNodes: MutableSet<ASTNode>
    ) {
        val firstModifier = firstNode.elementType as KtModifierKeywordToken
        val secondModifier = secondNode.elementType as KtModifierKeywordToken
        when (val compatibility = compatibility(firstModifier, secondModifier)) {
            Compatibility.COMPATIBLE -> {
            }
            Compatibility.REPEATED -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REPEATED_MODIFIER.on(secondNode.psi, firstModifier))
            }
            Compatibility.REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(secondNode.psi, secondModifier, firstModifier))
            Compatibility.REVERSE_REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(firstNode.psi, firstModifier, secondModifier))
            Compatibility.DEPRECATED -> {
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(firstNode.psi, firstModifier, secondModifier))
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(secondNode.psi, secondModifier, firstModifier))
            }
            Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, Compatibility.INCOMPATIBLE -> {
                if (compatibility == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY) {
                    if (owner is KtClassOrObject) return
                }
                if (incorrectNodes.add(firstNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(firstNode.psi, firstModifier, secondModifier))
                }
                if (incorrectNodes.add(secondNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(secondNode.psi, secondModifier, firstModifier))
                }
            }
        }
    }

    // Should return false if error is reported, true otherwise
    private fun checkTarget(trace: BindingTrace, node: ASTNode, actualTargets: List<KotlinTarget>): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken

        val possibleTargets = possibleTargetMap[modifier] ?: emptySet()
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(Errors.WRONG_MODIFIER_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
            return false
        }
        val deprecatedModifierReplacement = deprecatedModifierMap[modifier]
        val deprecatedTargets = deprecatedTargetMap[modifier] ?: emptySet()
        val redundantTargets = redundantTargetMap[modifier] ?: emptySet()
        when {
            deprecatedModifierReplacement != null ->
                trace.report(Errors.DEPRECATED_MODIFIER.on(node.psi, modifier, deprecatedModifierReplacement))
            actualTargets.any { it in deprecatedTargets } ->
                trace.report(
                    Errors.DEPRECATED_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
            actualTargets.any { it in redundantTargets } ->
                trace.report(
                    Errors.REDUNDANT_MODIFIER_FOR_TARGET.on(
                        node.psi,
                        modifier,
                        actualTargets.firstOrNull()?.description ?: "this"
                    )
                )
        }
        return true
    }

    // Should return false if error is reported, true otherwise
    private fun checkParent(
        trace: BindingTrace,
        node: ASTNode,
        parentDescriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken

        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
            is ClassDescriptor -> KotlinTarget.classActualTargets(
                parentDescriptor.kind,
                isInnerClass = parentDescriptor.isInner,
                isCompanionObject = parentDescriptor.isCompanionObject,
                isLocalClass = DescriptorUtils.isLocal(parentDescriptor)
            )
            is PropertySetterDescriptor -> KotlinTarget.PROPERTY_SETTER_LIST
            is PropertyGetterDescriptor -> KotlinTarget.PROPERTY_GETTER_LIST
            is FunctionDescriptor -> KotlinTarget.FUNCTION_LIST
            else -> KotlinTarget.FILE_LIST
        }
        val deprecatedParents = deprecatedParentTargetMap[modifier]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            trace.report(
                Errors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION.on(
                    node.psi,
                    modifier,
                    actualParents.firstOrNull()?.description ?: "this scope"
                )
            )
            return true
        }
        if (modifier == PROTECTED_KEYWORD && isFinalExpectClass(parentDescriptor)) {
            trace.report(
                Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(
                    node.psi,
                    modifier,
                    "final expect class"
                )
            )
        }
        val possibleParentPredicate = possibleParentTargetPredicateMap[modifier] ?: return true
        if (actualParents.any { possibleParentPredicate.isAllowed(it, languageVersionSettings) }) return true
        trace.report(
            Errors.WRONG_MODIFIER_CONTAINING_DECLARATION.on(
                node.psi,
                modifier,
                actualParents.firstOrNull()?.description ?: "this scope"
            )
        )
        return false
    }

    private fun checkLanguageLevelSupport(
        trace: BindingTrace,
        node: ASTNode,
        languageVersionSettings: LanguageVersionSettings,
        actualTargets: List<KotlinTarget>
    ): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken

        val dependencies = featureDependencies[modifier] ?: return true
        for (dependency in dependencies) {
            val restrictedTargets = featureDependenciesTargets[dependency]
            if (restrictedTargets != null && actualTargets.intersect(restrictedTargets).isEmpty()) {
                continue
            }

            val featureSupport = languageVersionSettings.getFeatureSupport(dependency)

            if (dependency == LanguageFeature.Coroutines) {
                checkCoroutinesFeature(languageVersionSettings, trace, node.psi)
                continue
            }

            if (dependency == LanguageFeature.InlineClasses) {
                if (languageVersionSettings.supportsFeature(LanguageFeature.JvmInlineValueClasses)) {
                    trace.report(Errors.INLINE_CLASS_DEPRECATED.on(node.psi))
                    continue
                }
            }

            val diagnosticData = dependency to languageVersionSettings
            when (featureSupport) {
                LanguageFeature.State.ENABLED_WITH_WARNING -> {
                    trace.report(Errors.EXPERIMENTAL_FEATURE_WARNING.on(node.psi, diagnosticData))
                }
                LanguageFeature.State.DISABLED -> {
                    trace.report(Errors.UNSUPPORTED_FEATURE.on(node.psi, diagnosticData))
                    return false
                }
                LanguageFeature.State.ENABLED -> {
                }
            }
        }

        return true
    }

    private fun isFinalExpectClass(d: DeclarationDescriptor?): Boolean {
        return d is ClassDescriptor && d.isFinalOrEnum && d.isExpect
    }
}