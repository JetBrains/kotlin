/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionBlockMember
import org.jetbrains.kotlin.fir.declarations.utils.isCompanionExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.extensions.FirExtensionApiInternals
import org.jetbrains.kotlin.resolve.AnnotationTargetList
import org.jetbrains.kotlin.resolve.AnnotationTargetLists

/**
 * Describes a particular kind of Kotlin declaration that can be matched.
 */
sealed interface DeclarationType {
    /**
     * Checks if [declaration] matches this type.
     *
     * This call may happen at a very early [FirResolvePhase],
     * likely just after the [COMPILER_REQUIRED_ANNOTATIONS][FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS] phase.
     * This means that the specified [declaration] will not be resolved in any way.
     * Only syntactical information (FIR class type, origin, status, etc.) may safely be used to determine the type of declaration.
     * It is ***not*** possible, for example, to filter based on what super types a [FirClass] may have within this function.
     */
    fun matches(declaration: FirDeclaration): Boolean

    @OptIn(FirExtensionApiInternals::class)
    companion object {
        /** Matches classes, interfaces, and all kinds of objects but not enum entries. */
        @JvmField val CLASS: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.CLASS)
        /** Matches classes only, not interfaces, objects, or local classes. */
        @JvmField val CLASS_ONLY: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.CLASS_ONLY)
        /** Matches interfaces only, not classes or objects. */
        @JvmField val INTERFACE: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.INTERFACE)
        /** Matches objects (including standalone and companion) but not anonymous object. */
        @JvmField val OBJECT: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.OBJECT)
        /** Matches only standalone objects, not companion or anonymous objects. */
        @JvmField val STANDALONE_OBJECT: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.STANDALONE_OBJECT)
        /** Matches only companion objects, not standalone or anonymous objects. */
        @JvmField val COMPANION_OBJECT: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.COMPANION_OBJECT)
        @JvmField val ENUM_CLASS: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.ENUM_CLASS)
        @JvmField val ENUM_ENTRY: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.ENUM_ENTRY)
        @JvmField val ANNOTATION_CLASS: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.ANNOTATION_CLASS)
        @JvmField val TYPEALIAS: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.TYPEALIAS)
        @JvmField val CONSTRUCTOR: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.CONSTRUCTOR)
        /** Matches all types of functions, including top-level, member, and companion-block functions. */
        @JvmField val FUNCTION: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.FUNCTION)
        /** Matches only top-level functions, not member or companion-block functions. */
        @JvmField val TOP_LEVEL_FUNCTION: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.TOP_LEVEL_FUNCTION)
        /** Matches only member functions, not top-level or companion-block functions. */
        @JvmField val MEMBER_FUNCTION: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.MEMBER_FUNCTION)
        /** Matches all types of properties, including top-level, member, and companion-block properties. */
        @JvmField val PROPERTY: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.PROPERTY)
        /** Matches only top-level properties, not member or companion-block properties. */
        @JvmField val TOP_LEVEL_PROPERTY: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.TOP_LEVEL_PROPERTY)
        /** Matches only member properties, not top-level or companion-block properties. */
        @JvmField val MEMBER_PROPERTY: DeclarationType = KotlinTargetDeclarationType(KotlinTarget.MEMBER_PROPERTY)
    }

    /**
     * Allows implementing a custom type for matching declarations.
     * See [matches] for restrictions on implementing a custom type.
     */
    @ExperimentalCompilerApi
    fun interface Custom : DeclarationType
}

@FirExtensionApiInternals
class KotlinTargetDeclarationType(val targets: Set<KotlinTarget>) : DeclarationType {
    constructor(vararg targets: KotlinTarget) : this(setOf(*targets))

    /**
     * Returns whether [declaration] has one of [targets]. An empty [targets] set matches any declaration.
     */
    override fun matches(declaration: FirDeclaration): Boolean {
        if (targets.isEmpty()) return true
        return targetsOf(declaration).any { it in targets }
    }

    /**
     * Returns the annotation targets of [declaration] that apply without a use-site target.
     *
     * This follows the compiler's `@Target` checks. It only reads the declaration's kind, status, and position,
     * so it doesn't resolve anything. Declarations that predicates can't target return an empty list.
     */
    private fun targetsOf(declaration: FirDeclaration): List<KotlinTarget> {
        val targetList: AnnotationTargetList = when (declaration) {
            is FirRegularClass -> return KotlinTarget.classActualTargets(
                declaration.classKind,
                declaration.isInner,
                declaration.isCompanion,
                isLocalClass = declaration.isLocal,
            )
            is FirEnumEntry -> return KotlinTarget.classActualTargets(
                ClassKind.ENUM_ENTRY,
                isInnerClass = false,
                isCompanionObject = false,
                isLocalClass = false,
            )
            is FirTypeAlias -> AnnotationTargetLists.T_TYPEALIAS
            is FirConstructor -> AnnotationTargetLists.T_CONSTRUCTOR
            is FirNamedFunction -> when {
                declaration.isLocal -> AnnotationTargetLists.T_LOCAL_FUNCTION
                declaration.isCompanionBlockMember -> AnnotationTargetLists.T_COMPANION_MEMBER_FUNCTION
                declaration.dispatchReceiverType != null -> AnnotationTargetLists.T_MEMBER_FUNCTION
                declaration.isCompanionExtension -> AnnotationTargetLists.T_COMPANION_EXTENSION_FUNCTION
                else -> AnnotationTargetLists.T_TOP_LEVEL_FUNCTION
            }
            // Whether a property has a backing field isn't known before body resolution.
            // None of the supported targets depend on it, so it's always treated as absent.
            is FirProperty -> when {
                declaration.isLocal -> return emptyList()
                declaration.dispatchReceiverType != null -> {
                    if (declaration.source?.kind == KtFakeSourceElementKind.PropertyFromParameter) {
                        AnnotationTargetLists.T_VALUE_PARAMETER_WITH_VAL
                    } else {
                        AnnotationTargetLists.T_MEMBER_PROPERTY(
                            backingField = false,
                            delegate = declaration.delegate != null,
                            isCompanionMember = false,
                        )
                    }
                }
                declaration.isCompanionBlockMember -> AnnotationTargetLists.T_MEMBER_PROPERTY(
                    backingField = false,
                    delegate = declaration.delegate != null,
                    isCompanionMember = true,
                )
                else -> AnnotationTargetLists.T_TOP_LEVEL_PROPERTY(
                    backingField = false,
                    delegate = declaration.delegate != null,
                    isCompanionExtension = declaration.isCompanionExtension,
                )
            }
            else -> return emptyList()
        }
        return targetList.defaultTargets
    }
}
