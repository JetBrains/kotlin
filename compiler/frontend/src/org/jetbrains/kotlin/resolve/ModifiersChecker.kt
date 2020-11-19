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
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.resolve.calls.checkers.checkCoroutinesFeature
import java.util.*

object ModifierCheckerCore {
    private enum class Compatibility {
        // modifier pair is compatible: ok (default)
        COMPATIBLE,
        // second is redundant to first: warning
        REDUNDANT,
        // first is redundant to second: warning
        REVERSE_REDUNDANT,
        // error
        REPEATED,
        // pair is deprecated, will become incompatible: warning
        DEPRECATED,
        // pair is incompatible: error
        INCOMPATIBLE,
        // same but only for functions / properties: error
        COMPATIBLE_FOR_CLASSES_ONLY
    }

    private val defaultVisibilityTargets = EnumSet.of(
        CLASS_ONLY, OBJECT, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS,
        MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
        MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR, TYPEALIAS
    )

    val possibleTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>(
        ENUM_KEYWORD to EnumSet.of(ENUM_CLASS),
        ABSTRACT_KEYWORD to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
        OPEN_KEYWORD to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, INTERFACE, MEMBER_PROPERTY, MEMBER_FUNCTION),
        FINAL_KEYWORD to EnumSet.of(CLASS_ONLY, LOCAL_CLASS, ENUM_CLASS, OBJECT, MEMBER_PROPERTY, MEMBER_FUNCTION),
        SEALED_KEYWORD to EnumSet.of(CLASS_ONLY),
        INNER_KEYWORD to EnumSet.of(CLASS_ONLY),
        OVERRIDE_KEYWORD to EnumSet.of(MEMBER_PROPERTY, MEMBER_FUNCTION),
        PRIVATE_KEYWORD to defaultVisibilityTargets,
        PUBLIC_KEYWORD to defaultVisibilityTargets,
        INTERNAL_KEYWORD to defaultVisibilityTargets,
        PROTECTED_KEYWORD to EnumSet.of(
            CLASS_ONLY, OBJECT, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS,
            MEMBER_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, MEMBER_PROPERTY, CONSTRUCTOR, TYPEALIAS
        ),
        IN_KEYWORD to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
        OUT_KEYWORD to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
        REIFIED_KEYWORD to EnumSet.of(TYPE_PARAMETER),
        VARARG_KEYWORD to EnumSet.of(VALUE_PARAMETER, PROPERTY_PARAMETER),
        COMPANION_KEYWORD to EnumSet.of(OBJECT),
        LATEINIT_KEYWORD to EnumSet.of(MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, LOCAL_VARIABLE),
        DATA_KEYWORD to EnumSet.of(CLASS_ONLY, LOCAL_CLASS),
        INLINE_KEYWORD to EnumSet.of(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, CLASS_ONLY),
        NOINLINE_KEYWORD to EnumSet.of(VALUE_PARAMETER),
        TAILREC_KEYWORD to EnumSet.of(FUNCTION),
        SUSPEND_KEYWORD to EnumSet.of(MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, LOCAL_FUNCTION),
        EXTERNAL_KEYWORD to EnumSet.of(FUNCTION, PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER, CLASS),
        ANNOTATION_KEYWORD to EnumSet.of(ANNOTATION_CLASS),
        CROSSINLINE_KEYWORD to EnumSet.of(VALUE_PARAMETER),
        CONST_KEYWORD to EnumSet.of(MEMBER_PROPERTY, TOP_LEVEL_PROPERTY),
        OPERATOR_KEYWORD to EnumSet.of(FUNCTION),
        INFIX_KEYWORD to EnumSet.of(FUNCTION),
        HEADER_KEYWORD to EnumSet.of(TOP_LEVEL_FUNCTION, TOP_LEVEL_PROPERTY, CLASS_ONLY, OBJECT, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS),
        IMPL_KEYWORD to EnumSet.of(
            TOP_LEVEL_FUNCTION,
            MEMBER_FUNCTION,
            TOP_LEVEL_PROPERTY,
            MEMBER_PROPERTY,
            CONSTRUCTOR,
            CLASS_ONLY,
            OBJECT,
            INTERFACE,
            ENUM_CLASS,
            ANNOTATION_CLASS,
            TYPEALIAS
        ),
        EXPECT_KEYWORD to EnumSet.of(TOP_LEVEL_FUNCTION, TOP_LEVEL_PROPERTY, CLASS_ONLY, OBJECT, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS),
        ACTUAL_KEYWORD to EnumSet.of(
            TOP_LEVEL_FUNCTION,
            MEMBER_FUNCTION,
            TOP_LEVEL_PROPERTY,
            MEMBER_PROPERTY,
            CONSTRUCTOR,
            CLASS_ONLY,
            OBJECT,
            INTERFACE,
            ENUM_CLASS,
            ANNOTATION_CLASS,
            TYPEALIAS
        ),
        FUN_KEYWORD to EnumSet.of(INTERFACE),
        VALUE_KEYWORD to EnumSet.of(CLASS_ONLY)
    )

    private val featureDependencies = mapOf(
        SUSPEND_KEYWORD to listOf(LanguageFeature.Coroutines),
        INLINE_KEYWORD to listOf(LanguageFeature.InlineProperties, LanguageFeature.InlineClasses),
        HEADER_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
        IMPL_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
        EXPECT_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
        ACTUAL_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
        LATEINIT_KEYWORD to listOf(LanguageFeature.LateinitTopLevelProperties, LanguageFeature.LateinitLocalVariables),
        FUN_KEYWORD to listOf(LanguageFeature.FunctionalInterfaceConversion),
        VALUE_KEYWORD to listOf(LanguageFeature.InlineClasses)
    )

    private val featureDependenciesTargets = mapOf(
        LanguageFeature.InlineProperties to setOf(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER),
        LanguageFeature.LateinitLocalVariables to setOf(LOCAL_VARIABLE),
        LanguageFeature.LateinitTopLevelProperties to setOf(TOP_LEVEL_PROPERTY),
        LanguageFeature.InlineClasses to setOf(CLASS_ONLY),
        LanguageFeature.FunctionalInterfaceConversion to setOf(INTERFACE)
    )

    // NOTE: deprecated targets must be possible!
    private val deprecatedTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>()

    private val deprecatedModifierMap = mapOf(
        HEADER_KEYWORD to EXPECT_KEYWORD,
        IMPL_KEYWORD to ACTUAL_KEYWORD
    )

    // NOTE: redundant targets must be possible!
    private val redundantTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>(
        OPEN_KEYWORD to EnumSet.of(INTERFACE)
    )

    private val possibleParentTargetPredicateMap = mapOf<KtModifierKeywordToken, TargetAllowedPredicate>(
        INNER_KEYWORD to or(
            always(CLASS_ONLY, LOCAL_CLASS, ENUM_CLASS),
            ifSupported(LanguageFeature.InnerClassInEnumEntryClass, ENUM_ENTRY)
        ),
        OVERRIDE_KEYWORD to always(CLASS_ONLY, LOCAL_CLASS, OBJECT, OBJECT_LITERAL, INTERFACE, ENUM_CLASS, ENUM_ENTRY),
        PROTECTED_KEYWORD to always(CLASS_ONLY, LOCAL_CLASS, ENUM_CLASS, COMPANION_OBJECT),
        INTERNAL_KEYWORD to always(CLASS_ONLY, LOCAL_CLASS, OBJECT, OBJECT_LITERAL, ENUM_CLASS, ENUM_ENTRY, FILE),
        PRIVATE_KEYWORD to always(CLASS_ONLY, LOCAL_CLASS, OBJECT, OBJECT_LITERAL, INTERFACE, ENUM_CLASS, ENUM_ENTRY, FILE),
        COMPANION_KEYWORD to always(CLASS_ONLY, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS),
        FINAL_KEYWORD to always(CLASS_ONLY, LOCAL_CLASS, OBJECT, OBJECT_LITERAL, ENUM_CLASS, ENUM_ENTRY, ANNOTATION_CLASS, FILE),
        VARARG_KEYWORD to always(CONSTRUCTOR, FUNCTION, CLASS)
    )

    private val deprecatedParentTargetMap = mapOf<KtModifierKeywordToken, Set<KotlinTarget>>()

    fun isPossibleParentTarget(
        modifier: KtModifierKeywordToken,
        parentTarget: KotlinTarget,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        deprecatedParentTargetMap[modifier]?.let {
            if (parentTarget in it) return false
        }

        possibleParentTargetPredicateMap[modifier]?.let {
            return it.isAllowed(parentTarget, languageVersionSettings)
        }

        return true
    }

    // First modifier in pair should be also first in declaration
    private val mutualCompatibility = buildCompatibilityMap()

    private fun buildCompatibilityMap(): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility>()
        // Variance: in + out are incompatible
        result += incompatibilityRegister(IN_KEYWORD, OUT_KEYWORD)
        // Visibilities: incompatible
        result += incompatibilityRegister(PRIVATE_KEYWORD, PROTECTED_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD)
        // Abstract + open + final + sealed: incompatible
        result += incompatibilityRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD)
        // data + open, data + inner, data + abstract, data + sealed, data + inline, data + value
        result += incompatibilityRegister(DATA_KEYWORD, OPEN_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, INNER_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, ABSTRACT_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, SEALED_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, INLINE_KEYWORD)
        result += incompatibilityRegister(DATA_KEYWORD, VALUE_KEYWORD)
        // open is redundant to abstract & override
        result += redundantRegister(ABSTRACT_KEYWORD, OPEN_KEYWORD)
        // abstract is redundant to sealed
        result += redundantRegister(SEALED_KEYWORD, ABSTRACT_KEYWORD)

        // const is incompatible with abstract, open, override
        result += incompatibilityRegister(CONST_KEYWORD, ABSTRACT_KEYWORD)
        result += incompatibilityRegister(CONST_KEYWORD, OPEN_KEYWORD)
        result += incompatibilityRegister(CONST_KEYWORD, OVERRIDE_KEYWORD)

        // private is incompatible with override
        result += incompatibilityRegister(PRIVATE_KEYWORD, OVERRIDE_KEYWORD)
        // private is compatible with open / abstract only for classes
        result += compatibilityForClassesRegister(PRIVATE_KEYWORD, OPEN_KEYWORD)
        result += compatibilityForClassesRegister(PRIVATE_KEYWORD, ABSTRACT_KEYWORD)

        result += incompatibilityRegister(CROSSINLINE_KEYWORD, NOINLINE_KEYWORD)

        // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
        // an instance of an outer sealed (effectively abstract) class
        // 2. subclasses of a non-top-level sealed class must be declared inside the class
        // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
        result += incompatibilityRegister(SEALED_KEYWORD, INNER_KEYWORD)

        // header / expect / impl / actual are all incompatible
        result += incompatibilityRegister(HEADER_KEYWORD, EXPECT_KEYWORD, IMPL_KEYWORD, ACTUAL_KEYWORD)

        return result
    }

    private fun redundantRegister(
        sufficient: KtModifierKeywordToken,
        redundant: KtModifierKeywordToken
    ): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        return mapOf(
            Pair(sufficient, redundant) to Compatibility.REDUNDANT,
            Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
        )
    }

    private fun compatibilityRegister(
        compatibility: Compatibility, vararg list: KtModifierKeywordToken
    ): Map<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility> {
        val result = hashMapOf<Pair<KtModifierKeywordToken, KtModifierKeywordToken>, Compatibility>()
        for (first in list) {
            for (second in list) {
                if (first != second) {
                    result[Pair(first, second)] = compatibility
                }
            }
        }
        return result
    }

    private fun compatibilityForClassesRegister(vararg list: KtModifierKeywordToken) =
        compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

    private fun incompatibilityRegister(vararg list: KtModifierKeywordToken) = compatibilityRegister(Compatibility.INCOMPATIBLE, *list)

    private fun deprecationRegister(vararg list: KtModifierKeywordToken) = compatibilityRegister(Compatibility.DEPRECATED, *list)

    private fun compatibility(first: KtModifierKeywordToken, second: KtModifierKeywordToken): Compatibility {
        return if (first == second) {
            Compatibility.REPEATED
        } else {
            mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
        }
    }

    private fun checkCompatibility(
        trace: BindingTrace,
        firstNode: ASTNode,
        secondNode: ASTNode,
        owner: PsiElement,
        incorrectNodes: MutableSet<ASTNode>
    ) {
        val first = firstNode.elementType as KtModifierKeywordToken
        val second = secondNode.elementType as KtModifierKeywordToken
        val compatibility = compatibility(first, second)
        when (compatibility) {
            Compatibility.COMPATIBLE -> {
            }
            Compatibility.REPEATED -> if (incorrectNodes.add(secondNode)) {
                trace.report(Errors.REPEATED_MODIFIER.on(secondNode.psi, first))
            }
            Compatibility.REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(secondNode.psi, second, first))
            Compatibility.REVERSE_REDUNDANT ->
                trace.report(Errors.REDUNDANT_MODIFIER.on(firstNode.psi, first, second))
            Compatibility.DEPRECATED -> {
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(firstNode.psi, first, second))
                trace.report(Errors.DEPRECATED_MODIFIER_PAIR.on(secondNode.psi, second, first))
            }
            Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, Compatibility.INCOMPATIBLE -> {
                if (compatibility == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY) {
                    if (owner is KtClassOrObject) return
                }
                if (incorrectNodes.add(firstNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(firstNode.psi, first, second))
                }
                if (incorrectNodes.add(secondNode)) {
                    trace.report(Errors.INCOMPATIBLE_MODIFIERS.on(secondNode.psi, second, first))
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


    // Should return false if error is reported, true otherwise
    private fun checkParent(
        trace: BindingTrace,
        node: ASTNode,
        parentDescriptor: DeclarationDescriptor?,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        val modifier = node.elementType as KtModifierKeywordToken
        val actualParents: List<KotlinTarget> = when (parentDescriptor) {
            is ClassDescriptor -> KotlinTarget.classActualTargets(parentDescriptor)
            is PropertySetterDescriptor -> listOf(PROPERTY_SETTER)
            is PropertyGetterDescriptor -> listOf(PROPERTY_GETTER)
            is FunctionDescriptor -> listOf(FUNCTION)
            else -> listOf(FILE)
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
}


private interface TargetAllowedPredicate {
    fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}


private fun always(target: KotlinTarget, vararg targets: KotlinTarget) = object : TargetAllowedPredicate {
    private val targetSet = EnumSet.of(target, *targets)

    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        target in targetSet
}

private fun ifSupported(languageFeature: LanguageFeature, target: KotlinTarget, vararg targets: KotlinTarget) =
    object : TargetAllowedPredicate {
        private val targetSet = EnumSet.of(target, *targets)

        override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
            languageVersionSettings.supportsFeature(languageFeature) && target in targetSet
    }

private fun or(p1: TargetAllowedPredicate, p2: TargetAllowedPredicate) = object : TargetAllowedPredicate {
    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        p1.isAllowed(target, languageVersionSettings) ||
                p2.isAllowed(target, languageVersionSettings)
}
