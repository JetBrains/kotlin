/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker

context(ExpectActualMatchingContext<*>)
internal fun getPossibleActualsByExpectName(
    expectMember: DeclarationSymbolMarker,
    actualMembersByName: Map<Name, List<DeclarationSymbolMarker>>,
): List<DeclarationSymbolMarker> {
    val actualMembers = actualMembersByName[expectMember.name]?.filter { actualMember ->
        expectMember is CallableSymbolMarker && actualMember is CallableSymbolMarker ||
                expectMember is RegularClassSymbolMarker && actualMember is RegularClassSymbolMarker
    }.orEmpty()
    return actualMembers
}

context(ExpectActualMatchingContext<*>)
internal val DeclarationSymbolMarker.name: Name
    get() = when (this) {
        is ConstructorSymbolMarker -> SpecialNames.INIT
        is ValueParameterSymbolMarker -> parameterName
        is CallableSymbolMarker -> callableId.callableName
        is RegularClassSymbolMarker -> classId.shortClassName
        is TypeAliasSymbolMarker -> classId.shortClassName
        is TypeParameterSymbolMarker -> parameterName
        else -> error("Unsupported declaration: $this")
    }

context(ExpectActualMatchingContext<*>)
internal fun areCompatibleTypeParameterUpperBounds(
    expectTypeParameterSymbols: List<TypeParameterSymbolMarker>,
    actualTypeParameterSymbols: List<TypeParameterSymbolMarker>,
    substitutor: TypeSubstitutorMarker,
): Boolean {
    for (i in expectTypeParameterSymbols.indices) {
        val expectBounds = expectTypeParameterSymbols[i].bounds
        val actualBounds = actualTypeParameterSymbols[i].bounds
        if (
            expectBounds.size != actualBounds.size ||
            !areCompatibleTypeLists(
                expectBounds.map { substitutor.safeSubstitute(it) },
                actualBounds,
                insideAnnotationClass = false
            )
        ) {
            return false
        }
    }

    return true
}

context(ExpectActualMatchingContext<*>)
internal fun areCompatibleTypeLists(
    expectedTypes: List<KotlinTypeMarker?>,
    actualTypes: List<KotlinTypeMarker?>,
    insideAnnotationClass: Boolean,
): Boolean {
    for (i in expectedTypes.indices) {
        if (!areCompatibleExpectActualTypes(
                expectedTypes[i], actualTypes[i], parameterOfAnnotationComparisonMode = insideAnnotationClass
            )
        ) {
            return false
        }
    }
    return true
}

/**
 * In terms of KMP, there is no such thing as `expect constructor` for enums,
 * but they are physically exist in FIR and IR, so we need to skip matching and checking for them
 */
context(ExpectActualMatchingContext<*>)
internal fun areEnumConstructors(
    expectDeclaration: CallableSymbolMarker,
    actualDeclaration: CallableSymbolMarker,
    expectContainingClass: RegularClassSymbolMarker?,
    actualContainingClass: RegularClassSymbolMarker?,
): Boolean = expectContainingClass?.classKind == ClassKind.ENUM_CLASS &&
        actualContainingClass?.classKind == ClassKind.ENUM_CLASS &&
        expectDeclaration is ConstructorSymbolMarker &&
        actualDeclaration is ConstructorSymbolMarker

context(ExpectActualMatchingContext<*>)
internal fun checkCallablesInvariants(
    expectDeclaration: CallableSymbolMarker,
    actualDeclaration: CallableSymbolMarker,
) {
    require(
        (expectDeclaration is ConstructorSymbolMarker && actualDeclaration is ConstructorSymbolMarker) ||
                expectDeclaration.callableId.callableName == actualDeclaration.callableId.callableName
    ) {
        "This function should be invoked only for declarations with the same name: $expectDeclaration, $actualDeclaration"
    }
    require((expectDeclaration.dispatchReceiverType == null) == (actualDeclaration.dispatchReceiverType == null)) {
        "This function should be invoked only for declarations in the same kind of container (both members or both top level): $expectDeclaration, $actualDeclaration"
    }
}
