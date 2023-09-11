/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType as IncompatibilityType

object K1AbstractExpectActualAnnotationMatchChecker {
    private val SKIPPED_CLASS_IDS = setOf(
        StandardClassIds.Annotations.Deprecated,
        StandardClassIds.Annotations.DeprecatedSinceKotlin,
        StandardClassIds.Annotations.ImplicitlyActualizedByJvmDeclaration,
        StandardClassIds.Annotations.OptionalExpectation,
        StandardClassIds.Annotations.RequireKotlin,
        StandardClassIds.Annotations.SinceKotlin,
        StandardClassIds.Annotations.Suppress,
        StandardClassIds.Annotations.WasExperimental,
        OptInNames.OPT_IN_CLASS_ID,
    )

    class Incompatibility(
        /**
         * [expectSymbol] and [actualSymbol] are declaration symbols where annotation been mismatched.
         * They are needed for writing whole declarations in diagnostic text.
         * They are not the same as symbols passed to checker as arguments in [areAnnotationsCompatible] in following cases:
         * 1. If [actualSymbol] is typealias, it will be expanded.
         * 2. If problem is in class member, [expectSymbol] will be mismatched member, not the original class.
         * 3. If annotation mismatched on function value parameter, symbols will be whole functions, not value parameter symbols.
         */
        val expectSymbol: DeclarationSymbolMarker,
        val actualSymbol: DeclarationSymbolMarker,

        /**
         * Link to source code element (possibly holding null, if no source) from actual declaration
         * where mismatched actual annotation is set (or should be set if it is missing).
         * Needed for the implementation of IDE intention.
         */
        val actualAnnotationTargetElement: SourceElementMarker,
        val type: IncompatibilityType<K1ExpectActualMatchingContext.AnnotationCallInfo>,
    )

    fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        context: K1ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) {
        areAnnotationsCompatible(expectSymbol, actualSymbol)
    }

    private fun K1ExpectActualMatchingContext<*>.areAnnotationsCompatible(
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

    private fun K1ExpectActualMatchingContext<*>.areCallableAnnotationsCompatible(
        expectSymbol: CallableSymbolMarker,
        actualSymbol: CallableSymbolMarker,
    ): Incompatibility? {
        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }
        areAnnotationsOnValueParametersCompatible(expectSymbol, actualSymbol)?.let { return it }

        if (expectSymbol is PropertySymbolMarker && actualSymbol is PropertySymbolMarker) {
            arePropertyGetterAndSetterAnnotationsCompatible(expectSymbol, actualSymbol)?.let { return it }
        }

        return null
    }

    private fun K1ExpectActualMatchingContext<*>.arePropertyGetterAndSetterAnnotationsCompatible(
        expectSymbol: PropertySymbolMarker,
        actualSymbol: PropertySymbolMarker,
    ): Incompatibility? {
        listOf(
            expectSymbol.getter to actualSymbol.getter,
            expectSymbol.setter to actualSymbol.setter,
        ).forEach { (expectAccessor, actualAccessor) ->
            if (expectAccessor != null && actualAccessor != null) {
                areAnnotationsSetOnDeclarationsCompatible(expectAccessor, actualAccessor)?.let {
                    // Write containing declarations into diagnostic
                    return Incompatibility(expectSymbol, actualSymbol, actualAccessor.getSourceElement(), it.type)
                }
            }
        }
        return null
    }

    private fun K1ExpectActualMatchingContext<*>.areClassAnnotationsCompatible(
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

    private fun K1ExpectActualMatchingContext<*>.commonForClassAndCallableChecks(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        areAnnotationsSetOnDeclarationsCompatible(expectSymbol, actualSymbol)?.let { return it }
        areAnnotationsOnTypeParametersCompatible(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    private fun K1ExpectActualMatchingContext<*>.areAnnotationsOnValueParametersCompatible(
        expectSymbol: CallableSymbolMarker,
        actualSymbol: CallableSymbolMarker,
    ): Incompatibility? {
        val expectParams = expectSymbol.valueParameters
        val actualParams = actualSymbol.valueParameters

        if (expectParams.size != actualParams.size) return null

        return expectParams.zip(actualParams).firstNotNullOfOrNull { (expectParam, actualParam) ->
            areAnnotationsSetOnDeclarationsCompatible(expectParam, actualParam)?.let {
                // Write containing declarations into diagnostic
                Incompatibility(expectSymbol, actualSymbol, actualParam.getSourceElement(), it.type)
            }
        }
    }

    private fun K1ExpectActualMatchingContext<*>.areAnnotationsOnTypeParametersCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        fun DeclarationSymbolMarker.getTypeParameters(): List<TypeParameterSymbolMarker>? {
            return when (this) {
                is FunctionSymbolMarker -> typeParameters
                is RegularClassSymbolMarker -> typeParameters
                else -> null
            }
        }

        val expectParams = expectSymbol.getTypeParameters() ?: return null
        val actualParams = actualSymbol.getTypeParameters() ?: return null
        if (expectParams.size != actualParams.size) return null

        return expectParams.zip(actualParams).firstNotNullOfOrNull { (expectParam, actualParam) ->
            areAnnotationsSetOnDeclarationsCompatible(expectParam, actualParam)?.let {
                // Write containing declarations into diagnostic
                Incompatibility(expectSymbol, actualSymbol, actualParam.getSourceElement(), it.type)
            }
        }
    }

    private fun K1ExpectActualMatchingContext<*>.areAnnotationsSetOnDeclarationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
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
                    actualSymbol.getSourceElement(),
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
                return Incompatibility(expectSymbol, actualSymbol, actualSymbol.getSourceElement(), incompatibilityType)
            }
        }
        return null
    }

    private fun getAnnotationCollectionArgumentsCompatibilityChecker(annotationClassId: ClassId):
            K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy {
        return if (annotationClassId == StandardClassIds.Annotations.Target) {
            K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy.ExpectIsSubsetOfActual
        } else {
            K1ExpectActualCollectionArgumentsCompatibilityCheckStrategy.Default
        }
    }

    private fun K1ExpectActualMatchingContext<*>.checkAnnotationsInClassMemberScope(
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
            val expectMember = expectToCompatibilityMap.filter { it.value == K1ExpectActualCompatibility.Compatible }.keys.singleOrNull()
            // Check also incompatible members if only one is found
                ?: expectToCompatibilityMap.keys.singleOrNull()
                ?: continue
            areAnnotationsCompatible(expectMember, actualMember)?.let { return it }
        }
        return null
    }

    private fun K1ExpectActualMatchingContext<*>.checkAnnotationsOnEnumEntries(
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