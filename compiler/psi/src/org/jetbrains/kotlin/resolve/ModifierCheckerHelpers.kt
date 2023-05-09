/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.*
import java.util.*

enum class Compatibility {
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

val compatibilityTypeMap = hashMapOf<Pair<KtKeywordToken, KtKeywordToken>, Compatibility>()

fun compatibility(first: KtKeywordToken, second: KtKeywordToken): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
    }
}

// First modifier in pair should be also first in declaration
private val mutualCompatibility = buildCompatibilityMap()

private fun buildCompatibilityMap(): Map<Pair<KtKeywordToken, KtKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<KtKeywordToken, KtKeywordToken>, Compatibility>()
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
    result += incompatibilityRegister(DATA_KEYWORD, OBJECT_KEYWORD, EXPECT_KEYWORD)
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

private fun incompatibilityRegister(vararg list: KtKeywordToken): Map<Pair<KtKeywordToken, KtKeywordToken>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

private fun redundantRegister(
    sufficient: KtKeywordToken,
    redundant: KtKeywordToken
): Map<Pair<KtKeywordToken, KtKeywordToken>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}

private fun compatibilityForClassesRegister(vararg list: KtKeywordToken) =
    compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

private fun compatibilityRegister(
    compatibility: Compatibility, vararg list: KtKeywordToken
): Map<Pair<KtKeywordToken, KtKeywordToken>, Compatibility> {
    val result = hashMapOf<Pair<KtKeywordToken, KtKeywordToken>, Compatibility>()
    for (first in list) {
        for (second in list) {
            if (first != second) {
                result[Pair(first, second)] = compatibility
            }
        }
    }
    return result
}

val featureDependencies = mapOf(
    SUSPEND_KEYWORD to listOf(LanguageFeature.Coroutines),
    INLINE_KEYWORD to listOf(LanguageFeature.InlineProperties, LanguageFeature.InlineClasses),
    HEADER_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
    IMPL_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
    EXPECT_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
    ACTUAL_KEYWORD to listOf(LanguageFeature.MultiPlatformProjects),
    LATEINIT_KEYWORD to listOf(LanguageFeature.LateinitTopLevelProperties, LanguageFeature.LateinitLocalVariables),
    FUN_KEYWORD to listOf(LanguageFeature.FunctionalInterfaceConversion),
    DATA_KEYWORD to listOf(LanguageFeature.DataObjects)
)

val featureDependenciesTargets = mapOf(
    LanguageFeature.InlineProperties to setOf(KotlinTarget.PROPERTY, KotlinTarget.PROPERTY_GETTER, KotlinTarget.PROPERTY_SETTER),
    LanguageFeature.LateinitLocalVariables to setOf(KotlinTarget.LOCAL_VARIABLE),
    LanguageFeature.LateinitTopLevelProperties to setOf(KotlinTarget.TOP_LEVEL_PROPERTY),
    LanguageFeature.InlineClasses to setOf(KotlinTarget.CLASS_ONLY),
    LanguageFeature.JvmInlineValueClasses to setOf(KotlinTarget.CLASS_ONLY),
    LanguageFeature.FunctionalInterfaceConversion to setOf(KotlinTarget.INTERFACE),
    LanguageFeature.DataObjects to setOf(KotlinTarget.STANDALONE_OBJECT)
)

val defaultVisibilityTargets: EnumSet<KotlinTarget> = EnumSet.of(
    KotlinTarget.CLASS_ONLY, KotlinTarget.OBJECT, KotlinTarget.INTERFACE, KotlinTarget.ENUM_CLASS, KotlinTarget.ANNOTATION_CLASS,
    KotlinTarget.MEMBER_FUNCTION, KotlinTarget.TOP_LEVEL_FUNCTION, KotlinTarget.PROPERTY_GETTER, KotlinTarget.PROPERTY_SETTER,
    KotlinTarget.MEMBER_PROPERTY, KotlinTarget.TOP_LEVEL_PROPERTY, KotlinTarget.CONSTRUCTOR, KotlinTarget.TYPEALIAS,
)

val possibleTargetMap = mapOf(
    ENUM_KEYWORD to EnumSet.of(KotlinTarget.ENUM_CLASS),
    ABSTRACT_KEYWORD to EnumSet.of(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.INTERFACE,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    OPEN_KEYWORD to EnumSet.of(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.INTERFACE,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    FINAL_KEYWORD to EnumSet.of(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    SEALED_KEYWORD to EnumSet.of(KotlinTarget.CLASS_ONLY, KotlinTarget.INTERFACE),
    INNER_KEYWORD to EnumSet.of(KotlinTarget.CLASS_ONLY),
    OVERRIDE_KEYWORD to EnumSet.of(KotlinTarget.MEMBER_PROPERTY, KotlinTarget.MEMBER_FUNCTION),
    PRIVATE_KEYWORD to defaultVisibilityTargets + KotlinTarget.BACKING_FIELD,
    PUBLIC_KEYWORD to defaultVisibilityTargets,
    INTERNAL_KEYWORD to defaultVisibilityTargets + KotlinTarget.BACKING_FIELD,
    PROTECTED_KEYWORD to EnumSet.of(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.PROPERTY_GETTER,
        KotlinTarget.PROPERTY_SETTER,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.TYPEALIAS
    ),
    IN_KEYWORD to EnumSet.of(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
    OUT_KEYWORD to EnumSet.of(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
    REIFIED_KEYWORD to EnumSet.of(KotlinTarget.TYPE_PARAMETER),
    VARARG_KEYWORD to EnumSet.of(KotlinTarget.VALUE_PARAMETER, KotlinTarget.PROPERTY_PARAMETER),
    COMPANION_KEYWORD to EnumSet.of(KotlinTarget.OBJECT),
    LATEINIT_KEYWORD to EnumSet.of(
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.LOCAL_VARIABLE,
        KotlinTarget.BACKING_FIELD
    ),
    DATA_KEYWORD to EnumSet.of(KotlinTarget.CLASS_ONLY, KotlinTarget.LOCAL_CLASS, KotlinTarget.STANDALONE_OBJECT),
    INLINE_KEYWORD to EnumSet.of(
        KotlinTarget.FUNCTION,
        KotlinTarget.PROPERTY,
        KotlinTarget.PROPERTY_GETTER,
        KotlinTarget.PROPERTY_SETTER,
        KotlinTarget.CLASS_ONLY
    ),
    NOINLINE_KEYWORD to EnumSet.of(KotlinTarget.VALUE_PARAMETER),
    TAILREC_KEYWORD to EnumSet.of(KotlinTarget.FUNCTION),
    SUSPEND_KEYWORD to EnumSet.of(
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.LOCAL_FUNCTION,
        KotlinTarget.ANONYMOUS_FUNCTION
    ),
    EXTERNAL_KEYWORD to EnumSet.of(
        KotlinTarget.FUNCTION,
        KotlinTarget.PROPERTY,
        KotlinTarget.PROPERTY_GETTER,
        KotlinTarget.PROPERTY_SETTER,
        KotlinTarget.CLASS
    ),
    ANNOTATION_KEYWORD to EnumSet.of(KotlinTarget.ANNOTATION_CLASS),
    CROSSINLINE_KEYWORD to EnumSet.of(KotlinTarget.VALUE_PARAMETER),
    CONST_KEYWORD to EnumSet.of(KotlinTarget.MEMBER_PROPERTY, KotlinTarget.TOP_LEVEL_PROPERTY),
    OPERATOR_KEYWORD to EnumSet.of(KotlinTarget.FUNCTION),
    INFIX_KEYWORD to EnumSet.of(KotlinTarget.FUNCTION),
    HEADER_KEYWORD to EnumSet.of(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS
    ),
    IMPL_KEYWORD to EnumSet.of(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.TYPEALIAS
    ),
    EXPECT_KEYWORD to EnumSet.of(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS
    ),
    ACTUAL_KEYWORD to EnumSet.of(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.TYPEALIAS
    ),
    FUN_KEYWORD to EnumSet.of(KotlinTarget.INTERFACE),
    VALUE_KEYWORD to EnumSet.of(KotlinTarget.CLASS_ONLY)
)

// NOTE: deprecated targets must be possible!
val deprecatedTargetMap = mapOf<KtKeywordToken, Set<KotlinTarget>>()

val deprecatedParentTargetMap = mapOf<KtKeywordToken, Set<KotlinTarget>>()

val deprecatedModifierMap = mapOf(
    HEADER_KEYWORD to EXPECT_KEYWORD,
    IMPL_KEYWORD to ACTUAL_KEYWORD
)

// NOTE: redundant targets must be possible!
val redundantTargetMap = mapOf<KtKeywordToken, Set<KotlinTarget>>(
    OPEN_KEYWORD to EnumSet.of(KotlinTarget.INTERFACE)
)

interface TargetAllowedPredicate {
    fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}

fun always(target: KotlinTarget, vararg targets: KotlinTarget) = object : TargetAllowedPredicate {
    private val targetSet = EnumSet.of(target, *targets)

    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        target in targetSet
}

fun ifSupported(languageFeature: LanguageFeature, target: KotlinTarget, vararg targets: KotlinTarget) =
    object : TargetAllowedPredicate {
        private val targetSet = EnumSet.of(target, *targets)

        override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
            languageVersionSettings.supportsFeature(languageFeature) && target in targetSet
    }

fun or(p1: TargetAllowedPredicate, p2: TargetAllowedPredicate) = object : TargetAllowedPredicate {
    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        p1.isAllowed(target, languageVersionSettings) ||
                p2.isAllowed(target, languageVersionSettings)
}

val possibleParentTargetPredicateMap = mapOf(
    INNER_KEYWORD to or(
        always(KotlinTarget.CLASS_ONLY, KotlinTarget.LOCAL_CLASS, KotlinTarget.ENUM_CLASS),
        ifSupported(LanguageFeature.InnerClassInEnumEntryClass, KotlinTarget.ENUM_ENTRY)
    ),
    OVERRIDE_KEYWORD to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY
    ),
    PROTECTED_KEYWORD to always(KotlinTarget.CLASS_ONLY, KotlinTarget.LOCAL_CLASS, KotlinTarget.ENUM_CLASS, KotlinTarget.COMPANION_OBJECT),
    INTERNAL_KEYWORD to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.FILE
    ),
    PRIVATE_KEYWORD to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.FILE
    ),
    COMPANION_KEYWORD to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS
    ),
    FINAL_KEYWORD to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.FILE
    ),
    VARARG_KEYWORD to always(KotlinTarget.CONSTRUCTOR, KotlinTarget.FUNCTION, KotlinTarget.CLASS)
)


