/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.ABSTRACT_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.ACTUAL_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.ANNOTATION_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.COMPANION_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.CONST_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.CROSSINLINE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.DATA_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.ENUM_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.ERROR_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.EXPECT_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.EXTERNAL_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.FINAL_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.FUN_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.INFIX_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.INLINE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.INNER_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.INTERNAL_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.IN_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.LATEINIT_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.NOINLINE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.OBJECT_KEYWORD
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.OPEN_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.OPERATOR_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.OUT_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.OVERRIDE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.PRIVATE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.PROTECTED_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.PUBLIC_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.REIFIED_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.SEALED_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.SUSPEND_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.TAILREC_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.VALUE_MODIFIER
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds.VARARG_MODIFIER
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.SyntaxIdSet
import java.util.*

// `Compatibility` and `TargetAllowedPredicate` are declared here rather than in `psi-frontend-utils`
// to be shared by the parse-tree-agnostic `KotlinModifierHelpers` and the PSI-based API in `ModifierCheckerHelpers.kt`.
// Their FQNs are kept, so moving them from `psi-frontend-utils` is binary compatible.

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

/**
 * Parse-tree-agnostic modifier checker helpers keyed by [SyntaxId]s (see [org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds]).
 *
 * It's the source of truth for both FIR modifier checkers and the PSI-based API in `ModifierCheckerHelpers.kt`,
 * which is preserved for compatibility with external clients (e.g., the IntelliJ Kotlin plugin).
 */
object KotlinModifierHelpers {
    fun compatibility(first: SyntaxId, second: SyntaxId): Compatibility {
        return if (first == second) {
            Compatibility.REPEATED
        } else {
            mutualCompatibility[Pair(first, second)] ?: Compatibility.COMPATIBLE
        }
    }

    // First modifier in pair should be also first in declaration
    private val mutualCompatibility: Map<Pair<SyntaxId, SyntaxId>, Compatibility> = buildCompatibilityMap()

    private fun buildCompatibilityMap(): Map<Pair<SyntaxId, SyntaxId>, Compatibility> {
        val result = hashMapOf<Pair<SyntaxId, SyntaxId>, Compatibility>()
        // Variance: in + out are incompatible
        result += incompatibilityRegister(IN_MODIFIER, OUT_MODIFIER)
        // Visibilities: incompatible
        result += incompatibilityRegister(PRIVATE_MODIFIER, PROTECTED_MODIFIER, PUBLIC_MODIFIER, INTERNAL_MODIFIER)
        // Abstract + open + final + sealed: incompatible
        result += incompatibilityRegister(ABSTRACT_MODIFIER, OPEN_MODIFIER, FINAL_MODIFIER, SEALED_MODIFIER)
        // data + open, data + inner, data + abstract, data + sealed, data + inline, data + value
        result += incompatibilityRegister(DATA_MODIFIER, OPEN_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, INNER_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, ABSTRACT_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, SEALED_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, INLINE_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, VALUE_MODIFIER)
        result += compatibilityRegister(Compatibility.INLINE_VALUE_ALLOWED, INLINE_MODIFIER, VALUE_MODIFIER)
        result += incompatibilityRegister(DATA_MODIFIER, OBJECT_KEYWORD, EXPECT_MODIFIER)
        // open is redundant to abstract & override
        result += redundantRegister(ABSTRACT_MODIFIER, OPEN_MODIFIER)
        // abstract is redundant to sealed
        result += redundantRegister(SEALED_MODIFIER, ABSTRACT_MODIFIER)

        // const is incompatible with abstract, open, override
        result += incompatibilityRegister(CONST_MODIFIER, ABSTRACT_MODIFIER)
        result += incompatibilityRegister(CONST_MODIFIER, OPEN_MODIFIER)
        result += incompatibilityRegister(CONST_MODIFIER, OVERRIDE_MODIFIER)

        // private is incompatible with override
        result += incompatibilityRegister(PRIVATE_MODIFIER, OVERRIDE_MODIFIER)
        // private is compatible with open / abstract only for classes
        result += compatibilityForClassesRegister(PRIVATE_MODIFIER, OPEN_MODIFIER)
        result += compatibilityForClassesRegister(PRIVATE_MODIFIER, ABSTRACT_MODIFIER)

        result += incompatibilityRegister(CROSSINLINE_MODIFIER, NOINLINE_MODIFIER)

        // 1. subclasses contained inside a sealed class can not be instantiated, because their constructors needs
        // an instance of an outer sealed (effectively abstract) class
        // 2. subclasses of a non-top-level sealed class must be declared inside the class
        // (see the KEEP https://github.com/Kotlin/KEEP/blob/master/proposals/sealed-class-inheritance.md)
        result += incompatibilityRegister(SEALED_MODIFIER, INNER_MODIFIER)

        // expect / actual are all incompatible
        result += incompatibilityRegister(EXPECT_MODIFIER, ACTUAL_MODIFIER)

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

    val featureDependencies: Map<SyntaxId, List<LanguageFeature>> = mapOf(
        INLINE_MODIFIER to listOf(LanguageFeature.InlineProperties, LanguageFeature.InlineClasses),
        EXPECT_MODIFIER to listOf(LanguageFeature.MultiPlatformProjects),
        ACTUAL_MODIFIER to listOf(LanguageFeature.MultiPlatformProjects),
    )

    val featureDependenciesTargets: Map<LanguageFeature, Set<KotlinTarget>> = mapOf(
        LanguageFeature.InlineClasses to setOf(KotlinTarget.CLASS_ONLY),
        LanguageFeature.JvmInlineValueClasses to setOf(KotlinTarget.CLASS_ONLY),
    )

    val defaultVisibilityTargetPredicate: TargetAllowedPredicate = always(
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

    val possibleTargetPredicateMap: Map<SyntaxId, TargetAllowedPredicate> = mapOf(
        ENUM_MODIFIER to always(KotlinTarget.ENUM_CLASS),
        ABSTRACT_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.INTERFACE,
            KotlinTarget.MEMBER_PROPERTY,
            KotlinTarget.MEMBER_FUNCTION
        ),
        OPEN_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.INTERFACE,
            KotlinTarget.MEMBER_PROPERTY,
            KotlinTarget.MEMBER_FUNCTION
        ),
        FINAL_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.OBJECT,
            KotlinTarget.MEMBER_PROPERTY,
            KotlinTarget.MEMBER_FUNCTION
        ),
        SEALED_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.INTERFACE),
        INNER_MODIFIER to always(KotlinTarget.CLASS_ONLY),
        OVERRIDE_MODIFIER to always(KotlinTarget.MEMBER_PROPERTY, KotlinTarget.MEMBER_FUNCTION),
        PRIVATE_MODIFIER to defaultVisibilityTargetPredicate,
        PUBLIC_MODIFIER to defaultVisibilityTargetPredicate,
        INTERNAL_MODIFIER to defaultVisibilityTargetPredicate,
        PROTECTED_MODIFIER to always(
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
        IN_MODIFIER to always(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
        OUT_MODIFIER to always(KotlinTarget.TYPE_PARAMETER, KotlinTarget.TYPE_PROJECTION),
        REIFIED_MODIFIER to always(KotlinTarget.TYPE_PARAMETER),
        VARARG_MODIFIER to always(KotlinTarget.VALUE_PARAMETER, KotlinTarget.PROPERTY_PARAMETER),
        COMPANION_MODIFIER to always(KotlinTarget.OBJECT, KotlinTarget.COMPANION_EXTENSION_PROPERTY, KotlinTarget.COMPANION_EXTENSION_FUNCTION),
        LATEINIT_MODIFIER to always(
            KotlinTarget.MEMBER_PROPERTY,
            KotlinTarget.COMPANION_MEMBER_PROPERTY,
            KotlinTarget.COMPANION_EXTENSION_PROPERTY,
            KotlinTarget.TOP_LEVEL_PROPERTY,
            KotlinTarget.LOCAL_VARIABLE,
        ),
        DATA_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.LOCAL_CLASS, KotlinTarget.STANDALONE_OBJECT),
        INLINE_MODIFIER to or(
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
        NOINLINE_MODIFIER to always(KotlinTarget.VALUE_PARAMETER),
        TAILREC_MODIFIER to always(KotlinTarget.FUNCTION),
        SUSPEND_MODIFIER to always(
            KotlinTarget.MEMBER_FUNCTION,
            KotlinTarget.COMPANION_MEMBER_FUNCTION,
            KotlinTarget.COMPANION_EXTENSION_FUNCTION,
            KotlinTarget.TOP_LEVEL_FUNCTION,
            KotlinTarget.LOCAL_FUNCTION,
            KotlinTarget.ANONYMOUS_FUNCTION
        ),
        EXTERNAL_MODIFIER to or(
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
        ANNOTATION_MODIFIER to always(KotlinTarget.ANNOTATION_CLASS),
        CROSSINLINE_MODIFIER to always(KotlinTarget.VALUE_PARAMETER),
        CONST_MODIFIER to always(
            KotlinTarget.MEMBER_PROPERTY,
            KotlinTarget.COMPANION_MEMBER_PROPERTY,
            KotlinTarget.COMPANION_EXTENSION_PROPERTY,
            KotlinTarget.TOP_LEVEL_PROPERTY
        ),
        OPERATOR_MODIFIER to always(KotlinTarget.FUNCTION),
        INFIX_MODIFIER to always(KotlinTarget.FUNCTION),
        EXPECT_MODIFIER to always(
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
        ACTUAL_MODIFIER to always(
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
        FUN_MODIFIER to always(KotlinTarget.INTERFACE),
        VALUE_MODIFIER to or(
            always(KotlinTarget.CLASS_ONLY),
            ifSupported(LanguageFeature.FullValueClasses, KotlinTarget.STANDALONE_OBJECT)
        ),
        ERROR_MODIFIER to always(KotlinTarget.CLASS_ONLY, KotlinTarget.OBJECT),
    )

    // NOTE: deprecated targets must be possible!
    val deprecatedTargetPredicateMap: Map<SyntaxId, TargetAllowedPredicate> = mapOf(
        INLINE_MODIFIER to ifUnsupported(
            LanguageFeature.ForbidInlineEnumEntries,
            KotlinTarget.ENUM_ENTRY
        ),
        EXTERNAL_MODIFIER to ifUnsupported(
            LanguageFeature.ForbidExternalEnumEntriesAndPrimaryConstructorProperties,
            KotlinTarget.ENUM_ENTRY,
            KotlinTarget.PROPERTY_PARAMETER
        ),
    )

    val deprecatedParentTargetMap = mapOf<SyntaxId, Set<KotlinTarget>>()

    // NOTE: redundant targets must be possible!
    val redundantTargetMap = mapOf<SyntaxId, Set<KotlinTarget>>(
        OPEN_MODIFIER to EnumSet.of(KotlinTarget.INTERFACE)
    )

    val allowedInContextParameters: SyntaxIdSet = SyntaxIdSet.of(NOINLINE_MODIFIER, CROSSINLINE_MODIFIER)

    val possibleParentTargetPredicateMap: Map<SyntaxId, TargetAllowedPredicate> = mapOf(
        INNER_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ENUM_ENTRY
        ),
        OVERRIDE_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.OBJECT,
            KotlinTarget.OBJECT_LITERAL,
            KotlinTarget.INTERFACE,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ENUM_ENTRY
        ),
        PROTECTED_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.COMPANION_OBJECT
        ),
        INTERNAL_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.OBJECT,
            KotlinTarget.OBJECT_LITERAL,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ENUM_ENTRY,
            KotlinTarget.INTERFACE,
            KotlinTarget.FILE
        ),
        PRIVATE_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.OBJECT,
            KotlinTarget.OBJECT_LITERAL,
            KotlinTarget.INTERFACE,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ENUM_ENTRY,
            KotlinTarget.FILE
        ),
        COMPANION_MODIFIER to or(
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
        FINAL_MODIFIER to always(
            KotlinTarget.CLASS_ONLY,
            KotlinTarget.LOCAL_CLASS,
            KotlinTarget.OBJECT,
            KotlinTarget.OBJECT_LITERAL,
            KotlinTarget.ENUM_CLASS,
            KotlinTarget.ENUM_ENTRY,
            KotlinTarget.ANNOTATION_CLASS,
            KotlinTarget.FILE
        ),
        VARARG_MODIFIER to always(KotlinTarget.CONSTRUCTOR, KotlinTarget.FUNCTION, KotlinTarget.CLASS)
    )
}
