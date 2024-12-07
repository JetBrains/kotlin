/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

fun FirAnnotation.useSiteTargetsFromMetaAnnotation(session: FirSession): Set<AnnotationUseSiteTarget> {
    return toAnnotationClass(session)
        ?.annotations
        ?.find { it.toAnnotationClassIdSafe(session) == StandardClassIds.Annotations.Target }
        ?.findUseSiteTargets(session)
        ?: DEFAULT_USE_SITE_TARGETS
}

private fun FirAnnotation.findUseSiteTargets(session: FirSession): Set<AnnotationUseSiteTarget> {
    return buildSet {
        forEachAnnotationTarget(session) {
            USE_SITE_TARGET_NAME_MAP[it.identifier]?.let { addAll(it) }
        }
    }
}

// See [org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.USE_SITE_MAPPING] (it's in reverse)
private val USE_SITE_TARGET_NAME_MAP = mapOf(
    "FIELD" to setOf(AnnotationUseSiteTarget.FIELD, AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD),
    "FILE" to setOf(AnnotationUseSiteTarget.FILE),
    "PROPERTY" to setOf(AnnotationUseSiteTarget.PROPERTY),
    "PROPERTY_GETTER" to setOf(AnnotationUseSiteTarget.PROPERTY_GETTER),
    "PROPERTY_SETTER" to setOf(AnnotationUseSiteTarget.PROPERTY_SETTER),
    "VALUE_PARAMETER" to setOf(
        AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER,
        AnnotationUseSiteTarget.RECEIVER,
        AnnotationUseSiteTarget.SETTER_PARAMETER,
    ),
)

// See [org.jetbrains.kotlin.descriptors.annotations.KotlinTarget] (the second argument of each entry)
private val DEFAULT_USE_SITE_TARGETS: Set<AnnotationUseSiteTarget> =
    USE_SITE_TARGET_NAME_MAP.values.fold(setOf<AnnotationUseSiteTarget>()) { a, b -> a + b } - setOf(AnnotationUseSiteTarget.FILE)

fun FirAnnotation.forEachAnnotationTarget(session: FirSession, action: (Name) -> Unit) {
    fun take(arg: FirExpression) {
        val callableSymbol = arg.toResolvedCallableSymbol(session) ?: return
        if (callableSymbol.containingClassLookupTag()?.classId == StandardClassIds.AnnotationTarget) {
            action(callableSymbol.callableId.callableName)
        }
    }

    if (this is FirAnnotationCall) {
        for (arg in argumentList.arguments) {
            arg.unwrapAndFlattenArgument(flattenArrays = true).forEach(::take)
        }
    } else {
        argumentMapping.mapping[StandardClassIds.Annotations.ParameterNames.targetAllowedTargets]
            ?.unwrapAndFlattenArgument(flattenArrays = true)
            ?.forEach(::take)
    }
}
