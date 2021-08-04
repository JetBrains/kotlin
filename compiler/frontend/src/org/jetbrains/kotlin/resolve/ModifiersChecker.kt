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
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.KeywordType.*
import org.jetbrains.kotlin.resolve.KeywordType.Annotation
import org.jetbrains.kotlin.resolve.calls.checkers.checkCoroutinesFeature

object ModifierCheckerCore {
    private val ktKeywordToKeywordTypeMap: Map<KtKeywordToken, KeywordType> = mapOf(
        INNER_KEYWORD to Inner,
        OVERRIDE_KEYWORD to Override,
        PUBLIC_KEYWORD to Public,
        PROTECTED_KEYWORD to Protected,
        INTERNAL_KEYWORD to Internal,
        PRIVATE_KEYWORD to Private,
        COMPANION_KEYWORD to KeywordType.Companion,
        FINAL_KEYWORD to Final,
        VARARG_KEYWORD to Vararg,
        ENUM_KEYWORD to KeywordType.Enum,
        ABSTRACT_KEYWORD to Abstract,
        OPEN_KEYWORD to Open,
        SEALED_KEYWORD to Sealed,
        IN_KEYWORD to In,
        OUT_KEYWORD to Out,
        REIFIED_KEYWORD to Reified,
        LATEINIT_KEYWORD to Lateinit,
        DATA_KEYWORD to Data,
        INLINE_KEYWORD to Inline,
        NOINLINE_KEYWORD to Noinline,
        TAILREC_KEYWORD to Tailrec,
        SUSPEND_KEYWORD to Suspend,
        EXTERNAL_KEYWORD to External,
        ANNOTATION_KEYWORD to Annotation,
        CROSSINLINE_KEYWORD to Crossinline,
        CONST_KEYWORD to Const,
        OPERATOR_KEYWORD to Operator,
        INFIX_KEYWORD to Infix,
        HEADER_KEYWORD to Header,
        IMPL_KEYWORD to Impl,
        EXPECT_KEYWORD to Expect,
        ACTUAL_KEYWORD to Actual,
        FUN_KEYWORD to Fun,
        VALUE_KEYWORD to Value
    )

    fun check(
        listOwner: KtModifierListOwner,
        trace: BindingTrace,
        descriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        if (listOwner is KtDeclarationWithBody) {
            // JetFunction or JetPropertyAccessor
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
        val (firstModifier, firstModifierType) = getModifierAndModifierType(firstNode)
        val (secondModifier, secondModifierType) = getModifierAndModifierType(secondNode)

        when (val compatibility = compatibility(firstModifierType, secondModifierType)) {
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
        val (modifier, modifierType) = getModifierAndModifierType(node)

        val possibleTargets = possibleTargetMap[modifierType] ?: emptySet()
        if (!actualTargets.any { it in possibleTargets }) {
            trace.report(Errors.WRONG_MODIFIER_TARGET.on(node.psi, modifier, actualTargets.firstOrNull()?.description ?: "this"))
            return false
        }
        val deprecatedModifierReplacement = deprecatedModifierMap[modifierType]
        val deprecatedTargets = deprecatedTargetMap[modifierType] ?: emptySet()
        val redundantTargets = redundantTargetMap[modifierType] ?: emptySet()
        when {
            deprecatedModifierReplacement != null ->
                trace.report(Errors.DEPRECATED_MODIFIER.on(node.psi, modifier, deprecatedModifierReplacement.render()))
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
        val (modifier, modifierType) = getModifierAndModifierType(node)

        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
            is ClassDescriptor -> KotlinTarget.classActualTargets(
                parentDescriptor.kind,
                isInnerClass = parentDescriptor.isInner,
                isCompanionObject = parentDescriptor.isCompanionObject,
                isLocalClass = DescriptorUtils.isLocal(parentDescriptor)
            )
            is PropertySetterDescriptor -> listOf(PROPERTY_SETTER)
            is PropertyGetterDescriptor -> listOf(PROPERTY_GETTER)
            is FunctionDescriptor -> listOf(FUNCTION)
            else -> listOf(FILE)
        }
        val deprecatedParents = deprecatedParentTargetMap[modifierType]
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
        val possibleParentPredicate = possibleParentTargetPredicateMap[modifierType] ?: return true
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
        val (_, modifierType) = getModifierAndModifierType(node)

        val dependencies = featureDependencies[modifierType] ?: return true
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
                LanguageFeature.State.ENABLED_WITH_ERROR -> {
                    trace.report(Errors.EXPERIMENTAL_FEATURE_ERROR.on(node.psi, diagnosticData))
                    return false
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

    private fun getModifierAndModifierType(node: ASTNode): Pair<KtModifierKeywordToken, KeywordType> {
        val modifier = node.elementType as KtModifierKeywordToken
        return Pair(modifier, ktKeywordToKeywordTypeMap[modifier]!!)
    }
}