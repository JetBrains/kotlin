/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.predicate

import org.jetbrains.kotlin.KtFakeSourceElementKind
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
 * Defines the supported declaration targets for [AbstractPredicate.MatchingType] and checks whether FIR declarations match them.
 */
object PredicateTargets {
    /**
     * The [KotlinTarget]s that [AbstractPredicate.MatchingType.targets] may contain.
     *
     * They cover the declarations that a [LookupPredicate] can return. Those are class-like declarations, type aliases,
     * named functions, constructors, and properties. Each target can be computed from the declaration itself without resolution.
     * Targets for backing fields and delegates are left out because they depend on resolved bodies.
     */
    val supported: Set<KotlinTarget> = setOf(
        KotlinTarget.CLASS,
        KotlinTarget.CLASS_ONLY,
        KotlinTarget.INTERFACE,
        KotlinTarget.OBJECT,
        KotlinTarget.STANDALONE_OBJECT,
        KotlinTarget.COMPANION_OBJECT,
        KotlinTarget.ENUM_CLASS,
        KotlinTarget.ENUM_ENTRY,
        KotlinTarget.ANNOTATION_CLASS,
        KotlinTarget.TYPEALIAS,
        KotlinTarget.CONSTRUCTOR,
        KotlinTarget.FUNCTION,
        KotlinTarget.TOP_LEVEL_FUNCTION,
        KotlinTarget.MEMBER_FUNCTION,
        KotlinTarget.PROPERTY,
        KotlinTarget.TOP_LEVEL_PROPERTY,
        KotlinTarget.MEMBER_PROPERTY,
    )

    /**
     * Returns whether [declaration] has one of [targets]. An empty [targets] set matches any declaration.
     */
    @FirExtensionApiInternals
    fun matches(targets: Set<KotlinTarget>, declaration: FirDeclaration): Boolean {
        if (targets.isEmpty()) return true
        return targetsOf(declaration).any { it in targets }
    }

    /**
     * Returns the annotation targets of [declaration] that apply without a use-site target.
     *
     * This follows the compiler's `@Target` checks. It only reads the declaration's kind, status, and position,
     * so it doesn't resolve anything. Declarations that predicates can't target return an empty list.
     */
    @FirExtensionApiInternals
    fun targetsOf(declaration: FirDeclaration): List<KotlinTarget> {
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

    internal fun requireSupported(targets: Set<KotlinTarget>) {
        val unsupported = targets - supported
        require(unsupported.isEmpty()) {
            "Unsupported predicate targets: $unsupported. Supported targets: $supported"
        }
    }
}
