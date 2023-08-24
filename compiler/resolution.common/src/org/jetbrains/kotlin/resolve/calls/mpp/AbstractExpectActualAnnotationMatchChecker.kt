/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType as IncompatibilityType

object AbstractExpectActualAnnotationMatchChecker {
    private val SKIPPED_CLASS_IDS = setOf(
        StandardClassIds.Annotations.Deprecated,
        StandardClassIds.Annotations.DeprecatedSinceKotlin,
        StandardClassIds.Annotations.OptionalExpectation,
        StandardClassIds.Annotations.RequireKotlin,
        StandardClassIds.Annotations.SinceKotlin,
        StandardClassIds.Annotations.Suppress,
        StandardClassIds.Annotations.WasExperimental,
        OptInNames.OPT_IN_CLASS_ID,
    )

    class Incompatibility(
        val expectSymbol: DeclarationSymbolMarker,
        val actualSymbol: DeclarationSymbolMarker,
        val type: IncompatibilityType<ExpectActualMatchingContext.AnnotationCallInfo>,
    )

    fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        context: ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) {
        areAnnotationsCompatible(expectSymbol, actualSymbol)
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        return when (expectSymbol) {
            is CallableSymbolMarker -> {
                areCallableAnnotationsCompatible(expectSymbol, actualSymbol as CallableSymbolMarker)
            }
            is RegularClassSymbolMarker -> {
                areClassAnnotationsCompatible(expectSymbol, actualSymbol as ClassLikeSymbolMarker)
            }
            else -> error("Incorrect types: $expectSymbol $actualSymbol")
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areCallableAnnotationsCompatible(
        expectSymbol: CallableSymbolMarker,
        actualSymbol: CallableSymbolMarker,
    ): Incompatibility? {
        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun areClassAnnotationsCompatible(
        expectSymbol: RegularClassSymbolMarker,
        actualSymbol: ClassLikeSymbolMarker,
    ): Incompatibility? {
        if (actualSymbol is TypeAliasSymbolMarker) {
            val expanded = actualSymbol.expandToRegularClass() ?: return null
            return areClassAnnotationsCompatible(expectSymbol, expanded)
        }
        check(actualSymbol is RegularClassSymbolMarker)

        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }

        if (checkClassScopesForAnnotationCompatibility) {
            checkAnnotationsInClassMemberScope(expectSymbol, actualSymbol)?.let { return it }
        }
        if (expectSymbol.classKind == ClassKind.ENUM_CLASS && actualSymbol.classKind == ClassKind.ENUM_CLASS) {
            checkAnnotationsOnEnumEntries(expectSymbol, actualSymbol)?.let { return it }
        }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun commonForClassAndCallableChecks(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        areAnnotationsSetOnDeclarationsCompatible(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsSetOnDeclarationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        // TODO(Roman.Efremov, KT-58551): check other annotation targets (constructors, types, value parameters, etc)

        val skipSourceAnnotations = actualSymbol.hasSourceAnnotationsErased
        val actualAnnotationsByName = actualSymbol.annotations.groupBy { it.classId }

        for (expectAnnotation in expectSymbol.annotations) {
            val expectClassId = expectAnnotation.classId ?: continue
            if (expectClassId in SKIPPED_CLASS_IDS || expectAnnotation.isOptIn) {
                continue
            }
            if (expectAnnotation.isRetentionSource && skipSourceAnnotations) {
                continue
            }
            val actualAnnotationsWithSameClassId = actualAnnotationsByName[expectClassId] ?: emptyList()
            if (actualAnnotationsWithSameClassId.isEmpty()) {
                return Incompatibility(
                    expectSymbol,
                    actualSymbol,
                    IncompatibilityType.MissingOnActual(expectAnnotation)
                )
            }
            val collectionCompatibilityChecker = getAnnotationCollectionArgumentsCompatibilityChecker(expectClassId)
            if (actualAnnotationsWithSameClassId.none {
                    areAnnotationArgumentsEqual(expectAnnotation, it, collectionCompatibilityChecker)
                }) {
                val incompatibilityType = if (actualAnnotationsWithSameClassId.size == 1) {
                    IncompatibilityType.DifferentOnActual(expectAnnotation, actualAnnotationsWithSameClassId.single())
                } else {
                    // In the case of repeatable annotations, we can't choose on which to report
                    IncompatibilityType.MissingOnActual(expectAnnotation)
                }
                return Incompatibility(expectSymbol, actualSymbol, incompatibilityType)
            }
        }
        return null
    }

    private fun getAnnotationCollectionArgumentsCompatibilityChecker(annotationClassId: ClassId):
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy {
        return if (annotationClassId == StandardClassIds.Annotations.Target) {
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy.ExpectIsSubsetOfActual
        } else {
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy.Default
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun checkAnnotationsInClassMemberScope(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
    ): Incompatibility? {
        for (actualMember in actualClass.collectAllMembers(isActualDeclaration = true)) {
            if (skipCheckingAnnotationsOfActualClassMember(actualMember)) {
                continue
            }
            val expectToCompatibilityMap = findPotentialExpectClassMembersForActual(
                expectClass, actualClass, actualMember,
                // Optimization: don't check class scopes, because:
                // 1. Annotation checker runs no matter if found expect class is compatible or not.
                // 2. Class always has at most one corresponding `expect` class (unlike for functions, which may have several overrides),
                //    so we are sure that we found the right member.
                checkClassScopesCompatibility = false,
            )
            val expectMember = expectToCompatibilityMap.filter { it.value == ExpectActualCompatibility.Compatible }.keys.singleOrNull()
            // Check also incompatible members if only one is found
                ?: expectToCompatibilityMap.keys.singleOrNull()
                ?: continue
            areAnnotationsCompatible(expectMember, actualMember)?.let { return it }
        }
        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun checkAnnotationsOnEnumEntries(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
    ): Incompatibility? {
        fun DeclarationSymbolMarker.getEnumEntryName(): Name =
            when (this) {
                is CallableSymbolMarker -> callableId.callableName
                is RegularClassSymbolMarker -> classId.shortClassName
                else -> error("Unexpected type $this")
            }

        val expectEnumEntries = expectClassSymbol.collectEnumEntries()
        val actualEnumEntriesByName = actualClassSymbol.collectEnumEntries().associateBy { it.getEnumEntryName() }

        for (expectEnumEntry in expectEnumEntries) {
            val actualEnumEntry = actualEnumEntriesByName[expectEnumEntry.getEnumEntryName()] ?: continue
            areAnnotationsSetOnDeclarationsCompatible(expectEnumEntry, actualEnumEntry)
                ?.let { return it }
        }
        return null
    }
}