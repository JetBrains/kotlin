/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve;

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import java.util.*
import org.jetbrains.kotlin.resolve.KeywordType.*

enum class KeywordType {
    Inner,
    Override,
    Public,
    Protected,
    Internal,
    Private,
    Companion,
    Final,
    Vararg,
    Enum,
    Abstract,
    Open,
    Sealed,
    In,
    Out,
    Reified,
    Lateinit,
    Data,
    Inline,
    Noinline,
    Tailrec,
    Suspend,
    External,
    Annotation,
    Crossinline,
    Const,
    Operator,
    Infix,
    Header,
    Impl,
    Expect,
    Actual,
    Fun,
    Value
}

fun KeywordType.render(): String {
    return toString().lowercase()
}

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

val compatibilityTypeMap = hashMapOf<Pair<KeywordType, KeywordType>, Compatibility>()

fun compatibility(first: KeywordType, second: KeywordType): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
    }
}

// First modifier in pair should be also first in declaration
private val mutualCompatibility = buildCompatibilityMap()

private fun buildCompatibilityMap(): Map<Pair<KeywordType, KeywordType>, Compatibility> {
    val result = hashMapOf<Pair<KeywordType, KeywordType>, Compatibility>()
    // Variance: in + out are incompatible
    result += incompatibilityRegister(In, Out)
    // Visibilities: incompatible
    result += incompatibilityRegister(Private, Protected, Public, Internal)
    // Abstract + open + final + sealed: incompatible
    result += incompatibilityRegister(Abstract, Open, Final, Sealed)
    // data + open, data + inner, data + abstract, data + sealed, data + inline, data + value
    result += incompatibilityRegister(Data, Open)
    result += incompatibilityRegister(Data, Inner)
    result += incompatibilityRegister(Data, Abstract)
    result += incompatibilityRegister(Data, Sealed)
    result += incompatibilityRegister(Data, Inline)
    result += incompatibilityRegister(Data, Value)
    // open is redundant to abstract & override
    result += redundantRegister(Abstract, Open)
    // abstract is redundant to sealed
    result += redundantRegister(Sealed, Abstract)

    // const is incompatible with abstract, open, override
    result += incompatibilityRegister(Const, Abstract)
    result += incompatibilityRegister(Const, Open)
    result += incompatibilityRegister(Const, Override)

    // private is incompatible with override
    result += incompatibilityRegister(Private, Override)
    // private is compatible with open / abstract only for classes
    result += compatibilityForClassesRegister(Private, Open)
    result += compatibilityForClassesRegister(Private, Abstract)

    result += incompatibilityRegister(Crossinline, Noinline)

    // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
    // an instance of an outer sealed (effectively abstract) class
    // 2. subclasses of a non-top-level sealed class must be declared inside the class
    // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
    result += incompatibilityRegister(Sealed, Inner)

    // header / expect / impl / actual are all incompatible
    result += incompatibilityRegister(Header, Expect, Impl, Actual)

    return result
}

private fun incompatibilityRegister(vararg list: KeywordType): Map<Pair<KeywordType, KeywordType>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

private fun redundantRegister(
    sufficient: KeywordType,
    redundant: KeywordType
): Map<Pair<KeywordType, KeywordType>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}

private fun compatibilityForClassesRegister(vararg list: KeywordType) =
    compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

private fun compatibilityRegister(
    compatibility: Compatibility, vararg list: KeywordType
): Map<Pair<KeywordType, KeywordType>, Compatibility> {
    val result = hashMapOf<Pair<KeywordType, KeywordType>, Compatibility>()
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
    Suspend to listOf(LanguageFeature.Coroutines),
    Inline to listOf(LanguageFeature.InlineProperties, LanguageFeature.InlineClasses),
    Header to listOf(LanguageFeature.MultiPlatformProjects),
    Impl to listOf(LanguageFeature.MultiPlatformProjects),
    Expect to listOf(LanguageFeature.MultiPlatformProjects),
    Actual to listOf(LanguageFeature.MultiPlatformProjects),
    Lateinit to listOf(LanguageFeature.LateinitTopLevelProperties, LanguageFeature.LateinitLocalVariables),
    Fun to listOf(LanguageFeature.FunctionalInterfaceConversion)
)

val featureDependenciesTargets = mapOf(
    LanguageFeature.InlineProperties to setOf(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER),
    LanguageFeature.LateinitLocalVariables to setOf(LOCAL_VARIABLE),
    LanguageFeature.LateinitTopLevelProperties to setOf(TOP_LEVEL_PROPERTY),
    LanguageFeature.InlineClasses to setOf(CLASS_ONLY),
    LanguageFeature.JvmInlineValueClasses to setOf(CLASS_ONLY),
    LanguageFeature.FunctionalInterfaceConversion to setOf(INTERFACE)
)

val defaultVisibilityTargets: EnumSet<KotlinTarget> = EnumSet.of(
    CLASS_ONLY, OBJECT, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS,
    MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER,
    MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, CONSTRUCTOR, TYPEALIAS
)

val possibleTargetMap = mapOf(
    KeywordType.Enum to EnumSet.of(ENUM_CLASS),
    Abstract to EnumSet.of(
        CLASS_ONLY,
        LOCAL_CLASS,
        INTERFACE,
        MEMBER_PROPERTY,
        MEMBER_FUNCTION
    ),
    Open to EnumSet.of(
        CLASS_ONLY,
        LOCAL_CLASS,
        INTERFACE,
        MEMBER_PROPERTY,
        MEMBER_FUNCTION
    ),
    Final to EnumSet.of(
        CLASS_ONLY,
        LOCAL_CLASS,
        ENUM_CLASS,
        OBJECT,
        MEMBER_PROPERTY,
        MEMBER_FUNCTION
    ),
    Sealed to EnumSet.of(CLASS_ONLY, INTERFACE),
    Inner to EnumSet.of(CLASS_ONLY),
    Override to EnumSet.of(MEMBER_PROPERTY, MEMBER_FUNCTION),
    Private to defaultVisibilityTargets,
    Public to defaultVisibilityTargets,
    Internal to defaultVisibilityTargets,
    Protected to EnumSet.of(
        CLASS_ONLY,
        OBJECT,
        INTERFACE,
        ENUM_CLASS,
        ANNOTATION_CLASS,
        MEMBER_FUNCTION,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        MEMBER_PROPERTY,
        CONSTRUCTOR,
        TYPEALIAS
    ),
    In to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
    Out to EnumSet.of(TYPE_PARAMETER, TYPE_PROJECTION),
    Reified to EnumSet.of(TYPE_PARAMETER),
    Vararg to EnumSet.of(VALUE_PARAMETER, PROPERTY_PARAMETER),
    KeywordType.Companion to EnumSet.of(OBJECT),
    Lateinit to EnumSet.of(MEMBER_PROPERTY, TOP_LEVEL_PROPERTY, LOCAL_VARIABLE),
    Data to EnumSet.of(CLASS_ONLY, LOCAL_CLASS),
    Inline to EnumSet.of(
        FUNCTION,
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        CLASS_ONLY
    ),
    Noinline to EnumSet.of(VALUE_PARAMETER),
    Tailrec to EnumSet.of(FUNCTION),
    Suspend to EnumSet.of(MEMBER_FUNCTION, TOP_LEVEL_FUNCTION, LOCAL_FUNCTION),
    External to EnumSet.of(
        FUNCTION,
        PROPERTY,
        PROPERTY_GETTER,
        PROPERTY_SETTER,
        CLASS
    ),
    KeywordType.Annotation to EnumSet.of(ANNOTATION_CLASS), // TODO: Workaround for FIR, https://youtrack.jetbrains.com/issue/KT-48157
    Crossinline to EnumSet.of(VALUE_PARAMETER),
    Const to EnumSet.of(MEMBER_PROPERTY, TOP_LEVEL_PROPERTY),
    Operator to EnumSet.of(FUNCTION),
    Infix to EnumSet.of(FUNCTION),
    Header to EnumSet.of(
        TOP_LEVEL_FUNCTION,
        TOP_LEVEL_PROPERTY,
        CLASS_ONLY,
        OBJECT,
        INTERFACE,
        ENUM_CLASS,
        ANNOTATION_CLASS
    ),
    Impl to EnumSet.of(
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
    Expect to EnumSet.of(
        TOP_LEVEL_FUNCTION,
        TOP_LEVEL_PROPERTY,
        CLASS_ONLY,
        OBJECT,
        INTERFACE,
        ENUM_CLASS,
        ANNOTATION_CLASS
    ),
    Actual to EnumSet.of(
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
    Fun to EnumSet.of(INTERFACE),
    Value to EnumSet.of(CLASS_ONLY)
)

// NOTE: deprecated targets must be possible!
val deprecatedTargetMap = mapOf<KeywordType, Set<KotlinTarget>>()

val deprecatedParentTargetMap = mapOf<KeywordType, Set<KotlinTarget>>()

val deprecatedModifierMap = mapOf(
    Header to Expect,
    Impl to Actual
)

// NOTE: redundant targets must be possible!
val redundantTargetMap = mapOf<KeywordType, Set<KotlinTarget>>(
    Open to EnumSet.of(INTERFACE)
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
    Inner to or(
        always(CLASS_ONLY, LOCAL_CLASS, ENUM_CLASS),
        ifSupported(LanguageFeature.InnerClassInEnumEntryClass, ENUM_ENTRY)
    ),
    Override to always(
        CLASS_ONLY,
        LOCAL_CLASS,
        OBJECT,
        OBJECT_LITERAL,
        INTERFACE,
        ENUM_CLASS,
        ENUM_ENTRY
    ),
    Protected to always(CLASS_ONLY, LOCAL_CLASS, ENUM_CLASS, COMPANION_OBJECT),
    Internal to always(
        CLASS_ONLY,
        LOCAL_CLASS,
        OBJECT,
        OBJECT_LITERAL,
        ENUM_CLASS,
        ENUM_ENTRY,
        FILE
    ),
    Private to always(
        CLASS_ONLY,
        LOCAL_CLASS,
        OBJECT,
        OBJECT_LITERAL,
        INTERFACE,
        ENUM_CLASS,
        ENUM_ENTRY,
        FILE
    ),
    KeywordType.Companion to always(CLASS_ONLY, INTERFACE, ENUM_CLASS, ANNOTATION_CLASS),
    Final to always(
        CLASS_ONLY,
        LOCAL_CLASS,
        OBJECT,
        OBJECT_LITERAL,
        ENUM_CLASS,
        ENUM_ENTRY,
        ANNOTATION_CLASS,
        FILE
    ),
    Vararg to always(CONSTRUCTOR, FUNCTION, CLASS)
)
