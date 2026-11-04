/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.KtTokenId
import org.jetbrains.kotlin.SyntaxId
import org.jetbrains.kotlin.SyntaxIdSet
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.syntaxIdSetOf
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
    COMPATIBLE_FOR_CLASSES_ONLY,

    /**
     * The combination of `inline` and `value` is allowed,
     * but only since [LanguageFeature.ExplicitInlineValueClassesSyntax].
     */
    INLINE_VALUE_ALLOWED,
}

fun compatibility(first: SyntaxId, second: SyntaxId): Compatibility {
    return if (first == second) {
        Compatibility.REPEATED
    } else {
        mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
    }
}

// First modifier in pair should be also first in declaration
private val mutualCompatibility = buildCompatibilityMap()

private fun buildCompatibilityMap(): Map<Pair<SyntaxId, SyntaxId>, Compatibility> {
    val result = hashMapOf<Pair<SyntaxId, SyntaxId>, Compatibility>()
    // Variance: in + out are incompatible
    result += incompatibilityRegister(KtTokenId.IN_MODIFIER, KtTokenId.OUT_MODIFIER)
    // Visibilities: incompatible
    result += incompatibilityRegister(
        KtTokenId.PRIVATE_MODIFIER, KtTokenId.PROTECTED_MODIFIER, KtTokenId.PUBLIC_MODIFIER, KtTokenId.INTERNAL_MODIFIER
    )
    // Abstract + open + final + sealed: incompatible
    result += incompatibilityRegister(
        KtTokenId.ABSTRACT_MODIFIER, KtTokenId.OPEN_MODIFIER, KtTokenId.FINAL_MODIFIER, KtTokenId.SEALED_MODIFIER
    )
    // data + open, data + inner, data + abstract, data + sealed, data + inline, data + value
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.OPEN_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.INNER_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.ABSTRACT_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.SEALED_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.INLINE_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.VALUE_MODIFIER)
    result += compatibilityRegister(Compatibility.INLINE_VALUE_ALLOWED, KtTokenId.INLINE_MODIFIER, KtTokenId.VALUE_MODIFIER)
    result += incompatibilityRegister(KtTokenId.DATA_MODIFIER, KtTokenId.OBJECT_KEYWORD, KtTokenId.EXPECT_MODIFIER)
    // open is redundant to abstract & override
    result += redundantRegister(KtTokenId.ABSTRACT_MODIFIER, KtTokenId.OPEN_MODIFIER)
    // abstract is redundant to sealed
    result += redundantRegister(KtTokenId.SEALED_MODIFIER, KtTokenId.ABSTRACT_MODIFIER)

    // const is incompatible with abstract, open, override
    result += incompatibilityRegister(KtTokenId.CONST_MODIFIER, KtTokenId.ABSTRACT_MODIFIER)
    result += incompatibilityRegister(KtTokenId.CONST_MODIFIER, KtTokenId.OPEN_MODIFIER)
    result += incompatibilityRegister(KtTokenId.CONST_MODIFIER, KtTokenId.OVERRIDE_MODIFIER)

    // private is incompatible with override
    result += incompatibilityRegister(KtTokenId.PRIVATE_MODIFIER, KtTokenId.OVERRIDE_MODIFIER)
    // private is compatible with open / abstract only for classes
    result += compatibilityForClassesRegister(KtTokenId.PRIVATE_MODIFIER, KtTokenId.OPEN_MODIFIER)
    result += compatibilityForClassesRegister(KtTokenId.PRIVATE_MODIFIER, KtTokenId.ABSTRACT_MODIFIER)

    result += incompatibilityRegister(KtTokenId.CROSSINLINE_MODIFIER, KtTokenId.NOINLINE_MODIFIER)

    // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
    // an instance of an outer sealed (effectively abstract) class
    // 2. subclasses of a non-top-level sealed class must be declared inside the class
    // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
    result += incompatibilityRegister(KtTokenId.SEALED_MODIFIER, KtTokenId.INNER_MODIFIER)

    // expect / actual are all incompatible
    result += incompatibilityRegister(KtTokenId.EXPECT_MODIFIER, KtTokenId.ACTUAL_MODIFIER)

    return result
}

private fun incompatibilityRegister(vararg list: SyntaxId): Map<Pair<SyntaxId, SyntaxId>, Compatibility> {
    return compatibilityRegister(Compatibility.INCOMPATIBLE, *list)
}

private fun redundantRegister(
    sufficient: SyntaxId,
    redundant: SyntaxId
): Map<Pair<SyntaxId, SyntaxId>, Compatibility> {
    return mapOf(
        Pair(sufficient, redundant) to Compatibility.REDUNDANT,
        Pair(redundant, sufficient) to Compatibility.REVERSE_REDUNDANT
    )
}

private fun compatibilityForClassesRegister(vararg list: SyntaxId) =
    compatibilityRegister(Compatibility.COMPATIBLE_FOR_CLASSES_ONLY, *list)

private fun compatibilityRegister(
    compatibility: Compatibility, vararg list: SyntaxId
): Map<Pair<SyntaxId, SyntaxId>, Compatibility> {
    val result = hashMapOf<Pair<SyntaxId, SyntaxId>, Compatibility>()
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
    KtTokenId.INLINE_MODIFIER to listOf(LanguageFeature.InlineProperties, LanguageFeature.InlineClasses),
    KtTokenId.EXPECT_MODIFIER to listOf(LanguageFeature.MultiPlatformProjects),
    KtTokenId.ACTUAL_MODIFIER to listOf(LanguageFeature.MultiPlatformProjects),
)

val featureDependenciesTargets = mapOf(
    LanguageFeature.InlineClasses to setOf(KotlinTarget.CLASS_ONLY),
    LanguageFeature.JvmInlineValueClasses to setOf(KotlinTarget.CLASS_ONLY),
)

val defaultVisibilityTargetPredicate = always(
    KotlinTarget.CLASS_ONLY,
    KotlinTarget.OBJECT,
    KotlinTarget.INTERFACE,
    KotlinTarget.ENUM_CLASS,
    KotlinTarget.ANNOTATION_CLASS,
    KotlinTarget.MEMBER_FUNCTION,
    KotlinTarget.COMPANION_MEMBER_FUNCTION,
    KotlinTarget.COMPANION_EXTENSION_FUNCTION,
    KotlinTarget.TOP_LEVEL_FUNCTION,
    KotlinTarget.PROPERTY_GETTER,
    KotlinTarget.PROPERTY_SETTER,
    KotlinTarget.MEMBER_PROPERTY,
    KotlinTarget.COMPANION_MEMBER_PROPERTY,
    KotlinTarget.COMPANION_EXTENSION_PROPERTY,
    KotlinTarget.TOP_LEVEL_PROPERTY,
    KotlinTarget.CONSTRUCTOR,
    KotlinTarget.TYPEALIAS,
)

val possibleTargetPredicateMap = mapOf(
    KtTokenId.ENUM_MODIFIER to always(KotlinTarget.ENUM_CLASS),
    KtTokenId.ABSTRACT_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.INTERFACE,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    KtTokenId.OPEN_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.INTERFACE,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    KtTokenId.FINAL_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.MEMBER_FUNCTION
    ),
    KtTokenId.SEALED_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.INTERFACE),
    KtTokenId.INNER_MODIFIER to always(KotlinTarget.CLASS_ONLY),
    KtTokenId.OVERRIDE_MODIFIER to always(KotlinTarget.MEMBER_PROPERTY, KotlinTarget.MEMBER_FUNCTION),
    KtTokenId.PRIVATE_MODIFIER to defaultVisibilityTargetPredicate,
    KtTokenId.PUBLIC_MODIFIER to defaultVisibilityTargetPredicate,
    KtTokenId.INTERNAL_MODIFIER to defaultVisibilityTargetPredicate,
    KtTokenId.PROTECTED_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.COMPANION_MEMBER_FUNCTION,
        KotlinTarget.PROPERTY_GETTER,
        KotlinTarget.PROPERTY_SETTER,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.COMPANION_MEMBER_PROPERTY,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.TYPEALIAS
    ),
    KtTokenId.IN_MODIFIER to always(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
    KtTokenId.OUT_MODIFIER to always(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
    KtTokenId.REIFIED_MODIFIER to always(KotlinTarget.TYPE_PARAMETER),
    KtTokenId.VARARG_MODIFIER to always(KotlinTarget.VALUE_PARAMETER, KotlinTarget.PROPERTY_PARAMETER),
    KtTokenId.COMPANION_MODIFIER to always(
        KotlinTarget.OBJECT,
        KotlinTarget.COMPANION_EXTENSION_PROPERTY,
        KotlinTarget.COMPANION_EXTENSION_FUNCTION,
    ),
    KtTokenId.LATEINIT_MODIFIER to always(
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.COMPANION_MEMBER_PROPERTY,
        KotlinTarget.COMPANION_EXTENSION_PROPERTY,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.LOCAL_VARIABLE,
    ),
    KtTokenId.DATA_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.LOCAL_CLASS, KotlinTarget.STANDALONE_OBJECT),
    KtTokenId.INLINE_MODIFIER to or(
        always(
            KotlinTarget.FUNCTION,
            KotlinTarget.MEMBER_PROPERTY_WITH_DELEGATE,
            KotlinTarget.MEMBER_PROPERTY_WITHOUT_FIELD_OR_DELEGATE,
            KotlinTarget.TOP_LEVEL_PROPERTY,
            KotlinTarget.COMPANION_EXTENSION_PROPERTY,
            KotlinTarget.PROPERTY_GETTER,
            KotlinTarget.PROPERTY_SETTER,
            KotlinTarget.CLASS_ONLY
        ),
        ifUnsupported(
            LanguageFeature.ForbidInlineEnumEntries,
            KotlinTarget.ENUM_ENTRY
        ),
        ifUnsupported(
            LanguageFeature.ProhibitInlineModifierOnPrimaryConstructorParameters,
            KotlinTarget.PROPERTY_PARAMETER
        )
    ),
    KtTokenId.NOINLINE_MODIFIER to always(KotlinTarget.VALUE_PARAMETER),
    KtTokenId.TAILREC_MODIFIER to always(KotlinTarget.FUNCTION),
    KtTokenId.SUSPEND_MODIFIER to always(
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.COMPANION_MEMBER_FUNCTION,
        KotlinTarget.COMPANION_EXTENSION_FUNCTION,
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.LOCAL_FUNCTION,
        KotlinTarget.ANONYMOUS_FUNCTION
    ),
    KtTokenId.EXTERNAL_MODIFIER to or(
        always(
            KotlinTarget.FUNCTION,
            KotlinTarget.MEMBER_PROPERTY_WITH_BACKING_FIELD,
            KotlinTarget.MEMBER_PROPERTY_WITH_DELEGATE,
            KotlinTarget.MEMBER_PROPERTY_WITHOUT_FIELD_OR_DELEGATE,
            KotlinTarget.TOP_LEVEL_PROPERTY,
            KotlinTarget.COMPANION_EXTENSION_PROPERTY,
            KotlinTarget.PROPERTY_GETTER,
            KotlinTarget.PROPERTY_SETTER,
            KotlinTarget.CLASS
        ),
        ifUnsupported(
            LanguageFeature.ForbidExternalEnumEntriesAndPrimaryConstructorProperties,
            KotlinTarget.ENUM_ENTRY,
            KotlinTarget.PROPERTY_PARAMETER
        )
    ),
    KtTokenId.ANNOTATION_MODIFIER to always(KotlinTarget.ANNOTATION_CLASS),
    KtTokenId.CROSSINLINE_MODIFIER to always(KotlinTarget.VALUE_PARAMETER),
    KtTokenId.CONST_MODIFIER to always(
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.COMPANION_MEMBER_PROPERTY,
        KotlinTarget.COMPANION_EXTENSION_PROPERTY,
        KotlinTarget.TOP_LEVEL_PROPERTY
    ),
    KtTokenId.OPERATOR_MODIFIER to always(KotlinTarget.FUNCTION),
    KtTokenId.INFIX_MODIFIER to always(KotlinTarget.FUNCTION),
    KtTokenId.EXPECT_MODIFIER to always(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.COMPANION_EXTENSION_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.COMPANION_EXTENSION_PROPERTY,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS
    ),
    KtTokenId.ACTUAL_MODIFIER to always(
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.COMPANION_EXTENSION_FUNCTION,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.COMPANION_MEMBER_FUNCTION,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.MEMBER_PROPERTY,
        KotlinTarget.COMPANION_MEMBER_PROPERTY,
        KotlinTarget.COMPANION_EXTENSION_PROPERTY,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.OBJECT,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.TYPEALIAS
    ),
    KtTokenId.FUN_MODIFIER to always(KotlinTarget.INTERFACE),
    KtTokenId.VALUE_MODIFIER to or(
        always(KotlinTarget.CLASS_ONLY),
        ifSupported(LanguageFeature.FullValueClasses, KotlinTarget.STANDALONE_OBJECT)
    ),
    KtTokenId.ERROR_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.OBJECT),
)

// NOTE: deprecated targets must be possible!
val deprecatedTargetPredicateMap = mapOf(
    KtTokenId.INLINE_MODIFIER to ifUnsupported(
        LanguageFeature.ForbidInlineEnumEntries,
        KotlinTarget.ENUM_ENTRY
    ),
    KtTokenId.EXTERNAL_MODIFIER to ifUnsupported(
        LanguageFeature.ForbidExternalEnumEntriesAndPrimaryConstructorProperties,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.PROPERTY_PARAMETER
    ),
)

val deprecatedParentTargetMap = mapOf<SyntaxId, Set<KotlinTarget>>()

// NOTE: redundant targets must be possible!
val redundantTargetMap = mapOf<SyntaxId, Set<KotlinTarget>>(
    KtTokenId.OPEN_MODIFIER to EnumSet.of(KotlinTarget.INTERFACE)
)

val allowedInContextParameters: SyntaxIdSet = syntaxIdSetOf(KtTokenId.NOINLINE_MODIFIER, KtTokenId.CROSSINLINE_MODIFIER)

interface TargetAllowedPredicate {
    fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings): Boolean
}

fun always(target: KotlinTarget, vararg targets: KotlinTarget): TargetAllowedPredicate = object : TargetAllowedPredicate {
    private val targetSet = EnumSet.of(target, *targets)

    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        target in targetSet
}

fun ifSupported(languageFeature: LanguageFeature, target: KotlinTarget, vararg targets: KotlinTarget): TargetAllowedPredicate =
    object : TargetAllowedPredicate {
        private val targetSet = EnumSet.of(target, *targets)

        override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
            languageVersionSettings.supportsFeature(languageFeature) && target in targetSet
    }

fun ifUnsupported(languageFeature: LanguageFeature, target: KotlinTarget, vararg targets: KotlinTarget): TargetAllowedPredicate =
    object : TargetAllowedPredicate {
        private val targetSet = EnumSet.of(target, *targets)

        override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
            !languageVersionSettings.supportsFeature(languageFeature) && target in targetSet
    }

fun or(vararg p: TargetAllowedPredicate): TargetAllowedPredicate = object : TargetAllowedPredicate {
    override fun isAllowed(target: KotlinTarget, languageVersionSettings: LanguageVersionSettings) =
        p.any { it.isAllowed(target, languageVersionSettings) }
}

val possibleParentTargetPredicateMap = mapOf(
    KtTokenId.INNER_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY
    ),
    KtTokenId.OVERRIDE_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY
    ),
    KtTokenId.PROTECTED_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.COMPANION_OBJECT,
    ),
    KtTokenId.INTERNAL_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.INTERFACE,
        KotlinTarget.FILE
    ),
    KtTokenId.PRIVATE_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.INTERFACE,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.FILE
    ),
    KtTokenId.COMPANION_MODIFIER to or(
        always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.INTERFACE,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ANNOTATION_CLASS,
        ),
        ifSupported(
            LanguageFeature.CompanionExtensions,
            KotlinTarget.FILE,
        )
    ),
    KtTokenId.FINAL_MODIFIER to always(
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.LOCAL_CLASS,
        KotlinTarget.OBJECT,
        KotlinTarget.OBJECT_LITERAL,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.FILE
    ),
    KtTokenId.VARARG_MODIFIER to always(KotlinTarget.CONSTRUCTOR, KotlinTarget.FUNCTION, KotlinTarget.CLASS)
)
