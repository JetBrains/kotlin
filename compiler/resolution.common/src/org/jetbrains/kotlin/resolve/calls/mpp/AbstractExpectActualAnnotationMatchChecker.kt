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
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType as IncompatibilityType

object AbstractExpectActualAnnotationMatchChecker {
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
        val type: IncompatibilityType<ExpectActualMatchingContext.AnnotationCallInfo>,
    )

    fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClass: RegularClassSymbolMarker?, // Only necessary for the frontend. IR doesn't use it
        context: ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) {
        areAnnotationsCompatible(expectSymbol, actualSymbol, containingExpectClass)
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        containingExpectClass: RegularClassSymbolMarker?,
    ): Incompatibility? {
        return when (expectSymbol) {
            is CallableSymbolMarker -> {
                areCallableAnnotationsCompatible(expectSymbol, actualSymbol as CallableSymbolMarker, containingExpectClass)
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
        containingExpectClass: RegularClassSymbolMarker?,
    ): Incompatibility? {
        // If the expect declaration is fake-override and is incompatible, then it means that it was overridden on actual.
        // In such case, regular rules for annotations on overridden declarations apply.
        if (expectSymbol.isFakeOverride(containingExpectClass)) return null
        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }
        areAnnotationsOnValueParametersCompatible(expectSymbol, actualSymbol)?.let { return it }

        if (checkPropertyAccessorsForAnnotationsCompatibility
            && expectSymbol is PropertySymbolMarker && actualSymbol is PropertySymbolMarker
        ) {
            arePropertyGetterAndSetterAnnotationsCompatible(expectSymbol, actualSymbol)?.let { return it }
        }

        areAnnotationsSetOnTypesCompatible(
            expectSymbol, actualSymbol, expectSymbol.extensionReceiverTypeRef, actualSymbol.extensionReceiverTypeRef
        )?.let { return it }

        areAnnotationsSetOnTypesCompatible(expectSymbol, actualSymbol, expectSymbol.returnTypeRef, actualSymbol.returnTypeRef)
            ?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun arePropertyGetterAndSetterAnnotationsCompatible(
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
        if (checkEnumEntriesForAnnotationsCompatibility && expectSymbol.classKind == ClassKind.ENUM_CLASS &&
            actualSymbol.classKind == ClassKind.ENUM_CLASS
        ) {
            checkAnnotationsOnEnumEntries(expectSymbol, actualSymbol)?.let { return it }
        }

        // actual may have more super types, that's why we can't just zip them.
        // Identifying super type with ClassId (without type parameters) is enough because same type can't be extended twice.
        val actualSuperTypes = actualSymbol.superTypesRefs.groupBy { it.getClassId() }
        for (expectSuperType in expectSymbol.superTypesRefs) {
            val expectClassId = expectSuperType.getClassId() ?: continue
            val actualSuperType = actualSuperTypes[expectClassId]?.singleOrNull() ?: continue
            areAnnotationsSetOnTypesCompatible(expectSymbol, actualSymbol, expectSuperType, actualSuperType)
                ?.let { return it }
        }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun commonForClassAndCallableChecks(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        areAnnotationsSetOnDeclarationsCompatible(expectSymbol, actualSymbol)?.let { return it }
        areAnnotationsOnTypeParametersCompatible(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsOnValueParametersCompatible(
        expectSymbol: CallableSymbolMarker,
        actualSymbol: CallableSymbolMarker,
    ): Incompatibility? {
        val expectParams = expectSymbol.valueParameters
        val actualParams = actualSymbol.valueParameters

        return expectParams.zipIfSizesAreEqual(actualParams)?.firstNotNullOfOrNull { (expectParam, actualParam) ->
            areAnnotationsSetOnDeclarationsCompatible(expectParam, actualParam)?.let {
                // Write containing declarations into diagnostic
                Incompatibility(expectSymbol, actualSymbol, actualParam.getSourceElement(), it.type)
            }
                ?: areAnnotationsSetOnTypesCompatible(expectSymbol, actualSymbol, expectParam.returnTypeRef, actualParam.returnTypeRef)
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsOnTypeParametersCompatible(
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

        return expectParams.zipIfSizesAreEqual(actualParams)?.firstNotNullOfOrNull { (expectParam, actualParam) ->
            areAnnotationsSetOnDeclarationsCompatible(expectParam, actualParam)?.let {
                // Write containing declarations into diagnostic
                Incompatibility(expectSymbol, actualSymbol, actualParam.getSourceElement(), it.type)
            }
                ?: areAnnotationsOnTypeParameterBoundsCompatible(expectSymbol, actualSymbol, expectParam, actualParam)
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsOnTypeParameterBoundsCompatible(
        expectDeclarationSymbol: DeclarationSymbolMarker,
        actualDeclarationSymbol: DeclarationSymbolMarker,
        expectParam: TypeParameterSymbolMarker,
        actualParam: TypeParameterSymbolMarker,
    ): Incompatibility? {
        val expectBounds = expectParam.boundsTypeRefs
        val actualBounds = actualParam.boundsTypeRefs

        return expectBounds.zipIfSizesAreEqual(actualBounds)?.firstNotNullOfOrNull { (expectType, actualType) ->
            // Identifying bounds by ClassId (without type parameters) is enough because type can't have two bounds of same type.
            if (expectType.getClassId() != actualType.getClassId()) {
                // Expect and actual type bounds must be same and have same order, otherwise expect-actual compatibility checker
                // reports error. So, skip in case of incompatibility to not report extra warning.
                return@firstNotNullOfOrNull null
            }
            areAnnotationsSetOnTypesCompatible(expectDeclarationSymbol, actualDeclarationSymbol, expectType, actualType)
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsSetOnTypesCompatible(
        expectDeclarationSymbol: DeclarationSymbolMarker,
        actualDeclarationSymbol: DeclarationSymbolMarker,
        expectTypeRef: TypeRefMarker?,
        actualTypeRef: TypeRefMarker?,
    ): Incompatibility? {
        if (expectTypeRef == null || actualTypeRef == null) return null

        var firstIncompatibility: Incompatibility? = null

        checkAnnotationsOnTypeRefAndArguments(
            expectDeclarationSymbol, actualDeclarationSymbol,
            expectTypeRef, actualTypeRef
        ) { expectAnnotations, actualAnnotations, actualTypeRefSource ->
            if (firstIncompatibility == null) {
                firstIncompatibility = areAnnotationListsCompatible(expectAnnotations, actualAnnotations, actualDeclarationSymbol)
                    ?.let { Incompatibility(expectDeclarationSymbol, actualDeclarationSymbol, actualTypeRefSource, type = it) }
            }
        }
        return firstIncompatibility
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsSetOnDeclarationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        return areAnnotationListsCompatible(expectSymbol.annotations, actualSymbol.annotations, actualSymbol)
            ?.let { Incompatibility(expectSymbol, actualSymbol, actualSymbol.getSourceElement(), it) }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationListsCompatible(
        expectAnnotations: List<ExpectActualMatchingContext.AnnotationCallInfo>,
        actualAnnotations: List<ExpectActualMatchingContext.AnnotationCallInfo>,
        actualContainerSymbol: DeclarationSymbolMarker,
    ): IncompatibilityType<ExpectActualMatchingContext.AnnotationCallInfo>? {
        val skipSourceAnnotations = actualContainerSymbol.hasSourceAnnotationsErased
        val actualAnnotationsByName = actualAnnotations.groupBy { it.classId }

        for (expectAnnotation in expectAnnotations) {
            val expectClassId = expectAnnotation.classId ?: continue
            if (expectClassId in SKIPPED_CLASS_IDS || expectAnnotation.isOptIn) {
                continue
            }
            if (expectAnnotation.isRetentionSource && skipSourceAnnotations) {
                continue
            }
            val actualAnnotationsWithSameClassId = actualAnnotationsByName[expectClassId] ?: emptyList()
            if (actualAnnotationsWithSameClassId.isEmpty()) {
                return IncompatibilityType.MissingOnActual(expectAnnotation)
            }
            val collectionCompatibilityChecker = getAnnotationCollectionArgumentsCompatibilityChecker(expectClassId)
            if (actualAnnotationsWithSameClassId.none {
                    areAnnotationArgumentsEqual(expectAnnotation, it, collectionCompatibilityChecker)
                }) {
                return if (actualAnnotationsWithSameClassId.size == 1) {
                    IncompatibilityType.DifferentOnActual(expectAnnotation, actualAnnotationsWithSameClassId.single())
                } else {
                    // In the case of repeatable annotations, we can't choose on which to report
                    IncompatibilityType.MissingOnActual(expectAnnotation)
                }
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
            )
            val expectMember = expectToCompatibilityMap
                .filter { it.value == ExpectActualMatchingCompatibility.MatchedSuccessfully }.keys.singleOrNull()
            // Check also incompatible members if only one is found
                ?: expectToCompatibilityMap.keys.singleOrNull()
                ?: continue
            areAnnotationsCompatible(expectMember, actualMember, expectClass)?.let { return it }
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